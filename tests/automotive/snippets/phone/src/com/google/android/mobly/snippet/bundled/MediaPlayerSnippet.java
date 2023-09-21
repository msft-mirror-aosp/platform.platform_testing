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

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

public class MediaPlayerSnippet implements Snippet {

    private final HelperAccessor<IAutoMediaHelper> mAutoMediaHelper =
            new HelperAccessor<>(IAutoMediaHelper.class);

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
    public void open() {
        mAutoMediaHelper.get().open();
    }
}