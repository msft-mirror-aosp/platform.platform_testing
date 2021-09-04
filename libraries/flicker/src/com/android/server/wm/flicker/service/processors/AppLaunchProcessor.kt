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

package com.android.server.wm.flicker.service.processors

import com.android.server.wm.flicker.service.ITagProcessor
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.TagState
import com.android.server.wm.traces.common.tags.TagTrace
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

class AppLaunchProcessor : ITagProcessor {
    override fun generateTags(wmTrace: WindowManagerTrace, layersTrace: LayersTrace): TagTrace {
        // TODO(b/196116270): Remove the mock data and add the implementation for App Launch
        return TagTrace(arrayOf(TagState(wmTrace.entries[0].timestamp,
            arrayOf(
                Tag(
                    id = 1,
                    transition = Transition.APP_LAUNCH,
                    isStartTag = true,
                    taskId = 2,
                    windowToken = "",
                    layerId = -1
                )
            )
        ), TagState(wmTrace.entries[1].timestamp,
            arrayOf(
                Tag(
                    id = 1,
                    transition = Transition.APP_LAUNCH,
                    isStartTag = false,
                    taskId = 2,
                    windowToken = "",
                    layerId = -1
                )
            )
        )), "")
    }
}
