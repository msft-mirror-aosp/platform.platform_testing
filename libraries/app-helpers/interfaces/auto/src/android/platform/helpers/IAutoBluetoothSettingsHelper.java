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

package android.platform.helpers;

/**
 * Helper class for functional tests that use the Bluetooth settings page (and device-specific
 * connection pages reached from the Bluetooth settings page)
 */
public interface IAutoBluetoothSettingsHelper extends IAppHelper, Scrollable {

    /**
     * Setup Expectations: The bluetooth settings view is open, and the looked-for device is present
     * under "paired devices"
     *
     * @param deviceName - The name of the connected device to disconnect
     */
    void pressBluetoothToggleOnDevice(String deviceName);

    /**
     * Opens the device bluetooth view by clicking the device name.
     *
     * <p>Setup Expectations: The bluetooth settings view is open. and the looked-for device is
     * present under "paired devices"
     *
     * @param deviceName - The name of the connected device to disconnect
     */
    void pressDevice(String deviceName);

    /**
     * Setup Expectations: A connected device bluetooth view is open ('level two')
     *
     * @return - The exact text displayed as the device's connection status
     */
    String getDeviceSummary();

    /**
     * Setup Expectations: A connected device bluetooth view is open ('level two')
     *
     * @return - Whether the device shows that it is connected.
     */
    boolean isConnected();

    /**
     * Presses the 'forget' button.
     *
     * <p>Setup Expectations: A connected device bluetooth view is open ('level two') and the
     * 'Forget' button is visible.
     */
    void pressForget();

    /**
     * Presses the 'Connect' or 'Disconnect' toggle button
     *
     * <p>Setup Expectations: A connected device bluetooth view is open ('level two')
     */
    void pressConnectionToggle();

    /**
     * Presses the back button to return from a device's connection page to the Bluetooth settings
     * page
     *
     * <p>Setup Expectations: A connected device bluetooth view is open ('level two')
     */
    void goBackToBluetoothSettings();
}
