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
import android.platform.helpers.IAutoMediaHelper;
import android.platform.test.option.StringOption;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MediaCardTest {

    private static final String MEDIA_APP = "media-app";

    private static final String TEST_MEDIA_APP = "Test Media App";

    private static final String DEFAULT_SONG_NAME = "A normal 1H song";

    private static final String RADIO_STATION = "99.7 Now!";
    private static final String LOG_TAG = MediaCardTest.class.getSimpleName();

    @ClassRule
    public static StringOption mMediaTestApp = new StringOption(MEDIA_APP).setRequired(false);

    public static String mDefaultSongName = new String(DEFAULT_SONG_NAME);

    private static HelperAccessor<IAutoMediaHelper> sMediaCenterHelper =
            new HelperAccessor<>(IAutoMediaHelper.class);
    private static HelperAccessor<IAutoAppGridHelper> sAppGridHelper =
            new HelperAccessor<>(IAutoAppGridHelper.class);

    @BeforeClass
    public static void setup() {
        // Make sure app grid is not open before testing.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        sAppGridHelper.get().exit();

        Log.i(LOG_TAG, "Act: Open Appgrid");
        sAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open Test Media App");
        sAppGridHelper.get().openApp(TEST_MEDIA_APP);

        Log.i(LOG_TAG, "Act: Select Normal 1H track song");
        sMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);

        Log.i(LOG_TAG, "Act: Exit Media App");
        sMediaCenterHelper.get().exit();
    }

    @After
    public void goMinimizeNowPlaying() {
        Log.i(LOG_TAG, "Act: Minimize playing song");
        sMediaCenterHelper.get().minimizeNowPlaying();

        Log.i(LOG_TAG, "Act: Exit Media App");
        sMediaCenterHelper.get().exit();
    }

    @Test
    public void testMediaThumbnailsOnMediaCard() {

        Log.i(LOG_TAG, "Act: Click on Media Card Thumbnail");
        sMediaCenterHelper.get().clickMediaCardThumbnail();

        Log.i(LOG_TAG, "Assert: Test Media App is open and playing the song");
        assertTrue(
                "Test Media App is Not open",
                sMediaCenterHelper.get().isMediaAppOpenAndTrackPlaying(DEFAULT_SONG_NAME));

        Log.i(LOG_TAG, "Act: Select Test Media App Category back to Basic");
        sMediaCenterHelper
                .get()
                .navigateMediaAppCategories(AutomotiveConfigConstants.BASIC_SONGS_CATEGORY);

        Log.i(LOG_TAG, "Act: Open Radio App and play a station");
        sMediaCenterHelper.get().openRadioAppAndPlayGivenStation(RADIO_STATION);

        Log.i(LOG_TAG, "Act: Click on Media Card Thumbnail");
        sMediaCenterHelper.get().clickMediaCardThumbnail();

        Log.i(LOG_TAG, "Assert: Radio App is open and playing the station");
        assertTrue(
                "Radio App is Not open",
                sMediaCenterHelper.get().isMediaAppOpenAndTrackPlaying(RADIO_STATION));
    }
}
