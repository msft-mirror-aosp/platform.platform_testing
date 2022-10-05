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

package com.android.server.wm.flicker.service

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.helpers.BrowserAppHelper
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [FlickerServiceTracesCollector] tests. To run this test: `atest
 * FlickerLibTest:FlickerServiceTracesCollectorTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerServiceTracesCollectorTest {
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val testApp: BrowserAppHelper = BrowserAppHelper(instrumentation)

    @Before
    fun before() {
        Assume.assumeTrue(isShellTransitionsEnabled)
    }

    @Test
    fun canCollectTraces() {
        val wmHelper = WindowManagerStateHelper(instrumentation)
        val collector = FlickerServiceTracesCollector(getDefaultFlickerOutputDir())
        collector.start()
        testApp.launchViaIntent(wmHelper)
        testApp.exit(wmHelper)
        collector.stop()
        val traces = collector.getCollectedTraces()

        Truth.assertThat(traces.wmTrace.entries).isNotEmpty()
        Truth.assertThat(traces.layersTrace.entries).isNotEmpty()
        Truth.assertThat(traces.transitionsTrace.entries).isNotEmpty()
    }
}
