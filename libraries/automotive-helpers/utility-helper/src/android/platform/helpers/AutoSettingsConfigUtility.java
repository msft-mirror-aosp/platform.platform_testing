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

import java.util.HashMap;
import java.util.Map;

/** Configuration for Settings Application */
public class AutoSettingsConfigUtility implements IAutoConfigUtility {
    private static final String LOG_TAG = AutoSettingsConfigUtility.class.getSimpleName();

    // Settings Config
    private static final String SETTINGS_TITLE_TEXT = "Settings";
    private static final String SETTING_APP_PACKAGE = "com.android.car.settings";
    private static final String SETTING_RRO_PACKAGE = "com.android.car.settings.googlecarui.rro";
    private static final String SETTING_INTELLIGENCE_PACKAGE = "com.android.settings.intelligence";
    private static final String PERMISSIONS_PACKAGE = "com.android.permissioncontroller";
    private static final String OPEN_SETTINGS_COMMAND = "am start -a android.settings.SETTINGS";
    private static final String OPEN_QUICK_SETTINGS_COMMAND =
            "am start -n "
                    + "com.android.car.settings/"
                    + "com.android.car.settings.common.CarSettingActivities$QuickSettingActivity";

    // Config Maps
    private Map<String, String[]> mSettingsOptionsMap;
    private Map<String, String[]> mSettingsPathMap;
    private Map<String, AutoConfiguration> mSettingsConfigMap;

    private static AutoSettingsConfigUtility sAutoSettingsConfigInstance = null;

    private AutoSettingsConfigUtility() {
        // Initialize Settings Config Maps
        mSettingsOptionsMap = new HashMap<String, String[]>();
        mSettingsPathMap = new HashMap<String, String[]>();
        mSettingsConfigMap = new HashMap<String, AutoConfiguration>();
    }

    /** Get instance of Auto Settings Utility */
    public static IAutoConfigUtility getInstance() {
        if (sAutoSettingsConfigInstance == null) {
            sAutoSettingsConfigInstance = new AutoSettingsConfigUtility();
        }
        return sAutoSettingsConfigInstance;
    }

    /**
     * Get Path for given Setting
     *
     * @param menu Setting Name
     */
    public String[] getPath(String menu) {
        String[] settingPath = mSettingsPathMap.get(menu);
        if (settingPath == null) {
            settingPath = new String[] {menu};
        }
        return settingPath;
    }

    /**
     * Add path for given Setting
     *
     * @param menu Setting Name
     * @param path Path
     */
    public void addPath(String menu, String[] path) {
        mSettingsPathMap.put(menu, path);
    }

    /**
     * Get available menu options for given Setting
     *
     * @param menu Setting Name
     */
    public String[] getAvailableOptions(String menu) {
        return mSettingsOptionsMap.get(menu);
    }

    /**
     * Add available menu options for given Setting
     *
     * @param menu Setting Name
     * @param options Available Options
     */
    public void addAvailableOptions(String menu, String[] options) {
        mSettingsOptionsMap.put(menu, options);
    }

    /**
     * Get Setting Config Resource
     *
     * @param configName Configuration Name
     * @param resourceName Resource Name
     */
    public AutoConfigResource getResourceConfiguration(String configName, String resourceName) {
        if (mSettingsConfigMap.get(configName) == null) {
            // Unknown Configuration
            return null;
        }
        return mSettingsConfigMap.get(configName).getResource(resourceName);
    }

    /**
     * Validate Setting Configuration
     *
     * @param configName Configuration Name
     */
    public boolean isValidConfiguration(String configName) {
        return (mSettingsConfigMap.get(configName) != null);
    }

    /**
     * Validate Setting Configuration Resource
     *
     * @param configName Configuration Name
     * @param resourceName Resource Name
     */
    public boolean isValidResource(String configName, String resourceName) {
        if (mSettingsConfigMap.get(configName) == null) {
            return false;
        }
        return (mSettingsConfigMap.get(configName).getResource(resourceName) != null);
    }

    /**
     * Validate Setting Options
     *
     * @param menu Setting Name
     */
    public boolean isValidOption(String menu) {
        return (mSettingsOptionsMap.get(menu) != null);
    }

    /**
     * Validate Setting Path
     *
     * @param menu Setting Name
     */
    public boolean isValidPath(String menu) {
        return (mSettingsPathMap.get(menu) != null);
    }

    /** Load default configuration for Settings application */
    public void loadDefaultConfig(Map<String, String> mApplicationConfigMap) {
        // Default Settings Application Config
        loadDefaultSettingsAppConfig(mApplicationConfigMap);

        // Default Setting Options
        loadDefaultSettingOptions(mSettingsOptionsMap);

        // Default Setting Paths
        loadDefaultSettingPaths(mSettingsPathMap);

        // Default Setting Resource Config
        loadDefaultSettingResourceConfig(mSettingsConfigMap);
    }

    private void loadDefaultSettingsAppConfig(Map<String, String> mApplicationConfigMap) {
        // Add default settings title text
        mApplicationConfigMap.put(AutoConfigConstants.SETTINGS_TITLE_TEXT, SETTINGS_TITLE_TEXT);
        // Add default settings package
        mApplicationConfigMap.put(AutoConfigConstants.SETTINGS_PACKAGE, SETTING_APP_PACKAGE);
        // Add default settings rro package
        mApplicationConfigMap.put(AutoConfigConstants.SETTINGS_RRO_PACKAGE, SETTING_RRO_PACKAGE);
        // Add default open settings (full settings) command
        mApplicationConfigMap.put(AutoConfigConstants.OPEN_SETTINGS_COMMAND, OPEN_SETTINGS_COMMAND);
        // Add default open quick settings command
        mApplicationConfigMap.put(
                AutoConfigConstants.OPEN_QUICK_SETTINGS_COMMAND, OPEN_QUICK_SETTINGS_COMMAND);
        // Add default settings spli screen UI config
        mApplicationConfigMap.put(AutoConfigConstants.SPLIT_SCREEN_UI, "TRUE");
    }

    private void loadDefaultSettingOptions(Map<String, String[]> mSettingsOptionsMap) {
        mSettingsOptionsMap.put(
                AutoConfigConstants.DISPLAY_SETTINGS, new String[] {"Brightness level"});
        mSettingsOptionsMap.put(
                AutoConfigConstants.SOUND_SETTINGS, new String[] {"Media volume", "Alarm volume"});
        mSettingsOptionsMap.put(AutoConfigConstants.NETWORK_AND_INTERNET_SETTINGS, new String[] {});
        mSettingsOptionsMap.put(AutoConfigConstants.BLUETOOTH_SETTINGS, new String[] {});
        mSettingsOptionsMap.put(
                AutoConfigConstants.APPS_AND_NOTIFICATIONS_SETTINGS, new String[] {});
        mSettingsOptionsMap.put(
                AutoConfigConstants.DATE_AND_TIME_SETTINGS,
                new String[] {"Automatic date & time", "Automatic time zone"});
        mSettingsOptionsMap.put(AutoConfigConstants.USER_SETTINGS, new String[] {"Guest"});
        mSettingsOptionsMap.put(
                AutoConfigConstants.ACCOUNT_SETTINGS, new String[] {"Automatically sync data"});
        mSettingsOptionsMap.put(
                AutoConfigConstants.SYSTEM_SETTINGS, new String[] {"About", "Legal information"});
        mSettingsOptionsMap.put(AutoConfigConstants.SECURITY_SETTINGS, new String[] {});
    }

    private void loadDefaultSettingPaths(Map<String, String[]> mSettingsPathMap) {
        mSettingsPathMap.put(AutoConfigConstants.DISPLAY_SETTINGS, new String[] {"Display"});
        mSettingsPathMap.put(AutoConfigConstants.SOUND_SETTINGS, new String[] {"Sound"});
        mSettingsPathMap.put(
                AutoConfigConstants.NETWORK_AND_INTERNET_SETTINGS,
                new String[] {"Network & internet"});
        mSettingsPathMap.put(AutoConfigConstants.BLUETOOTH_SETTINGS, new String[] {"Bluetooth"});
        mSettingsPathMap.put(
                AutoConfigConstants.APPS_AND_NOTIFICATIONS_SETTINGS,
                new String[] {"Apps & notifications"});
        mSettingsPathMap.put(
                AutoConfigConstants.DATE_AND_TIME_SETTINGS, new String[] {"Date & time"});
        mSettingsPathMap.put(AutoConfigConstants.USER_SETTINGS, new String[] {"Users"});
        mSettingsPathMap.put(AutoConfigConstants.ACCOUNT_SETTINGS, new String[] {"Accounts"});
        mSettingsPathMap.put(AutoConfigConstants.SYSTEM_SETTINGS, new String[] {"System"});
        mSettingsPathMap.put(AutoConfigConstants.SECURITY_SETTINGS, new String[] {"Security"});
    }

    private void loadDefaultSettingResourceConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        // Full Settings Config
        loadDefaultFullSettingsConfig(mSettingsConfigMap);

        // Quick Settings Config
        loadDefaultQuickSettingsConfig(mSettingsConfigMap);

        // Display Settings Config
        loadDefaultDisplaySettingsConfig(mSettingsConfigMap);

        // Sound Settings Config
        loadDefaultSoundSettingsConfig(mSettingsConfigMap);

        // Network And Internet Settings Config
        loadDefaultNetworkSettingsConfig(mSettingsConfigMap);

        // Bluetooth Settings Config
        loadDefaultBluetoothSettingsConfig(mSettingsConfigMap);

        // App and Notifications Settings Config
        loadDefaultAppAndNotificationsSettingsConfig(mSettingsConfigMap);

        // Date and Time Settings Config
        loadDefaultDateAndTimeSettingsConfig(mSettingsConfigMap);

        // System Settings Config
        loadDefaultSystemSettingsConfig(mSettingsConfigMap);

        // Users Settings Config
        loadDefaultUserSettingsConfig(mSettingsConfigMap);

        // Account Settings Config
        loadDefaultAccountSettingsConfig(mSettingsConfigMap);

        // Security Settings Config
        loadDefaultSecuritySettingsConfig(mSettingsConfigMap);
    }

    private void loadDefaultFullSettingsConfig(Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration fullSettingsConfiguration = new AutoConfiguration();
        fullSettingsConfiguration.addResource(
                AutoConfigConstants.PAGE_TITLE,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID,
                        "car_ui_toolbar_title",
                        SETTING_APP_PACKAGE));
        fullSettingsConfiguration.addResource(
                AutoConfigConstants.SEARCH,
                new AutoConfigResource(AutoConfigConstants.DESCRIPTION, "Search"));
        fullSettingsConfiguration.addResource(
                AutoConfigConstants.SEARCH_BOX,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID,
                        "car_ui_toolbar_search_bar",
                        SETTING_INTELLIGENCE_PACKAGE));
        fullSettingsConfiguration.addResource(
                AutoConfigConstants.SEARCH_RESULTS,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID,
                        "recycler_view",
                        SETTING_INTELLIGENCE_PACKAGE));
        mSettingsConfigMap.put(AutoConfigConstants.FULL_SETTINGS, fullSettingsConfiguration);
    }

    private void loadDefaultQuickSettingsConfig(Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration quickSettingsConfiguration = new AutoConfiguration();
        quickSettingsConfiguration.addResource(
                AutoConfigConstants.OPEN_MORE_SETTINGS,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID,
                        "toolbar_menu_item_1",
                        SETTING_APP_PACKAGE));
        quickSettingsConfiguration.addResource(
                AutoConfigConstants.NIGHT_MODE,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Night mode"));
        mSettingsConfigMap.put(AutoConfigConstants.QUICK_SETTINGS, quickSettingsConfiguration);
    }

    private void loadDefaultDisplaySettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration displaySettingsConfiguration = new AutoConfiguration();
        displaySettingsConfiguration.addResource(
                AutoConfigConstants.BRIGHTNESS_LEVEL,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID, "seekbar", SETTING_APP_PACKAGE));
        mSettingsConfigMap.put(AutoConfigConstants.DISPLAY_SETTINGS, displaySettingsConfiguration);
    }

    private void loadDefaultSoundSettingsConfig(Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration soundSettingsConfiguration = new AutoConfiguration();
        mSettingsConfigMap.put(AutoConfigConstants.SOUND_SETTINGS, soundSettingsConfiguration);
    }

    private void loadDefaultNetworkSettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration networkSettingsConfiguration = new AutoConfiguration();
        networkSettingsConfiguration.addResource(
                AutoConfigConstants.TOGGLE_WIFI,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID,
                        "action_widget_container",
                        SETTING_APP_PACKAGE));
        networkSettingsConfiguration.addResource(
                AutoConfigConstants.TOGGLE_HOTSPOT,
                new AutoConfigResource(AutoConfigConstants.DESCRIPTION, "Hotspot toggle switch"));
        mSettingsConfigMap.put(
                AutoConfigConstants.NETWORK_AND_INTERNET_SETTINGS, networkSettingsConfiguration);
    }

    private void loadDefaultBluetoothSettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration bluetoothSettingsConfiguration = new AutoConfiguration();
        bluetoothSettingsConfiguration.addResource(
                AutoConfigConstants.TOGGLE_BLUETOOTH,
                new AutoConfigResource(AutoConfigConstants.DESCRIPTION, "Bluetooth toggle switch"));
        mSettingsConfigMap.put(
                AutoConfigConstants.BLUETOOTH_SETTINGS, bluetoothSettingsConfiguration);
    }

    private void loadDefaultAppAndNotificationsSettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration appsAndNotificationsSettingsConfiguration = new AutoConfiguration();
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.PERMISSIONS_PAGE_TITLE,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID,
                        "car_ui_toolbar_title",
                        PERMISSIONS_PACKAGE));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.SHOW_ALL_APPS,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Show all apps"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.ENABLE_DISABLE_BUTTON,
                new AutoConfigResource(
                        AutoConfigConstants.RESOURCE_ID, "button1Text", SETTING_APP_PACKAGE));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.DISABLE_BUTTON_TEXT,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Disable"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.ENABLE_BUTTON_TEXT,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Enable"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.DISABLE_APP_BUTTON,
                new AutoConfigResource(AutoConfigConstants.TEXT, "DISABLE APP"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.FORCE_STOP_BUTTON,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Force stop"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.OK_BUTTON,
                new AutoConfigResource(AutoConfigConstants.TEXT, "OK"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.PERMISSIONS_MENU,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Permissions?"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.ALLOW_BUTTON,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Allow"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.DENY_BUTTON,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Deny"));
        appsAndNotificationsSettingsConfiguration.addResource(
                AutoConfigConstants.DENY_ANYWAY_BUTTON,
                new AutoConfigResource(AutoConfigConstants.TEXT, "Deny anyway"));
        mSettingsConfigMap.put(
                AutoConfigConstants.APPS_AND_NOTIFICATIONS_SETTINGS,
                appsAndNotificationsSettingsConfiguration);
    }

    private void loadDefaultDateAndTimeSettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration dateAndTimeSettingsConfiguration = new AutoConfiguration();
        mSettingsConfigMap.put(
                AutoConfigConstants.DATE_AND_TIME_SETTINGS, dateAndTimeSettingsConfiguration);
    }

    private void loadDefaultSystemSettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration systemSettingsConfiguration = new AutoConfiguration();
        mSettingsConfigMap.put(AutoConfigConstants.SYSTEM_SETTINGS, systemSettingsConfiguration);
    }

    private void loadDefaultUserSettingsConfig(Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration userSettingsConfiguration = new AutoConfiguration();
        mSettingsConfigMap.put(AutoConfigConstants.USER_SETTINGS, userSettingsConfiguration);
    }

    private void loadDefaultAccountSettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration accountSettingsConfiguration = new AutoConfiguration();
        mSettingsConfigMap.put(AutoConfigConstants.ACCOUNT_SETTINGS, accountSettingsConfiguration);
    }

    private void loadDefaultSecuritySettingsConfig(
            Map<String, AutoConfiguration> mSettingsConfigMap) {
        AutoConfiguration securitySettingsConfiguration = new AutoConfiguration();
        mSettingsConfigMap.put(
                AutoConfigConstants.SECURITY_SETTINGS, securitySettingsConfiguration);
    }
}
