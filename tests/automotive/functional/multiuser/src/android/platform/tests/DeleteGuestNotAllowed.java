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

package android.platform.tests;

import static junit.framework.Assert.assertFalse;

import android.Manifest;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.SettingsConstants;
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

/**
 * This test will create user through API and delete the same user from UI
 */
@RunWith(AndroidJUnit4.class)
public class DeleteGuestNotAllowed {
    @Rule
    public final AdoptShellPermissionsRule mShellPermissionsRule =
            new AdoptShellPermissionsRule(
                    InstrumentationRegistry.getInstrumentation().getUiAutomation(),
                    Manifest.permission.CREATE_USERS,
                    Manifest.permission.MANAGE_USERS);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final UiAutomation mUiAutomation = mInstrumentation.getUiAutomation();

    private static final String guestUser = MultiUserConstants.GUEST_NAME;
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    private static final String LOG_TAG = DeleteGuestNotAllowed.class.getSimpleName();

    public DeleteGuestNotAllowed() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Before
    public void openAccountsFacet() {
        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Go back to settings");
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testDeleteGuestNotAllowed() throws Exception {
        // verify that guest user cannot be seen and deleted from list of profiles
        Log.i(LOG_TAG, "Assert: Guest user cannot be seen and deleted from list of profiles");
        assertFalse(mUsersHelper.get().isUserPresent(guestUser));
    }
}
