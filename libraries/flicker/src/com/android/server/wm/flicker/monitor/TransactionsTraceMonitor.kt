/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker.monitor

import android.view.WindowManagerGlobal
import com.android.server.wm.flicker.FlickerRunResult
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.traces.common.layers.LayersTrace
import java.nio.file.Path

/**
 * Captures [LayersTrace] from SurfaceFlinger.
 *
 * Use [LayersTraceSubject.assertThat] to make assertions on the trace
 */
open class TransactionsTraceMonitor
@JvmOverloads
constructor(
    outputDir: Path = getDefaultFlickerOutputDir(),
    sourceFile: Path = TRACE_DIR.resolve("transactions_trace$WINSCOPE_EXT")
) : TransitionMonitor(outputDir, sourceFile) {

    private val windowManager = WindowManagerGlobal.getWindowManagerService()

    override fun startTracing() {
        windowManager.setActiveTransactionTracing(true)
    }

    override fun stopTracing() {
        windowManager.setActiveTransactionTracing(false)
    }

    override val isEnabled: Boolean
        get() = true

    override fun setResult(result: FlickerRunResult) {
        result.setTransactionsTrace(outputFile.toFile())
    }
}
