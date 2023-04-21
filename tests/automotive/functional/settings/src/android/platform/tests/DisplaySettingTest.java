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

package android.platform.tests;

import static junit.framework.Assert.assertTrue;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.SettingsConstants;

import org.junit.Test;

public class DisplaySettingTest {

    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    private static final String SCREEN_BRIGHTNESS = "screen_brightness";
    private static final int STARTING_SCREEN_BRIGHTNESS_VALUE = 10;

    public DisplaySettingTest() throws Exception {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Test
    public void testBrightnessIncrease() {
        mSettingHelper.get().openSetting(SettingsConstants.DISPLAY_SETTINGS);
        assertTrue(
                "Display Setting did not open",
                mSettingHelper.get().checkMenuExists("Brightness level"));
        mSettingHelper.get().setValue(SCREEN_BRIGHTNESS, STARTING_SCREEN_BRIGHTNESS_VALUE);

        // Increase the screen brightness
        mSettingHelper.get().changeSeekbarLevel(0, IAutoSettingHelper.ChangeType.INCREASE);

        // Verify that the screen brightness has changed.
        int newBrightnessLevel = mSettingHelper.get().getValue(SCREEN_BRIGHTNESS);
        assertTrue(
                "Brightness was not increased (from "
                        + STARTING_SCREEN_BRIGHTNESS_VALUE
                        + " to "
                        + newBrightnessLevel
                        + ")",
                newBrightnessLevel > STARTING_SCREEN_BRIGHTNESS_VALUE);
    }
}
