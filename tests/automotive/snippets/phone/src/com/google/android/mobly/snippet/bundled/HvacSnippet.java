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

package com.google.android.mobly.snippet.bundled;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoHvacHelper;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

public class HvacSnippet implements Snippet {
    private final HelperAccessor<IAutoHvacHelper> mHelper;

    public HvacSnippet() {
        mHelper = new HelperAccessor<>(IAutoHvacHelper.class);
    }

    @Rpc(description = "Attempt to open/close the HVAC panel")
    public void showHideHvac() {
        mHelper.get().showHideHvac();
    }

    @Rpc(description = "Report whether AC is on")
    public boolean checkAcToggle() {
        return mHelper.get().checkAcToggle();
    }

    @Rpc(description = "Click the AC toggle")
    public void clickAcToggle() {
        mHelper.get().clickAcToggle();
    }

    @Rpc(description = "Report whether HVAC auto mode is enabled/disabled")
    public boolean checkAutoModeToggle() {
        return mHelper.get().checkAutoModeToggle();
    }

    @Rpc(description = "Click the auto mode toggle")
    public void clickAutoModeToggle() {
        mHelper.get().clickAutoModeToggle();
    }

    @Rpc(description = "Report whether recirculated air is enabled/disabled")
    public boolean checkRecirculationToggle() {
        return mHelper.get().checkRecirculationToggle();
    }

    @Rpc(description = "Click the recirculated air toggle")
    public void clickRecirculationToggle() {
        mHelper.get().clickRecirculationToggle();
    }

    @Rpc(description = "Report whether front defroster is enabled")
    public boolean checkFrontDefrostToggle() {
        return mHelper.get().checkFrontDefrostToggle();
    }

    @Rpc(description = "Click the front defroster toggle")
    public void clickFrontDefrostToggle() {
        mHelper.get().clickFrontDefrostToggle();
    }

    @Rpc(description = "Report whether rear defroster is enabled")
    public boolean checkRearDefrostToggle() {
        return mHelper.get().checkRearDefrostToggle();
    }

    @Rpc(description = "Click the rear defroster toggle")
    public void clickRearDefrostToggle() {
        mHelper.get().clickRearDefrostToggle();
    }

    @Rpc(description = "Increase the driver HVAC temperature")
    public void driverIncreaseTemperature() {
        mHelper.get().driverIncreaseTemperature();
    }

    @Rpc(description = "Decrease the driver HVAC temperature")
    public void driverDecreaseTemperature() {
        mHelper.get().driverDecreaseTemperature();
    }

    @Rpc(description = "Increase the passenger HVAC temperature")
    public void passengerIncreaseTemperature() {
        mHelper.get().passengerIncreaseTemperature();
    }

    @Rpc(description = "Decrease the passenger HVAC temperature")
    public void passengerDecreaseTemperature() {
        mHelper.get().passengerDecreaseTemperature();
    }

    @Rpc(description = "Click the driver seat temperature icon")
    public void clickDriverSeatTemperature() {
        mHelper.get().clickDriverSeatTemperature();
    }
}
