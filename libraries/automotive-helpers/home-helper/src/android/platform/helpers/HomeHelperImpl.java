/*
 * Copyright (C) 2021 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;

public class HomeHelperImpl extends AbstractStandardAppHelper implements IAutoHomeHelper {
    private static final int WAIT_TIME_MS = 30000;

    public HomeHelperImpl(Instrumentation instr) {
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
    public boolean hasBluetoothButton() {
        BySelector bluetoothWidgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_BLUETOOTH_BUTTON);
        return getSpectatioUiUtil().hasUiElement(bluetoothWidgetSelector);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNetworkButton() {
        BySelector networkWidgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_NETWORK_BUTTON);
        return getSpectatioUiUtil().hasUiElement(networkWidgetSelector);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDisplayBrightness() {
        BySelector displayBrightnessWidgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_DISPLAY_BRIGHTNESS_BUTTON);
        return getSpectatioUiUtil().hasUiElement(displayBrightnessWidgetSelector);
    }

    public boolean hasAssistantWidget() {
        BySelector assistantWidgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_TOP_CARD);
        return (getSpectatioUiUtil().hasUiElement(assistantWidgetSelector));
    }

    public boolean hasMediaWidget() {
        BySelector mediaWidgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_BOTTOM_CARD);
        return (getSpectatioUiUtil().hasUiElement(mediaWidgetSelector));
    }

    /** {@inheritDoc} */
    @Override
    public void openBrightnessPalette() {
        BySelector brightnesButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.STATUS_BAR_BRIGHTNESS_BUTTON);
        UiObject2 brightnessButton = getSpectatioUiUtil().findUiObject(brightnesButtonSelector);
        validateUiObject(brightnessButton, AutomotiveConfigConstants.STATUS_BAR_BRIGHTNESS_BUTTON);
        getSpectatioUiUtil().clickAndWait(brightnessButton);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDisplayBrightessPalette() {
        BySelector displaybrightnessPaletteSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.STATUS_BAR_DISPLAY_BRIGHTNESS_PALETTE);
        return (getSpectatioUiUtil().hasUiElement(displaybrightnessPaletteSelector));
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAdaptiveBrightness() {
        BySelector adaptiveBrightnessSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.STATUS_BAR_ADAPTIVE_BRIGHTNESS);
        return (getSpectatioUiUtil().hasUiElement(adaptiveBrightnessSelector));
    }

    @Override
    public void openMediaWidget() {
        getSpectatioUiUtil().pressHome();
        getSpectatioUiUtil().waitForIdle();
        BySelector mediaWidgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_BOTTOM_CARD);
        UiObject2 mediaWidget = getSpectatioUiUtil().findUiObject(mediaWidgetSelector);
        validateUiObject(mediaWidget, AutomotiveConfigConstants.HOME_BOTTOM_CARD);
        getSpectatioUiUtil().clickAndWait(mediaWidget);
    }

    /** {@inheritDoc} */
    @Override
    public String getUserProfileName() {
        getSpectatioUiUtil().waitForIdle();
        getSpectatioUiUtil().waitNSeconds(WAIT_TIME_MS);
        BySelector profileGuestIconWidgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_PROFILE_GUEST_ICON);
        UiObject2 guestIconButtonLink =
                getSpectatioUiUtil().findUiObject(profileGuestIconWidgetSelector);
        validateUiObject(guestIconButtonLink, AutomotiveConfigConstants.HOME_PROFILE_GUEST_ICON);
        String profileText = guestIconButtonLink.getText();
        return profileText;
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

    @Override
    public List<String> getTemperature() {
        getSpectatioUiUtil().pressHome();
        getSpectatioUiUtil().waitForIdle();
        BySelector temperatureSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.HOME_TEMPERATURE_BUTTON);
        List<UiObject2> temperature = getSpectatioUiUtil().findUiObjects(temperatureSelector);
        validateUiObject(temperature.get(0), AutomotiveConfigConstants.HOME_TEMPERATURE_BUTTON);
        validateUiObject(temperature.get(1), AutomotiveConfigConstants.HOME_TEMPERATURE_BUTTON);
        List<String> temperatureText = new ArrayList<>();
        for (UiObject2 uiObject : temperature) {
            temperatureText.add(getSpectatioUiUtil().getTextForUiElement(uiObject));
        }
        return temperatureText;
    }
}
