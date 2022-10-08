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

package com.android.server.wm.flicker

import com.android.server.wm.flicker.assertiongenerator.AssertionGenConfigTestConst
import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfigurationSimplified
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.service.ScenarioType
import com.google.common.truth.Truth
import com.google.gson.reflect.TypeToken
import org.junit.Test

/**
 * Contains [TraceFileReader] tests
 *
 * To run this test: `atest FlickerLibTest:TraceFileReaderTest`
 */
class TraceFileReaderTest {
    @Test
    fun getGoldenTracesConfig() {
        val config = TraceFileReader.getGoldenTracesConfig("/assertiongenerator_config_test")
        val scenario = Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0)
        val expectedAppLaunchConfig_traceConfigurations =
            arrayOf(AssertionGenConfigTestConst.deviceTraceConfigurationTestFile)
        Truth.assertThat(config[scenario]?.traceConfigurations)
            .isEqualTo(expectedAppLaunchConfig_traceConfigurations)
        Truth.assertThat(config[scenario]?.deviceTraceDumps?.get(0)?.isValid).isTrue()
    }

    @Test
    fun readJsonFromString() {
        val jsonAsString =
            "[\n" +
                "  {\n" +
                "    \"componentToTypeMap\": {\n" +
                "      \"openingLayerName_toBeCompleted\": \"OPENING_APP\",\n" +
                "      \"closingLayerName_toBeCompleted\": \"CLOSING_APP\"\n" +
                "    }\n" +
                "  }\n" +
                "]"
        val typeToken = object : TypeToken<List<DeviceTraceConfigurationSimplified>>() {}.type
        val jsonAsObject =
            TraceFileReader.readJsonFromString<DeviceTraceConfigurationSimplified>(
                jsonAsString,
                typeToken
            )
        val actualTraceConfig = DeviceTraceConfiguration.fromSimplifiedTrace(jsonAsObject[0])
        Truth.assertThat(actualTraceConfig)
            .isEqualTo(AssertionGenConfigTestConst.deviceTraceConfiguration)
    }

    @Test
    fun readObjectFromResource() {
        val layersConfigPath =
            "/assertiongenerator_config_test/AppLaunch/ROTATION_0/trace1/" +
                "layers_trace_configuration.json"
        val layersTraceConfiguration =
            layersConfigPath.let {
                val layersType =
                    object : TypeToken<List<DeviceTraceConfigurationSimplified>>() {}.type
                TraceFileReader.readObjectFromResource<DeviceTraceConfigurationSimplified>(
                        layersConfigPath,
                        layersType
                    )
                    ?.let { DeviceTraceConfiguration.fromSimplifiedTrace(it[0]) }
            }
        Truth.assertThat(layersTraceConfiguration)
            .isEqualTo(AssertionGenConfigTestConst.deviceTraceConfiguration)
    }
}
