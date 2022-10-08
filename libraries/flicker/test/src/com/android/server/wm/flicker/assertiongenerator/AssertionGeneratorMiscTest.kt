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

package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.AssertionGenConfigTestConst.Companion.deviceTraceConfigurationTestFile
import com.android.server.wm.flicker.assertiongenerator.AssertionGenConfigTestConst.Companion.expectedOpenComponentTypeMatcher
import com.android.server.wm.flicker.service.AssertionGeneratorConfigProducer
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.service.ScenarioType
import com.google.common.truth.Truth
import org.junit.Test

/**
 * Contains [Utils] tests.
 *
 * To run this test: `atest FlickerLibTest:AssertionGeneratorMiscTest`
 */
class AssertionGeneratorMiscTest {
    @Test
    fun Utils_componentNameMatcherFromNameWithConfig_junk() {
        val openCompMatcher =
            Utils.componentNameMatcherFromNameWithConfig(
                "Extra junk@com.android.server.wm.flicker.testapp/.SimpleActivity#1755",
                deviceTraceConfigurationTestFile
            )
        Truth.assertThat(openCompMatcher).isEqualTo(expectedOpenComponentTypeMatcher)
    }

    /** It always fails, was made to easily extract layer names as an "error" message */
    // @Test
    fun getLayerNames() {
        val defaultConfigProducer = AssertionGeneratorConfigProducer()
        val config = defaultConfigProducer.produce()
        val scenario = Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0)
        val traceDump = config[scenario]?.deviceTraceDumps?.get(0)
        traceDump ?: run { throw RuntimeException("No deviceTraceDump for scenario APP_LAUNCH") }
        val layersTrace = traceDump.layersTrace
        val layersEntry = layersTrace!!.entries[0]
        var layerStr = ""
        for (layer in layersEntry.flattenedLayers) {
            layerStr += layer.name + "\n"
        }
        throw RuntimeException(layerStr)
    }
}
