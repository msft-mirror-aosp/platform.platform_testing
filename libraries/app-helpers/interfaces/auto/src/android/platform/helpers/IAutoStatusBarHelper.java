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
}
