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

import android.Manifest.permission.MANAGE_DISPLAYS
import android.Manifest.permission.MODIFY_USER_PREFERRED_DISPLAY_MODE
import android.annotation.SuppressLint
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED
import android.hardware.display.DisplayManager.EVENT_TYPE_DISPLAY_ADDED
import android.hardware.display.DisplayManager.EVENT_TYPE_DISPLAY_CHANGED
import android.hardware.display.DisplayManager.EVENT_TYPE_DISPLAY_REMOVED
import android.hardware.display.DisplayManager.PRIVATE_EVENT_TYPE_DISPLAY_CONNECTION_CHANGED
import android.hardware.display.DisplayTopology
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.SparseArray
import android.view.Display
import android.view.Display.Mode.FLAG_ANISOTROPY_CORRECTION
import android.view.Display.Mode.FLAG_SIZE_OVERRIDE
import androidx.annotation.GuardedBy
import androidx.core.util.size
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** An interface for a Condition to evaluate . */
fun interface Condition {
    /**
     * Evaluates a condition. If the call is made while initially waiting for the
     * condition - [isWaitingForCondition] is true. If the call is made just verify that an already
     * established state is still valid - [isWaitingForCondition] is false. Returns True if the
     * condition is satisfied, false otherwise.
     */
    fun evaluate(isWaitingForCondition: Boolean): Boolean
}

/**
 * Observes the changes in displays, and evaluates the condition every time a change happens. If
 * condition was true, but then evaluated to false -> assert error is triggered.
 */
class DisplayMonitor(caller: String, val allowDisablingDisplays: Boolean = false) : AutoCloseable {
    private val tag = "$caller DisplayMonitor"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private val displayManager: DisplayManager =
        context.getSystemService(DisplayManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                logD("onDisplayAdded($displayId)")
                onUpdate()
            }

            override fun onDisplayRemoved(displayId: Int) {
                logD("onDisplayRemoved($displayId)")
                // Defer update: if the display is disconnected, onDisplayDisconnected is coming but
                // there is a delay
                handler.postDelayed(updateRunnable, BACKOFF_DELAY.inWholeMilliseconds)
            }

            override fun onDisplayConnected(displayId: Int) {
                logD("onDisplayConnected($displayId)")
                // Defer update: if the display is auto-enabled, onDisplayAdded is coming but there
                // is a delay
                handler.postDelayed(updateRunnable, BACKOFF_DELAY.inWholeMilliseconds)
            }

            override fun onDisplayDisconnected(displayId: Int) {
                logD("onDisplayDisconnected($displayId)")
                onUpdate()
            }

            override fun onDisplayChanged(displayId: Int) {
                logD("onDisplayChanged($displayId)")
                onUpdate()
            }
        }
    private val topologyListener =
        Consumer<DisplayTopology> {
            // Instead of using *it* for topology, make a call to displayManager again,
            // this is because topology can get quickly updated with multiple displays.
            // we need to get the latest topology to avoid failing with invalid state.
            topology = displayManager.displayTopology
            logD("onTopology(received=$it, get=$topology)")
            onUpdate()
        }

    private val updateRunnable = Runnable { onUpdate() }

    @Volatile private var topology: DisplayTopology? = null

    @GuardedBy("this") @Volatile private var latch: CountDownLatch = CountDownLatch(1)

    @GuardedBy("this") @Volatile private var condition: Condition? = null

    @GuardedBy("this") @Volatile private var monitoringEnabled = false
    private var adoptedManageDisplaysPermission = false
    private var initialized = false

    override fun close() {
        displayManager.unregisterDisplayListener(displayListener)
        displayManager.unregisterTopologyListener(topologyListener)
        handler.removeCallbacks(updateRunnable)
        Log.i(tag, "unregister listeners")
        initialized = false
        if (adoptedManageDisplaysPermission) {
            Log.i(tag, "drop all shell permissions")
            uiAutomation.dropShellPermissionIdentity()
            adoptedManageDisplaysPermission = false
        }
    }

    fun getConnectedDisplays(): List<Display> =
        displayManager.getDisplays(DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED).filterNotNull()

    fun getEnabledDisplays(): List<Display> = displayManager.displays.filterNotNull()

    /**
     * Will match peripherals to the currently enabled displays. May perform other actions to ensure
     * displays are ready for testing.
     *
     * @param isWaitingForCondition True while waiting for a state change, false if it's a check on
     *   an already established state.
     * @param peripherals The list of [DisplayPeripheral]s to match against the available displays.
     * @param isSimulated Whether we need to match simulated peripherals or physical.
     * @return A list of [Pair]s where each [DisplayPeripheral] is matched to a [Display], or `null`
     *   if a complete match cannot be made or the topology is invalid.
     */
    fun matchPeripherals(
        isWaitingForCondition: Boolean,
        peripherals: List<DisplayPeripheral>,
        isSimulated: Boolean,
    ): List<Pair<DisplayPeripheral, Display>>? {
        val allEnabledDisplays = getEnabledDisplays()
        val enabledDisplays = allEnabledDisplays.filter { matchName(it.name, isSimulated) }
        val connectedDisplays = getConnectedDisplays().filter { matchName(it.name, isSimulated) }
        val enabledDisplaysIds = enabledDisplays.map { it.displayId }
        val connectedDisplaysIds = connectedDisplays.map { it.displayId }
        if (enabledDisplaysIds.intersect(connectedDisplaysIds).size != enabledDisplaysIds.size) {
            Log.w(
                tag,
                "matchPeripherals: enabled size(${enabledDisplaysIds.size}) is not subset" +
                    " of connectedDisplaysIds.size(${connectedDisplaysIds.size})",
            )
            return null
        }
        val matched = matchDisplaysToExpectations(connectedDisplays, peripherals)
        val matchedDisplays = matched.map { it.second }
        if (matched.size != peripherals.size) {
            logW(
                isWaitingForCondition,
                "matchPeripherals: matched size(${matched.size}) != peripherals.size(${peripherals.size})",
            )
            logD("matchPeripherals: connectedDisplays=$connectedDisplays")
            logD("matchPeripherals: peripherals=$peripherals")
            return null
        }
        if (
            isWaitingForCondition &&
                !verifyAndKeepOnlyMatchedEnabled(enabledDisplays, matched, allowDisablingDisplays)
        ) {
            return null
        }
        if (isWaitingForCondition && !verifyAndChangeResolutionIfNeeded(matched)) {
            Log.w(tag, "matchPeripherals: matched, but resolutions need to be changed")
            logD("matchPeripherals: matched=$matched")
            return null
        }
        if (!validateTopology(isWaitingForCondition, allEnabledDisplays, matchedDisplays)) {
            logW(isWaitingForCondition, "matchPeripherals: Topology is invalid")
            return null
        }
        logD("matchPeripherals: matched! $matched")
        return matched
    }

    private fun matchName(name: String, isSimulated: Boolean): Boolean {
        return name.startsWith(OVERLAY_DISPLAY_NAME_PREFIX) == isSimulated
    }

    fun startMonitoring(newCondition: Condition): Boolean {
        initIfNeeded()
        val conditionSatisfied =
            synchronized(this) {
                monitoringEnabled = true
                condition = newCondition
                latch = CountDownLatch(1)
                logD("startMonitoring $condition")
                evaluateCondition(newCondition, latch)
            }
        logD("startMonitoring conditionSatisfied=$conditionSatisfied")
        return conditionSatisfied
    }

    fun stopMonitoring() {
        synchronized(this) { monitoringEnabled = false }
    }

    fun waitForCondition(timeout: Duration): Boolean {
        val latchCopy =
            synchronized(this) {
                val currentExpectation = condition ?: return true
                if (!monitoringEnabled) {
                    startMonitoring(currentExpectation)
                }
                latch
            }

        return try {
            latchCopy.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        } catch (ex: InterruptedException) {
            throw AssertionError("Waiting for condition interrupted", ex)
        }
    }

    private fun onUpdate() {
        handler.removeCallbacks(updateRunnable)
        synchronized(this) {
            if (!monitoringEnabled) return
            val currentCondition = condition ?: return
            logD("onDisplayUpdate($condition)")
            evaluateCondition(currentCondition, latch)
        }
    }

    @GuardedBy("this")
    private fun evaluateCondition(
        targetCondition: Condition,
        targetLatch: CountDownLatch,
    ): Boolean {
        val isWaitingForCondition = targetLatch.count == 1L
        if (targetCondition.evaluate(isWaitingForCondition)) {
            // Condition is set, and it is returning true - this is valid state.
            logD("targetExpectation satisfied: $targetCondition")
            targetLatch.countDown()
            return true
        }
        // Condition is set, and evaluated to false
        // if isWaitingForCondition is false - previously the state was valid but became
        // invalid
        // It means that while waiting initially for the condition, isWaitingForCondition is true.
        // As soon as the condition satisfied for the first time, isWaitingForCondition becomes
        // false. And isWaitingForCondition stays false forever (until startMonitoring is called
        // again). This line executes when condition is not satisfied. In case it happens while
        // we are waiting initially - it is fine, but if isWaitingForCondition is already false,
        // this is not fine. It means that something changed and condition is no longer satisfied
        // for some reason.
        assertWithMessage("Failed condition: state was valid but became invalid '$targetCondition'")
            .that(isWaitingForCondition)
            .isTrue()
        return false
    }

    /**
     * Tries to find one best matching display from [displays] for each of the [peripherals] such
     * that each display can be assigned to only one peripheral. So, the number of [displays] must
     * be greater or equal to the number of [peripherals].
     *
     * For example, in case there are 2 [displays] available: Display#1: supportedModes =
     * [1080p, 2k, 4k], Display#2: supportedModes = [2k]
     *
     * If [peripherals] contains 1080p and 2k resolutions. There is a possibility that 2k gets
     * matched to Display#1 instead of Display#2. As the result, the 1080p peripheral could no
     * longer be matched at all.
     *
     * To alleviate this problem we first consider [peripherals] that match a smaller number of
     * [displays] (ideally 1). The algorithm is still not ideal for a greater number of [displays]
     * but implementation is simple.
     */
    private fun matchDisplaysToExpectations(
        displays: List<Display>,
        peripherals: List<DisplayPeripheral>,
    ): List<Pair<DisplayPeripheral, Display>> {
        val unmatchedDisplays = displays.toMutableList()
        return peripherals
            .map { p ->
                Pair(p, displays.filter { d -> d.supportedModes.any(matchDisplayMode(p)) })
            }
            .sortedBy { it.second.size }
            .mapNotNull { p ->
                p.second
                    .find { it in unmatchedDisplays }
                    ?.let { matchedDisplay ->
                        unmatchedDisplays.remove(matchedDisplay)
                        p.first to matchedDisplay
                    }
            }
    }

    private fun initIfNeeded() {
        if (initialized) return

        try {
            Log.i(tag, "register listeners")
            registerListeners()
        } catch (_: SecurityException) {
            Log.w(tag, "retry register listeners")
            adoptShellPermissions()
            registerListeners()
        }
        topology = displayManager.displayTopology
        initialized = true
    }

    private fun validateTopology(
        isWaitingForCondition: Boolean,
        allEnabledDisplays: List<Display>,
        matchedDisplays: List<Display>,
    ): Boolean {
        val allEnabledDisplayIds = allEnabledDisplays.map { it.displayId }
        val displayIds = matchedDisplays.map { it.displayId }
        val mirroringState =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
                0,
            )
        // If we are not mirroring, need to validate that topology is consistent with the displays
        val idsInTopology = topology?.getAbsoluteBounds()?.getKeys()
        if (mirroringState == 0) {
            if (idsInTopology == null) {
                logW(isWaitingForCondition, "No topology received")
                return false
            }
            if (idsInTopology.intersect(displayIds).size != displayIds.size) {
                logW(
                    isWaitingForCondition,
                    "validateTopology:" +
                        " Not all matched displays are found in topology:" +
                        " idsInTopology=$idsInTopology" +
                        " displayIds=$displayIds",
                )
                return false
            }
            if (
                isWaitingForCondition // If still waiting for condition (not monitoring)
                && idsInTopology.intersect(allEnabledDisplayIds).size != idsInTopology.size
            ) {
                // Displays might be removed, but topology update is delayed.
                logW(
                    isWaitingForCondition,
                    "validateTopology:" +
                        " Not all displays from topology are found in all displays, waiting" +
                        " for topology update:" +
                        " idsInTopology=$idsInTopology" +
                        " allEnabledDisplayIds=$allEnabledDisplayIds",
                )
                return false
            }
        } else if ((idsInTopology?.size ?: 0) > 1) {
            logW(
                isWaitingForCondition,
                "validateTopology:" +
                    " We are mirroring but topology still has too many displays:" +
                    " idsInTopology=$idsInTopology" +
                    " displayIds=$displayIds",
            )
            return false
        }
        return true
    }

    private fun logW(isWaitingForCondition: Boolean, msg: String) =
        if (isWaitingForCondition) logD(msg) else Log.w(tag, msg)

    private fun logD(msg: String) {
        if (DEBUG) Log.d(tag, msg)
    }

    private fun matchDisplayMode(p: DisplayPeripheral): (Display.Mode) -> Boolean = { mode ->
        approxEqual(mode.physicalWidth, p.size.width) &&
            approxEqual(mode.physicalHeight, p.size.height)
    }

    private fun approxEqual(size1: Int, size2: Int): Boolean {
        val maxSize = Math.max(size1, size2)
        val minSize = Math.min(size1, size2)
        return (maxSize - minSize) < maxSize * MAX_SIZE_MISMATCH
    }

    private fun adoptShellPermissions() {
        Log.w(
            tag,
            "Adopting MANAGE_DISPLAYS, MODIFY_USER_PREFERRED_DISPLAY_MODE permissions," +
                " dropping other permissions",
        )
        uiAutomation.adoptShellPermissionIdentity(
            MANAGE_DISPLAYS,
            MODIFY_USER_PREFERRED_DISPLAY_MODE,
        )
        adoptedManageDisplaysPermission = true
    }

    private fun registerListeners() {
        displayManager.registerDisplayListener(
            displayListener,
            handler,
            EVENT_TYPE_DISPLAY_ADDED or EVENT_TYPE_DISPLAY_CHANGED or EVENT_TYPE_DISPLAY_REMOVED,
            PRIVATE_EVENT_TYPE_DISPLAY_CONNECTION_CHANGED,
        )
        displayManager.registerTopologyListener(handler::post, topologyListener)
    }

    /**
     * Checks [matched] displays on whether they are all enabled, and enables the [matched] displays
     * if they are not yet enabled. If [isDisablingDisplaysAllowed] is true, then not [matched]
     * displays will be disabled.
     *
     * @param enabledDisplays all enabled displays.
     * @param matched list of peripheral to display pairs, display may be disabled at this point.
     * @param isDisablingDisplaysAllowed if true, allows to disable displays if not [matched].
     * @return true if all [matched] displays are enabled, and allowed to be disabled are disabled.
     */
    private fun verifyAndKeepOnlyMatchedEnabled(
        enabledDisplays: List<Display>,
        matched: List<Pair<Peripheral, Display>>,
        isDisablingDisplaysAllowed: Boolean,
    ): Boolean {
        val enabledDisplayIds = enabledDisplays.map { it.displayId }.toSet()
        val matchedDisabledDisplayIds =
            matched.map { it.second }.map { it.displayId }.filterNot { it in enabledDisplayIds }
        if (matchedDisabledDisplayIds.isNotEmpty()) {
            try {
                Log.i(tag, "matchPeripherals: Try enable displays $matchedDisabledDisplayIds")
                enableDisplays(matchedDisabledDisplayIds)
            } catch (_: SecurityException) {
                Log.i(tag, "matchPeripherals: Retry enable displays $matchedDisabledDisplayIds")
                adoptShellPermissions()
                enableDisplays(matchedDisabledDisplayIds)
            }
            return false
        }
        if (!isDisablingDisplaysAllowed) {
            // If we don't allow disabling displays, then number of displays must be equal to
            // the requested
            return enabledDisplays.size == matched.size
        }
        val matchedDisplayIds = matched.map { it.second.displayId }.toSet()
        val unmatchedEnabledDisplayIds = enabledDisplayIds.filterNot { it in matchedDisplayIds }
        if (unmatchedEnabledDisplayIds.isNotEmpty()) {
            try {
                Log.i(tag, "matchPeripherals: Try disable displays $unmatchedEnabledDisplayIds")
                disableDisplays(unmatchedEnabledDisplayIds)
            } catch (_: SecurityException) {
                Log.i(tag, "matchPeripherals: Retry disable displays $unmatchedEnabledDisplayIds")
                adoptShellPermissions()
                disableDisplays(unmatchedEnabledDisplayIds)
            }
            return false
        }
        return true
    }

    private fun verifyAndChangeResolutionIfNeeded(
        matched: List<Pair<DisplayPeripheral, Display>>
    ): Boolean =
        matched.all {
            val p = it.first
            val d = it.second
            if (matchDisplayMode(p)(getDisplayMode(d))) {
                logD("verifyAndChangeResolutionIfNeeded matched")
                true
            } else {
                logD("verifyAndChangeResolutionIfNeeded Not matched")
                setUserPreferredDisplayMode(
                    d.displayId,
                    d.supportedModes.find(matchDisplayMode(p))!!,
                )
                false
            }
        }

    private fun getDisplayMode(display: Display): Display.Mode {
        val userPreferredMode = display.userPreferredDisplayMode
        if (userPreferredMode != null && (userPreferredMode.flags and FLAG_SIZE_OVERRIDE) != 0) {
            return userPreferredMode
        }
        if (
            userPreferredMode != null &&
                (userPreferredMode.flags and FLAG_ANISOTROPY_CORRECTION) != 0
        ) {
            val parentMode =
                display.supportedModes.find { it.modeId == userPreferredMode.parentModeId }
            // active mode size matches with parent mode size
            if (
                parentMode != null &&
                    display.mode.matches(parentMode.physicalWidth, parentMode.physicalHeight)
            ) {
                return userPreferredMode
            }
        }
        return display.mode
    }

    @SuppressLint("MissingPermission")
    private fun setUserPreferredDisplayMode(displayId: Int, mode: Display.Mode) {
        logD("setUserPreferredDisplayMode $displayId $mode")
        try {
            displayManager.setUserPreferredDisplayMode(displayId, mode, /* storeMode= */ false)
        } catch (_: SecurityException) {
            adoptShellPermissions()
            displayManager.setUserPreferredDisplayMode(displayId, mode, /* storeMode= */ false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableDisplays(displayIds: List<Int>) {
        displayIds.forEach(displayManager::enableConnectedDisplay)
    }

    @SuppressLint("MissingPermission")
    private fun disableDisplays(displayIds: List<Int>) {
        displayIds.forEach(displayManager::disableConnectedDisplay)
    }

    private companion object {
        const val DEBUG = true
        const val MAX_SIZE_MISMATCH = 0.025
        val BACKOFF_DELAY = 50L.milliseconds
        // Matches com.android.internal.R.string.display_manager_overlay_display_name.
        const val OVERLAY_DISPLAY_NAME_PREFIX = "Overlay #"

        fun <T> SparseArray<T>.getKeys(): ArrayList<Int> {
            val res = ArrayList<Int>(this.size)
            for (i in 0..<this.size) {
                res.add(i, this.keyAt(i))
            }
            return res
        }
    }
}
