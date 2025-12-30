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

public interface IAutoHvacHelper extends IAppHelper {
    void showHvac();
    void hideHvac();

    boolean checkAcToggle();
    void clickAcToggle();

    boolean checkAutoModeToggle();
    void clickAutoModeToggle();

    boolean checkRecirculationToggle();
    void clickRecirculationToggle();

    boolean checkFrontDefrostToggle();
    void clickFrontDefrostToggle();
    boolean checkRearDefrostToggle();
    void clickRearDefrostToggle();

    void clickDriverSeatTemperature();

    void driverIncreaseTemperature();
    void driverDecreaseTemperature();
    void passengerIncreaseTemperature();
    void passengerDecreaseTemperature();

    void setFanSpeed(int fanSpeed);
}
