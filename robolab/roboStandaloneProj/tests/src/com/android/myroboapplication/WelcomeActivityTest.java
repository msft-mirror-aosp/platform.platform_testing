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

package com.android.myroboapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import com.android.robotestutil.RobolectricTestUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class WelcomeActivityTest {

    @Before
    public void setup() throws Exception {}

    @Test
    public void clickingLogin_shouldStartLoginActivity() throws Exception {
        ActivityController<WelcomeActivity> controller =
                Robolectric.buildActivity(WelcomeActivity.class);
        controller.setup(); // Moves Activity to RESUMED state
        WelcomeActivity activity = controller.get();

        activity.findViewById(R.id.login).performClick();
        Intent expectedIntent = new Intent(activity, LoginActivity.class);
        Intent actual = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actual.getComponent());
    }

    @Test
    public void testTypeCheck() {
        boolean isRoboTest = RobolectricTestUtil.isRobolectricTest();
        assertTrue(isRoboTest);
    }
}
