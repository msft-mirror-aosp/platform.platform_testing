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

import android.view.Display
import com.android.server.testutils.TestUtils
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.desktop.interactive.DesktopTestOptionsProvider

/** An interface for a controller to request peripherals. */
fun interface PeripheralsController {
    /**
     * Requests the peripherals specified in the [request]. If the list is empty, disconnects all
     * the peripherals. Returns [PeripheralsResponse] containing [PeripheralDevices] that were
     * connected or disconnected.
     */
    fun requestPeripherals(request: PeripheralsRequest): PeripheralsResponse
}

/** An interface for a peripheral device returned by a [PeripheralsController]. */
sealed interface PeripheralDevice {
    val peripheral: Peripheral
    val connected: Boolean
}

/** An interface for a display device returned by a [PeripheralsController]. */
sealed interface DisplayDevice : PeripheralDevice {
    val displayId: Int
    val display: Display?
}

/**
 * A display device used internally by a [PeripheralsController].
 *
 * @property peripheral The peripheral that was requested.
 * @property connected Whether the display is connected.
 * @property displayId The display identifier.
 * @property display The display object.
 */
internal data class AnyDisplayDevice(
    override val peripheral: Peripheral,
    override val connected: Boolean,
    override val displayId: Int,
    override val display: Display?,
) : DisplayDevice

/** Interface for peripherals to simplify their classification. */
sealed interface Peripheral {
    val type: PeripheralType
}

/** The type of the peripheral. */
enum class PeripheralType {
    SIMULATED,
    PHYSICAL,
    PHYSICAL_OR_SIMULATED,
}

/** A display resolution. */
enum class DisplaySize(val width: Int, val height: Int) {
    SIZE_1080P(1920, 1080),
    SIZE_1080P_ULTRA_WIDE(2560, 1080),
    SIZE_2K(2560, 1440),
    SIZE_2K_ULTRA_WIDE(3440, 1440),
    SIZE_4K(3840, 2160),
    SIZE_4K_ULTRA_WIDE(5120, 2160),
}

data class DisplayMode(
    val size: DisplaySize,
    val refreshRate: Float = PeripheralDeviceTestRule.DEFAULT_REFRESH_RATE,
)

/** A display peripheral. */
open class DisplayPeripheral(override val type: PeripheralType, open val size: DisplaySize) :
    Peripheral

// Display peripheral supporting multiple display modes to be set up, could only be requested for
// SIMULATED display
data class SimulatedDisplayPeripheral(val modes: List<DisplayMode>) :
    DisplayPeripheral(PeripheralType.SIMULATED, modes.first().size) {
    init {
        require(modes.isNotEmpty()) { "Must provide at least one mode" }
    }
}

/** A request to connect [peripherals]. */
class PeripheralsRequest(val peripherals: List<Peripheral>, val timeout: Duration) {

    /** Returns a new [PeripheralsRequest] with all peripherals of the specified [type]. */
    fun getByType(type: PeripheralType): PeripheralsRequest =
        PeripheralsRequest(peripherals.filter { it.type == type }, timeout)

    /** Validates that all peripherals are of the [types]. */
    fun validate(vararg types: PeripheralType) {
        val supportedTypes = types.toList()

        assertWithMessage("There shouldn't be any non-supported peripherals")
            .that(peripherals.filter { it.type !in supportedTypes })
            .isEmpty()
    }

    operator fun plus(other: PeripheralsRequest): PeripheralsRequest =
        PeripheralsRequest(peripherals + other.peripherals, timeout)

    /** Returns a new [PeripheralsRequest] with all peripherals that are not connected. */
    fun notConnected(response: PeripheralsResponse): PeripheralsRequest {
        val connectedPeripherals = response.devices.filter { it.connected }.map { it.peripheral }
        return PeripheralsRequest(
            peripherals.filter { p -> connectedPeripherals.none { it === p } },
            timeout,
        )
    }

    override fun toString(): String = "peripherals=${peripherals.joinToString(", ")}"
}

/** A response with [devices] after connecting and disconnecting peripherals. */
class PeripheralsResponse(val devices: List<PeripheralDevice> = emptyList()) {
    override fun toString(): String = "devices=${devices.joinToString(", ")}"

    operator fun plus(other: PeripheralsResponse): PeripheralsResponse =
        PeripheralsResponse(devices + other.devices)
}

/**
 * A test rule allowing to request peripherals during the test and disconnects them after the test.
 */
class PeripheralDeviceTestRule : TestRule, PeripheralsController {
    private val optionsProvider = DesktopTestOptionsProvider.getInstance()
    private val physicalController = PhysicalDeviceController()
    private val simulatedController = SimulatedDeviceController()

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                // try-with-resources, ensuring cleanup steps are executed during
                // CleanupExecutor#close(). If the close() method also throws an exception(s),
                // the exception(s) from close() are suppressed and added to the primary exception.
                TestUtils.CleanupExecutor(TAG).use { cl ->
                    // Last cleanup steps: release all resources
                    cl.addCleanup(simulatedController::close)
                    cl.addCleanup(physicalController::close)

                    // Don't implicitly cleanup peripherals before the test, e.g. in case of
                    // after-reboot tests, if these tests need to cleanup peripherals, they need
                    // to do it explicitly, or don't pass
                    // [DesktopTestOptions.KEEP_PERIPHERALS_BEFORE_TEST] option
                    if (!optionsProvider.keepPeripheralsBeforeTest()) {
                        // Ensure no peripherals connected before the test
                        disconnectAll()
                    }
                    // First cleanup step: disconnect all peripherals
                    cl.addCleanup {
                        // Don't implicitly cleanup peripherals after the test, e.g. in case of
                        // before-reboot tests, if these tests need to cleanup peripherals, tests
                        // need to do it explicitly, or simply don't pass
                        // [DesktopTestOptions.KEEP_PERIPHERALS_AFTER_TEST] option.
                        // If test evaluation has an exception - cleanup must be done in any case.
                        if (cl.hasException() || !optionsProvider.keepPeripheralsAfterTest()) {
                            disconnectAll()
                        }
                    }
                    // Run the test
                    base.evaluate()
                }
            }
        }

    fun startMonitoring(): Boolean {
        val sim = simulatedController.startMonitoring(TIMEOUT)
        val phys = physicalController.startMonitoring(TIMEOUT)
        return sim && phys
    }

    fun stopMonitoring() {
        simulatedController.stopMonitoring()
        physicalController.stopMonitoring()
    }

    fun disconnectAll() = requestPeripherals(PeripheralsRequest(emptyList(), TIMEOUT))

    fun requestPeripherals(vararg peripherals: Peripheral): PeripheralsResponse =
        requestPeripherals(PeripheralsRequest(peripherals.toList(), TIMEOUT))

    fun getPeripherals(vararg peripherals: Peripheral): PeripheralsResponse =
        requestPeripherals(PeripheralsRequest(peripherals.toList(), 0.seconds))

    /**
     * Tries to connect all peripherals specified in the [request]. If some peripherals are no
     * longer requested, they are disconnected and returned by this method in [PeripheralsResponse]
     * as disconnected PeripheralDevices.
     *
     * For [PeripheralType.SIMULATED] or [PeripheralType.PHYSICAL_OR_SIMULATED],
     * [SimulatedController] sets up simulated type of peripheral If simulated peripheral fails to
     * be set up, the test fails.
     *
     * For [PeripheralType.PHYSICAL] or [PeripheralType.PHYSICAL_OR_SIMULATED], [PhysicalController]
     * connects them using a LabController then waits for them to be connected and configured. If
     * physical peripheral fails to connect or be set up, the test is skipped.
     */
    override fun requestPeripherals(request: PeripheralsRequest): PeripheralsResponse {
        val physicalOrSimulated = request.getByType(PeripheralType.PHYSICAL_OR_SIMULATED)
        val physicalRequest = physicalOrSimulated + request.getByType(PeripheralType.PHYSICAL)
        val physicalResponse = physicalController.requestPeripherals(physicalRequest)
        assumePhysicalConnected(physicalRequest, physicalResponse)

        val remaining = physicalOrSimulated.notConnected(physicalResponse)
        val simulatedRequest = remaining + request.getByType(PeripheralType.SIMULATED)
        val simulatedResponse = simulatedController.requestPeripherals(simulatedRequest)
        assertSimulatedConnected(simulatedRequest, simulatedResponse)

        return physicalResponse + simulatedResponse
    }

    private fun assumePhysicalConnected(
        request: PeripheralsRequest,
        response: PeripheralsResponse,
    ) {
        val physicalRequestedCount =
            request.peripherals.count { it.type == PeripheralType.PHYSICAL }
        val physicalConnectedCount =
            response.devices.count { it.connected && it.peripheral.type == PeripheralType.PHYSICAL }
        // Assume that all physical peripherals are connected, if not - skip the test.
        assumeTrue(
            "Can't connect all " +
                "requested physical peripherals: $request " +
                "response: $response",
            physicalRequestedCount == physicalConnectedCount,
        )
    }

    private fun assertSimulatedConnected(
        request: PeripheralsRequest,
        response: PeripheralsResponse,
    ) {
        assertEquals(
            request.peripherals.size,
            response.devices.count { it.connected },
            "Can't connect all " +
                "requested simulated peripherals: $request " +
                "response: $response",
        )
    }

    companion object {
        const val DEFAULT_REFRESH_RATE = 60f
        private const val TAG = "PeripheralDeviceTestR"
        private val TIMEOUT = 30.seconds
    }
}
