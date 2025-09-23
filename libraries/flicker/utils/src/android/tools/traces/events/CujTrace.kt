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

package android.tools.traces.events

import android.tools.Timestamp
import android.tools.Trace

class CujTrace(override val entries: Collection<Cuj>) : Trace<Cuj> {
    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): CujTrace {
        return CujTrace(
            entries
                .dropWhile { it.endTimestamp < startTimestamp }
                .dropLastWhile { it.startTimestamp > endTimestamp }
        )
    }
}
