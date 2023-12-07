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

package android.tools.device.traces.monitors

import android.tools.common.traces.DeviceTraceDump
import android.tools.device.traces.parsers.DeviceDumpParser
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MonitorUtilsTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    @Test
    fun withTracing() {
        val trace = withTracing {
            device.pressHome()
            device.pressRecentApps()
        }

        validateTrace(trace)
    }

    @Test
    fun recordTraces() {
        val trace = recordTraces {
            device.pressHome()
            device.pressRecentApps()
        }

        val dump = DeviceDumpParser.fromTrace(trace)
        validateTrace(dump)
    }

    private fun validateTrace(dump: DeviceTraceDump) {
        Truth.assertWithMessage("Could not obtain SF trace")
            .that(dump.layersTrace?.entries ?: emptyArray())
            .asList()
            .isNotEmpty()
        Truth.assertWithMessage("Could not obtain WM trace")
            .that(dump.wmTrace?.entries ?: emptyArray())
            .asList()
            .isNotEmpty()
    }
}
