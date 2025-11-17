/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoHomeHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class DockTest {

    private static final String SETTINGS = "Settings";

    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoHomeHelper> mHomeHelper;
    private static final String LOG_TAG = DockTest.class.getSimpleName();

    public DockTest() {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
    }

    @Test
    public void testUnpinAppOnDock() {
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Settings App");
        mAppGridHelper.get().openApp(SETTINGS);
        Log.i(LOG_TAG, "Assert: Settings App is open");
        assertTrue(
                "Settings app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.SETTINGS_PACKAGE));
        Log.i(LOG_TAG, "Act: Go to Home Screen");
        mAppGridHelper.get().goToHomePage();
        Log.i(LOG_TAG, "Assert: Google Playstore App is Present on DOCK");
        assertTrue(
                "Playstore App is NOT Present on Dock",
                mAppGridHelper
                        .get()
                        .verifyAppOnDock(AutomotiveConfigConstants.PLAY_STORE_APP_ON_DOCK));
        Log.i(LOG_TAG, "Act: Unpin the Playstore app on Dock");
        mAppGridHelper.get().unpinAppOnDock(AutomotiveConfigConstants.PLAY_STORE_APP_ON_DOCK);
        Log.i(LOG_TAG, "Assert: Settings App is Present on DOCK");
        assertTrue(
                "Settings App is Present on Dock",
                mAppGridHelper
                        .get()
                        .verifyAppOnDock(AutomotiveConfigConstants.SETTINGS_APP_ON_DOCK));
    }

    @Test
    public void testClickAndVerifyConstantAppsOnDock() {
        mHomeHelper.get().clickMapsAppOnDock();
        // Uncomment this after bug b/455634890 is fixed
        /* assertFalse(
        "Maps app is not opened",
              mHomeHelper.get().hasMediaWidget());*/
        mHomeHelper.get().open();
        mHomeHelper.get().clickPlaystoreAppOnDock();
        assertTrue(
                "Play Store app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.PLAY_STORE_PACKAGE));
        mHomeHelper.get().open();
        mHomeHelper.get().clickBluetoothAudioAppOnDock();
        assertTrue(
                "Bluetooth Audio app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.MEDIA_CENTER_PACKAGE));
        mHomeHelper.get().open();
    }
}
