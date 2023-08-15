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
import android.platform.helpers.IAutoHomeHelper;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StatusBarTest {

    private HelperAccessor<IAutoHomeHelper> mHomeHelper;

    public StatusBarTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
    }

    @Test
    public void testToverifyDefaultStatusbar() {
        assertTrue("Bluetooth Button is not displayed", mHomeHelper.get().hasBluetoothButton());
        assertTrue("Network Button is not displayed", mHomeHelper.get().hasNetworkButton());
        assertTrue("Brightness Button is not displayed", mHomeHelper.get().hasDisplayBrightness());
    }
}