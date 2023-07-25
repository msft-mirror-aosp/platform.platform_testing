/*
 * Copyright (C) 2023 The Android Open Source Project
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
package android.platform.test.rule

import android.hardware.input.InputManager
import android.os.ParcelFileDescriptor
import android.platform.helpers.uinput.UInputDevice
import android.platform.uiautomator_helpers.DeviceHelpers
import android.view.InputDevice
import androidx.core.content.getSystemService
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.runner.Description

/**
 * This rule allows end-to-end tests to add input devices through Uinput more easily. Additionally
 * it will wait for registration to complete and unregister devices after the test is complete.
 *
 * Sample usage:
 * ```
 * class InputDeviceTest {
 *     @get:Rule
 *     val inputDeviceRule = InputDeviceRule()
 *
 *     @Test
 *     fun testWithInputDevice() {
 *         inputDeviceRule.registerDevice(UinputKeyboard())
 *         // Continue test with input device added
 *     }
 * }
 * ```
 */
class InputDeviceRule : TestWatcher(), UInputDevice.EventInjector {

    private val inputManager = DeviceHelpers.context.getSystemService<InputManager>()!!
    private val deviceAddedMap = mutableMapOf<UInputDevice, CountDownLatch>()
    private val inputManagerDevices = mutableMapOf<DeviceId, UInputDevice>()

    private lateinit var inputStream: ParcelFileDescriptor.AutoCloseInputStream
    private lateinit var outputStream: ParcelFileDescriptor.AutoCloseOutputStream

    private val inputDeviceListenerDelegate =
        object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) = updateInputDevice(DeviceId(deviceId))

            override fun onInputDeviceChanged(deviceId: Int) = updateInputDevice(DeviceId(deviceId))

            override fun onInputDeviceRemoved(deviceId: Int) {
                val deviceIdWrapped = DeviceId(deviceId)
                inputManagerDevices[deviceIdWrapped]?.let {
                    deviceAddedMap.remove(it)
                    inputManagerDevices.remove(deviceIdWrapped)
                }
            }
        }

    override fun starting(description: Description?) {
        super.starting(description)

        val (stdOut, stdIn) =
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .executeShellCommandRw("uinput -")

        inputStream = ParcelFileDescriptor.AutoCloseInputStream(stdOut)
        outputStream = ParcelFileDescriptor.AutoCloseOutputStream(stdIn)

        inputManager.registerInputDeviceListener(
            inputDeviceListenerDelegate,
            DeviceHelpers.context.mainThreadHandler
        )
    }

    override fun finished(description: Description?) {
        inputStream.close()
        outputStream.close()

        inputManager.unregisterInputDeviceListener(inputDeviceListenerDelegate)
        deviceAddedMap.clear()
        inputManagerDevices.clear()
    }

    /**
     * Registers the provided device with Uinput. This call waits for
     * [InputManager.InputDeviceListener.onInputDeviceAdded] to be called before returning
     *
     * @throws RuntimeException if the device did not register successfully.
     */
    fun registerDevice(device: UInputDevice) {
        deviceAddedMap.putIfAbsent(device, CountDownLatch(1))

        writeCommand(device.getRegisterCommand())

        deviceAddedMap[device]!!.let { latch ->
            latch.await(20, TimeUnit.SECONDS)
            if (latch.count != 0L) {
                throw RuntimeException(
                    "Did not receive added notification for device ${device.name}"
                )
            }
        }
    }

    /** Send the [keycode] event, both key down and key up events, for the provided [deviceId]. */
    override fun sendKeyEvent(deviceId: Int, keycode: Int) {
        injectEvdevEvents(deviceId, listOf(EV_KEY, keycode, KEY_DOWN, EV_SYN, SYN_REPORT, 0))
        injectEvdevEvents(deviceId, listOf(EV_KEY, keycode, KEY_UP, EV_SYN, SYN_REPORT, 0))
    }

    /**
     * Inject array of uinput events for a device. The following is an example of events: [[EV_KEY],
     * [KEY_UP], [KEY_DOWN], [EV_SYN], [SYN_REPORT], 0]. The number of entries in the provided
     * [evdevEvents] has to be a multiple of 3.
     *
     * @param deviceId The id corresponding to [UInputDevice] to associate with the [evdevEvents]
     * @param evdevEvents The uinput events to be injected
     */
    private fun injectEvdevEvents(deviceId: Int, evdevEvents: List<Int>) {
        assert(evdevEvents.size % 3 == 0) { "Number of injected events should be a multiple of 3" }

        val command = """{"command": "inject","id": $deviceId,"events": $evdevEvents}"""
        writeCommand(command)
    }

    private fun writeCommand(command: String) {
        outputStream.write(command.toByteArray())
        outputStream.flush()
    }

    private fun updateInputDevice(deviceId: DeviceId) {
        val device: InputDevice = inputManager.getInputDevice(deviceId.deviceId) ?: return
        val uinputDevice = deviceAddedMap.keys.firstOrNull { it.isInputDevice(device) } ?: return

        inputManagerDevices[deviceId] = uinputDevice
        deviceAddedMap[uinputDevice]!!.countDown()
    }

    @JvmInline value class DeviceId(val deviceId: Int)

    private companion object {
        // See
        // https://cs.android.com/android/kernel/superproject/+/common-android-mainline:common/include/uapi/linux/input-event-codes.h
        // for these mappings.
        const val EV_KEY = 1
        const val EV_SYN = 0
        const val SYN_REPORT = 0
        const val KEY_UP = 0
        const val KEY_DOWN = 1
    }
}
