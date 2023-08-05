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

package android.platform.scenario.multiuser;

import android.app.UiAutomation;
import android.content.pm.UserInfo;
import android.os.SystemClock;
import android.platform.helpers.MultiUserHelper;
import android.platform.test.scenario.annotation.Scenario;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * This test will always switch to a newly created guest from default initial user.
 *
 * <p>It should be running under user 0, otherwise instrumentation may be killed after user
 * switched.
 */
@Scenario
@RunWith(JUnit4.class)
public class SwitchToNewGuest {

    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private int mGuestId;
    private UiAutomation mUiAutomation = null;
    private static final String CREATE_USERS_PERMISSION = "android.permission.CREATE_USERS";

    @Before
    public void setup() throws Exception {
        /*
        TODO: Create setup util API
         */
        // Execute user manager APIs with elevated permissions
        mUiAutomation = getUiAutomation();
        mUiAutomation.adoptShellPermissionIdentity(CREATE_USERS_PERMISSION);
        UserInfo currentUser = mMultiUserHelper.getCurrentForegroundUserInfo();

        // Drop elevated permissions
        mUiAutomation.dropShellPermissionIdentity();

        if (currentUser.id != MultiUserConstants.DEFAULT_INITIAL_USER) {
            SystemClock.sleep(MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);

            // Execute user manager APIs with elevated permissions
            mUiAutomation = getUiAutomation();
            mUiAutomation.adoptShellPermissionIdentity(CREATE_USERS_PERMISSION);
            mMultiUserHelper.switchAndWaitForStable(
                MultiUserConstants.DEFAULT_INITIAL_USER, MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);

            // Drop elevated permissions
            mUiAutomation.dropShellPermissionIdentity();
        }

        if (!MultiUserConstants.INCLUDE_CREATION_TIME) {
            // Execute user manager APIs with elevated permissions
            mUiAutomation = getUiAutomation();
            mUiAutomation.adoptShellPermissionIdentity(CREATE_USERS_PERMISSION);
            mGuestId = mMultiUserHelper.createUser(MultiUserConstants.GUEST_NAME, true);

            // Drop elevated permissions
            mUiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    public void testSwitch() throws Exception {

        // Execute user manager APIs with elevated permissions
        mUiAutomation = getUiAutomation();
        mUiAutomation.adoptShellPermissionIdentity(CREATE_USERS_PERMISSION);

        if (MultiUserConstants.INCLUDE_CREATION_TIME) {
            mGuestId = mMultiUserHelper.createUser(MultiUserConstants.GUEST_NAME, true);
        }
        mMultiUserHelper.switchToUserId(mGuestId);

        // Drop elevated permissions
        mUiAutomation.dropShellPermissionIdentity();
    }

    private UiAutomation getUiAutomation() {
        return InstrumentationRegistry.getInstrumentation().getUiAutomation();
    }
}
