/*
 * Copyright (C) 2025 The Android Open Source Project
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

package platform.test.desktop

import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Tests for [PeripheralDeviceTestRule] and [HostDrivenTestRule]. */
class PeripheralDeviceTest {
    @get:Rule val hostDrivenRule = HostDrivenTestRule()
    @get:Rule val peripheralDeviceRule = PeripheralDeviceTestRule()

    @Test
    fun testSimulatedDisplay() {
        val response =
            peripheralDeviceRule.requestPeripherals(
                DisplayPeripheral(PeripheralType.SIMULATED, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is SimulatedDisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    fun testSimulatedDisplay_multiModes() {
        val expectedMode1 = DisplayMode(DisplaySize.SIZE_1080P, 90f)
        val expectedMode2 = DisplayMode(DisplaySize.SIZE_2K, 60f)

        val response =
            peripheralDeviceRule.requestPeripherals(
                SimulatedDisplayPeripheral(listOf(expectedMode1, expectedMode2))
            )

        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is SimulatedDisplayDevice -> {
                    val display = it.display ?: fail("Missing display info")
                    val supportedModes = display.supportedModes
                    assertThat(supportedModes).hasLength(2)
                    val mode1 = supportedModes.get(0)
                    val mode2 = supportedModes.get(1)
                    assertThat(mode1.physicalWidth).isEqualTo(expectedMode1.size.width)
                    assertThat(mode1.physicalHeight).isEqualTo(expectedMode1.size.height)
                    assertThat(mode1.refreshRate).isEqualTo(expectedMode1.refreshRate)
                    assertThat(mode2.physicalWidth).isEqualTo(expectedMode2.size.width)
                    assertThat(mode2.physicalHeight).isEqualTo(expectedMode2.size.height)
                    assertThat(mode2.refreshRate).isEqualTo(expectedMode2.refreshRate)
                }

                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    fun testSimulatedDisplay_predefinedModes() {
        val response =
            peripheralDeviceRule.requestPeripherals(
                DisplayPeripheral(PeripheralType.SIMULATED, DisplaySize.SIZE_2K)
            )

        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is SimulatedDisplayDevice -> {
                    val display = it.display ?: fail("Missing display info")
                    val supportedModes = display.supportedModes
                    assertThat(supportedModes).hasLength(2)
                    val mode1 = supportedModes.get(0)
                    val mode2 = supportedModes.get(1)
                    assertThat(mode1.physicalWidth).isEqualTo(DisplaySize.SIZE_2K.width)
                    assertThat(mode1.physicalHeight).isEqualTo(DisplaySize.SIZE_2K.height)
                    assertThat(mode1.refreshRate)
                        .isEqualTo(PeripheralDeviceTestRule.DEFAULT_REFRESH_RATE)
                    assertThat(mode2.physicalWidth).isEqualTo(DisplaySize.SIZE_1080P.width)
                    assertThat(mode2.physicalHeight).isEqualTo(DisplaySize.SIZE_1080P.height)
                    assertThat(mode2.refreshRate)
                        .isEqualTo(PeripheralDeviceTestRule.DEFAULT_REFRESH_RATE)
                }

                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    fun testPhysicalDisplay() {
        val response =
            peripheralDeviceRule.requestPeripherals(
                DisplayPeripheral(PeripheralType.PHYSICAL, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is PhysicalDisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    @HostDrivenTest
    fun testPhysicalDisplay_afterReboot() {
        val response =
            peripheralDeviceRule.getPeripherals(
                DisplayPeripheral(PeripheralType.PHYSICAL, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is DisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    fun testPhysicalOrSimulatedDisplay() {
        val response =
            peripheralDeviceRule.requestPeripherals(
                DisplayPeripheral(PeripheralType.PHYSICAL_OR_SIMULATED, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is DisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    @HostDrivenTest
    fun testPhysicalOrSimulatedDisplay_afterReboot() {
        val response =
            peripheralDeviceRule.getPeripherals(
                DisplayPeripheral(PeripheralType.PHYSICAL_OR_SIMULATED, DisplaySize.SIZE_1080P)
            )
        assertThat(response.devices.filter { it.connected }).hasSize(1)
        response.devices.forEach {
            when (it) {
                is DisplayDevice -> assertThat(it.displayId).isGreaterThan(0)
                else -> fail("Unexpected peripheral device: $it")
            }
        }
    }

    @Test
    fun testCleanupAfterPhysicalOrSimulatedDisplay() {
        peripheralDeviceRule.apply(
            object : Statement() {
                override fun evaluate() {
                    testPhysicalOrSimulatedDisplay()
                }
            },
            Description.createTestDescription(
                this::class.java,
                "testCleanupAfterPhysicalOrSimulatedDisplay",
            ),
        )
        assertThat(peripheralDeviceRule.getPeripherals().devices).hasSize(0)
    }
}
