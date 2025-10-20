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

package android.tools

import android.tools.io.DumpType
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.traces.NullableDeviceStateDump
import android.tools.traces.getCurrentState
import android.tools.traces.getCurrentStateDumpNullable
import com.google.common.truth.Truth
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Contains [android.os.traces] utils tests. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UtilsTest {
    private fun getCurrState(
        vararg dumpTypes: DumpType = arrayOf(DumpType.SF, DumpType.WM)
    ): ByteArray {
        return getCurrentState(*dumpTypes)
    }

    private fun getCurrStateDump(
        vararg dumpTypes: DumpType = arrayOf(DumpType.SF, DumpType.WM)
    ): NullableDeviceStateDump {
        return getCurrentStateDumpNullable(arrayOf(*dumpTypes), clearCacheAfterParsing = false)
    }

    @Test
    fun canFetchCurrentDeviceState() {
        val currState = this.getCurrState()
        Truth.assertThat(currState).isNotEmpty()
    }

    @Test
    fun canFetchCurrentDeviceStateOnlyWm() {
        val currStateDump = this.getCurrState(DumpType.WM)
        Truth.assertThat(currStateDump).isNotEmpty()
        val currState = this.getCurrStateDump(DumpType.WM)
        Truth.assertThat(currState.wmState).isNotNull()
        Truth.assertThat(currState.layerState).isNull()
    }

    @Test
    fun canFetchCurrentDeviceStateOnlyLayers() {
        val currStateDump = this.getCurrState(DumpType.SF)
        Truth.assertThat(currStateDump).isNotEmpty()
        val currState = this.getCurrStateDump(DumpType.SF)
        Truth.assertThat(currState.wmState).isNull()
        Truth.assertThat(currState.layerState).isNotNull()
    }

    @Test
    fun canParseCurrentDeviceState() {
        val currState = this.getCurrStateDump()
        val wmArray = currState.wmState?.asTrace()?.entries ?: emptyList()
        Truth.assertThat(wmArray).hasSize(1)
        Truth.assertThat(wmArray.first().windowStates).isNotEmpty()
        val layersArray = currState.layerState?.asTrace()?.entries ?: emptyList()
        Truth.assertThat(layersArray).hasSize(1)
        Truth.assertThat(layersArray.first().flattenedLayers).isNotEmpty()
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
