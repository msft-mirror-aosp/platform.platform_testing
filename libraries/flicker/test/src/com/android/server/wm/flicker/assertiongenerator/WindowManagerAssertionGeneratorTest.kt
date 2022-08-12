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

import com.android.server.wm.flicker.assertiongenerator.AssertionGeneratorTestConsts.Companion.expectedAppLaunchAssertions
import com.android.server.wm.flicker.assertiongenerator.common.AssertionObject
import com.android.server.wm.flicker.assertiongenerator.windowmanager.WindowManagerAssertionGenerator
import com.android.server.wm.flicker.getTestTraceDump
import com.android.server.wm.flicker.service.config.common.Scenario
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import org.junit.Test

/**
 * Contains [WindowManagerAssertionGenerator] tests.
 *
 * To run this test: `atest FlickerLibTest:WindowManagerAssertionGeneratorTest`
 */
class WindowManagerAssertionGeneratorTest {
    private lateinit var traceDump: DeviceTraceDump
    var wmTrace: WindowManagerTrace? = null
    lateinit var scenario: Scenario
    lateinit var config: Map<Scenario, Array<out WindowManagerTrace>>
    private lateinit var configTraceArray: Array<WindowManagerTrace>
    private lateinit var assertionGen: WindowManagerAssertionGenerator
    lateinit var assertions: Array<AssertionObject>

    fun setup(traceDump: DeviceTraceDump, scenario: Scenario){
        this.traceDump = traceDump
        wmTrace = traceDump.wmTrace
        this.scenario = scenario
        val config = mutableMapOf<Scenario,
                Array<WindowManagerTrace>>()
        configTraceArray = (wmTrace?.let { arrayOf(wmTrace!!) } ?: run { emptyArray() })
        config[scenario] = configTraceArray
        this.config = config
        assertionGen = WindowManagerAssertionGenerator(config)
        assertions = assertionGen.getAssertionsForScenario(scenario)
    }

    @Test
    fun getTracesForScenario_valid(){
        setup(
            getTestTraceDump(
            "tracefile_reader_data/",
            "wm_trace.winscope",
            "layers_trace.winscope"),
            Scenario.APP_LAUNCH)
        assert(config[scenario].contentEquals(arrayOf(wmTrace)))
    }

    @Test
    fun getAssertionsForScenario_valid_APP_LAUNCH() {
        setup(getTestTraceDump(
            "tracefile_reader_data/",
            "wm_trace.winscope",
            "layers_trace.winscope"),
            Scenario.APP_LAUNCH)
        assert(assertions.contentEquals(expectedAppLaunchAssertions))
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
