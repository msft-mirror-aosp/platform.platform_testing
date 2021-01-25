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

import java.util.Map;
import java.util.HashMap;

public class SettingConstants {

    private static final String LOG_TAG = SettingConstants.class.getSimpleName();
    private static Map<String, String[]> expectedOptions;
    private static Map<String, String[]> availableSettings;

    public SettingConstants() {
        expectedOptions = new HashMap<String, String[]>();
        expectedOptions.put(AutoUtility.DISPLAY_SETTINGS, new String[] {"Brightness level"});
        expectedOptions.put(
                AutoUtility.SOUND_SETTINGS, new String[] {"Media volume", "Alarm volume"});
        expectedOptions.put("App info", new String[] {"Settings", "Phone", "Radio"});
        expectedOptions.put(
                AutoUtility.DATE_AND_TIME_SETTINGS,
                new String[] {"Automatic date & time", "Automatic time zone"});
        expectedOptions.put(AutoUtility.USER_SETTINGS, new String[] {"Guest"});
        expectedOptions.put(AutoUtility.ACCOUNT_SETTINGS, new String[] {"Accounts"});
        expectedOptions.put(
                AutoUtility.SYSTEM_SETTINGS, new String[] {"About", "Legal information"});

        // Available settings and their variations
        availableSettings = new HashMap<String, String[]>();
        availableSettings.put(AutoUtility.DISPLAY_SETTINGS, new String[] {"Display"});
        availableSettings.put(AutoUtility.SOUND_SETTINGS, new String[] {"Sound"});
        availableSettings.put(
                AutoUtility.NETWORK_AND_INTERNET_SETTINGS,
                new String[] {"Network & internet", "Network and Internet"});
        availableSettings.put(AutoUtility.BLUETOOTH_SETTINGS, new String[] {"Bluetooth"});
        availableSettings.put(
                AutoUtility.APPS_AND_NOTIFICATIONS_SETTINGS,
                new String[] {"Apps & notifications", "Apps and notifications"});
        availableSettings.put(AutoUtility.DATE_AND_TIME_SETTINGS, new String[] {"Date & time"});
        availableSettings.put(AutoUtility.SYSTEM_SETTINGS, new String[] {"System"});
        availableSettings.put(AutoUtility.USER_SETTINGS, new String[] {"Users"});
        availableSettings.put(AutoUtility.ACCOUNT_SETTINGS, new String[] {"Accounts"});
        availableSettings.put(AutoUtility.SECURITY_SETTINGS, new String[] {"Security"});
    }

    public static String[] getExpectedOptions(String setting) {
        return expectedOptions.get(setting);
    }

    public static String[] getSettingNameVariations(String setting) {
        String[] settingNameVariations = availableSettings.get(setting);
        if (settingNameVariations == null) {
            settingNameVariations = new String[] {setting};
        }
        return settingNameVariations;
    }
}
