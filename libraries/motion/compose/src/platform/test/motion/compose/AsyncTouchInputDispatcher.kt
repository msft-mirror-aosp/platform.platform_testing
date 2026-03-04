/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.motion.compose

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCHSCREEN
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Runs [gestureControl] in context of [onSemanticsNode] and schedules the [MotionEvent]s for the
 * gesture.
 *
 * This function returns after the last event belonging to the gesture has been dispatched. The
 * virtual clock has to be advanced separately.
 *
 * Same as with the regular Compose `performTouchInput`, the MotionEvents are pre-calculated
 * initially, and cannot react to changes in layout.
 *
 * WARNING: This is a short-term hack around Compose API limitations until the API improvements
 * land. The code here is a slimmed down fork of AndroidInputDispatcher.android.kt and related
 * interfaces. No in-depth tests cover this, since its mostly copy-pasted. New API to allow this
 * type of test is expected to land in Compose by early 2026Q2, at which point this workaround has
 * to be removed.
 *
 * @param onSemanticsNode The node that provides the size/position to which events are relative.
 * @param gestureControl The lambda describing the touch gesture
 * @param root the view to which the events are dispatched
 * @param frameOffsetMillis the offset to the actual frame rhythm, as in `n * frameDurationMillis +
 *   frameOffsetMillis`. must be in range of `0..15`.
 */
internal suspend fun ComposeTestRule.doPerformTouchInputAsync(
    onSemanticsNode: SemanticsNode,
    gestureControl: TouchInjectionScope.() -> Unit,
    root: ViewRootForTest,
    frameOffsetMillis: Long,
) {
    require(frameOffsetMillis in 0..<frameDurationMillis) {
        "frameTimeOffsetMillis out of bounds ($frameOffsetMillis)"
    }

    val batchedEvents =
        with(
            AsyncTouchInputDispatcher(mainClock.currentTime, semanticsNode = onSemanticsNode, root)
        ) {
            gestureControl()
            batchedEvents
        }

    for (event in batchedEvents) {
        val nextFrameTime =
            ((event.eventTime - frameOffsetMillis + frameDurationMillis) / frameDurationMillis) *
                frameDurationMillis + frameOffsetMillis

        // Dispatch the motion event exactly 1ms before the next frame. This is done to simulate
        // the "Drain pending work before withFrameNanos":
        delay(nextFrameTime - mainClock.currentTime - 1)
        runOnUiThread {
            try {
                root.view.dispatchTouchEvent(event)
            } finally {
                event.recycle()
            }
        }
    }
}

/**
 * The state of the current gesture. Contains the current position of all pointers and the down time
 * (start time) of the gesture. For the current time, see [InputDispatcher.currentTime].
 *
 * @param downTime The time of the first down event of this gesture
 * @param startPosition The position of the first down event of this gesture
 * @param pointerId The pointer id of the first down event of this gesture
 */
private class PartialGesture(val downTime: Long, startPosition: Offset, pointerId: Int) {
    val lastPositions = mutableMapOf(Pair(pointerId, startPosition))
    var hasPointerUpdates: Boolean = false
}

/**
 * Crude fork of AndroidInputDispatcher.android.kt and related classes, with only the touch handling
 * kept in place.
 */
private class AsyncTouchInputDispatcher(
    var nextEventTime: Long,
    private val semanticsNode: SemanticsNode,
    private val root: ViewRootForTest,
) : TouchInjectionScope, Density by semanticsNode.layoutInfo.density {

    val batchedEvents = mutableListOf<MotionEvent>()

    /**
     * Returns and stores the visible bounds of the [semanticsNode] we're interacting with. This
     * applies clipping, which is almost always the correct thing to do when injecting gestures, as
     * gestures operate on visible UI.
     */
    private val boundsInRoot: Rect by lazy { semanticsNode.boundsInRoot }

    /**
     * Returns the size of the visible part of the node we're interacting with. This is contrary to
     * [SemanticsNode.size], which returns the unclipped size of the node.
     */
    override val visibleSize: IntSize by lazy {
        IntSize(boundsInRoot.width.roundToInt(), boundsInRoot.height.roundToInt())
    }

    /**
     * Transforms the [position] to root coordinates.
     *
     * @param position A position in local coordinates
     * @return [position] transformed to coordinates relative to the containing root.
     */
    internal fun localToRoot(position: Offset): Offset {
        return if (position.isValid()) {
            position + boundsInRoot.topLeft
        } else {
            // Allows invalid position to still pass back through Compose (for testing)
            position
        }
    }

    internal fun rootToLocal(position: Offset): Offset {
        return if (position.isValid()) {
            position - boundsInRoot.topLeft
        } else {
            // Allows invalid position to still pass back through Compose (for testing)
            position
        }
    }

    override val viewConfiguration: ViewConfiguration
        get() = semanticsNode.layoutInfo.viewConfiguration

    /**
     * Increases the current event time by [durationMillis].
     *
     * Depending on the [keyInputState], there may be repeat key events that need to be sent within
     * the given duration. If there are, the clock will be forwarded until it is time for the repeat
     * key event, the key event will be sent, and then the clock will be forwarded by the remaining
     * duration.
     *
     * @param durationMillis The duration of the delay. Must be positive
     */
    override fun advanceEventTime(durationMillis: Long) {
        require(durationMillis >= 0) {
            "duration of a delay can only be positive, not $durationMillis"
        }

        val endTime = nextEventTime + durationMillis
        nextEventTime = endTime
    }

    /** The state of the current touch gesture. If `null`, no touch gesture is in progress. */
    protected var partialGesture: PartialGesture? = null

    /**
     * During a touch gesture, returns the position of the last touch event of the given
     * [pointerId]. Returns `null` if no touch gesture is in progress for that [pointerId].
     *
     * @param pointerId The id of the pointer for which to return the current position
     * @return The current position of the pointer with the given [pointerId], or `null` if the
     *   pointer is not currently in use
     */
    fun getCurrentTouchPosition(pointerId: Int): Offset? {
        return partialGesture?.lastPositions?.get(pointerId)
    }

    override fun currentPosition(pointerId: Int): Offset? {
        val positionInRoot = getCurrentTouchPosition(pointerId) ?: return null
        return rootToLocal(positionInRoot)
    }

    override fun down(pointerId: Int, position: Offset) {
        val positionInRoot = localToRoot(position)
        this.enqueueTouchDown(pointerId, positionInRoot)
    }

    override fun updatePointerTo(pointerId: Int, position: Offset) {
        val positionInRoot = localToRoot(position)
        this.updateTouchPointer(pointerId, positionInRoot)
    }

    override fun move(delayMillis: Long) {
        advanceEventTime(delayMillis)
        this.enqueueTouchMove()
    }

    @ExperimentalTestApi
    override fun moveWithHistoryMultiPointer(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
        delayMillis: Long,
    ) {
        repeat(relativeHistoricalTimes.size) {
            check(relativeHistoricalTimes[it] < 0) {
                "Relative historical times should be negative, in order to be in the past" +
                    "(offset $it was: ${relativeHistoricalTimes[it]})"
            }
            check(relativeHistoricalTimes[it] >= -delayMillis) {
                "Relative historical times should not be earlier than the previous event " +
                    "(offset $it was: ${relativeHistoricalTimes[it]}, ${-delayMillis})"
            }
        }

        advanceEventTime(delayMillis)
        this.enqueueTouchMoves(relativeHistoricalTimes, historicalCoordinates)
    }

    override fun up(pointerId: Int) {
        this.enqueueTouchUp(pointerId)
    }

    override fun cancel(delayMillis: Long) {
        advanceEventTime(delayMillis)
        this.enqueueTouchCancel()
    }

    /**
     * Generates a down touch event at [position] for the pointer with the given [pointerId]. Starts
     * a new touch gesture if no other [pointerId]s are down. Only possible if the [pointerId] is
     * not currently being used, although pointer ids may be reused during a touch gesture.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The coordinate of the down event
     * @see enqueueTouchMove
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun enqueueTouchDown(pointerId: Int, position: Offset) {
        var gesture = partialGesture

        // Check if this pointer is not already down
        require(gesture == null || !gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send DOWN event, a gesture is already in progress for pointer $pointerId"
        }

        // Send a MOVE event if pointers have changed since the last event
        gesture?.flushPointerUpdates()

        // Start a new gesture, or add the pointerId to the existing gesture
        if (gesture == null) {
            gesture = PartialGesture(nextEventTime, position, pointerId)
            partialGesture = gesture
        } else {
            gesture.lastPositions[pointerId] = position
        }

        // Send the DOWN event
        gesture.enqueueDown(pointerId)
    }

    /**
     * Generates a move touch event without moving any of the pointers. Use this to commit all
     * changes in pointer location made with [updateTouchPointer]. The generated event will contain
     * the current position of all pointers.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     * @see enqueueTouchMoves
     */
    fun enqueueTouchMove() {
        val gesture =
            checkNotNull(partialGesture) { "Cannot send MOVE event, no gesture is in progress" }
        gesture.enqueueMove()
        gesture.hasPointerUpdates = false
    }

    /**
     * Enqueue the current time+coordinates as a move event, with the historical parameters
     * preceding it (so that they are ultimately available from methods like
     * MotionEvent.getHistoricalX).
     *
     * @see enqueueTouchMove
     * @see TouchInjectionScope.moveWithHistory
     */
    fun enqueueTouchMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
    ) {
        val gesture =
            checkNotNull(partialGesture) { "Cannot send MOVE event, no gesture is in progress" }
        gesture.enqueueMoves(relativeHistoricalTimes, historicalCoordinates)
        gesture.hasPointerUpdates = false
    }

    /**
     * Updates the position of the touch pointer with the given [pointerId] to the given [position],
     * but does not generate a move touch event. Use this to move multiple pointers simultaneously.
     * To generate the next move touch event, which will contain the current position of _all_
     * pointers (not just the moved ones), call [enqueueTouchMove]. If you move one or more pointers
     * and then call [enqueueTouchDown], without calling [enqueueTouchMove] first, a move event will
     * be generated right before that down event.
     *
     * @param pointerId The id of the pointer to move, as supplied in [enqueueTouchDown]
     * @param position The position to move the pointer to
     * @see enqueueTouchDown
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     * @see enqueueTouchCancel
     */
    fun updateTouchPointer(pointerId: Int, position: Offset) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) { "Cannot move pointers, no gesture is in progress" }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot move pointer $pointerId, it is not active in the current gesture"
        }

        gesture.lastPositions[pointerId] = position
        gesture.hasPointerUpdates = true
    }

    /**
     * Generates an up touch event for the given [pointerId] at the current position of that
     * pointer.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [enqueueTouchDown]
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchCancel
     */
    fun enqueueTouchUp(pointerId: Int) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) { "Cannot send UP event, no gesture is in progress" }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send UP event for pointer $pointerId, it is not active in the current gesture"
        }

        // First send the UP event
        gesture.enqueueUp(pointerId)

        // Then remove the pointer, and end the gesture if no pointers are left
        gesture.lastPositions.remove(pointerId)
        if (gesture.lastPositions.isEmpty()) {
            partialGesture = null
        }
    }

    /**
     * Generates a cancel touch event for the current touch gesture. Sent automatically when mouse
     * events are sent while a touch gesture is in progress.
     *
     * @see enqueueTouchDown
     * @see updateTouchPointer
     * @see enqueueTouchMove
     * @see enqueueTouchUp
     */
    fun enqueueTouchCancel() {
        val gesture =
            checkNotNull(partialGesture) { "Cannot send CANCEL event, no gesture is in progress" }
        gesture.enqueueCancel()
        partialGesture = null
    }

    /**
     * Generates a move event with all pointer locations, if any of the pointers has been moved by
     * [updateTouchPointer] since the last move event.
     */
    private fun PartialGesture.flushPointerUpdates() {
        if (hasPointerUpdates) {
            enqueueTouchMove()
        }
    }

    fun PartialGesture.enqueueDown(pointerId: Int) {
        enqueueTouchEvent(
            if (lastPositions.size == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            lastPositions.keys.sorted().indexOf(pointerId),
        )
    }

    fun PartialGesture.enqueueMove() {
        enqueueTouchEvent(ACTION_MOVE, 0)
    }

    fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
    ) {
        val entries = lastPositions.entries.sortedBy { it.key }
        val absoluteHistoricalTimes = relativeHistoricalTimes.map { nextEventTime + it }
        enqueueTouchEvent(
            downTime = downTime,
            action = ACTION_MOVE,
            actionIndex = 0,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = absoluteHistoricalTimes + listOf(nextEventTime),
            coordinates =
                List(entries.size) { historicalCoordinates[it] + listOf(entries[it].value) },
        )
    }

    fun PartialGesture.enqueueUp(pointerId: Int) {
        enqueueTouchEvent(
            if (lastPositions.size == 1) ACTION_UP else ACTION_POINTER_UP,
            lastPositions.keys.sorted().indexOf(pointerId),
        )
    }

    fun PartialGesture.enqueueCancel() {
        enqueueTouchEvent(ACTION_CANCEL, 0)
    }

    /**
     * Generates a MotionEvent with the given [action] and [actionIndex], adding all pointers that
     * are currently in the gesture, and adds the MotionEvent to the batch.
     *
     * @see MotionEvent.getAction
     * @see MotionEvent.getActionIndex
     */
    fun PartialGesture.enqueueTouchEvent(action: Int, actionIndex: Int) {
        val entries = lastPositions.entries.sortedBy { it.key }
        enqueueTouchEvent(
            downTime = downTime,
            action = action,
            actionIndex = actionIndex,
            pointerIds = List(entries.size) { entries[it].key },
            eventTimes = listOf(nextEventTime),
            coordinates = List(entries.size) { listOf(entries[it].value) },
        )
    }

    /** Generates an event with the given parameters. */
    private fun enqueueTouchEvent(
        downTime: Long,
        action: Int,
        actionIndex: Int,
        pointerIds: List<Int>,
        eventTimes: List<Long>,
        coordinates: List<List<Offset>>,
    ) {
        check(coordinates.size == pointerIds.size) {
            "Coordinates size should equal pointerIds size " +
                "(was: ${coordinates.size}, ${pointerIds.size})"
        }
        repeat(pointerIds.size) { pointerIndex ->
            check(eventTimes.size == coordinates[pointerIndex].size) {
                "Historical eventTimes size should equal coordinates[$pointerIndex] size " +
                    "(was: ${eventTimes.size}, ${coordinates[pointerIndex].size})"
            }
        }

        val positionInScreen = run {
            val array = intArrayOf(0, 0)
            root.view.getLocationOnScreen(array)
            Offset(array[0].toFloat(), array[1].toFloat())
        }
        val motionEvent =
            MotionEvent.obtain(
                    /* downTime = */ downTime,
                    /* eventTime = */ eventTimes[0],
                    /* action = */ action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
                    /* pointerCount = */ coordinates.size,
                    /* pointerProperties = */ Array(coordinates.size) { pointerIndex ->
                        PointerProperties().apply {
                            id = pointerIds[pointerIndex]
                            toolType = MotionEvent.TOOL_TYPE_FINGER
                        }
                    },
                    /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                        PointerCoords().apply {
                            val startOffset = coordinates[pointerIndex][0]

                            // Allows for non-valid numbers/Offsets to be passed along to
                            // Compose to
                            // test if it handles them properly (versus breaking here and we not
                            // knowing
                            // if Compose properly handles these values).
                            x =
                                if (startOffset.isValid()) {
                                    positionInScreen.x + startOffset.x
                                } else {
                                    Float.NaN
                                }

                            y =
                                if (startOffset.isValid()) {
                                    positionInScreen.y + startOffset.y
                                } else {
                                    Float.NaN
                                }
                        }
                    },
                    /* metaState = */ 0,
                    /* buttonState = */ 0,
                    /* xPrecision = */ 1f,
                    /* yPrecision = */ 1f,
                    /* deviceId = */ 0,
                    /* edgeFlags = */ 0,
                    /* source = */ SOURCE_TOUCHSCREEN,
                    /* flags = */ 0,
                )
                .apply {
                    // The current time & coordinates are the last element in the lists, and
                    // need to
                    // be passed into the final addBatch call. If there are no historical
                    // events,
                    // the list sizes are 1 and we don't need to call addBatch at all.
                    for (timeIndex in 1 until eventTimes.size) {
                        addBatch(
                            /* eventTime = */ eventTimes[timeIndex],
                            /* pointerCoords = */ Array(coordinates.size) { pointerIndex ->
                                PointerCoords().apply {
                                    val currentOffset = coordinates[pointerIndex][timeIndex]

                                    // Allows for non-valid numbers/Offsets to be passed along
                                    // to
                                    // Compose to test if it handles them properly (versus
                                    // breaking
                                    // here and we not knowing if Compose properly handles these
                                    // values).
                                    x =
                                        if (currentOffset.isValid()) {
                                            positionInScreen.x + currentOffset.x
                                        } else {
                                            Float.NaN
                                        }

                                    y =
                                        if (currentOffset.isValid()) {
                                            positionInScreen.y + currentOffset.y
                                        } else {
                                            Float.NaN
                                        }
                                }
                            },
                            /* metaState = */ 0,
                        )
                    }
                    offsetLocation(-positionInScreen.x, -positionInScreen.y)
                }

        batchedEvents.add(motionEvent)
    }
}
