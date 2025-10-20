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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoMediaHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoTestMediaAppHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.test.option.StringOption;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MediaTestAppTest {

    private static final String MEDIA_APP = "media-app";
    private static final String TEST_MEDIA_APP = "Test Media App";
    private static final String DEFAULT_SONG_NAME = "A normal 1H song";
    private static final String ADVANCE_SONG_NAME = "Standard Custom Actions";
    private static final String RABITHOLE_SONG_NAME = "A normal 15s song";
    private static final String CUSTOM_SONG_NAME = "Long playback error message";
    private static final String LOG_TAG = MediaTestAppTest.class.getSimpleName();

    @ClassRule
    public static StringOption mMediaTestApp = new StringOption(MEDIA_APP).setRequired(false);

    public static String mDefaultSongName = new String(DEFAULT_SONG_NAME);

    private static HelperAccessor<IAutoMediaHelper> sMediaCenterHelper =
            new HelperAccessor<>(IAutoMediaHelper.class);
    private static HelperAccessor<IAutoTestMediaAppHelper> sTestMediaAppHelper =
            new HelperAccessor<>(IAutoTestMediaAppHelper.class);
    private static HelperAccessor<IAutoHomeHelper> sAutoHomeHelper =
            new HelperAccessor<>(IAutoHomeHelper.class);
    private static HelperAccessor<IAutoAppGridHelper> sAppGridHelper =
            new HelperAccessor<>(IAutoAppGridHelper.class);
    private static HelperAccessor<IAutoSettingHelper> sSettingHelper =
            new HelperAccessor<>(IAutoSettingHelper.class);

    @BeforeClass
    public static void setup() {
        // Make sure app grid is not open before testing.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        sAppGridHelper.get().exit();

        Log.i(LOG_TAG, "Act: Open Appgrid");
        sAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open Test Media App");
        sAppGridHelper.get().openApp(TEST_MEDIA_APP);

        Log.i(LOG_TAG, "Act: Open Media App settings page");
        sMediaCenterHelper.get().openTestMediaAppSettings();

        Log.i(LOG_TAG, "Act: Wait for load Media on Test Media App");
        sTestMediaAppHelper.get().loadMediaInLocalMediaTestApp();

        Log.i(LOG_TAG, "Act: Select Normal 1H track song");
        sMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);
    }

    @After
    public void goMinimizeNowPlaying() {
        Log.i(LOG_TAG, "Act: Minimize playing song");
        sMediaCenterHelper.get().minimizeNowPlaying();
    }

    @Test
    public void testPlayPauseMedia() {
        Log.i(LOG_TAG, "Act: Play media song");
        sMediaCenterHelper.get().playMedia();
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song not playing.", sMediaCenterHelper.get().isPlaying());
        Log.i(LOG_TAG, "Act: Minimize playing song");
        sMediaCenterHelper.get().minimizeNowPlaying();
        Log.i(LOG_TAG, "Act: Select Normal 1H track song");
        sMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);
        Log.i(LOG_TAG, "Act: Pause media song");
        sMediaCenterHelper.get().pauseMedia();
        Log.i(LOG_TAG, "Assert: Media song is paused");
        assertFalse("Song not paused.", sMediaCenterHelper.get().isPlaying());
    }

    @Test
    public void testNextPreviousTrack() {
        Log.i(LOG_TAG, "Act: Get media track name");
        String currentSong = sMediaCenterHelper.get().getMediaTrackName();
        Log.i(LOG_TAG, "Act: Click on next track");
        sMediaCenterHelper.get().clickNextTrack();
        Log.i(LOG_TAG, "Act: Media Song playing has changed");
        assertNotEquals(
                "Song playing has not been changed",
                currentSong,
                sMediaCenterHelper.get().getMediaTrackName());
        Log.i(LOG_TAG, "Act: Get media track name");
        currentSong = sMediaCenterHelper.get().getMediaTrackName();
        Log.i(LOG_TAG, "Act: Click on previous track");
        sMediaCenterHelper.get().clickPreviousTrack();
        Log.i(LOG_TAG, "Act: Media Song playing has changed");
        assertNotEquals(
                "Song playing has not been changed",
                currentSong,
                sMediaCenterHelper.get().getMediaTrackName());
    }

    @Test
    public void testMediaPlayStateAfterGoingToHomeScreen() {
        Log.i(LOG_TAG, "Act: Play media song");
        sMediaCenterHelper.get().playMedia();
        Log.i(LOG_TAG, "Act: Select Normal 1H track song");
        sMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is not playing", sMediaCenterHelper.get().isPlaying());
        Log.i(LOG_TAG, "Act: Exit Media App");
        sMediaCenterHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Media Widget is displayed");
        assertTrue(sAutoHomeHelper.get().hasMediaWidget());
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is not playing", sMediaCenterHelper.get().isPlaying());
        Log.i(LOG_TAG, "Act: Open Media widget");
        sAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is not playing", sMediaCenterHelper.get().isPlaying());
    }

    @Test
    public void testMediaPauseStateAfterGoingToHomeScreen() {
        Log.i(LOG_TAG, "Act: Play media song");
        sMediaCenterHelper.get().playMedia();
        Log.i(LOG_TAG, "Act: Pause media song");
        sMediaCenterHelper.get().pauseMedia();
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is playing, it should be paused", sMediaCenterHelper.get().isPaused());
        Log.i(LOG_TAG, "Act: Exit Media App");
        sMediaCenterHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Media Widget is displayed");
        assertTrue(sAutoHomeHelper.get().hasMediaWidget());
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is playing, it should be paused", sMediaCenterHelper.get().isPaused());
        Log.i(LOG_TAG, "Act: Open Media widget");
        sAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Assert: Media song is paused");
        assertTrue("Song is playing, it should be paused", sMediaCenterHelper.get().isPaused());
    }

    @Test
    public void testMediaAppCategories() {

        Log.i(LOG_TAG, "Assert: Media Song playing has changed according to Basic Category");
        assertTrue(
                "Media Song playing has not changed according to Basic Category",
                sMediaCenterHelper
                        .get()
                        .checkPlayingTrackFromMediaAppCategories(
                                AutomotiveConfigConstants.BASIC_SONGS_CATEGORY, mDefaultSongName));

        Log.i(LOG_TAG, "Assert: Media Song playing has  changed according to Advance Category");
        assertTrue(
                "Media Song playing has not changed according to Advance Category",
                sMediaCenterHelper
                        .get()
                        .checkPlayingTrackFromMediaAppCategories(
                                AutomotiveConfigConstants.ADVANCED_CATEGORY, ADVANCE_SONG_NAME));

        Log.i(LOG_TAG, "Assert: Media Song playing has changed according to RABBIT Category");
        assertTrue(
                "Media Song playing has not changed according to RABIT Category",
                sMediaCenterHelper
                        .get()
                        .checkPlayingTrackFromMediaAppCategories(
                                AutomotiveConfigConstants.RABBIT_HOLE_CATEGORY,
                                RABITHOLE_SONG_NAME));

        Log.i(LOG_TAG, "Act: Minimize playing song");
        sMediaCenterHelper.get().minimizeNowPlaying();

        Log.i(LOG_TAG, "Act: Select Media Category as Empty");
        sMediaCenterHelper
                .get()
                .navigateMediaAppCategories(AutomotiveConfigConstants.EMPTY_CATEGORY);

        Log.i(LOG_TAG, "Assert: Media Song playing has not changed for Empty Category");
        assertEquals(
                "Song playing has been changed for Empty category",
                RABITHOLE_SONG_NAME,
                sMediaCenterHelper.get().getMediaTrackName());

        Log.i(LOG_TAG, "Act: Select Media Category back to Basic");
        sMediaCenterHelper
                .get()
                .navigateMediaAppCategories(AutomotiveConfigConstants.BASIC_SONGS_CATEGORY);
    }

    @Test
    public void testMediaPlayQueueSongs() {
        Log.i(LOG_TAG, "Act: Maximize playing song");
        sMediaCenterHelper.get().maximizeNowPlaying();

        Log.i(LOG_TAG, "Assert: Playlist Icon is visible");
        assertTrue(
                "Playlist Icon is NOT visible", sMediaCenterHelper.get().isPlaylistIconVisible());

        Log.i(LOG_TAG, "Act: Open Playlist songs");
        sMediaCenterHelper.get().clickOnPlaylistIcon();

        Log.i(LOG_TAG, "Act: Select Long playback error message track song");
        sMediaCenterHelper.get().selectMediaTrack(CUSTOM_SONG_NAME);

        Log.i(LOG_TAG, "Act: Play media song");
        sMediaCenterHelper.get().playMedia();

        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is not playing", sMediaCenterHelper.get().isPlaying());

        Log.i(LOG_TAG, "Assert: Song track changed to Long playback error message");
        assertEquals(
                "Song playing has not been changed",
                CUSTOM_SONG_NAME,
                sMediaCenterHelper.get().getMediaTrackName());

        Log.i(LOG_TAG, "Act: Open Playlist songs");
        sMediaCenterHelper.get().clickOnPlaylistIcon();

        Log.i(LOG_TAG, "Assert: Playlist Scroll Up button is visible");
        assertTrue(
                "Playlist Scroll Up button is NOT visible",
                sMediaCenterHelper.get().isPlaylistScrollUpVisible());

        Log.i(LOG_TAG, "Assert: Playlist Scroll Down button is visible");
        assertTrue(
                "Playlist Scroll Down button is NOT visible",
                sMediaCenterHelper.get().isPlaylistScrollDownVisible());
    }

    @Test
    public void testMetadataOfCurrentPlayingMedia() {
        Log.i(LOG_TAG, "Act: Select Normal 1H track song");
        sMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);

        Log.i(LOG_TAG, "Assert: Album title is displaying");
        assertNotNull("Album title is not displaying", sMediaCenterHelper.get().getAlbumTitle());

        Log.i(LOG_TAG, "Assert: Artist title is displaying");
        assertNotNull("Artist title is not displaying", sMediaCenterHelper.get().getArtistrTitle());

        Log.i(LOG_TAG, "Assert: Current song playing time is displaying");
        assertNotNull(
                "Current song playing time is not displaying",
                sMediaCenterHelper.get().getSongCurrentPlayingTime());

        Log.i(LOG_TAG, "Assert: Current song max time is disdplaying");
        assertNotNull(
                "Current song max playing time is not displaying",
                sMediaCenterHelper.get().getCurrentSongMaxPlayingTime());

        Log.i(LOG_TAG, "Assert: Album thumbnail is displaying");
        assertTrue(
                "Album thumbnail is not displaying",
                sMediaCenterHelper.get().isAlbumThumbnailDisplaying());
    }

    @Test
    public void testMediaIncDecVolume() {
        Log.i(LOG_TAG, "Act: Open the Sound Setting");
        sSettingHelper.get().openSetting(SettingsConstants.SOUND_SETTINGS);

        assertTrue(
                "Sound Setting did not open", sSettingHelper.get().checkMenuExists("Media volume"));

        // Decrease the media volume
        Log.i(LOG_TAG, "Act: Set Media volume to Low");
        int lowMediaVolume = sSettingHelper.get().setMediaSoundLevelLow();

        // Increase the media volume
        Log.i(LOG_TAG, "Act: Set Media volume to High");
        int highMediaVolume = sSettingHelper.get().setMediaSoundLevelHigh();

        // Verify that the media volume  has changed.
        Log.i(LOG_TAG, "Assert: Media volume is adjusted");
        assertTrue(
                "Media volume was not increased (from "
                        + lowMediaVolume
                        + " to "
                        + highMediaVolume
                        + ")",
                lowMediaVolume < highMediaVolume);

        // Close settings app
        Log.i(LOG_TAG, "Act: Exit Settings App");
        sSettingHelper.get().exit();

        Log.i(LOG_TAG, "Act: Open Media widget");
        sAutoHomeHelper.get().openMediaWidget();
    }
}
