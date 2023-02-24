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

package com.android.server.wm.flicker.io

import androidx.collection.LruCache
import com.android.server.wm.flicker.RunStatus
import java.nio.file.Path

/**
 * Contents of a flicker run (e.g. files, status, event log) using a [LruCache]
 *
 * @param _artifactPath Path to the artifact file
 * @param _transitionTimeRange Transition start and end time
 * @param _executionError Transition execution error (if any)
 * @param _runStatus Status of the run
 */
class ResultDataWithLru(
    _artifactPath: Path,
    _transitionTimeRange: TransitionTimeRange,
    _executionError: Throwable?,
    _runStatus: RunStatus
) : ResultData(_artifactPath, _transitionTimeRange, _executionError, _runStatus) {
    override fun getArtifactBytes(): ByteArray {
        val data = cache[artifactPath] ?: super.getArtifactBytes()
        cache.put(artifactPath, data)
        return data
    }

    override fun updateStatus(newStatus: RunStatus) = apply {
        val newFile = getNewFilePath(newStatus)
        super.updateStatus(newStatus)
        cache.put(newFile, getArtifactBytes())
    }

    companion object {
        private val cache = LruCache<Path, ByteArray>(1)
    }
}
