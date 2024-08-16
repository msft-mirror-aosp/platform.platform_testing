/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.tools.traces.protolog

import android.tools.Timestamps
import android.tools.TraceEntry
import com.android.internal.protolog.common.LogLevel

class ProtoLogMessage(
    elapsedTimestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stacktrace: String?,
    val location: String?,
) : TraceEntry {
    override val timestamp = Timestamps.from(elapsedNanos = elapsedTimestamp)
}
