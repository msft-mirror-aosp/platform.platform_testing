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

package android.tools.device.traces.io

import android.tools.common.Timestamp
import android.tools.common.io.Artifact
import android.tools.common.io.RunStatus
import android.tools.common.io.TransitionTimeRange

/** Contents of a flicker run (e.g. files, status, event log) */
interface IResultData {
    val transitionTimeRange: TransitionTimeRange
    val executionError: Throwable?
    val artifact: Artifact
    val runStatus: RunStatus

    /** updates the artifact status to [newStatus] */
    fun updateStatus(newStatus: RunStatus): IResultData

    /** @return a subsection of the trace */
    fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): IResultData
}
