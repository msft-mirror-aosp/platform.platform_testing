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

package com.google.android.mobly.snippet.bundled;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoVehicleHardKeysHelper;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

/** Snippet class for exposing Vehicle HardKeys Helper Functions. */
public class VehicleHardKeysSnippet implements Snippet {
    private final HelperAccessor<IAutoVehicleHardKeysHelper> mHelper;

    public VehicleHardKeysSnippet() {
        mHelper = new HelperAccessor<>(IAutoVehicleHardKeysHelper.class);
    }

    /** Snippet for pressReceiveCallKey */
    @Rpc(description = "Press Receive Call Key")
    public void pressReceiveCallKey() {
        //common_typos_disable
        mHelper.get().pressRecieveCallKey();
    }

    /** Snippet for pressEndCallKey */
    @Rpc(description = "Press End Call Key")
    public void pressEndCallKey() {
        mHelper.get().pressEndCallKey();
    }

    /** Snippet for pressMediaNextTrackKey */
    @Rpc(description = "Press media next track key")
    public void pressMediaNextTrackKey() {
        mHelper.get().pressMediaNextTrackKey();
    }

    /** Snippet for pressMediaPreviousTrackKey */
    @Rpc(description = "Press media previous track key")
    public void pressMediaPreviousTrackKey() {
        mHelper.get().pressMediaPreviousTrackKey();
    }

    /** Snippet for tuneVolumeUpKey */
    @Rpc(description = "Tune Volume Up key")
    public void tuneVolumeUpKey() {
        mHelper.get().tuneVolumeUpKey();
    }

    /** Snippet for tuneVolumeDownKey */
    @Rpc(description = "Tune Volume Down key")
    public void tuneVolumeDownKey() {
        mHelper.get().tuneVolumeDownKey();
    }

    /** Snippet for pressBrightnessUpKey */
    @Rpc(description = "Press Brightness Up key")
    public void pressBrightnessUpKey() {
        mHelper.get().pressBrightnessUpKey();
    }

    /** Snippet for pressBrightnessDownKey */
    @Rpc(description = "Press Brightness Down key")
    public void pressBrightnessDownKey() {
        mHelper.get().pressBrightnessDownKey();
    }

    /** Snippet for tuneMuteKey */
    @Rpc(description = "Toggle Mute key")
    public void tuneMuteKey() {
        mHelper.get().tuneMuteKey();
    }
}
