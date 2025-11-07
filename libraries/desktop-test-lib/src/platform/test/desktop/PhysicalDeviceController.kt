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

import android.provider.Settings
import android.util.Log
import android.view.Display
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import platform.test.desktop.interactive.ConnectDevicesDialogProvider
import platform.test.desktop.interactive.DesktopTestOptionsProvider

/**
 * A physical display device returned by a [PhysicalDeviceController].
 *
 * @param d The underlying [DisplayDevice].
 */
data class PhysicalDisplayDevice(val d: DisplayDevice) : DisplayDevice by d

/**
 * A controller for physical peripherals.
 *
 * This controller supports several scenarios for interacting with physical displays:
 * - **Manual Mode:** Enabled by passing the instrumentation argument `ENABLE_MANUAL:=true`. In this
 *   mode, the test will use [HumanDialog] to prompt the user to manually connect or disconnect
 *   physical displays to match the [PeripheralsRequest]. This is useful for testing with actual
 *   physical hardware.
 * - **Automated with VKMS:** This mode is currently not implemented, as `checkVkms()` always
 *   returns `false`. The intention is to use a Virtual Kernel Mode Setting (VKMS) driver to
 *   automate the simulation of physical displays.
 * - **Handling `PeripheralType.PHYSICAL_OR_SIMULATED`:** When a [PeripheralsRequest] includes
 *   peripherals of type `PHYSICAL_OR_SIMULATED`, this controller will attempt to find and match
 *   actual physical displays. If a `PeripheralType.PHYSICAL` is also requested, and no matching
 *   physical devices are found, the test will fail. If only `PHYSICAL_OR_SIMULATED` is requested,
 *   the test may proceed even without finding physical devices, allowing other controllers to
 *   potentially provide simulated alternatives.
 */
class PhysicalDeviceController : PeripheralsController {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var optionsProvider = DesktopTestOptionsProvider.getInstance()
    private val displayMonitor = DisplayMonitor(TAG, optionsProvider.allowDisablingDisplays())
    private val isAutomatedWithVkms = checkVkms()
    private var currentDisplaysPeripherals: List<Pair<DisplayPeripheral, Display>>? = null

    fun close() = displayMonitor.close()

    fun startMonitoring(timeout: Duration) = displayMonitor.waitForCondition(timeout)

    fun stopMonitoring() = displayMonitor.stopMonitoring()

    override fun requestPeripherals(request: PeripheralsRequest): PeripheralsResponse {
        request.validate(PeripheralType.PHYSICAL, PeripheralType.PHYSICAL_OR_SIMULATED)

        val displayPeripherals = request.peripherals.filterIsInstance<DisplayPeripheral>()

        val mustRunTest = peripheralsSetup(displayPeripherals, request.timeout)

        val removedDisplays = currentDisplaysPeripherals?.filter { it.first !in displayPeripherals }
        currentDisplaysPeripherals =
            displayMonitor.matchPeripherals(
                /*isWaitingForCondition=*/ false,
                displayPeripherals,
                Display.TYPE_EXTERNAL,
            )

        if (mustRunTest) {
            assertWithMessage("Could not match all peripherals")
                .that(currentDisplaysPeripherals)
                .isNotNull()
        } else if (currentDisplaysPeripherals == null) {
            // Expectations are not matched, nothing to monitor.
            displayMonitor.close()
        }

        val addedResponse =
            PeripheralsResponse(
                currentDisplaysPeripherals?.map { (peripheral, display) ->
                    PhysicalDisplayDevice(
                        AnyDisplayDevice(peripheral, connected = true, display.displayId, display)
                    )
                } ?: emptyList()
            )

        val removedResponse =
            PeripheralsResponse(
                removedDisplays?.map { (peripheral, display) ->
                    PhysicalDisplayDevice(
                        AnyDisplayDevice(
                            peripheral,
                            connected = false,
                            display.displayId,
                            display = display,
                        )
                    )
                } ?: emptyList()
            )

        return addedResponse + removedResponse
    }

    /**
     * @param displayPeripherals required
     * @param timeout for how long we should wait for peripherals
     * @return true if the test must run. false if there is no way to run the test e.g. due to lack
     *   of infrastructure support, but the test still may run if it can.
     */
    private fun peripheralsSetup(
        displayPeripherals: List<DisplayPeripheral>,
        timeout: Duration,
    ): Boolean {
        if (!optionsProvider.isManual() && !optionsProvider.isAutomated() && !isAutomatedWithVkms) {
            if (!displayPeripherals.isEmpty()) {
                Log.i(
                    TAG,
                    "Physical devices support is not enabled. To run it manually" +
                        " e.g. for DesktopTestLibTests module add atest arg:  -- --module-arg" +
                        " DesktopTestLibTests:instrumentation-arg:ENABLE_MANUAL:=true",
                )
            }
            // not required to run the test, but it still may with simulated peripherals.
            return false
        }
        // We must run the test if there is a PHYSICAL only peripheral required.
        val mustRunTest = displayPeripherals.any { it.type == PeripheralType.PHYSICAL }
        if (!displayMonitor.startMonitoring(createDisplayExpectation(displayPeripherals))) {
            if (timeout.isPositive()) {
                sendPeripheralsRequest(displayPeripherals)
            }
            val isConditionSatisfied =
                try {
                    if (
                        !timeout.isPositive() ||
                            (optionsProvider.isManual() && HumanDialog.show(displayPeripherals))
                    ) {
                        // Don't wait anymore as user either confirmed the connection, or
                        // condition is satisfied, or timeout is reached.
                        // result of waitForCondition is set to isConditionSatisfied
                        displayMonitor.waitForCondition(0.seconds)
                    } else if (optionsProvider.isAutomated() || isAutomatedWithVkms) {
                        // result of waitForCondition is set to isConditionSatisfied
                        displayMonitor.waitForCondition(timeout)
                    } else {
                        // not required to run the test, but it still may run with simulated
                        // peripherals.
                        return false
                    }
                } catch (e: NoClassDefFoundError) {
                    Log.w(
                        TAG,
                        "Can't show human dialog due to missing resources. Is " +
                            "'Interactive' library missing in Android.bp static_libs?",
                        e,
                    )
                    displayMonitor.waitForCondition(timeout)
                } catch (e: ClassNotFoundException) {
                    // In case HumanDialog fails to show up due to resources missing
                    // just wait for the peripherals
                    Log.w(
                        TAG,
                        "Can't show human dialog due to missing resources. is " +
                            "'desktop-test-lib-interactive' library missing" +
                            " in Android.bp static_libs?",
                        e,
                    )
                    displayMonitor.waitForCondition(timeout)
                }

            assertWithMessage("waitForExpectation failed")
                .that(isConditionSatisfied || !mustRunTest)
                .isTrue()
        }
        return mustRunTest
    }

    private fun sendPeripheralsRequest(displayPeripherals: List<DisplayPeripheral>) {
        if (isAutomatedWithVkms) {
            // TODO(449940785)
        } else {
            val displaySettings =
                displayPeripherals.joinToString(separator = ";") {
                    "${it.size.width}x${it.size.height}"
                }
            Settings.Global.putString(
                context.contentResolver,
                "peripheral_devices",
                displaySettings,
            )
        }
    }

    private fun createDisplayExpectation(peripherals: List<DisplayPeripheral>): Condition =
        Condition { isWaitingForCondition ->
            val result: Boolean =
                displayMonitor.matchPeripherals(
                    isWaitingForCondition,
                    peripherals,
                    Display.TYPE_EXTERNAL,
                ) != null
            if (result) {
                // If condition is satisfied - unblock the dialog
                HumanDialog.unblock()
            }
            result
        }

    // TODO(449940785)
    private fun checkVkms() = false

    object HumanDialog {
        fun unblock() {
            ConnectDevicesDialogProvider.instance.unblockDialog()
        }

        fun show(displayPeripherals: List<DisplayPeripheral>): Boolean {
            // Show dialog, blocking execution until user confirmation or condition satisfaction
            // or timeout
            val textToShow =
                if (displayPeripherals.isEmpty()) {
                    "Disconnect peripherals"
                } else {
                    "Connect: ${displayPeripherals.map { "${it::class.simpleName}:${it.size}" }}"
                }
            return ConnectDevicesDialogProvider.instance.showDialog(
                    textToShow, TIMEOUT.inWholeMilliseconds)
        }
    }

    companion object {
        private const val TAG = "Physical"
        private val TIMEOUT = 30.seconds
    }
}
