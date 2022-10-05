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

import android.util.Log
import com.android.server.wm.flicker.readLayerTraceFromFile
import com.android.server.wm.flicker.readTransactionsTraceFromFile
import com.android.server.wm.flicker.readTransitionsTraceFromFile
import com.android.server.wm.flicker.readWmTraceFromFile
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [AssertionEngine] tests. To run this test: `atest FlickerLibTest:AssertionEngineTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AssertionEngineTest {
    private val assertionEngine =
        AssertionEngine(AssertionGeneratorConfigProducer()) { m -> Log.d("AssertionEngineTest", m) }

    @Test
    fun canHandleTransactionsWithNoVSync() {
        // Some start/finish transition transactions in this trace have no vSyncIds
        val path = "service/CloseAppBackButtonTest_ROTATION_90_GESTURAL_NAV_with_no_vsyncids"
        val wmTrace = readWmTraceFromFile("$path/wm_trace.winscope")
        val layersTrace = readLayerTraceFromFile("$path/layers_trace.winscope")
        val transactionsTrace = readTransactionsTraceFromFile("$path/transactions_trace.winscope")
        val transitionsTrace =
            readTransitionsTraceFromFile("$path/transition_trace.winscope", transactionsTrace)

        assertionEngine.analyze(wmTrace, layersTrace, transitionsTrace)
    }
}
