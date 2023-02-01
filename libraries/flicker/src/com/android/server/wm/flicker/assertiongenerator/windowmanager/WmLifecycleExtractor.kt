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

package com.android.server.wm.flicker.assertiongenerator.windowmanager

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.common.ILifecycleExtractor
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer

class WmLifecycleExtractor : ILifecycleExtractor {
    override fun extract(
        traceDump: DeviceTraceDump,
        deviceTraceConfiguration: DeviceTraceConfiguration
    ): ITraceLifecycle? {
        val elementLifecycles = WmTraceLifecycle()
        val trace = traceDump.wmTrace ?: return null
        val traceLength = trace.entries.size
        for ((index, entry) in trace.entries.withIndex()) {
            elementLifecycles.focusedApps.add(entry.focusedApp)
            for (windowContainer in entry.windowContainers) {
                val componentMatcher =
                    Utils.componentNameMatcherFromNameWithConfig(
                        windowContainer.title,
                        deviceTraceConfiguration
                    )
                var wmElementLifecycleWasInitialized = false
                componentMatcher?.run {
                    elementLifecycles[componentMatcher]?.let { it ->
                        it[windowContainer.title]?.let {
                            it.states[index] = windowContainer
                            wmElementLifecycleWasInitialized = true
                        }
                    }
                    if (!wmElementLifecycleWasInitialized) {
                        val statesArray: Array<WindowContainer?> = arrayOfNulls(traceLength)
                        statesArray[index] = windowContainer
                        val wmElementLifecycle = WmElementLifecycle(statesArray.toMutableList())
                        elementLifecycles.add(
                            componentMatcher,
                            windowContainer.title,
                            wmElementLifecycle
                        )
                    }
                }
            }
        }
        return elementLifecycles
    }
}
