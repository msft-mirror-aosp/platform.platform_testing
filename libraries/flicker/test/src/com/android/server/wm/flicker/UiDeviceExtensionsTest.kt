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

package com.android.server.wm.flicker

import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.traces.parser.DeviceStateDump
import com.android.server.wm.traces.parser.FLAG_STATE_DUMP_FLAG_LAYERS
import com.android.server.wm.traces.parser.FLAG_STATE_DUMP_FLAG_WM
import com.android.server.wm.traces.parser.WmStateDumpFlags
import com.android.server.wm.traces.parser.getCurrentState
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [UiDeviceExtensions] tests.
 *
 * To run this test: `atest FlickerLibTest:UiDeviceExtensionsTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UiDeviceExtensionsTest {
    private fun getCurrState(
        @WmStateDumpFlags dumpFlags: Int = FLAG_STATE_DUMP_FLAG_WM.or(FLAG_STATE_DUMP_FLAG_LAYERS)
    ): DeviceStateDump {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        return getCurrentState(instrumentation.uiAutomation, dumpFlags)
    }

    @Test
    fun canFetchCurrentDeviceState() {
        val currState = getCurrState()
        Truth.assertThat(currState.wmTraceData).isNotEmpty()
        Truth.assertThat(currState.layersTraceData).isNotEmpty()
    }

    @Test
    fun canFetchCurrentDeviceStateOnlyWm() {
        val currState = getCurrState(FLAG_STATE_DUMP_FLAG_WM)
        Truth.assertThat(currState.wmTraceData).isNotEmpty()
        Truth.assertThat(currState.layersTraceData).isEmpty()
        Truth.assertThat(currState.wmTrace).isNotNull()
        Truth.assertThat(currState.layersTrace).isNull()
    }

    @Test
    fun canFetchCurrentDeviceStateOnlyLayers() {
        val currState = getCurrState(FLAG_STATE_DUMP_FLAG_LAYERS)
        Truth.assertThat(currState.wmTraceData).isEmpty()
        Truth.assertThat(currState.layersTraceData).isNotEmpty()
        Truth.assertThat(currState.wmTrace).isNull()
        Truth.assertThat(currState.layersTrace).isNotNull()
    }

    @Test
    fun canParseCurrentDeviceState() {
        val currState = getCurrState()
        Truth.assertThat(currState.wmTrace?.entries).hasSize(1)
        Truth.assertThat(currState.wmTrace?.entries?.first()?.windowStates).isNotEmpty()
        Truth.assertThat(currState.layersTrace?.entries).hasSize(1)
        Truth.assertThat(currState.layersTrace?.entries?.first()?.flattenedLayers).isNotEmpty()
    }
}