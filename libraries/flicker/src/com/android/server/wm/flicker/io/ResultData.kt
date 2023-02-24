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

package com.android.server.wm.flicker.io

import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.Utils
import java.nio.file.Files
import java.nio.file.Path

/**
 * Contents of a flicker run (e.g. files, status, event log)
 *
 * @param _artifactPath Path to the artifact file
 * @param _transitionTimeRange Transition start and end time
 * @param _executionError Transition execution error (if any)
 * @param _runStatus Status of the run
 */
open class ResultData(
    _artifactPath: Path,
    _transitionTimeRange: TransitionTimeRange,
    _executionError: Throwable?,
    _runStatus: RunStatus
) {
    var artifactPath: Path = _artifactPath
        private set
    var transitionTimeRange: TransitionTimeRange = _transitionTimeRange
        private set
    var executionError: Throwable? = _executionError
        private set
    var runStatus: RunStatus = _runStatus
        private set

    open fun getArtifactBytes(): ByteArray = Files.readAllBytes(artifactPath)

    protected fun getNewFilePath(newStatus: RunStatus): Path {
        val currTestName = artifactPath.fileName.toString().dropWhile { it != '_' }
        return artifactPath.resolveSibling("${newStatus.prefix}_$currTestName")
    }

    /** updates the artifact status to [newStatus] */
    internal open fun updateStatus(newStatus: RunStatus) = apply {
        val currFile = artifactPath
        require(RunStatus.fromFile(currFile) != RunStatus.UNDEFINED) {
            "File name should start with a value from `RunStatus`, instead it was $currFile"
        }
        val newFile = getNewFilePath(newStatus)
        if (currFile != newFile) {
            Utils.moveFile(currFile, newFile)
            artifactPath = newFile
            runStatus = newStatus
        }
    }

    override fun toString(): String = buildString {
        append(artifactPath)
        append(" (status=")
        append(runStatus)
        executionError?.let {
            append(", error=")
            append(it.message)
        }
        append(") ")
    }
}
