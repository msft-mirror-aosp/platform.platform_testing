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
import android.view.Display
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration

/**
 * A simulated display device returned by a [SimulatedDeviceController].
 *
 * @param d The underlying [DisplayDevice].
 */
data class SimulatedDisplayDevice(val d: DisplayDevice) : DisplayDevice by d

/**
 * A controller for simulated peripherals.
 *
 * Simulated displays can't be incrementally changed (yet), so they have to be destroyed and
 * re-created. [DisplayMonitor] may become confused and detect an invalid state, throw an exception.
 * To avoid this the test needs to ask displays to be fully destroyed by passing empty
 * [PeripheralsRequest].
 */
class SimulatedDeviceController : PeripheralsController {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var currentDisplaysPeripherals: List<Pair<DisplayPeripheral, Display>>? = null
    private val displayMonitor = DisplayMonitor(TAG)

    fun close() = displayMonitor.close()

    fun assertNoFailedConditions() = displayMonitor.assertNoFailedConditions()

    fun startMonitoring(timeout: Duration) = displayMonitor.waitForCondition(timeout)

    fun stopMonitoring() = displayMonitor.stopMonitoring()

    override fun requestPeripherals(request: PeripheralsRequest): PeripheralsResponse {
        request.validate(PeripheralType.SIMULATED, PeripheralType.PHYSICAL_OR_SIMULATED)
        return setupSimulatedDisplays(
            request.peripherals.filterIsInstance<DisplayPeripheral>(),
            request.timeout,
        )
    }

    private fun setupSimulatedDisplays(
        peripherals: List<DisplayPeripheral>,
        timeout: Duration,
    ): PeripheralsResponse {
        // If we need some displays created
        assertTrue(
            currentDisplaysPeripherals.isNullOrEmpty() || peripherals.isEmpty(),
            "Simulated displays can't be incrementally changed (yet): " +
                peripherals +
                " " +
                currentDisplaysPeripherals,
        )

        // Expect new displays created
        if (!displayMonitor.startMonitoring(createDisplayExpectation(peripherals))) {
            val displaySettings =
                peripherals.joinToString(separator = ";") { peripheral ->
                    val modes =
                        when (peripheral) {
                            is SimulatedDisplayPeripheral -> peripheral.modes
                            is DisplayPeripheral ->
                                PREDEFINED_MODES[peripheral.size]
                                    ?: listOf(DisplayMode(peripheral.size))
                        }
                    val modeString =
                        modes.joinToString(separator = "|") { mode ->
                            val size = mode.size
                            "${size.width}x${size.height}/$DEFAULT_DENSITY@${mode.refreshRate}"
                        }
                    val uniqueId = createUniqueId(peripheral, peripherals)
                    "$modeString,external,unique_id=$uniqueId,disable_window_interaction"
                }
            if (timeout.isPositive()) {
                Settings.Global.putString(
                    context.contentResolver,
                    Settings.Global.OVERLAY_DISPLAY_DEVICES,
                    displaySettings,
                )
            }
            assertTrue(displayMonitor.waitForCondition(timeout), "waitForExpectation failed")
        }

        val removedDisplays =
            currentDisplaysPeripherals?.filterNot { it.first in peripherals } ?: emptyList()
        currentDisplaysPeripherals =
            displayMonitor.matchPeripherals(
                /*isWaitingForCondition=*/ false,
                peripherals,
                /*isSimulated=*/ true,
            )

        val curDisplayPeripherals =
            assertNotNull(currentDisplaysPeripherals, "Could not match all peripherals")

        val addedResponse =
            PeripheralsResponse(
                curDisplayPeripherals.map { (peripheral, display) ->
                    SimulatedDisplayDevice(
                        AnyDisplayDevice(peripheral, connected = true, display.displayId, display)
                    )
                }
            )

        val removedResponse =
            PeripheralsResponse(
                removedDisplays.map { (peripheral, display) ->
                    SimulatedDisplayDevice(
                        AnyDisplayDevice(
                            peripheral,
                            connected = false,
                            display.displayId,
                            display = display,
                        )
                    )
                }
            )

        return addedResponse + removedResponse
    }

    private fun createUniqueId(
        peripheral: DisplayPeripheral,
        peripherals: List<DisplayPeripheral>,
    ): String {
        var sameSizeCounter = 0
        for (p in peripherals) {
            if (p == peripheral) {
                break
            }
            if (p.size == peripheral.size) {
                sameSizeCounter += 1
            }
        }
        return "${sameSizeCounter}_${peripheral.size.name.lowercase()}"
    }

    private fun createDisplayExpectation(peripherals: List<DisplayPeripheral>): Condition =
        Condition {
            displayMonitor.matchPeripherals(
                /*isWaitingForCondition=*/ it,
                peripherals,
                /*isSimulated=*/ true,
            ) != null
        }

    private companion object {
        const val TAG = "Simulated"
        const val DEFAULT_DENSITY = 160

        val PREDEFINED_MODES: Map<DisplaySize, List<DisplayMode>> =
            mapOf(
                DisplaySize.SIZE_1080P to listOf(DisplayMode(DisplaySize.SIZE_1080P)),
                DisplaySize.SIZE_1080P_ULTRA_WIDE to
                    listOf(
                        DisplayMode(DisplaySize.SIZE_1080P_ULTRA_WIDE),
                        DisplayMode(DisplaySize.SIZE_1080P),
                    ),
                DisplaySize.SIZE_2K to
                    listOf(DisplayMode(DisplaySize.SIZE_2K), DisplayMode(DisplaySize.SIZE_1080P)),
                DisplaySize.SIZE_2K_ULTRA_WIDE to
                    listOf(
                        DisplayMode(DisplaySize.SIZE_2K_ULTRA_WIDE),
                        DisplayMode(DisplaySize.SIZE_2K),
                        DisplayMode(DisplaySize.SIZE_1080P_ULTRA_WIDE),
                        DisplayMode(DisplaySize.SIZE_1080P),
                    ),
                DisplaySize.SIZE_4K to
                    listOf(
                        DisplayMode(DisplaySize.SIZE_4K),
                        DisplayMode(DisplaySize.SIZE_2K),
                        DisplayMode(DisplaySize.SIZE_1080P),
                    ),
                DisplaySize.SIZE_4K_ULTRA_WIDE to
                    listOf(
                        DisplayMode(DisplaySize.SIZE_4K_ULTRA_WIDE),
                        DisplayMode(DisplaySize.SIZE_4K),
                        DisplayMode(DisplaySize.SIZE_2K_ULTRA_WIDE),
                        DisplayMode(DisplaySize.SIZE_2K),
                        DisplayMode(DisplaySize.SIZE_1080P_ULTRA_WIDE),
                        DisplayMode(DisplaySize.SIZE_1080P),
                    ),
            )
    }
}
