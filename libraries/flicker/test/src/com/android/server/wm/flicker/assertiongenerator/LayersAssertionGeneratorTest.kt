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

import com.android.server.wm.flicker.assertiongenerator.common.AssertionObject
import com.android.server.wm.flicker.assertiongenerator.layers.LayersAssertionGenerator
import com.android.server.wm.flicker.getTestTraceDump
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.Scenario
import org.junit.Test

/**
 * Contains [LayersAssertionGenerator] tests.
 *
 * To run this test: `atest FlickerLibTest:LayersAssertionGeneratorTest`
 */
class LayersAssertionGeneratorTest {
    private lateinit var traceDump: DeviceTraceDump
    var layersTrace: LayersTrace? = null
    lateinit var scenario: Scenario
    lateinit var config: Map<Scenario, Array<out LayersTrace>>
    private lateinit var configTraceArray: Array<LayersTrace>
    private lateinit var assertionGen: LayersAssertionGenerator
    lateinit var assertions: Array<AssertionObject>

    fun setup(traceDump: DeviceTraceDump, scenario: Scenario){
        this.traceDump = traceDump
        layersTrace = traceDump.layersTrace
        this.scenario = scenario
        val config = mutableMapOf<Scenario,
                Array<LayersTrace>>()
        configTraceArray = (layersTrace?.let { arrayOf(layersTrace!!) } ?: run { emptyArray() })
        config[scenario] = configTraceArray
        this.config = config
        assertionGen = LayersAssertionGenerator(config)
        assertions = assertionGen.getAssertionsForScenario(scenario)
    }

    @Test
    fun getTracesForScenario_valid(){
        setup(getTestTraceDump(
            "tracefile_reader_data/",
            "wm_trace.winscope",
            "layers_trace.winscope"),
            Scenario.APP_LAUNCH)
        assert(config[scenario].contentEquals(arrayOf(layersTrace)))
    }

    @Test
    fun getAssertionsForScenario_valid_APP_LAUNCH() {
        setup(getTestTraceDump(
            "tracefile_reader_data/",
            "wm_trace.winscope",
            "layers_trace.winscope"),
            Scenario.APP_LAUNCH)
        assert(assertions.contentEquals(AssertionGeneratorTestConsts.expectedAppLaunchAssertions))
    }

    @Test
    fun getAssertionsForScenario_nullTrace(){
        setup(
            DeviceTraceDump(null, null),
            Scenario.APP_LAUNCH
        )
        assert(assertions.isEmpty())
    }
}
