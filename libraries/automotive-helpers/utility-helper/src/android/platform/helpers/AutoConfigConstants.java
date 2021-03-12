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

// Automotive Configuration Constants
public class AutoConfigConstants {
    // Resource Types
    public static final String RESOURCE_ID = "RESOURCE_ID";
    public static final String TEXT = "TEXT";
    public static final String DESCRIPTION = "DESCRIPTION";

    // SETTINGS
    public static final String SETTINGS = "SETTINGS";
    public static final String SETTINGS_TITLE_TEXT = "SETTINGS_TITLE_TEXT";
    public static final String SETTINGS_PACKAGE = "SETTINGS_PACKAGE";
    public static final String SETTINGS_RRO_PACKAGE = "SETTINGS_RRO_PACKAGE";
    public static final String OPEN_SETTINGS_COMMAND = "OPEN_SETTINGS_COMMAND";
    public static final String OPEN_QUICK_SETTINGS_COMMAND = "OPEN_QUICK_SETTINGS_COMMAND";
    public static final String SPLIT_SCREEN_UI = "SPLIT_SCREEN_UI";
    // Full Settings
    public static final String FULL_SETTINGS = "FULL_SETTINGS";
    public static final String PAGE_TITLE = "PAGE_TITLE";
    public static final String SEARCH = "SEARCH";
    public static final String SEARCH_BOX = "SEARCH_BOX";
    public static final String SEARCH_RESULTS = "SEARCH_RESULTS";
    // Quick Settings
    public static final String QUICK_SETTINGS = "QUICK_SETTINGS";
    public static final String OPEN_MORE_SETTINGS = "OPEN_MORE_SETTINGS";
    public static final String NIGHT_MODE = "NIGHT_MODE";
    // Display
    public static final String DISPLAY_SETTINGS = "DISPLAY";
    public static final String BRIGHTNESS_LEVEL = "BRIGHTNESS_LEVEL";
    // Sound
    public static final String SOUND_SETTINGS = "SOUND";
    // Network and Internet
    public static final String NETWORK_AND_INTERNET_SETTINGS = "NETWORK_AND_INTERNET";
    public static final String TOGGLE_WIFI = "TOGGLE_WIFI";
    public static final String TOGGLE_HOTSPOT = "TOGGLE_HOTSPOT";
    // Bluetooth
    public static final String BLUETOOTH_SETTINGS = "BLUETOOTH";
    public static final String TOGGLE_BLUETOOTH = "TOGGLE_BLUETOOTH";
    // Apps and Notification
    public static final String APPS_AND_NOTIFICATIONS_SETTINGS = "APPS_AND_NOTIFICATIONS";
    public static final String PERMISSIONS_PAGE_TITLE = "PERMISSIONS_PAGE_TITLE";
    public static final String SHOW_ALL_APPS = "SHOW_ALL_APPS";
    public static final String ENABLE_DISABLE_BUTTON = "ENABLE_DISABLE_BUTTON";
    public static final String DISABLE_BUTTON_TEXT = "DISABLE_BUTTON_TEXT";
    public static final String ENABLE_BUTTON_TEXT = "ENABLE_BUTTON_TEXT";
    public static final String DISABLE_APP_BUTTON = "DISABLE_APP_BUTTON";
    public static final String FORCE_STOP_BUTTON = "FORCE_STOP_BUTTON";
    public static final String OK_BUTTON = "OK_BUTTON";
    public static final String PERMISSIONS_MENU = "PERMISSIONS_MENU";
    public static final String ALLOW_BUTTON = "ALLOW_BUTTON";
    public static final String DENY_BUTTON = "DENY_BUTTON";
    public static final String DENY_ANYWAY_BUTTON = "DENY_ANYWAY_BUTTON";
    // Date and Time
    public static final String DATE_AND_TIME_SETTINGS = "DATE_AND_TIME";
    // System
    public static final String SYSTEM_SETTINGS = "SYSTEM";
    // Users
    public static final String USER_SETTINGS = "USERS";
    // Accounts
    public static final String ACCOUNT_SETTINGS = "ACCOUNTS";
    // Security
    public static final String SECURITY_SETTINGS = "SECURITY";

    // PHONE
    public static final String PHONE = "PHONE";
    public static final String DIAL_PACKAGE = "DIAL_PACKAGE";
    public static final String OPEN_DIAL_PAD_COMMAND = "OPEN_DIAL_PAD_COMMAND";
    public static final String PHONE_ACTIVITY = "PHONE_ACTIVITY";
    // In Call Screen
    public static final String IN_CALL_VIEW = "IN_CALL_VIEW";
    public static final String DIALED_CONTACT_TITLE = "DIALED_CONTACT_TITLE";
    public static final String DIALED_CONTACT_NUMBER = "DIALED_CONTACT_NUMBER";
    public static final String END_CALL = "END_CALL";
    public static final String MUTE_CALL = "MUTE_CALL";
    public static final String SWITCH_TO_DIAL_PAD = "SWITCH_TO_DIAL_PAD";
    public static final String CHANGE_VOICE_CHANNEL = "CHANGE_VOICE_CHANNEL";
    public static final String VOICE_CHANNEL_CAR = "VOICE_CHANNEL_CAR";
    public static final String VOICE_CHANNEL_PHONE = "VOICE_CHANNEL_PHONE";
    // Dial Pad Screen
    public static final String DIAL_PAD_VIEW = "DIAL_PAD_VIEW";
    public static final String DIAL_PAD_MENU = "DIAL_PAD_MENU";
    public static final String DIAL_PAD_FRAGMENT = "DIAL_PAD_FRAGMENT";
    public static final String DIALED_NUMBER = "DIALED_NUMBER";
    public static final String MAKE_CALL = "MAKE_CALL";
    public static final String DELETE_NUMBER = "DELETE_NUMBER";
    // Contacts Screen
    public static final String CONTACTS_VIEW = "CONTACTS_VIEW";
    public static final String CONTACTS_MENU = "CONTACTS_MENU";
    public static final String CONTACT_INFO = "CALL_HISTORY_INFO";
    public static final String CONTACT_NAME = "CONTACT_NAME";
    public static final String CONTACT_DETAIL = "CONTACT_DETAIL";
    public static final String ADD_CONTACT_TO_FAVORITE = "ADD_CONTACT_TO_FAVORITE";
    public static final String SEARCH_CONTACT = "SEARCH_CONTACT";
    public static final String CONTACT_SEARCH_BAR = "CONTACT_SEARCH_BAR";
    public static final String SEARCH_RESULT = "SEARCH_RESULT";
    public static final String CONTACT_SETTINGS = "CONTACT_SETTINGS";
    public static final String CONTACT_ORDER = "CONTACT_ORDER";
    public static final String SORT_BY_FIRST_NAME = "SORT_BY_FIRST_NAME";
    public static final String SORT_BY_LAST_NAME = "SORT_BY_LAST_NAME";
    public static final String CONTACT_TYPE_WORK = "CONTACT_TYPE_WORK";
    public static final String CONTACT_TYPE_MOBILE = "CONTACT_TYPE_MOBILE";
    public static final String CONTACT_TYPE_HOME = "CONTACT_TYPE_HOME";
    // Call History Screen
    public static final String CALL_HISTORY_VIEW = "CALL_HISTORY_VIEW";
    public static final String CALL_HISTORY_MENU = "CALL_HISTORY_MENU";
    public static final String CALL_HISTORY_INFO = "CALL_HISTORY_INFO";
    // Favorites Screen
    public static final String FAVORITES_VIEW = "FAVORITES_VIEW";
    public static final String FAVORITES_MENU = "FAVORITES_MENU";

    // Notifications
    public static final String NOTIFICATIONS = "NOTIFICATIONS";
    public static final String OPEN_NOTIFICATIONS_COMMAND = "OPEN_NOTIFICATIONS_COMMAND";
    public static final String RECYCLER_VIEW_CLASS = "RECYCLER_VIEW_CLASS";
    // Expanded Notifications Screen
    public static final String EXPANDED_NOTIFICATIONS_SCREEN = "EXPANDED_NOTIFICATIONS_SCREEN";
    public static final String NOTIFICATION_LIST = "NOTIFICATION_LIST";
    public static final String NOTIFICATION_VIEW = "NOTIFICATION_VIEW";
    public static final String CLEAR_ALL_BUTTON = "CLEAR_ALL_BUTTON";
    public static final String STATUS_BAR = "STATUS_BAR";
    public static final String APP_ICON = "APP_ICON";
    public static final String APP_NAME = "APP_NAME";
    public static final String NOTIFICATION_TITLE = "NOTIFICATION_TITLE";
    public static final String NOTIFICATION_BODY = "NOTIFICATION_BODY";
    public static final String CARD_VIEW = "CARD_VIEW";
}
