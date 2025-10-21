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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoMediaHelper;
import android.platform.test.option.StringOption;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MediaCardTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();
    private static final String MEDIA_APP = "media-app";

    private static final String NEWS_APP = "News";
    private static final String NEWS_CHANNEL_NAME = "Reuters TV (U.S.)";
    private static final String TEST_MEDIA_APP = "Test Media App";

    private static final String DEFAULT_SONG_NAME = "NPV links";

    private static final String RADIO_STATION = "99.7 Now!";
    private static final String LOG_TAG = MediaCardTest.class.getSimpleName();

    @ClassRule
    public static StringOption mMediaTestApp = new StringOption(MEDIA_APP).setRequired(false);

    public static String mDefaultSongName = new String(DEFAULT_SONG_NAME);
    public static String mNewsChannelName = new String(NEWS_CHANNEL_NAME);
    private HelperAccessor<IAutoMediaHelper> mMediaCenterHelper =
            new HelperAccessor<>(IAutoMediaHelper.class);
    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper =
            new HelperAccessor<>(IAutoAppGridHelper.class);

    @Test
    public void testMediaThumbnailsOnMediaCard() {
        openMediaAppFromGrid();
        Log.i(LOG_TAG, "Act: Click on Media Card Thumbnail");
        mMediaCenterHelper.get().clickMediaCardThumbnail();

        Log.i(LOG_TAG, "Assert: Test Media App is open and playing the song");
        assertTrue(
                "Test Media App is Not open",
                mMediaCenterHelper.get().isMediaAppOpenAndTrackPlaying(DEFAULT_SONG_NAME));

        Log.i(LOG_TAG, "Act: Select Test Media App Category back to Basic");
        mMediaCenterHelper
                .get()
                .navigateMediaAppCategories(AutomotiveConfigConstants.BASIC_SONGS_CATEGORY);

        Log.i(LOG_TAG, "Act: Open Radio App and play a station");
        mMediaCenterHelper.get().openRadioAppAndPlayGivenStation(RADIO_STATION);

        Log.i(LOG_TAG, "Act: Click on Media Card Thumbnail");
        mMediaCenterHelper.get().clickMediaCardThumbnail();

        Log.i(LOG_TAG, "Assert: Radio App is open and playing the station");
        assertTrue(
                "Radio App is Not open",
                mMediaCenterHelper.get().isMediaAppOpenAndTrackPlaying(RADIO_STATION));
    }

    @Test
    public void testPlayListButton() {
        openMediaAppFromGrid();

        Log.i(LOG_TAG, "Act: Click on media card playlist");
        mMediaCenterHelper.get().openMediaCardPlayList();

        Log.i(LOG_TAG, "Assert: Queue list is displaying");
        assertTrue("Queue list is not displaying", mMediaCenterHelper.get().isQueueListDisplayed());

        Log.i(LOG_TAG, "Act: Close media card playlist");
        mMediaCenterHelper.get().closeMediaCardPlayList();
    }

    @Test
    public void testMediaCardMetadata() {
        openMediaAppFromGrid();
        Log.i(LOG_TAG, "Assert: Playing song name in media card is displaying");
        assertNotNull(
                "Media card song is not displaying",
                mMediaCenterHelper.get().getPlayingSongInMediaCard());

        Log.i(LOG_TAG, "Assert: Playing song author name is displaying");
        assertNotNull(
                "Media card song is not displaying",
                mMediaCenterHelper.get().getMediaCardSongAuthorName());

        Log.i(LOG_TAG, "Assert: Previous button is displaying");
        assertTrue(
                "Media card previous button is not displaying",
                mMediaCenterHelper.get().isMediaCardPreviousButtonDisplaying());

        Log.i(LOG_TAG, "Assert: Pause button displaying");
        assertTrue(
                "Media card pause button is not displaying",
                mMediaCenterHelper.get().isMediaCardPauseButtonDisplaying());

        Log.i(LOG_TAG, "Assert: Next button is displaying");
        assertTrue(
                "Media card pause button is not displaying",
                mMediaCenterHelper.get().isMediaCardNextButtonDisplaying());
    }
    private void openMediaAppFromGrid() {
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open Test Media App");
        mAppGridHelper.get().openApp(TEST_MEDIA_APP);

        Log.i(LOG_TAG, "Assert: Media App is Open");
        assertTrue(
                "Media app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.MEDIA_CENTER_PACKAGE));

        Log.i(LOG_TAG, "Act: Select any track song");
        mMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);

        Log.i(LOG_TAG, "Act: Minimize playing song");
        mMediaCenterHelper.get().minimizeNowPlaying();

        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().goToHomePage();
    }
    private void openNewsFromGrid() {
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open News App");
        mAppGridHelper.get().openApp(NEWS_APP);

        Log.i(LOG_TAG, "Assert: News App is Open");
        assertTrue(
                "News app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.MEDIA_CENTER_PACKAGE));

        Log.i(LOG_TAG, "Act: Select News track");
        mMediaCenterHelper.get().selectNewsTrack(mNewsChannelName);

        Log.i(LOG_TAG, "Act: Minimize playing track");
        mMediaCenterHelper.get().minimizeNowPlaying();

        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().goToHomePage();
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testSwitchMediaAppsFromAppLauncher() {
        for (int i = 0; i < 2; i++) {
            openMediaAppFromGrid();
            openNewsFromGrid();
        }
    }

    @Test
    public void testSwitchMediaAppsFromMediaSource() {
        Log.i(LOG_TAG, "Act: Open News from all launcher as pre- condition");
        openNewsFromGrid();

        Log.i(LOG_TAG, "Act: Open Media Source History from Media card Widget");
        mMediaCenterHelper.get().openMediaSource();

        Log.i(LOG_TAG, "Assert: Media Source history displayed News App");
        assertTrue(
                "Media Source History do not show news",
                mMediaCenterHelper.get().isNewsDisplayedInMediaSourceHistory());

        Log.i(LOG_TAG, "Assert: Media Source history displayed News channel Name");
        assertEquals(
                "News Channel Name doesn't match ",
                mNewsChannelName,
                mMediaCenterHelper.get().getNewsChannelNameFromMediaSourceHistory());

        Log.i(LOG_TAG, "Assert: Media Source history displayed Test media App");
        assertTrue(
                "Media Source History do not show news",
                mMediaCenterHelper.get().isTestMediaAppDisplayedInMediaSourceHistory());

        Log.i(LOG_TAG, "Assert: Media Source history displayed Song Name");
        assertEquals(
                "News Channel Name does't match ",
                mDefaultSongName,
                mMediaCenterHelper.get().getTestMediaAppSongNameFromMediaSourceHistory());

        Log.i(LOG_TAG, "Act: Switch to Test media App song from Media Soucre History");
        mMediaCenterHelper.get().clickOnTestMediaAppSongFromMediaSource();

        Log.i(LOG_TAG, "Assert: Media card displayed Current Song Name");
        assertTrue(
                "Media card do not displayed current song ",
                mMediaCenterHelper
                        .get()
                        .isTestMediaAppSongNameDisplayedOnMediaCard(mDefaultSongName));
    }

    @Test
    public void testMediaCardPlayNextPreviousButton() {
        openMediaAppFromGrid();

        Log.i(LOG_TAG, "Assert: Song track is playing on Media Card");
        assertTrue("Song is Paused", mMediaCenterHelper.get().isPlaying());

        Log.i(LOG_TAG, "Act: Song track is paused on Media Card");
        mMediaCenterHelper.get().playPauseMediaFromHomeScreen();

        Log.i(LOG_TAG, "Assert: Song track is paused on Media Card");
        assertTrue("Song is Playing", mMediaCenterHelper.get().isPaused());

        Log.i(LOG_TAG, "Act: Click on Next track on Media Card");
        mMediaCenterHelper.get().clickNextTrackFromHomeScreen();

        Log.i(LOG_TAG, "Assert: Next Song track is playing on Media Card");
        assertFalse(
                "Next Track is not Playing",
                mDefaultSongName.equals(
                        mMediaCenterHelper.get().getMediaTrackNameFromHomeScreen()));

        Log.i(LOG_TAG, "Act: Click on Previous track on Media Card");
        mMediaCenterHelper.get().clickPreviousTrackFromHomeScreen();

        Log.i(LOG_TAG, "Assert: Previous Song track is playing on Media Card");
        assertEquals(
                "Previous Track is not Playing",
                mDefaultSongName,
                mMediaCenterHelper.get().getMediaTrackNameFromHomeScreen());
    }
}
