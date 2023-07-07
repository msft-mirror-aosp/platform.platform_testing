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

package android.tools.device.flicker.datastore

import android.annotation.SuppressLint
import android.tools.CleanFlickerEnvironmentRule
import android.tools.TEST_SCENARIO
import android.tools.TestTraces
import android.tools.common.io.TraceType
import android.tools.device.traces.TRACE_CONFIG_REQUIRE_CHANGES
import android.tools.newTestResultWriter
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

/** Tests for [CachedResultReaderTest] */
@SuppressLint("VisibleForTests")
class CachedResultReaderTest {
    @Before
    fun setup() {
        DataStore.clear()
    }

    @Test
    fun readFromStore() {
        val writer = newTestResultWriter()
        writer.addTraceResult(TraceType.EVENT_LOG, TestTraces.EventLog.FILE)
        val result = writer.write()
        DataStore.addResult(TEST_SCENARIO, result)
        val reader = CachedResultReader(TEST_SCENARIO, TRACE_CONFIG_REQUIRE_CHANGES)
        val actual = reader.readEventLogTrace()
        Truth.assertWithMessage("Event log size").that(actual).isNotNull()
    }

    companion object {
        @ClassRule @JvmField val cleanFlickerEnvironmentRule = CleanFlickerEnvironmentRule()
    }
}
