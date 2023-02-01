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

package com.android.server.wm.flicker.junit

import android.util.Log
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.datastore.CachedResultReader
import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.ITracesCollector
import com.android.server.wm.traces.common.IScenario

class LegacyFlickerTraceCollector(private val scenario: IScenario) : ITracesCollector {
    override fun start() {}

    override fun stop() {}

    override fun getResultReader(): IReader {
        Log.d("FAAS", "LegacyFlickerTraceCollector#getCollectedTraces")
        return CachedResultReader(scenario, DEFAULT_TRACE_CONFIG)
    }
}
