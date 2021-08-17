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

package com.android.server.wm.flicker.service

import android.util.Log
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.service.detectors.AppLaunchDetector
import com.android.server.wm.traces.common.errors.ErrorState
import com.android.server.wm.traces.common.errors.ErrorTrace
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.errors.toProto
import java.io.IOException
import java.nio.file.Files.createDirectories
import java.nio.file.Files.write

/**
 * Invokes the configured detectors and summarizes the results.
 */
class AssertionEngine {
    private val flickerDetectors = listOf<IFlickerDetector>(
        // TODO: Add new detectors to invoke
        AppLaunchDetector()
    )

    fun analyze(wmTrace: WindowManagerTrace, sfTrace: LayersTrace): ErrorTrace {
        val allStates = mutableListOf<ErrorState>()

        for (flickerDetector in flickerDetectors) {
            allStates.addAll(flickerDetector.analyze(wmTrace, sfTrace).entries.asList())
        }

        /* Ensure all error states with same timestamp are merged */
        val errorStates = allStates.distinct()
                .groupBy({ it.timestamp }, { it.errors.asList() })
                .mapValues { ErrorState(it.value.flatten().toTypedArray(), it.key) }
                .values.toTypedArray()

        val errorTrace = ErrorTrace(errorStates, "")
        writeFile(errorTrace)
        return errorTrace
    }

    /**
     * Stores the error trace in a .winscope file
     */
    private fun writeFile(errorTrace: ErrorTrace) {
        val bytes = errorTrace.toProto().toByteArray()
        // TODO(b/196595789): Change the outputDir and testTag based on the test rule parameters
        val fileName = "${errorTrace.hashCode()}.winscope"
        val outFile = getDefaultFlickerOutputDir().resolve(fileName)

        try {
            Log.i("FLICKER_ERROR_TRACE", outFile.toString())
            createDirectories(getDefaultFlickerOutputDir())
            write(outFile, bytes)
        } catch (e: IOException) {
            throw RuntimeException("Unable to create trace file: ${e.message}", e)
        }
    }
}
