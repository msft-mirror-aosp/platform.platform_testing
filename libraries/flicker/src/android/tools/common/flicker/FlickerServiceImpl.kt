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

package android.tools.common.flicker

import android.tools.common.Logger
import android.tools.common.flicker.config.FlickerServiceConfig
import android.tools.common.flicker.extractors.CombinedScenarioExtractor
import android.tools.common.flicker.extractors.ScenarioExtractor
import android.tools.common.io.Reader

/** Contains the logic for Flicker as a Service. */
internal class FlickerServiceImpl(
    private val scenarioExtractor: ScenarioExtractor =
        CombinedScenarioExtractor(FlickerServiceConfig.getExtractors())
) : FlickerService {
    override fun detectScenarios(reader: Reader): Collection<ScenarioInstance> {
        return Logger.withTracing("FlickerService#detectScenarios") {
            scenarioExtractor.extract(reader)
        }
    }
}
