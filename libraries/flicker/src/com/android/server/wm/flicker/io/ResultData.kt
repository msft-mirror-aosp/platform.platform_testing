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
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import java.nio.file.Path

/**
 * Contents of a flicker run (e.g. files, status, event log)
 *
 * @param _artifactPath Path to the artifact file
 * @param _eventLog Event log contents // TODO: Move to a file in the future
 * @param _transitionTimeRange Transition start and end time
 * @param _executionError Transition execution error (if any)
 * @param _runStatus Status of the run
 */
class ResultData(
    _artifactPath: Path,
    _eventLog: List<FocusEvent>?,
    _transitionTimeRange: TransitionTimeRange,
    _executionError: Throwable?,
    _runStatus: RunStatus
) {
    var artifactPath: Path = _artifactPath
        private set
    var eventLog: List<FocusEvent>? = _eventLog
        private set
    var transitionTimeRange: TransitionTimeRange = _transitionTimeRange
        private set
    var executionError: Throwable? = _executionError
        private set
    var runStatus: RunStatus = _runStatus
        private set

    /** updates the artifact status to [newStatus] */
    internal fun updateStatus(newStatus: RunStatus) = apply {
        val currFile = artifactPath
        require(RunStatus.fromFile(currFile) != RunStatus.UNDEFINED) {
            "File name should start with a value from `RunStatus`, instead it was $currFile"
        }
        val currTestName = currFile.fileName.toString().dropWhile { it != '_' }
        val newFile = currFile.resolveSibling("${newStatus.prefix}_$currTestName")
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
