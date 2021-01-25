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
import android.os.SystemClock;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import androidx.test.InstrumentationRegistry;

import java.util.regex.Pattern;

public class AutoUtility {
    private static final String CARLAUNCHER_PACKAGE = "com.android.car.carlauncher";
    private static final String DO_NOT_SHOW_AGAIN = "DO NOT SHOW AGAIN";

    private static final BySelector DISMISS_INITIAL_USER_NOTICE_SELECTOR =
            By.clickable(true).text(Pattern.compile(DO_NOT_SHOW_AGAIN, Pattern.CASE_INSENSITIVE));

    private static final int UI_WAIT_TIME = 5000;
    private static final int UI_WAIT_TIME_TWENTY_SEC = 20000;

    private static Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private static UiDevice mDevice = UiDevice.getInstance(mInstrumentation);

    public static final String DISPLAY_SETTINGS = "DISPLAY";
    public static final String SOUND_SETTINGS = "SOUND";
    public static final String NETWORK_AND_INTERNET_SETTINGS = "NETWORK_AND_INTERNET";
    public static final String BLUETOOTH_SETTINGS = "BLUETOOTH";
    public static final String APPS_AND_NOTIFICATIONS_SETTINGS = "APPS_AND_NOTIFICATIONS";
    public static final String DATE_AND_TIME_SETTINGS = "DATE_AND_TIME";
    public static final String SYSTEM_SETTINGS = "SYSTEM";
    public static final String USER_SETTINGS = "USERS";
    public static final String ACCOUNT_SETTINGS = "ACCOUNTS";
    public static final String SECURITY_SETTINGS = "SECURITY";

    public static void exitSuw() {
        mDevice.pressHome();
        dismissInitialUserNoticeUiServiceDialog();
        SystemClock.sleep(UI_WAIT_TIME_TWENTY_SEC);
    }

    private static void dismissInitialUserNoticeUiServiceDialog() {
        mDevice.pressHome();
        UiObject2 object =
                mDevice.wait(Until.findObject(DISMISS_INITIAL_USER_NOTICE_SELECTOR), UI_WAIT_TIME);
        if (object != null) {
            object.click();
        }
    }
}
