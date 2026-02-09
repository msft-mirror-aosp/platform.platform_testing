/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.Manifest;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.pm.UserInfo;
import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoLockScreenHelper;
import android.platform.helpers.IAutoPrivacySettingsHelper;
import android.platform.helpers.IAutoSecuritySettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.scenario.multiuser.MultiUserConstants;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** This test will create user through API and test lock screen on new user */
@RunWith(AndroidJUnit4.class)
public class MultiUserLockScreenTest {
    @Rule
    public final AdoptShellPermissionsRule mShellPermissionsRule =
            new AdoptShellPermissionsRule(
                    InstrumentationRegistry.getInstrumentation().getUiAutomation(),
                    Manifest.permission.CREATE_USERS,
                    Manifest.permission.MANAGE_USERS);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final UiAutomation mUiAutomation = mInstrumentation.getUiAutomation();

    private static final String PIN = "1013";
    private static final String USER_NAME = MultiUserConstants.SECONDARY_USER_NAME;
    private static final String DRIVER = AutomotiveConfigConstants.HOME_DRIVER_BUTTON;
    private UserInfo mNewUser;
    private UserInfo mInitialUser;
    private int mTargetUserId;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSecuritySettingsHelper> mSecuritySettingsHelper;
    private HelperAccessor<IAutoPrivacySettingsHelper> mPrivacyHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoLockScreenHelper> mLockScreenHelper;

    private static final String LOG_TAG = MultiUserMicrophoneOnStatusBarTest.class.getSimpleName();

    public MultiUserLockScreenTest() {

        mSecuritySettingsHelper = new HelperAccessor<>(IAutoSecuritySettingsHelper.class);
        mLockScreenHelper = new HelperAccessor<>(IAutoLockScreenHelper.class);
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mPrivacyHelper = new HelperAccessor<>(IAutoPrivacySettingsHelper.class);
    }

    @Before
    public void setLockScreenForProfiles() throws Exception {

        Log.i(LOG_TAG, "ACT: Get Initial User Info");
        mInitialUser = mMultiUserHelper.getCurrentForegroundUserInfo();

        // Set Lock screen for driver
        setLockScreenPin(mInitialUser.id);

        // create new user
        Log.i(LOG_TAG, "Act: Create a new non admin user");
        mTargetUserId = mMultiUserHelper.createUser(USER_NAME, false);
        mNewUser = mMultiUserHelper.getCurrentForegroundUserInfo();

        // Set Lock screen for driver
        setLockScreenPin(mTargetUserId);
    }

    @After
    public void goBackToHomeScreen() {

        Log.i(LOG_TAG, "ACT: Switch to Initial User");
        mUsersHelper.get().switchUsingUserIcon(DRIVER);
        // Clear Security Lock screen pin on Driver Profile
        clearLockScreenPin();
        Log.i(LOG_TAG, "Act: Remove User");
        mMultiUserHelper.removeUser(mNewUser);
        Log.i(LOG_TAG, "Act: Go back to Home Screen");
        mSettingHelper.get().exit();
    }

    private void setLockScreenPin(int userId) {

        Log.i(LOG_TAG, "Act: Set Security Pin");
        mSecuritySettingsHelper.get().setLockByPinUsingApi(userId);
    }

    private void clearLockScreenPin() {

        Log.i(LOG_TAG, "Act: Clear Security Pin");
        mSecuritySettingsHelper.get().clearLockPinUsingApi();
    }

    @Test
    public void testLockscreenOnNewProfileAndDriverProfile() throws Exception {
        Log.i(LOG_TAG, "ACT: Switch to New User");
        // switched to new user and wait for it to load
        Log.i(LOG_TAG, "Act: Switch to new user");
        mMultiUserHelper.switchAndWaitForStable(
                mTargetUserId, MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);
        Log.i(LOG_TAG, "Act: Verify the screen Lock Screen ");
        assertTrue(
                "Lock Screen for driver profile is not visible",
                mSecuritySettingsHelper.get().isLockScreenVisible());
        Log.i(LOG_TAG, "Act: Unlock the screen Lock with pin ");
        mSecuritySettingsHelper.get().unLockScreenProfiles(PIN);
        clearLockScreenPin();
        Log.i(LOG_TAG, "ACT: Switch to Initial User");
        mUsersHelper.get().switchUsingUserIcon(DRIVER);

        Log.i(LOG_TAG, "Act: Verify the screen Lock Screen ");
        assertTrue(
                "Lock Screen for driver profile is not visible",
                mSecuritySettingsHelper.get().isLockScreenVisible());
        Log.i(LOG_TAG, "Act: Unlock the screen Lock with pin ");
        mSecuritySettingsHelper.get().unLockScreenProfiles(PIN);
    }
}
