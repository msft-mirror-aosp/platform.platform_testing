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
import androidx.test.uiautomator.UiDevice
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
    private fun getCurrState(): DeviceStateDump {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)
        return uiDevice.getCurrState(instrumentation.uiAutomation)
    }

    @Test
    fun canFetchCurrentDeviceState() {
        val currState = getCurrState()
        Truth.assertThat(currState.wmTraceData).isNotEmpty()
        Truth.assertThat(currState.layersTraceData).isNotEmpty()
    }

    @Test
    fun canParseCurrentDeviceState() {
        val currState = getCurrState()
        Truth.assertThat(currState.wmTrace.entries).hasSize(1)
        Truth.assertThat(currState.wmTrace.entries.first().windows).isNotEmpty()
        Truth.assertThat(currState.layersTrace.entries).hasSize(1)
        Truth.assertThat(currState.layersTrace.entries.first().flattenedLayers).isNotEmpty()
    }
}