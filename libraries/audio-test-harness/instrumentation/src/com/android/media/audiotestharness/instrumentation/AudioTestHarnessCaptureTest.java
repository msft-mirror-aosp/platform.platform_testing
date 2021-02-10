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
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.android.media.audiotestharness.client.core.AudioCaptureStream;
import com.android.media.audiotestharness.client.core.AudioTestHarnessClient;
import com.android.media.audiotestharness.client.grpc.GrpcAudioTestHarnessClient;
import com.android.media.audiotestharness.proto.AudioFormatOuterClass.AudioFormat;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;

/** Sample test that demonstrates the capture functionality of the Audio Test Harness */
public class AudioTestHarnessCaptureTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private static final String TAG = AudioTestHarnessCaptureTest.class.getSimpleName();

    /** On device path to the file that should be played back during the test. */
    private static final String TEST_FILE = "/system/product/media/audio/ringtones/Lollipop.ogg";

    /** Duration that the file should play. */
    private static final int TEST_DURATION = 3;

    /** Amplitude value at which we consider a test PASSED. */
    private static final int TEST_AMPLITUDE_THRESHOLD = 250;

    private static final int NUM_CHANNELS_MONO = 1;
    private static final int BITS_PER_SAMPLE_16BIT = 16;

    public AudioTestHarnessCaptureTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Launch the activity which is required for connections to the Audio Test Harness to go
        // through. Without this, connections result in ECONNREFUSED.
        getActivity();
    }

    /**
     * Tests that Media Player properly outputs audio through the device speaker by checking that
     * the microphone in the box picks up audio playback.
     */
    @LargeTest
    public void testPlayAudioFile_outputsAudio() throws Exception {

        // Create a new Audio Test Harness client, and start a capture session with the harness.
        try (AudioTestHarnessClient audioTestHarnessClient =
                GrpcAudioTestHarnessClient.builder().build()) {
            try (AudioCaptureStream audioCaptureStream = audioTestHarnessClient.startCapture()) {

                // Playback the test file (Lollipop.ogg) and capture three seconds of audio
                // from the harness, ensuring that it is written as a test artifact.
                MediaPlayer mediaPlayer = startAudioFilePlayback(TEST_FILE);
                byte[] captured =
                        captureBlockingFromStream(
                                audioCaptureStream, Duration.ofSeconds(TEST_DURATION));
                writeBytesToAppStorage("testPlayAudioFile_outputsAudio.pcm", captured);
                mediaPlayer.release();

                // Verify that the amplitude was far above ambient and thus audio playback
                // was heard by the microphone.
                int maxAmplitude =
                        calculateMaxAmplitudeFromCapturedAudio(
                                audioCaptureStream.getAudioFormat(), captured);
                Log.v(TAG, String.format("Maximum Amplitude of Capture: %d", maxAmplitude));
                assertTrue(
                        "Could not detect audio playback", maxAmplitude > TEST_AMPLITUDE_THRESHOLD);
            }
        }
    }

    /** Calculates the maximum amplitude of a given recording from provided raw audio data. */
    private static int calculateMaxAmplitudeFromCapturedAudio(
            AudioFormat audioFormat, byte[] capturedAudioBytes) {
        if (audioFormat.getSampleSizeBits() != BITS_PER_SAMPLE_16BIT
                || audioFormat.getChannels() != NUM_CHANNELS_MONO) {
            throw new UnsupportedOperationException(
                    "Currently only supports 16-bit mono audio data");
        }

        // Convert the data into the signed PCM values.
        short[] samples = new short[capturedAudioBytes.length / 2];
        ByteBuffer.wrap(capturedAudioBytes)
                .order(audioFormat.getBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(samples);

        int maxAmplitude = 0;
        for (int i = 0; i < samples.length; i++) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(samples[i]));
        }

        return maxAmplitude;
    }

    /**
     * Captures a given duration of audio from the provided {@link AudioCaptureStream} returning a
     * byte[] of the raw audio.
     *
     * @throws IOException if any errors occur while reading from the {@link AudioCaptureStream}.
     */
    private static byte[] captureBlockingFromStream(
            AudioCaptureStream captureStream, Duration captureDuration) throws IOException {

        // Calculate the bitrate in bytes per second of the incoming stream.
        AudioFormat format = captureStream.getAudioFormat();
        int bitrate =
                (format.getSampleSizeBits()
                                * Math.round(format.getSampleRate())
                                * format.getChannels())
                        / 8;

        // Rate at which we read from the input stream.
        int readRateBytes = bitrate / 10;

        // Number of bytes to read before we know we collected the right duration of audio.
        int captureSizeBytes = (int) (bitrate * captureDuration.getSeconds());

        // Number of bytes to allocate for our capture, slightly larger to account for
        // irregular read amounts from the stream.
        int captureArraySizeBytes = (int) captureSizeBytes + readRateBytes;

        int bytesRead = 0;
        byte[] captureBytes = new byte[captureArraySizeBytes];
        while (bytesRead < captureSizeBytes) {
            bytesRead += captureStream.read(captureBytes, bytesRead, readRateBytes);
        }

        return captureBytes;
    }

    /**
     * Plays audio file for given amount of time.
     *
     * <p>Instantiates a MediaPlayer and plays the passed in audioFile for audioPlayDuration
     * milliseconds. If the player fails to instantiate or any exception happened during the play,
     * the test will fail.
     *
     * @return the MediaPlayer instance.
     */
    private static MediaPlayer startAudioFilePlayback(String audioFile) {
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
        } catch (IOException e) {
            Log.e(
                    TAG,
                    String.format("Exception happened while playing audio file: %s", audioFile),
                    e);
            fail("FailedToPlayAudioFile");
        }

        return mp;
    }

    /**
     * Writes a provided byte[] to app storage with a given filename. These files are picked up as
     * test artifacts after the test completes. Any existing files are overwritten.
     *
     * @throws IOException if any errors occur while writing the file.
     */
    private void writeBytesToAppStorage(String filename, byte[] toWrite) throws IOException {
        File outputFile = new File(getActivity().getFilesDir(), filename);

        if (outputFile.exists()) {
            outputFile.delete();
        }
        outputFile.createNewFile();

        Files.write(toWrite, outputFile);

        Log.v(TAG, String.format("Wrote file (%s) of size (%d)", outputFile, toWrite.length));
    }
}
