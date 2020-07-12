/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker

import androidx.annotation.VisibleForTesting
import java.nio.file.Files
import java.nio.file.Path

/** Stores paths to all test artifacts.  */
class TransitionResult @VisibleForTesting internal constructor(
    val layersTracePath: Path?,
    val layersTraceChecksum: String,
    val windowManagerTracePath: Path?,
    val windowManagerTraceChecksum: String,
    val screenCaptureVideo: Path?,
    val screenCaptureVideoChecksum: String
) {
    private var flaggedForSaving = true

    val layersTrace: ByteArray
        get() {
            return try {
                Files.readAllBytes(layersTracePath)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

    val windowManagerTrace: ByteArray
        get() {
            return try {
                Files.readAllBytes(windowManagerTracePath)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

    fun flagForSaving() {
        flaggedForSaving = true
    }

    fun canDelete(): Boolean {
        return !flaggedForSaving
    }

    fun layersTraceExists(): Boolean {
        return layersTracePath != null && Files.exists(layersTracePath)
    }

    fun windowManagerTraceExists(): Boolean {
        return windowManagerTracePath != null && Files.exists(windowManagerTracePath)
    }

    fun screenCaptureVideoExists(): Boolean {
        return screenCaptureVideo != null && Files.exists(screenCaptureVideo)
    }

    fun screenCaptureVideoPath(): Path? {
        return screenCaptureVideo
    }

    fun delete() {
        if (layersTraceExists()) {
            layersTracePath?.toFile()?.delete()
        }
        if (windowManagerTraceExists()) {
            windowManagerTracePath?.toFile()?.delete()
        }
        if (screenCaptureVideoExists()) {
            screenCaptureVideo?.toFile()?.delete()
        }
    }
}