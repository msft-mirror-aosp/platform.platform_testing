/*
 * Copyright (C) 2016 The Android Open Source Project
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

import java.util.List;

public interface IAutoMediaHelper extends IAppHelper, Scrollable {
    /**
     * Setup expectations: media app is open
     *
     * This method is used to play media.
     */
    void playMedia();

    /**
     * Setup expectations: on home screen.
     *
     * This method is used to play media from home screen.
     */
    void playPauseMediaFromHomeScreen();

    /**
     * Setup expectations: media app is open.
     *
     * This method is used to pause media.
     */
    void pauseMedia();

    /**
     * Setup expectations: media app is open.
     *
     * This method is used to select next track.
     */
    void clickNextTrack();

    /**
     * Setup expectations: on home screen.
     *
     * This method is used to select next track from home screen.
     */
    void clickNextTrackFromHomeScreen();

    /**
     * Setup expectations: media app is open.
     *
     * This method is used to select previous track.
     */
    void clickPreviousTrack();

    /**
     * Setup expectations: on home screen.
     *
     * This method is used to select previous track from home screen.
     */
    void clickPreviousTrackFromHomeScreen();

    /**
     * Setup expectations: media app is open.
     *
     * This method is used to shuffle tracks.
     */
    void clickShuffleAll();

    /**
     * Setup expectations: media app is open.
     *
     * This method is used to click on nth instance among the visible menu items
     *
     * @param - instance is the index of the menu item (starts from 0)
     */
    void clickMenuItem(int instance);

    /**
     * Setup expectations: media app is open.
     *
     * This method is used to open Folder Menu with menuOptions.
     * Example - openMenu->Folder->Mediafilename->trackName
     *           openMenuWith(Folder,mediafilename,trackName);
     *
     * @param - menuOptions used to pass multiple level of menu options in one go.
     */
    void openMenuWith(String... menuOptions);

    /**
     * Setup expectations: media app is open.
     *
     * This method is used to used to open mediafilename from now playing list.
     *
     *  @param - trackName - media to be played.
     */
    void openNowPlayingWith(String trackName);

    /**
     * Setup expectations: Media app is open.
     *
     * @return to get current playing track name.
     */
    String getMediaTrackName();

    /**
     * Setup expectations: on home screen.
     *
     * @return to get current playing track name from home screen.
     */
    String getMediaTrackNameFromHomeScreen();

    /**
     * Setup expectations: Media app is open. User navigates to sub-page of the Media Player
     *
     * This method is to go back to the Media Player main page from any sub-page.
     */
    void goBackToMediaHomePage();

    /**
     * This method is used to check if media is currently playing Returns true if media is playing
     * else returns false
     */
    boolean isPlaying();

    /**
     * This method is used to check if media is currently Paused Returns true if media is paused
     * else returns false
     */
    boolean isPaused();

    /**
     * Setup expectations: Media app is open.
     *
     * @return Media App Title
     */
    String getMediaAppTitle();

    /**
     * Setup expectations: Media app is open.
     * Opens the drop down menu in the Media Apps
     */
    void openMediaAppMenuItems();

    /**
     * Setup expectations: "Media apps" Grid is open.
     *
     * @param mediaAppsNames : List of media apps names
     * @return true if all app names in mediaAppsNames shows up in Media Apps Grid
     */
    boolean areMediaAppsPresent(List<String> mediaAppsNames);

    /**
     * Setup expectations: "Media apps" Grid is open.
     *
     * @param appName App name to open
     */
    void openApp(String appName);

    /**
     * Setup expectations: Media app is open.
     */
    void openMediaAppSettingsPage();

    /** Setup expectations: Media app is open. */
    void openTestMediaAppSettings();

    /**
     * Setup expectations: Media app is open.
     *
     * <p>Opens Test Meida Search
     */
    void openTestMediaAppSearch();

    /**
     * Setup expectations: Media app search is open.
     *
     * <p>Validates Media apps search restricted message is displayed
     */
    boolean isMediaSearchRestrictedMessagedDisplayed();

    /**
     * Setup expectations: Media app search is open.
     *
     * <p>Validates Test Media App search is taking input
     */
    boolean isSearchBarTakingInput();

    /**
     * Setup expectations: Media app is open. Account not logged in.
     *
     * @return Error message for no user login
     */
    String getMediaAppUserNotLoggedInErrorMessage();

    /**
     * Setup expectations: In Media.
     *
     * <p>Scroll up on page.
     */
    boolean scrollUpOnePage();

    /**
     * Setup expectations: In Media.
     *
     * <p>Scroll down on page.
     */
    boolean scrollDownOnePage();

    /**
     * Setup expectations: media test app is open.
     *
     * <p>This method is used to open Folder Menu with menuOptions and scroll into view the track.
     * Example - openMenu->Folder->Mediafilename->trackName
     * openMenuWith(Folder,mediafilename,trackName);
     *
     * @param menuOptions used to pass multiple level of menu options in one go.
     */
    void selectMediaTrack(String... menuOptions);

    /**
     * Setup expectations: Now Playing is open.
     *
     * <p>This method is used to select previous track.
     */
    void minimizeNowPlaying();

    /**
     * Setup expectations: media test app is open and Minimize control bar present.
     *
     * <p>This method is used to maximize the play back screen.
     */
    void maximizeNowPlaying();

    /**
     * Setup expectations: Bluetooth Audio page opened.
     *
     * <p>This method is return is Bluetooth Audio disconnected label visible.
     */
    boolean isBluetoothAudioDisconnectedLabelVisible();

    /**
     * Setup expectations: Bluetooth Audio page opened.
     *
     * <p>This method returns whether connect to bluetooth label visible or not.
     */
    boolean isConnectToBluetoothLabelVisible();

    /**
     * Setup expectations: Bluetooth Audio page opened.
     *
     * <p>This method returns whether cancel button visible or not.
     */
    boolean isCancelButtonVisible();

    /**
     * Setup expectations: on home screen.
     *
     * <p>This method is used to open Bluetooth Audio screen.
     */
    void openBluetoothMediaApp();

    /**
     * Setup expectations: Bluetooth Settings page opened.
     *
     * <p>This method is used to enable/disable Bluetooth conncetion.
     */
    void clickOnBluetoothToggle();

    /**
     * Setup expectations: Bluetooth Audio page opened.
     *
     * <p>This method is used to Cancel Bluetooth Audio conncetion.
     */
    void cancelBluetoothAudioConncetion();

    /**
     * Setup expectations: Bluetooth Audio page opened.
     *
     * <p>This method is used to Scroll down playlist.
     */
    void scrollPlayListDown();

    /**
     * Setup expectations: Bluetooth Audio page opened.
     *
     * <p>This method is used to select song from playlist with index.
     */
    void clickOnSongFromPlaylist(int index);

    /**
     * Setup expectations: Media card is open and playing any song.
     *
     * @return get current playing song author name
     */
    String getMediaCardSongAuthorName();

    /**
     * Setup expectations: Media card is open and playing any song.
     *
     * <p>This method validates media card previous button is displaying
     */
    boolean isMediaCardPreviousButtonDisplaying();

    /**
     * Setup expectations: Media card is open and playing any song.
     *
     * <p>This method validates media card pause button is displaying
     */
    boolean isMediaCardPauseButtonDisplaying();

    /**
     * Setup expectations: Media card is open and playing any song.
     *
     * <p>This method validates media card next button is displaying
     */
    boolean isMediaCardNextButtonDisplaying();

    /**
     * Setup expectations: Media card is open and playing any song.
     *
     * <p>This method closes media card play list
     */
    void closeMediaCardPlayList();

    /**
     * Setup expectations: Media app is open and maximized now playing.
     *
     * @return get current artist tile
     */
    String getArtistrTitle();

    /**
     * Setup expectations: Media app is open and maximized now playing.
     *
     * @return get current album tile
     */
    String getAlbumTitle();

    /**
     * Setup expectations: Media app is open and maximized now playing.
     *
     * <p>validates album thumbnail is displaying or not
     */
    boolean isAlbumThumbnailDisplaying();

    /**
     * Setup expectations: Media app is open and maximized now playing.
     *
     * @return get current song playing time
     */
    String getSongCurrentPlayingTime();

    /**
     * Setup expectations: Media app is open and maximized now playing.
     *
     * @return get current song max playing time
     */
    String getCurrentSongMaxPlayingTime();

    /**
     * Setup expectations: Bluetooth Audio track maximized.
     *
     * <p>This method is return is "Now Playing" label visible.
     */
    boolean isNowPlayingLabelVisible();

    /**
     * Setup expectations: Bluetooth Audio track maximized.
     *
     * <p>This method is return is Playlist icon visible.
     */
    boolean isPlaylistIconVisible();

    /**
     * Setup expectations: Bluetooth Audio track maximized.
     *
     * <p>This method is used to click on playlist icon.
     */
    void clickOnPlaylistIcon();

    /**
     * Setup expectations: Grant restricted parmissions for BT Media
     *
     * <p>This method is used to Grant restricted parmissions in runtime
     */
    void grantRestrictedPermissionsForBTMedia(String permission);

    /**
     * Setup expectations: Radio is open
     *
     * <p>This method verifies if the Radio app is Open
     */
    boolean isRadioAppLaunched();

    /**
     * Setup expectations: Radio is open
     *
     * <p>This method returns the Radio station name
     */
    String getRadioStationName();

    /**
     * Setup expectations: Media app is open
     *
     * <p>This method is used to navigate through different media categories
     */
    void navigateMediaAppCategories(String automotiveconfig);

    /**
     * Setup expectations: Media app is open
     *
     * <p>This method is used to navigate through different media categories and play song
     */
    boolean checkPlayingTrackFromMediaAppCategories(String automotiveconfig, String track);

    /**
     * Setup expectations: Media card is open
     *
     * <p>This method is used to click Media card thumbnail
     */
    void clickMediaCardThumbnail();

    /**
     * Setup expectations: Media card is present
     *
     * <p>This method checks Media App opens after Media card thumbnail is clicked
     */
    boolean isMediaAppOpenAndTrackPlaying(String track);

    /**
     * Setup expectations: on home screen.
     *
     * <p>This method opens Radio app and play a Radio station
     */
    void openRadioAppAndPlayGivenStation(String media);

    /**
     * Setup expectations: Media app is open
     *
     * <p>This method verifies if the Scroll up option is visible
     */
    boolean isPlaylistScrollUpVisible();

    /**
     * Setup expectations: Media app is open
     *
     * <p>This method clicks on Continue button on allow contacts pop up
     */
    void clickContactsContinueButton();

    /**
     * Setup expectations: Media app is open
     *
     * <p>This method verifies if the Scroll Down option is visible
     */
    boolean isPlaylistScrollDownVisible();

    /**
     * Setup expectations: Media card is playing any media
     *
     * <p>This method click on play list button from media button
     */
    void openMediaCardPlayList();

    /**
     * Setup expectations: Media card queue list is opened
     *
     * <p>This method validates queue list on media is opened
     */
    boolean isQueueListDisplayed();

    /**
     * Setup expectations: Media card queue list is opened
     *
     * <p>This method plays a song from playlist in media card and returns selected song
     */
    String getPlayingSongInMediaCard();

    /**
     * Setup expectations: News test app is open.
     *
     * <p>This method is used to menuOptions click the track.
     *
     * <p>openMenuWith(Folder,mediafilename,trackName);
     *
     * @param menuOptions used to pass multiple level of menu options in one go.
     */
    void selectNewsTrack(String... menuOptions);

    /**
     * Setup expectations: on home screen.
     *
     * <p>This method is used to open Media Source on Media Card Widget.
     */
    void openMediaSource();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to open News from Media Source/ History folder on Media Card Widget.
     */
    void openNewsAppFromMediaSource();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to verify News displayed in Media Source history folder on Media Card
     * Widget.
     */
    boolean isNewsDisplayedInMediaSourceHistory();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to get the text of News channel displayed in Media Source history
     * folder on Media Card Widget.
     */
    String getNewsChannelNameFromMediaSourceHistory();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to verify Test Media app displayed in Media Source history folder on
     * Media Card Widget.
     */
    boolean isTestMediaAppDisplayedInMediaSourceHistory();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to get the text of song displayed in Media Source history folder on
     * Media Card Widget.
     */
    String getTestMediaAppSongNameFromMediaSourceHistory();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to click on Song from Media Source/ History folder on Media Card
     * Widget.
     */
    void clickOnTestMediaAppSongFromMediaSource();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to verify the Song playing on Media Card Widget.
     *
     * @param defaultSongName is a song name played in Media Player App
     */
    boolean isTestMediaAppSongNameDisplayedOnMediaCard(String defaultSongName);

    /**
     * Setup expectations: Media app is open
     *
     * <p>This method is used to play song from media app
     */
    void openMediaAppAndPlayGivenSong(String appName, String media);

    /**
     * Setup expectations: News app is open
     *
     * <p>This method is used to play a news channel
     */
    void openNewsAppAndPlayGivenChannel(String media);

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to click on overflow/three dot button on Media Card Widget.
     */
    void clickOnThreeDotButtonMediaCard();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to click on extended menu close button on Media Card Widget.
     */
    void closeExtendedMenu();

    /**
     * Setup expectations: on Media card
     *
     * <p>This method is used to verify the extended menu is opened on Media Card Widget.
     */
    boolean isExtendedMenuDisplayedOnMediaCard();
}
