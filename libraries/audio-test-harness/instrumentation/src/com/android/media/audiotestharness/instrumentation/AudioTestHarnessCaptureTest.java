/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.media.audiotestharness.instrumentation;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.io.IOException;

/** Sample test that demonstrates the capture functionality of the Audio Test Harness */
public class AudioTestHarnessCaptureTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private static final String TAG = AudioTestHarnessCaptureTest.class.getSimpleName();

    /** On device path to the file that should be played back during the test. */
    private static final String TEST_FILE = "/system/product/media/audio/ringtones/Lollipop.ogg";

    /** Duration that the file should play. */
    private static final int TEST_DURATION = 3 * 1000;

    public AudioTestHarnessCaptureTest() {
        super(MainActivity.class);
    }

    @LargeTest
    public void testPlayAudioFile_outputsAudio() throws Exception {
        // TODO(b/168814241): Add logic to use the harness to capture the recorded audio and
        // assert on it.
        playAudioFile(TEST_FILE, TEST_DURATION);
    }

    /**
     * Plays audio file for given amount of time.
     *
     * <p>Instantiates a MediaPlayer and plays the passed in audioFile for audioPlayDuration
     * milliseconds. If the player fails to instantiate or any exception happened during the play,
     * the test will fail.
     */
    private static void playAudioFile(String audioFile, int audioPlayDuration) {
        Log.v(TAG, String.format("Playing audio file: %s", audioFile));
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build());
            mp.setDataSource(audioFile);
            mp.prepare();
            int duration = mp.getDuration();
            if (duration <= 0) {
                Log.e(TAG, "Failed to grab duration from audio file.");
                fail("AudioFileWithNegativeDuration");
            }
            mp.start();
            // This test demonstrates how to play the audio file from device for certain amount of
            // time, and the test actually runs on host machine so the listener is not adapted here.
            Log.v(
                    TAG,
                    String.format(
                            "Wait for audio file to play for duration: %d", audioPlayDuration));
            SystemClock.sleep(audioPlayDuration);
        } catch (IOException e) {
            Log.e(
                    TAG,
                    String.format("Exception happened while playing audio file: %s", audioFile),
                    e);
            fail("FailedToPlayAudioFile");
        } finally {
            if (mp != null) {
                Log.v(TAG, "Release media player.");
                mp.release();
            }
        }
    }
}
