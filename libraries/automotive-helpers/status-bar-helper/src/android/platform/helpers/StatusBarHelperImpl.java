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

import android.app.Instrumentation;
import android.platform.helpers.exceptions.UnknownUiException;

import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

/** Helper file for status bar tests */
public class StatusBarHelperImpl extends AbstractStandardAppHelper implements IAutoStatusBarHelper {

    public StatusBarHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.HOME_PACKAGE);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }

    /** {@inheritDoc} */
    @Override
    public void openBluetoothPalette() {
        BySelector bluetoothButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_BUTTON);
        UiObject2 bluetoothButtonLink = getSpectatioUiUtil().findUiObject(bluetoothButtonSelector);
        validateUiObject(
                bluetoothButtonLink, AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_BUTTON);
        getSpectatioUiUtil().clickAndWait(bluetoothButtonLink);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasBluetoothSwitch() {
        BySelector bluetoothSwitchSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON);
        return (getSpectatioUiUtil().hasUiElement(bluetoothSwitchSelector));
    }

    /** {@inheritDoc} */
    @Override
    public void openBluetoothSwitch() {
        BySelector bluetoothButtonSwitchSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON);
        UiObject2 bluetoothButtonSwitchLink =
                getSpectatioUiUtil().findUiObject(bluetoothButtonSwitchSelector);
        validateUiObject(
                bluetoothButtonSwitchLink,
                AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON);
        getSpectatioUiUtil().clickAndWait(bluetoothButtonSwitchLink);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasToggleOnMessage() {
        BySelector toggleOnMessageSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON_MESSAGE);
        return (getSpectatioUiUtil().hasUiElement(toggleOnMessageSelector));
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasToggleOffMessage() {
        BySelector toggleOffMessageSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_OFF_MESSAGE);
        return (getSpectatioUiUtil().hasUiElement(toggleOffMessageSelector));
    }

    /** {@inheritDoc} */
    @Override
    public void openBluetoothSettings() {
        BySelector openBluetoothSettingsSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_SETTINGS);
        UiObject2 bluetoothSettingsLink =
                getSpectatioUiUtil().findUiObject(openBluetoothSettingsSelector);
        validateUiObject(
                bluetoothSettingsLink, AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_SETTINGS);
        getSpectatioUiUtil().clickAndWait(bluetoothSettingsLink);
        getSpectatioUiUtil().wait5Seconds();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasBluetoothSettingsPageTitle() {
        BySelector bluetoothSettingsPageTitleSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_SETTINGS_PAGE_TITLE);
        return (getSpectatioUiUtil().hasUiElement(bluetoothSettingsPageTitleSelector));
    }

    @Override
    public boolean isBluetoothOn() {
        BySelector enableOptionSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON);
        UiObject2 enableOption = getSpectatioUiUtil().findUiObject(enableOptionSelector);
        validateUiObject(enableOption, AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON);
        return enableOption.isChecked();
    }

    /** {@inheritDoc} */
    @Override
    public void turnOnOffBluetooth(boolean onOff) {
        boolean isOn = isBluetoothOn();
        if (isOn != onOff) {
            BySelector enableOptionSelector =
                    getUiElementFromConfig(
                            AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON);
            UiObject2 enableOption = getSpectatioUiUtil().findUiObject(enableOptionSelector);
            validateUiObject(
                    enableOption, AutomotiveConfigConstants.STATUS_BAR_BLUETOOTH_TOGGLE_ON);
            getSpectatioUiUtil().clickAndWait(enableOption);
        } else {
            throw new RuntimeException(
                    "Bluetooth enabled state is already " + (onOff ? "on" : "off"));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void open() {
        getSpectatioUiUtil().pressHome();
        getSpectatioUiUtil().waitForIdle();
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }
}
