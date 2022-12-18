/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.platform.helpers.Constants.MAX_VERIFICATION_TIME_IN_SECONDS;

import static com.google.common.truth.Truth.assertThat;

import static java.lang.String.format;

import android.app.Instrumentation;
import android.media.AudioManager;
import android.platform.helpers.features.common.VolumeDialog;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

/**
 * Helper utils for Setting and adjusting the device volume
 * @deprecated use classes from the "systemui-tapl" library instead
 */
@Deprecated
public class VolumeUtils {
    private static final Instrumentation mInstrument =
            InstrumentationRegistry.getInstrumentation();
    private static final UiDevice mUidevice = UiDevice.getInstance(mInstrument);

    private static String sTag = "VolumeUtils";
    private static VolumeDialog sVolumeDialog = new VolumeDialog();

    private VolumeUtils() {
    }

    /**
     * Function to set the volume.
     *
     * @param streamType The stream whose volume index should be set.
     * @param volRange   The volume index to set. See {@link AudioManager#getStreamMaxVolume(int)}
     *                   for the largest valid value.
     */
    public static void setVolume(int streamType, int volRange) {
        Log.d(sTag, format("Set[%s] volume to: %s", streamType, volRange));
        getAudioManager().setStreamVolume(streamType, volRange, AudioManager.FLAG_SHOW_UI);
        assertVolumeDialogVisible();
    }

    /**
     * Function to adjust the volume.
     *
     * @param streamType      what type of stream volume you want to adjust.
     * @param adjustDirection The direction to adjust the volume. One of
     *                        {@link AudioManager#ADJUST_LOWER}, {@link AudioManager#ADJUST_RAISE},
     *                        {@link AudioManager#ADJUST_SAME}, {@link AudioManager#ADJUST_MUTE},
     *                        {@link AudioManager#ADJUST_UNMUTE},
     *                        or {@link AudioManager#ADJUST_TOGGLE_MUTE}.
     */
    public static void adjustVolume(int streamType, int adjustDirection) {
        Log.d(sTag, format("Adjust[%s] volume to: %s", streamType, adjustDirection));
        getAudioManager().adjustSuggestedStreamVolume(adjustDirection, streamType,
                AudioManager.FLAG_SHOW_UI);
        assertVolumeDialogVisible();
    }

    /**
     * Asserts that the volume dialog is visible.
     */
    public static void assertVolumeDialogVisible() {
        assertThat(mUidevice.wait(Until.hasObject(sVolumeDialog.getPageTitleSelector()),
                MAX_VERIFICATION_TIME_IN_SECONDS * 1000)).isTrue();
    }

    /**
     * To get an instance of device Audio Manager.
     *
     * @return an instance of devie Audio Manager.
     */
    public static AudioManager getAudioManager() {
        return mInstrument.getContext().getSystemService(AudioManager.class);
    }
}
