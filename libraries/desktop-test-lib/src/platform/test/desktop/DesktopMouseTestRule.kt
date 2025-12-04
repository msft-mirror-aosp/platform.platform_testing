/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.desktop

import android.Manifest
import android.companion.virtual.VirtualDeviceManager
import android.companion.virtual.VirtualDeviceParams
import android.graphics.PointF
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopology.dpToPx
import android.hardware.display.DisplayTopologyGraph
import android.hardware.input.InputManager
import android.hardware.input.VirtualMouse
import android.hardware.input.VirtualMouseButtonEvent
import android.hardware.input.VirtualMouseButtonEvent.ACTION_BUTTON_PRESS
import android.hardware.input.VirtualMouseButtonEvent.ACTION_BUTTON_RELEASE
import android.hardware.input.VirtualMouseButtonEvent.BUTTON_PRIMARY
import android.hardware.input.VirtualMouseConfig
import android.hardware.input.VirtualMouseRelativeEvent
import android.os.Handler
import android.os.Looper
import android.platform.uiautomatorhelpers.ShellPrivilege
import android.platform.uiautomatorhelpers.WaitUtils
import android.util.Log
import android.view.Display.DEFAULT_DISPLAY
import android.view.DisplayInfo
import androidx.annotation.VisibleForTesting
import androidx.core.util.isNotEmpty
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeNotNull
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.desktop.LogicalPhysicalDisplayTransformHelper.Companion.getPointPhysicalPx
import platform.test.desktop.LogicalPhysicalDisplayTransformHelper.Companion.minus
import platform.test.desktop.LogicalPhysicalDisplayTransformHelper.DeltaLogicalPxF
import platform.test.desktop.LogicalPhysicalDisplayTransformHelper.DeltaPhysicalPxF

/**
 * A [TestRule] to support [VirtualMouse] move and drag within a single display / crossing across
 * displays.
 *
 * If [deferSetup] is set to true, please call [setupMouse] before calling any move method
 */
class DesktopMouseTestRule(private val deferSetup: Boolean = false) : TestRule {
    private val fakeAssociationRule = FakeAssociationRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation()
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val inputManager = context.getSystemService(InputManager::class.java)
    private val resourceTracker = ResourceTracker()
    private val ruleChain = RuleChain.outerRule(fakeAssociationRule).around(resourceTracker)

    override fun apply(base: Statement, description: Description) =
        ruleChain.apply(base, description)

    /**
     * Internal test rule that tracks created virtual mouse and modified display topology and
     * ensures they are properly closed / restored after the test.
     */
    private inner class ResourceTracker() : ExternalResource() {
        private val virtualDeviceManager =
            context.getSystemService(VirtualDeviceManager::class.java)

        // TODO: b/392534769 - Refactor this to use UinputMouse.
        private lateinit var virtualDevice: VirtualDeviceManager.VirtualDevice
        private var virtualMouse: VirtualMouse? = null
        private val displayIdsWithMouseScalingDisabled = mutableListOf<Int>()
        private val handler = Handler(Looper.getMainLooper())
        private val displayListener =
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {
                    disableMouseScaling(displayId)
                }

                override fun onDisplayRemoved(displayId: Int) {}

                override fun onDisplayChanged(displayId: Int) {}
            }

        val requireVirtualMouse: VirtualMouse
            get() = checkNotNull(virtualMouse) { "Failed to initialize VirtualMouse" }

        /**
         * Sets up a virtual mouse that will start on any Display inside [DisplayTopology].
         *
         * Note: If the mouse needs to start at different display, first call
         * [DesktopMouseTestRule.move].
         */
        override fun before() = runBlocking {
            assumeNotNull(virtualDeviceManager)
            if (!deferSetup) {
                setup()
            }
        }

        fun setup() = runBlocking {
            if (::virtualDevice.isInitialized) {
                Log.w(TAG, "setup() called more than once, ignoring")
                return@runBlocking
            }
            ShellPrivilege(uiAutomation, Manifest.permission.CREATE_VIRTUAL_DEVICE).use {
                virtualDevice =
                    virtualDeviceManager.createVirtualDevice(
                        fakeAssociationRule.associationInfo.id,
                        VirtualDeviceParams.Builder().build(),
                    )
            }

            Log.i(
                TAG,
                "Starting with a DisplayTopology state of\n${displayManager.displayTopology}",
            )
            val startDisplayId = getDisplayIdIncludedInDisplayTopology()
            Log.i(TAG, "Attaching mouse to display#$startDisplayId")
            val inputDeviceFlow = callbackFlow {
                val inputDeviceListener =
                    object : InputManager.InputDeviceListener {
                        override fun onInputDeviceAdded(deviceId: Int) {
                            val device = inputManager.getInputDevice(deviceId) ?: return
                            if (
                                device.vendorId == VIRTUAL_MOUSE_VENDOR_ID &&
                                    device.productId == VIRTUAL_MOUSE_PRODUCT_ID
                            ) {
                                trySend(deviceId)
                                close()
                            }
                        }

                        override fun onInputDeviceRemoved(deviceId: Int) {}

                        override fun onInputDeviceChanged(deviceId: Int) {}
                    }
                inputManager.registerInputDeviceListener(inputDeviceListener, handler)

                ShellPrivilege(uiAutomation, Manifest.permission.INJECT_EVENTS).use {
                    virtualMouse =
                        virtualDevice.createVirtualMouse(
                            VirtualMouseConfig.Builder()
                                .setVendorId(VIRTUAL_MOUSE_VENDOR_ID)
                                .setProductId(VIRTUAL_MOUSE_PRODUCT_ID)
                                .setInputDeviceName("VirtualMouse_ConnectedDisplaysTest")
                                .setAssociatedDisplayId(startDisplayId)
                                .build()
                        )
                }
                awaitClose { inputManager.unregisterInputDeviceListener(inputDeviceListener) }
            }

            withTimeoutOrNull(TIMEOUT) { inputDeviceFlow.first() }
                ?: error("Timed out waiting for input device to be added.")

            for (display in displayManager.displays) {
                disableMouseScaling(display.displayId)
            }
            displayManager.registerDisplayListener(displayListener, handler)
            ensureCursorStartsInDisplayTopology(startDisplayId)
        }

        private fun disableMouseScaling(displayId: Int) {
            displayIdsWithMouseScalingDisabled += displayId
            ShellPrivilege(uiAutomation, Manifest.permission.SET_POINTER_SPEED).use {
                inputManager.setMouseScalingEnabled(false, displayId)
            }
        }

        private fun ensureCursorStartsInDisplayTopology(displayId: Int) {
            val display = displayManager.getDisplay(displayId)
            Log.i(TAG, "Ensuring cursor starts on center of display#$displayId")
            move(LogicalDisplayPointPx(display.displayId, display.width / 2, display.height / 2))
        }

        private fun getDisplayIdIncludedInDisplayTopology(): Int {
            val idBoundsMap = displayManager.displayTopology?.absoluteBounds
            if (
                idBoundsMap != null && idBoundsMap.isNotEmpty() && DEFAULT_DISPLAY !in idBoundsMap
            ) {
                return idBoundsMap.keyAt(0)
            }
            return DEFAULT_DISPLAY
        }

        override fun after() {
            displayManager.unregisterDisplayListener(displayListener)
            for (displayId in displayIdsWithMouseScalingDisabled) {
                try {
                    inputManager.setMouseScalingEnabled(true, displayId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore mouse scaling for display#$displayId", e)
                }
            }
            if (::virtualDevice.isInitialized) {
                virtualMouse?.close()
                virtualDevice.close()
            }
            super.after()
        }
    }

    fun setupMouse() = runBlocking { resourceTracker.setup() }

    fun startDrag() {
        resourceTracker.requireVirtualMouse.sendButtonEvent(
            VirtualMouseButtonEvent.Builder()
                .setAction(ACTION_BUTTON_PRESS)
                .setButtonCode(BUTTON_PRIMARY)
                .build()
        )
        Thread.sleep(MOUSE_INPUT_DELAY.inWholeMilliseconds)
    }

    fun stopDrag() {
        resourceTracker.requireVirtualMouse.sendButtonEvent(
            VirtualMouseButtonEvent.Builder()
                .setAction(ACTION_BUTTON_RELEASE)
                .setButtonCode(BUTTON_PRIMARY)
                .build()
        )
        Thread.sleep(MOUSE_INPUT_DELAY.inWholeMilliseconds)
    }

    /**
     * Performs a mouse click of the specified [button], or a primary-button click by default, at
     * the current cursor location.
     */
    fun click(@VirtualMouseButtonEvent.Button button: Int = BUTTON_PRIMARY) {
        resourceTracker.requireVirtualMouse.sendButtonEvent(
            VirtualMouseButtonEvent.Builder()
                .setAction(ACTION_BUTTON_PRESS)
                .setButtonCode(button)
                .build()
        )

        resourceTracker.requireVirtualMouse.sendButtonEvent(
            VirtualMouseButtonEvent.Builder()
                .setAction(ACTION_BUTTON_RELEASE)
                .setButtonCode(button)
                .build()
        )
    }

    /**
     * Moves the mouse cursor to [target]. If the target display is different from the current
     * display, it finds a path and moves the cursor across display(s).
     *
     * NOTE: While InputManager APIs are using PointF for both get/set, the underlying
     * implementation is actually using Int. For example, evdev injection only supports Int.
     * Therefore, expect delta < 1f difference between the target and actual cursor position.
     *
     * NOTE: This method blocks the thread to wait for the cursor position to be expected. Do not
     * call this in the main thread.
     *
     * @param target The destination point, which includes the target display ID and coordinates.
     */
    fun move(target: LogicalDisplayPointPx) {
        Log.i(TAG, "Try moving to $target")
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "This method should not be called on the main thread"
        }

        val targetDisplayId = target.displayId
        val displayTransform = LogicalPhysicalDisplayTransformHelper(uiAutomation, displayManager)
        val currentCursorDisplayId = getCursorDisplayId()

        if (targetDisplayId != currentCursorDisplayId) {
            Log.i(
                TAG,
                "Start moving from display#$currentCursorDisplayId -> display#$targetDisplayId",
            )
            moveToDisplay(currentCursorDisplayId, targetDisplayId, displayTransform)
        } else {
            Log.i(TAG, "Cursor is already on the same display with $targetDisplayId")
        }

        val currentPosition = getCursorPosition(targetDisplayId)
        performSteppedMove(
            target.getPointPhysicalPx(displayTransform) -
                currentPosition.getPointPhysicalPx(displayTransform)
        )

        val displayScale = displayTransform.getScale(targetDisplayId)
        WaitUtils.ensureThat(
            errorProvider = {
                val displayId = getCursorDisplayId()
                "Failed to move cursor from: $currentPosition to: $target.\n" +
                    "Current pos: ${getCursorPosition(displayId)}.\n" +
                    "Display scale: $displayScale"
            }
        ) {
            val finalPosition = getCursorPosition(targetDisplayId)
            val dx = abs(finalPosition.getPointF().x - target.getPointF().x)
            val dy = abs(finalPosition.getPointF().y - target.getPointF().y)
            // As mentioned in the javadoc above, InputManager API doesn't support floating-point
            // movements. Hence, with all the floating-point calculation above, there might be
            // slight difference (within `FLOATING_ROUND_CORRECTION`) in the final cursor position.
            // On top of that, there's also display scale difference that might cause the move to be
            // inaccurate within the range of `displayScale`
            dx < max(FLOATING_ROUNDING_CORRECTION, displayScale.scaleX) &&
                dy < max(FLOATING_ROUNDING_CORRECTION, displayScale.scaleY)
        }
        Log.i(
            TAG,
            "Successfully moved to display#$targetDisplayId ${getCursorPosition(targetDisplayId)}",
        )
    }

    /**
     * Requests to move the mouse cursor by the specified delta. Different from the [move] method,
     * this does not consider where the current mouse cursor is, and does not ensure that the cursor
     * will move to any target position.
     *
     * @param dxPx The delta X (PX) coordinate.
     * @param dyPx The delta Y (PX) coordinate.
     */
    fun moveDelta(dxPx: Int, dyPx: Int) {
        val displayTransform = LogicalPhysicalDisplayTransformHelper(uiAutomation, displayManager)
        performSteppedMove(
            DeltaLogicalPxF(dxPx.toFloat(), dyPx.toFloat())
                .toPhysicalPx(getCursorDisplayId(), displayTransform)
        )
    }

    /**
     * Besides density scaling (dp <-> px) happening in WM side, there's also logical <-> physical
     * PX scaling happening in Input and SurfaceFlinger side.
     *
     * When logical-physical display scale is > 1.0, input move might be no-op for being too small.
     * For example, physical display = 500x1000, logical display = 1000x2000, scale = 2.0. In this
     * case, a 1px move in logical display, would be translated to 0.5px move in physical display.
     * Which seems to be fine as we should just send a 0.5px move.
     *
     * However, we have to round the delta because Linux evdev supports only integer values for
     * REL_X/Y. This means a 0.5px delta becomes 0px, resulting in a no-op.
     *
     * Therefore, this function serves as additional helper method if testRule users want to ensure
     * that the move will be executed, by checking if delta in LogicalPx >= [getMouseMinMovePx]
     */
    fun getMouseMinMovePx(displayId: Int): DeltaLogicalPxF {
        val displayScale =
            LogicalPhysicalDisplayTransformHelper(uiAutomation, displayManager).getScale(displayId)
        return DeltaLogicalPxF(displayScale.scaleX, displayScale.scaleY)
    }

    private fun moveToDisplay(
        startingDisplayId: Int,
        targetDisplayId: Int,
        displayTransform: LogicalPhysicalDisplayTransformHelper,
    ) {
        var currentCursorDisplayId = startingDisplayId
        val topology =
            checkNotNull(displayManager.displayTopology) { "DisplayTopology must be available." }
        val displayAbsoluteBounds = topology.absoluteBounds
        val topologyGraph = topology.graph
        val path = findPath(currentCursorDisplayId, targetDisplayId, topologyGraph)
        Log.i(TAG, "Computed display paths ${path.stream().map{it.displayId}.toList()}")

        path.forEach { (nextDisplayId, position) ->
            val dpi = getDpiForDisplay(currentCursorDisplayId)
            val currentBounds = displayAbsoluteBounds[currentCursorDisplayId]
            val nextBounds = displayAbsoluteBounds[nextDisplayId]

            // Calculate where to cross and the crossing delta
            val crossingDetail = calculateCrossingDetailsDp(currentBounds, nextBounds, position)
            // Cursor moves in PX, however there's no notion of global PX bounds since density
            // of each displays could be different, and there's only global DP bounds.
            // Therefore, to solve the calculation, first convert globalDP -> localDP, then
            // apply DP->PX conversion.
            val edgeIntersectionPx =
                LogicalDisplayPointPx(
                    currentCursorDisplayId,
                    dpToPx(crossingDetail.targetPointDp.x - currentBounds.left, dpi),
                    dpToPx(crossingDetail.targetPointDp.y - currentBounds.top, dpi),
                )
            val crossingDelta =
                DeltaLogicalPxF(
                    dpToPx(crossingDetail.toCrossDxDp, dpi),
                    dpToPx(crossingDetail.toCrossDyDp, dpi),
                )

            val currentPosition = getCursorPosition(currentCursorDisplayId)
            // Move to the center of the edge intersection (border between displays).
            performSteppedMove(
                edgeIntersectionPx.getPointPhysicalPx(displayTransform) -
                    currentPosition.getPointPhysicalPx(displayTransform)
            )

            // Perform a small move to cross the boundary
            performSteppedMove(crossingDelta.toPhysicalPx(currentCursorDisplayId, displayTransform))
            // Validate cursor crossed display
            WaitUtils.ensureThat(
                errorProvider = {
                    val currentDisplayId = getCursorDisplayId()
                    "Failed to move cursor from " +
                        "display#$currentCursorDisplayId -> display#$nextDisplayId. " +
                        "Cursor is still at " +
                        "display#$currentDisplayId ${getCursorPosition(currentDisplayId)}"
                }
            ) {
                getCursorDisplayId() == nextDisplayId
            }
            Log.i(
                TAG,
                "Cursor moved from display#$currentCursorDisplayId -> display#$nextDisplayId",
            )
            currentCursorDisplayId = nextDisplayId

            // InputDevice reconfiguration will happen when cursor changed display, and might jam
            // the input queue. Waiting for input transactions to finish ensure any subsequent
            // `getCursorPosition()` calls return accurate position
            uiAutomation.syncInputTransactions()
        }
    }

    /**
     * Divides delta to multiple small movements
     *
     * @param deltaPx delta movement in physical display PX, either dx or dy must be non-zero
     * @param maxSteps the maximum number of times move events would be sent
     */
    private fun performSteppedMove(
        deltaPx: DeltaPhysicalPxF,
        maxSteps: Int = MAX_MOUSE_MOVE_STEPS_COUNT,
    ) {
        // Find ideal number of steps to move a number of PX
        val idealSteps = max(abs(deltaPx.dx), abs(deltaPx.dy)) / MIN_PX_PER_STEP

        // Limit the number of steps while ensuring it's not zero
        val steps = max(1, min(maxSteps, idealSteps.toInt()))
        val stepX = (deltaPx.dx / steps).toInt()
        val stepY = (deltaPx.dy / steps).toInt()
        repeat(steps) { moveInternal(stepX, stepY) }

        // Move any remaining delta
        val remainingDx = deltaPx.dx - (stepX * steps)
        val remainingDy = deltaPx.dy - (stepY * steps)
        moveInternal(remainingDx.roundToInt(), remainingDy.roundToInt())
    }

    private fun moveInternal(dx: Int, dy: Int) {
        if (dx == 0 && dy == 0) return
        resourceTracker.requireVirtualMouse.sendRelativeEvent(
            VirtualMouseRelativeEvent.Builder()
                .setRelativeX(dx.toFloat())
                .setRelativeY(dy.toFloat())
                .build()
        )
        Thread.sleep(MOUSE_INPUT_DELAY.inWholeMilliseconds)
    }

    private fun getCursorPosition(displayId: Int): LogicalDisplayPointPx {
        val position =
            checkNotNull(inputManager.getCursorPosition(displayId)) {
                "Cursor is not on display#$displayId"
            }
        return LogicalDisplayPointPx(displayId, position.x, position.y)
    }

    private fun getCursorDisplayId(): Int {
        // Query cursor position on all displays and find the one with non-null values
        // This is a hack since getCursorPosition API, doesn't directly provide the displayId it's
        // currently at.
        for (display in displayManager.displays) {
            val cursorPosition = inputManager.getCursorPosition(display.displayId)
            if (cursorPosition != null) {
                return display.displayId
            }
        }
        throw NoCursorFoundException("Cursor doesn't exist on any display")
    }

    private fun getDpiForDisplay(displayId: Int): Int {
        val display =
            checkNotNull(displayManager.getDisplay(displayId)) { "Display#$displayId not found" }
        val displayInfo = DisplayInfo()
        display.getDisplayInfo(displayInfo)
        return displayInfo.logicalDensityDpi
    }

    /**
     * Representing details on how to cross from one display to another based on their adjacency
     * relation.
     *
     * @property targetPointDp The center of an intersection edge of 2 adjacent displays. This will
     *   be the target (x, y) to move to, wherever the current cursor is.
     * @property toCrossDeltaDp The small (dx, dy) required to cross display boundary when cursor is
     *   already on the `targetPointDp.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    data class DisplayCrossingDetail(
        val targetPointDp: PointF,
        private val toCrossDeltaDp: DeltaDpF,
    ) {

        // Sample explanation
        //
        //   +-------------------------------+
        //   |           Display 2           |
        //   |                               |
        //   +=========*****X*****===========+
        //             R    |    R Bottom of D2 / Top of D1
        //              +-------+
        //              |Display|
        //              |   1   |
        //              |       |
        //              |       |
        //              +-------+
        //  R = range of the intersection
        //  X = center of the intersection (`targetPointDp`)

        val toCrossDxDp: Float
            get() = toCrossDeltaDp.dx

        val toCrossDyDp: Float
            get() = toCrossDeltaDp.dy
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    data class AdjacentDisplay(val displayId: Int, val position: Position) {
        // Side of the other display which touches this display.
        enum class Position {
            LEFT,
            RIGHT,
            TOP,
            BOTTOM;

            companion object {
                fun from(value: Int): Position {
                    return when (value) {
                        DisplayTopology.POSITION_LEFT -> LEFT
                        DisplayTopology.POSITION_TOP -> TOP
                        DisplayTopology.POSITION_RIGHT -> RIGHT
                        DisplayTopology.POSITION_BOTTOM -> BOTTOM
                        else ->
                            throw IllegalArgumentException(
                                "Invalid integer value for Position: $value"
                            )
                    }
                }
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    data class DeltaDpF(val dx: Float, val dy: Float)

    class NoCursorFoundException(message: String) : Exception(message)

    class NoPathFoundException(message: String) : Exception(message)

    companion object {

        /**
         * Finds a path of adjacent display IDs from `startId` to `endId` using BFS.
         *
         * Returned path doesn't include the start node. If `startId` == `endId`, then empty list
         * will be returned
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun findPath(
            startId: Int,
            endId: Int,
            topologyGraph: DisplayTopologyGraph,
        ): List<AdjacentDisplay> {
            if (startId == endId) return listOf()
            val adjacencyGraph = topologyGraph.displayNodes
            val queue = ArrayDeque<Int>().apply { add(startId) }
            val visited = mutableSetOf(startId)
            // Maps display id to its parent in the context of `startId`->`endIf` traversal.
            val parentMap = mutableMapOf<Int, AdjacentDisplay>()

            while (queue.isNotEmpty()) {
                val currentId = queue.removeFirst()
                if (currentId == endId) {
                    // Path found, reconstruct path
                    val path = ArrayDeque<AdjacentDisplay>()
                    var backtrackId = endId
                    while (backtrackId != startId) {
                        val parentNode =
                            parentMap[backtrackId]
                                ?: throw IllegalStateException(
                                    "Path should exist since start node can be reached"
                                )
                        path.addFirst(AdjacentDisplay(backtrackId, parentNode.position))
                        backtrackId = parentNode.displayId
                    }
                    return path
                }

                val currentNode = adjacencyGraph.get(currentId) ?: continue
                // Check neighbors
                for (adjacentEdge in currentNode.adjacentEdges) {
                    val neighborId = adjacentEdge.displayNode.displayId
                    val position = adjacentEdge.position
                    if (neighborId in visited) continue
                    visited.add(neighborId)
                    parentMap[neighborId] =
                        AdjacentDisplay(currentId, AdjacentDisplay.Position.from(position))
                    queue.add(neighborId)
                }
            }
            throw NoPathFoundException("No path found from $startId to $endId")
        }

        /**
         * Calculates crossing details (intersection-midpoint, crossing-delta) in DP, between
         * adjacent bounds.
         *
         * @see [DisplayCrossingDetail] for details
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun calculateCrossingDetailsDp(
            source: RectF,
            target: RectF,
            position: AdjacentDisplay.Position,
        ): DisplayCrossingDetail {
            val overlapTop = max(source.top, target.top)
            val overlapBottom = min(source.bottom, target.bottom)
            val overlapLeft = max(source.left, target.left)
            val overlapRight = min(source.right, target.right)
            val offset = MOUSE_CROSS_DISPLAY_OFFSET_DP

            return when (position) {
                AdjacentDisplay.Position.RIGHT -> // target is to the right of source
                DisplayCrossingDetail(
                        PointF(source.right, (overlapTop + overlapBottom) / 2f),
                        DeltaDpF(offset, 0f),
                    )
                AdjacentDisplay.Position.LEFT -> // target is to the left of source
                DisplayCrossingDetail(
                        PointF(source.left, (overlapTop + overlapBottom) / 2f),
                        DeltaDpF(-offset, 0f),
                    )
                AdjacentDisplay.Position.BOTTOM -> // target is below source
                DisplayCrossingDetail(
                        PointF((overlapLeft + overlapRight) / 2f, source.bottom),
                        DeltaDpF(0f, offset),
                    )
                AdjacentDisplay.Position.TOP -> // target is above source
                DisplayCrossingDetail(
                        PointF((overlapLeft + overlapRight) / 2f, source.top),
                        DeltaDpF(0f, -offset),
                    )
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MOUSE_CROSS_DISPLAY_OFFSET_DP = 5.0f
        private const val FLOATING_ROUNDING_CORRECTION = 1f
        private const val MIN_PX_PER_STEP = 1
        private const val MAX_MOUSE_MOVE_STEPS_COUNT = 20
        private const val VIRTUAL_MOUSE_VENDOR_ID = 123
        private const val VIRTUAL_MOUSE_PRODUCT_ID = 456
        private const val TAG = "DesktopMouseTestRule"

        // Mimics UiAutomator delay for injecting MotionEvent
        private val MOUSE_INPUT_DELAY = 5.milliseconds
        private val TIMEOUT: Duration = 10.seconds
    }
}
