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

package android.tools.flicker

import android.tools.flicker.config.FlickerConfig
import android.tools.io.Reader
import android.tools.withTracing

/** Contains the logic for Flicker as a Service. */
class FlickerServiceImpl(private val flickerConfig: FlickerConfig) : FlickerService {
    override fun detectScenarios(reader: Reader): Collection<ScenarioInstance> {
        validateTrace(reader)

        return withTracing("FlickerService#detectScenarios") {
            flickerConfig.getEntries().flatMap { configEntry ->
                configEntry.extractor.extract(reader).map { traceSlice ->
                    ScenarioInstanceImpl.fromSlice(traceSlice, reader, configEntry)
                }
            }
        }
    }

    private fun validateTrace(reader: Reader) {
        val layersTrace =
            reader.readLayersTrace()
                ?: throw FlickerTraceException(
                    "Missing layers trace: Cannot run Flicker Service without this trace!"
                )
        if (layersTrace.entries.size <= 1) {
            throw FlickerTraceException(
                buildString {
                    appendLine("Layers trace must have at least two entries.")
                    appendLine("This is likely due to nothing happening on the device.")
                    appendLine()
                    appendLine("Checklist:")
                    appendLine("1. Verify the test action (e.g., button click) occurred.")
                    appendLine("2. Check if the screen was off or keyguard was showing.")
                    appendLine("3. Ensure transition duration matches the actual animation.")
                }
            )
        }
    }
}
