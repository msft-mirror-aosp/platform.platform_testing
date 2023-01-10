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

package com.android.server.wm.flicker.monitor

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.android.server.wm.flicker.FLICKER_TAG
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path

/** Runnable to record the screen contents and winscope metadata */
class ScreenRecordingRunnable(
    private val outputFile: Path,
    context: Context,
    private val width: Int = 720,
    private val height: Int = 1280
) : Runnable {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var finished = false
    internal var isFrameRecorded = false

    private val metrics: DisplayMetrics
        get() {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)
            return metrics
        }

    private val encoder = createEncoder()
    private val inputSurface = encoder.createInputSurface()
    private val virtualDisplay =
        displayManager.createVirtualDisplay(
            "Recording Display",
            width,
            height,
            metrics.densityDpi,
            inputSurface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            null,
            null
        )
    private val muxer = createMuxer()
    private var metadataTrackIndex = -1
    private var videoTrackIndex = -1

    internal fun stop() {
        encoder.signalEndOfInputStream()
        finished = true
    }

    override fun run() {
        Log.d(FLICKER_TAG, "Starting screen recording to file $outputFile")

        val timestamps = mutableListOf<Long>()
        try {
            // Start encoder and muxer
            encoder.start()
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val bufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_MS)
                if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    prepareMuxer()
                } else if (bufferIndex >= 0) {
                    val elapsedRealTimeMicros = writeSample(bufferIndex, bufferInfo)
                    val endOfStream = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    // end of the stream samples have 0 timestamp
                    if (endOfStream > 0) {
                        break
                    } else {
                        timestamps.add(elapsedRealTimeMicros)
                    }
                }
            }
        } finally {
            // metadata writing ont working yet
            // writeMetadata(timestamps)
            encoder.stop()
            muxer.stop()
            muxer.release()
            encoder.release()
            inputSurface.release()
            virtualDisplay.release()
        }
    }

    /**
     * Fetches a sample from the encoder and writes it to the video file
     *
     * @return sample timestamp (or 0 for invalid buffers)
     */
    private fun writeSample(bufferIndex: Int, bufferInfo: MediaCodec.BufferInfo): Long {
        val data = encoder.getOutputBuffer(bufferIndex)
        return if (data != null) {
            val endOfStream = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM

            if (endOfStream == 0) {
                val outputBuffer =
                    encoder.getOutputBuffer(bufferIndex) ?: error("Unable to acquire next frame")

                muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                isFrameRecorded = true
            }
            encoder.releaseOutputBuffer(bufferIndex, /* render */ false)
            bufferInfo.presentationTimeUs
        } else {
            0
        }
    }

    private fun prepareMuxer() {
        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
        val metadataFormat = MediaFormat()
        metadataFormat.setString(MediaFormat.KEY_MIME, MIME_TYPE_METADATA)
        metadataTrackIndex = muxer.addTrack(metadataFormat)
        muxer.start()
    }

    /**
     * Saves frames presentation time relative to the elapsed realtime clock in microseconds
     * preceded by a Winscope magic string and frame count to a metadata track. This metadata is
     * used by the Winscope tool to sync video with SurfaceFlinger and WindowManager traces.
     *
     * The metadata is written as a binary array as follows:
     * - winscope magic string (kWinscopeMagicString constant), without trailing null char,
     * - the number of recorded frames (as little endian uint32),
     * - for every frame its presentation time relative to the elapsed realtime clock in
     * microseconds (as little endian uint64).
     */
    private fun writeMetadata(timestamps: List<Long>) {
        Log.v(
            FLICKER_TAG,
            "Writing metadata (size=${timestamps.size} " +
                "(timestamps=${timestamps.first()}-${timestamps.last()})"
        )
        val magicStringBytes = WINSCOPE_MAGIC_STRING.toByteArray()
        val size = magicStringBytes.size + Int.SIZE_BYTES + (timestamps.size * Long.SIZE_BYTES)
        val buffer =
            ByteBuffer.allocate(size)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(magicStringBytes)
                .putInt(timestamps.size)
        timestamps.forEach { buffer.putLong(it) }
        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.size = size
        muxer.writeSampleData(metadataTrackIndex, buffer, bufferInfo)
    }

    /**
     * Create and configure a MediaCodec encoder with [MIME_TYPE_VIDEO] format.
     *
     * @return a Surface that can be used to record
     */
    private fun createEncoder(): MediaCodec {
        val format = MediaFormat.createVideoFormat(MIME_TYPE_VIDEO, width, height)
        val displayMode = windowManager.defaultDisplay.mode
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setFloat(MediaFormat.KEY_FRAME_RATE, displayMode.refreshRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        format.setInteger(MediaFormat.KEY_WIDTH, width)
        format.setInteger(MediaFormat.KEY_HEIGHT, height)
        format.setString(MediaFormat.KEY_MIME, MIME_TYPE_VIDEO)

        val mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE_VIDEO)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return mediaCodec
    }

    private fun createMuxer(): MediaMuxer {
        Files.deleteIfExists(outputFile)
        require(!Files.exists(outputFile))
        Files.createFile(outputFile)
        val inputStream = FileOutputStream(outputFile.toFile())
        return MediaMuxer(inputStream.fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    companion object {
        private const val WINSCOPE_MAGIC_STRING = "#VV1NSC0PET1ME!#"
        private const val MIME_TYPE_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MIME_TYPE_METADATA = "application/octet-stream"
        private const val BIT_RATE = 2000000 // 2Mbps
        private const val IFRAME_INTERVAL = 2 // 2 second between I-frames
        private const val TIMEOUT_MS = 100L
    }
}
