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

package android.platform.helpers;

import android.app.Instrumentation;

import androidx.test.uiautomator.UiObject2;

import com.google.common.collect.ImmutableMap;

public class HvacHelperImpl extends AbstractStandardAppHelper implements IAutoHvacHelper {
    private static final String LOG_TAG = HvacHelperImpl.class.getSimpleName();

    /*
     * In this map, keys are fan speed property values, and values are the resource IDs for the
     * corresponding UI elements.  Property value 1 turns the fan off and has a UI element to match.
     * Property values 2-5 activate the fan 1-4 UI elements.  Property value 6 activates "max".
     */
    private static final ImmutableMap<Integer, String> FAN_SPEED_UI =
            ImmutableMap.<Integer, String>builder()
                .put(1, AutomotiveConfigConstants.HVAC_FAN_OFF)
                .put(2, AutomotiveConfigConstants.HVAC_FAN_1)
                .put(3, AutomotiveConfigConstants.HVAC_FAN_2)
                .put(4, AutomotiveConfigConstants.HVAC_FAN_3)
                .put(5, AutomotiveConfigConstants.HVAC_FAN_4)
                .put(6, AutomotiveConfigConstants.HVAC_FAN_MAX)
                .build();

    public HvacHelperImpl(Instrumentation instr) {
        super(instr);
    }

    private void showHideHvac(boolean show) {
        UiObject2 temperatureBar = getSpectatioUiUtil().findUiObject(
                getUiElementFromConfig(AutomotiveConfigConstants.HVAC_TEMPERATURE_BAR)
        );
        if ((temperatureBar == null) == show) {
            clickConfigObject(AutomotiveConfigConstants.HOME_TEMPERATURE_BUTTON);
        }
    }

    @Override
    public void showHvac() {
        showHideHvac(true);
    }

    @Override
    public void hideHvac() {
        showHideHvac(false);
    }

    @Override
    public boolean checkAcToggle() {
        return checkSelected(AutomotiveConfigConstants.HVAC_AC_TOGGLE);
    }

    @Override
    public void clickAcToggle() {
        clickConfigObject(AutomotiveConfigConstants.HVAC_AC_TOGGLE);
    }

    @Override
    public boolean checkAutoModeToggle() {
        return checkSelected(AutomotiveConfigConstants.HVAC_AUTO_MODE_TOGGLE);
    }

    @Override
    public void clickAutoModeToggle() {
        clickConfigObject(AutomotiveConfigConstants.HVAC_AUTO_MODE_TOGGLE);
    }

    @Override
    public boolean checkRecirculationToggle() {
        return checkSelected(AutomotiveConfigConstants.HVAC_RECIRCULATION_TOGGLE);
    }

    @Override
    public void clickRecirculationToggle() {
        clickConfigObject(AutomotiveConfigConstants.HVAC_RECIRCULATION_TOGGLE);
    }

    @Override
    public boolean checkFrontDefrostToggle() {
        return checkSelected(AutomotiveConfigConstants.HVAC_FRONT_DEFROST_TOGGLE);
    }

    @Override
    public void clickFrontDefrostToggle() {
        clickConfigObject(AutomotiveConfigConstants.HVAC_FRONT_DEFROST_TOGGLE);
    }

    @Override
    public boolean checkRearDefrostToggle() {
        return checkSelected(AutomotiveConfigConstants.HVAC_REAR_DEFROST_TOGGLE);
    }

    @Override
    public void clickRearDefrostToggle() {
        clickConfigObject(AutomotiveConfigConstants.HVAC_REAR_DEFROST_TOGGLE);
    }

    @Override
    public void clickDriverSeatTemperature() {
        clickConfigObject(AutomotiveConfigConstants.DRIVER_SEAT_TEMPERATURE_BUTTON);
    }

    @Override
    public void driverIncreaseTemperature() {
        clickConfigObject(AutomotiveConfigConstants.DRIVER_HVAC_INCREASE_BUTTON);
    }

    @Override
    public void driverDecreaseTemperature() {
        clickConfigObject(AutomotiveConfigConstants.DRIVER_HVAC_DECREASE_BUTTON);
    }

    @Override
    public void passengerIncreaseTemperature() {
        clickConfigObject(AutomotiveConfigConstants.PASSENGER_HVAC_INCREASE_BUTTON);
    }

    @Override
    public void passengerDecreaseTemperature() {
        clickConfigObject(AutomotiveConfigConstants.PASSENGER_HVAC_DECREASE_BUTTON);
    }

    @Override
    public void setFanSpeed(int fanSpeed) {
        clickConfigObject(FAN_SPEED_UI.get(fanSpeed));
    }

    private void clickConfigObject(String key) {
        // this really should be more globally available.
        getSpectatioUiUtil()
                .clickAndWait(getSpectatioUiUtil().findUiObject(getUiElementFromConfig(key)));
    }

    private boolean checkSelected(String key) {
        return getSpectatioUiUtil().findUiObject(getUiElementFromConfig(key)).isSelected();
    }

    @Override
    public void dismissInitialDialogs() {
        // nothing to dismiss
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }
}
