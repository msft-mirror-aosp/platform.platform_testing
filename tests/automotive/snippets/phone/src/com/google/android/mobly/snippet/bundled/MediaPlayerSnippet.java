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

package com.google.android.mobly.snippet.bundled;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoMediaHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

public class MediaPlayerSnippet implements Snippet {

    private final HelperAccessor<IAutoVehicleHardKeysHelper> mHardkeysHelper =
            new HelperAccessor<>(IAutoVehicleHardKeysHelper.class);
    private final HelperAccessor<IAutoMediaHelper> mAutoMediaHelper =
            new HelperAccessor<>(IAutoMediaHelper.class);

    @Rpc(description = "Toggle muting of audio stream by hardkey")
    public void hardkeyMute() {
        mHardkeysHelper.get().tuneMuteKey();
    }

    @Rpc(description = "Decrease volume by hardkey")
    public void hardkeyVolumeDown() {
        mHardkeysHelper.get().tuneVolumeDownKey();
    }

    @Rpc(description = "Increase volume by hardkey")
    public void hardkeyVolumeUp() {
        mHardkeysHelper.get().tuneVolumeUpKey();
    }

    @Rpc(description = "Play Media")
    public void playMedia() {
        mAutoMediaHelper.get().playMedia();
    }

    @Rpc(description = "Pause Media")
    public void pauseMedia() {
        mAutoMediaHelper.get().pauseMedia();
    }

    @Rpc(description = "Click on next track")
    public void clickNextTrack() {
        mAutoMediaHelper.get().clickNextTrack();
    }

    @Rpc(description = "Click on previous track")
    public void clickPreviousTrack() {
        mAutoMediaHelper.get().clickPreviousTrack();
    }

    @Rpc(description = "Get Media song name")
    public String getMediaTrackName() {
        return mAutoMediaHelper.get().getMediaTrackName();
    }

    @Rpc(description = "Minimize now playing")
    public void minimizeNowPlaying() {
        mAutoMediaHelper.get().minimizeNowPlaying();
    }

    @Rpc(description = "Maximize now playing")
    public void maximizeNowPlaying() {
        mAutoMediaHelper.get().maximizeNowPlaying();
    }

    @Rpc(description = "Is song playing")
    public boolean isPlaying() {
        return mAutoMediaHelper.get().isPlaying();
    }

    @Rpc(description = "Open Media app menu items")
    public void openMediaAppMenuItems() {
        mAutoMediaHelper.get().openMediaAppMenuItems();
    }

    @Rpc(description = "Open Media app")
    public void openMediaApp() {
        mAutoMediaHelper.get().open();
    }

    /** open the given app */
    @Rpc(description = "Open the given app")
    public void openApp(String mediaApp) {
        mAutoMediaHelper.get().openApp(mediaApp);
    }

    @Rpc(description = "Is Bluetooth Audio disconnected label present")
    public boolean isBluetoothAudioDisconnectedLabelVisible() {
        return mAutoMediaHelper.get().isBluetoothAudioDisconnectedLabelVisible();
    }

    /** This method used to waitfor bluetooth audio message to disappear */
    @Rpc(description = "Wait For Bluetooth Audio disconnected label not present")
    public void waitForBluetoothAudioDisconnectedLabelToDisappear() {
        mAutoMediaHelper.get().waitForBluetoothAudioDisconnectedLabelToDisappear();
    }

    @Rpc(description = "Is Connect to Bluetooth label present")
    public boolean isConnectToBluetoothLabelVisible() {
        return mAutoMediaHelper.get().isConnectToBluetoothLabelVisible();
    }

    @Rpc(description = "Is Cancel button present")
    public boolean isCancelButtonVisible() {
        return mAutoMediaHelper.get().isCancelButtonVisible();
    }

    @Rpc(description = "Open Bluetooth Audio app")
    public void openBluetoothMediaApp() {
        mAutoMediaHelper.get().openBluetoothMediaApp();
    }

    @Rpc(description = "Click on Bluetooth conncetion togle")
    public void clickOnBluetoothToggle() {
        mAutoMediaHelper.get().clickOnBluetoothToggle();
    }

    /** Click on Mediacard playlist */
    @Rpc(description = "Click on Mediacard playlist")
    public void openMediaCardPlayList() {
        mAutoMediaHelper.get().openMediaCardPlayList();
    }

    @Rpc(description = "Click on Cancel Bluetooth Audio conncetion button")
    public void cancelBluetoothAudioConncetion() {
        mAutoMediaHelper.get().cancelBluetoothAudioConncetion();
    }

    @Rpc(description = "Scroll play list down")
    public void scrollPlayListDown() {
        mAutoMediaHelper.get().scrollPlayListDown();
    }

    /** Select Media Track* */
    @Rpc(description = "Select media track")
    public void selectMediaTrack(String songName) {
        mAutoMediaHelper.get().selectMediaTrack(songName);
    }

    @Rpc(description = "Select song from playlist")
    public void clickOnSongFromPlaylist(int index) {
        mAutoMediaHelper.get().clickOnSongFromPlaylist(index);
    }

    /** Select song from mediacard playlist */
    @Rpc(description = "Select song from mediacard playlist")
    public void clickOnSongFromMediaCardPlaylist(int index) {
        mAutoMediaHelper.get().clickOnSongFromMediaCardPlaylist(index);
    }

    @Rpc(description = "Get Artist tile")
    public String getArtistrTitle() {
        return mAutoMediaHelper.get().getArtistrTitle();
    }

    @Rpc(description = "Get Album tile")
    public String getAlbumTitle() {
        return mAutoMediaHelper.get().getAlbumTitle();
    }

    @Rpc(description = "Get song current playing time")
    public String getSongCurrentPlayingTime() {
        return mAutoMediaHelper.get().getSongCurrentPlayingTime();
    }

    @Rpc(description = "Get song max playing time")
    public String getCurrentSongMaxPlayingTime() {
        return mAutoMediaHelper.get().getCurrentSongMaxPlayingTime();
    }

    @Rpc(description = "Is Now Playing label present")
    public boolean isNowPlayingLabelVisible() {
        return mAutoMediaHelper.get().isNowPlayingLabelVisible();
    }

    @Rpc(description = "Is Playlist icon visible")
    public boolean isPlaylistIconVisible() {
        return mAutoMediaHelper.get().isPlaylistIconVisible();
    }
    /** click on continue button */
    @Rpc(description = "click on Continue button on Allow contacts pop up")
    public void clickContactsContinueButton() {
        mAutoMediaHelper.get().clickContactsContinueButton();
    }

    @Rpc(description = "Click on PLaylist icon")
    public void clickOnPlaylistIcon() {
        mAutoMediaHelper.get().clickOnPlaylistIcon();
    }

    @Rpc(description = "Grant restricted parmissions")
    public void grantRestrictedPermissionsForBTMedia(String permission) {
        mAutoMediaHelper.get().grantRestrictedPermissionsForBTMedia(permission);
    }

    /** Scroll Down one page */
    @Rpc(description = "ScrollUp One Page")
    public boolean scrollUpOnePage() {
        return mAutoMediaHelper.get().scrollUpOnePage();
    }
}
