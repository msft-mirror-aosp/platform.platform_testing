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

package com.android.server.wm.flicker.service.detectors

import com.android.server.wm.flicker.service.IFlickerDetector
import com.android.server.wm.traces.common.errors.Error
import com.android.server.wm.traces.common.errors.ErrorState
import com.android.server.wm.traces.common.errors.ErrorTrace
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

class AppLaunchDetector : IFlickerDetector {
    override fun analyzeWmTrace(wmTrace: WindowManagerTrace): ErrorTrace {
        // TODO(b/196574615): Remove the mock data and add the assertions for App Launch
        if (wmTrace.entries.isNotEmpty()) {
            return ErrorTrace(arrayOf(ErrorState(
                arrayOf(
                    Error(
                        stacktrace = "test stacktrace",
                        message = "Test message error",
                        layerId = -1,
                        windowToken = "",
                        taskId = 2
                    )
                ), wmTrace.entries.first().timestamp)), source = "")
        }
        return ErrorTrace(emptyArray(), source = "")
    }

    override fun analyzeLayersTrace(layersTrace: LayersTrace): ErrorTrace {
        // TODO(b/196574615): Remove the mock data and add the assertions for App Launch
        if (layersTrace.entries.isNotEmpty()) {
            return ErrorTrace(arrayOf(ErrorState(
                arrayOf(
                    Error(
                        stacktrace = "test stacktrace",
                        message = "Test message error",
                        layerId = 2,
                        windowToken = "",
                        taskId = -1
                    )
                ), layersTrace.entries.last().timestamp)), source = "")
        }
        return ErrorTrace(emptyArray(), source = "")
    }
}
