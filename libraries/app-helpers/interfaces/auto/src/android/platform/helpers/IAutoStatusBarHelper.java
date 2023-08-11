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

/** Interface file for status bar tests */
public interface IAutoStatusBarHelper extends IAppHelper {

    /** Opens bluetooth palette. */
    void openBluetoothPalette();

    /**
     * Setup expectations: Should be on home screen.
     *
     * <p>Checks if bluetooth switch is present.
     */
    boolean hasBluetoothSwitch();

    /** Opens bluetooth switch. */
    void openBluetoothSwitch();

    /**
     * Setup expectations: Should be on home screen.
     *
     * <p>Checks if bluetooth on message is present.
     */
    boolean hasToggleOnMessage();

    /**
     * Setup expectations: Should be on home screen.
     *
     * <p>Checks if bluetooth off message is present.
     */
    boolean hasToggleOffMessage();

    /** Opens bluetooth settings. */
    void openBluetoothSettings();

    /**
     * Setup expectations: Should be on home screen.
     *
     * <p>Checks if bluetooth settings page title is present
     */
    boolean hasBluetoothSettingsPageTitle();

    /**
     * Setup expectations: Should be on home screen.
     *
     * <p>Checks if bluetooth enabled or not
     */
    boolean isBluetoothOn();

    /** Bluetooth switch button. */
    void turnOnOffBluetooth(boolean onOff);

    /**
     * Setup expectations: Open Bluetooth Palette
     *
     * <p>This method clicks bluetooth button</>
     */
    void clickBluetoothButton();

    /**
     * Setup expectations: Open Bluetooth Palette
     *
     * <p>This method checks bluetooth connected text</>
     */
    boolean isBluetoothConnected();

    /**
     * Setup expectations: Verify Bluetooth Button
     *
     * <p>This method verifies bluetooth button from bluetooth palette</>
     */
    boolean verifyBluetooth();

    /**
     * Setup expectations: Verify Phone Button
     *
     * <p>This method verifies phone button from bluetooth palette</>
     */
    boolean verifyPhone();

    /**
     * Setup expectations: Verify Media Button
     *
     * <p>This method verifies media button from bluetooth palette</>
     */
    boolean verifyMedia();

    /**
     * Setup expectations: Verify the Device name
     *
     * <p>This method verifies the connected device name</>
     */
    boolean verifyDeviceName();

    /**
     * Setup expectations: Verify the Disabled Bluetooth profile
     *
     * <p>This method verifies the disabled bluetooth profile</>
     */
    boolean verifyDisabledBluetoothProfile();

    /**
     * Setup expectations: Verify the Disabled Phone profile
     *
     * <p>This method verifies the disabled phone profile</>
     */
    boolean verifyDisabledPhoneProfile();

    /**
     * Setup expectations: Verify the Disabled Media profile
     *
     * <p>This method verifies the disabled Media profile</>
     */
    boolean verifyDisabledMediaProfile();

    /**
     * Setup expectations: Status bar Network palette is open.
     *
     * <p>Open status bar network palette.
     */
    void openNetworkPalette();

    /**
     * Setup expectations: Toggle ON/OFF
     *
     * <p>Click on toggle button from status bar palette
     *
     * @param name options in the palette
     */
    void networkPaletteToggleOnOff(String name);

    /**
     * Setup expectations: Hotspot Name
     *
     * <p>Check if the Hotspot name is displayed
     */
    boolean isHotspotNameDisplayed();

    /**
     * Setup expectations: Status of Toggle ON/OFF
     *
     * <p>Checks if toggle is enabled on status bar palette
     *
     * @param target options in the palette
     */
    boolean isNetworkSwitchEnabled(String target);

    /**
     * Setup expectations: Wi-Fi Name
     *
     * <p>Check if the Wifi name is displayed
     */
    boolean isWifiNameDisplayed();

    /**
     * Setup expectations: Network & Internet
     *
     * <p>click on forget button
     */
    void forgetWifi();
}
