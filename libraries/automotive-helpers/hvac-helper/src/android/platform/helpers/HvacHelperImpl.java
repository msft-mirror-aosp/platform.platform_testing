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

public class HvacHelperImpl extends AbstractStandardAppHelper implements IAutoHvacHelper {
    private static final String LOG_TAG = HvacHelperImpl.class.getSimpleName();

    public HvacHelperImpl(Instrumentation instr) {
        super(instr);
    }

    @Override
    public void showHideHvac() {
        clickConfigObject(AutomotiveConfigConstants.HOME_TEMPERATURE_BUTTON);
    }

    @Override
    public void clickDriverSeatTemperature() {
        clickConfigObject(AutomotiveConfigConstants.DRIVER_SEAT_TEMPERATURE_BUTTON);
    }

    private void clickConfigObject(String key) {
        // this really should be more globally available.
        getSpectatioUiUtil()
                .clickAndWait(getSpectatioUiUtil().findUiObject(getUiElementFromConfig(key)));
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
