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

import android.tools.common.Timestamps
import android.tools.common.flicker.config.FlickerConfig
import android.tools.common.flicker.config.FlickerConfigEntry
import android.tools.common.flicker.config.ScenarioId
import android.tools.common.flicker.extractors.ScenarioExtractor
import android.tools.common.flicker.extractors.TraceSlice
import android.tools.getTraceReaderFromScenario
import android.tools.rules.CleanFlickerEnvironmentRule
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.mockito.Mockito

/** Contains [FlickerService] tests. To run this test: `atest FlickerLibTest:FlickerServiceTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerServiceTest {

    @Test
    fun generatesAssertionsFromExtractedScenarios() {
        val reader = getTraceReaderFromScenario("AppLaunch")
        val mockFlickerConfig = Mockito.mock(FlickerConfig::class.java)
        val mockScenarioExtractor = Mockito.mock(ScenarioExtractor::class.java)
        val mockConfigEntry =
            FlickerConfigEntry(
                scenarioId = ScenarioId("TEST_SCENARIO"),
                extractor = mockScenarioExtractor,
                assertions = emptyMap()
            )

        val traceSlice =
            TraceSlice(startTimestamp = Timestamps.min(), endTimestamp = Timestamps.max())

        Mockito.`when`(mockFlickerConfig.getEntries()).thenReturn(listOf(mockConfigEntry))
        Mockito.`when`(mockScenarioExtractor.extract(reader)).thenReturn(listOf(traceSlice))

        val service = FlickerService(mockFlickerConfig)
        service.detectScenarios(reader)

        Mockito.verify(mockScenarioExtractor).extract(reader)
    }

    @Test
    fun executesAssertionsReturnedByAssertionFactories() {
        val reader = getTraceReaderFromScenario("AppLaunch")
        val mockFlickerConfig = Mockito.mock(FlickerConfig::class.java)
        val mockScenarioExtractor = Mockito.mock(ScenarioExtractor::class.java)
        val mockConfigEntry =
            FlickerConfigEntry(
                scenarioId = ScenarioId("TEST_SCENARIO"),
                extractor = mockScenarioExtractor,
                assertions = emptyMap()
            )

        val traceSlice =
            TraceSlice(startTimestamp = Timestamps.min(), endTimestamp = Timestamps.max())

        Mockito.`when`(mockFlickerConfig.getEntries()).thenReturn(listOf(mockConfigEntry))
        Mockito.`when`(mockScenarioExtractor.extract(reader)).thenReturn(listOf(traceSlice))

        val service = FlickerService(mockFlickerConfig)
        service.detectScenarios(reader)

        Mockito.verify(mockScenarioExtractor).extract(reader)
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
