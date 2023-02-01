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

import com.android.server.wm.traces.common.Timestamp

object TestTraces {
    object LayerTrace {
        private const val ASSET = "layers_trace.winscope"
        val START_TIME = Timestamp(systemUptimeNanos = 1618663562444)
        val SLICE_TIME = Timestamp(systemUptimeNanos = 1618715108595)
        val END_TIME = Timestamp(systemUptimeNanos = 1620770824112)
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    object WMTrace {
        private const val ASSET = "wm_trace.winscope"
        val START_TIME = Timestamp(elapsedNanos = 1618650751245)
        val SLICE_TIME = Timestamp(elapsedNanos = 1618730362295)
        val END_TIME = Timestamp(elapsedNanos = 1620756218174)
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    object EventLog {
        private const val ASSET = "eventlog.winscope"
        val START_TIME = Timestamp(unixNanos = 1670594369069951546)
        val SLICE_TIME = Timestamp(unixNanos = 1670594384516466159)
        val END_TIME = Timestamp(unixNanos = 1670594389958451901)
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    object TransactionTrace {
        private const val ASSET = "transactions_trace.winscope"
        val START_TIME = Timestamp(systemUptimeNanos = 1556111744859, elapsedNanos = 1556111744859)
        val VALID_SLICE_TIME =
            Timestamp(systemUptimeNanos = 1556147625539, elapsedNanos = 1556147625539)
        val INVALID_SLICE_TIME = Timestamp(systemUptimeNanos = 1622127714039 + 1)
        val END_TIME = Timestamp(systemUptimeNanos = 1622127714039, elapsedNanos = 1622127714039)
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    object TransitionTrace {
        private const val ASSET = "transition_trace.winscope"
        val START_TIME =
            Timestamp(
                elapsedNanos = 1619596081652,
                systemUptimeNanos = TransactionTrace.START_TIME.systemUptimeNanos
            )
        val VALID_SLICE_TIME =
            Timestamp(
                elapsedNanos = 1620068193157,
                systemUptimeNanos = TransactionTrace.VALID_SLICE_TIME.systemUptimeNanos
            )
        val INVALID_SLICE_TIME =
            Timestamp(
                elapsedNanos = 0L,
                systemUptimeNanos = TransactionTrace.INVALID_SLICE_TIME.systemUptimeNanos
            )
        val END_TIME =
            Timestamp(
                elapsedNanos = 1620120359204,
                systemUptimeNanos = TransactionTrace.END_TIME.systemUptimeNanos
            )
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    val TIME_5 = Timestamp(5, 5, 5)
    val TIME_10 = Timestamp(10, 10, 10)

    val TEST_TRACE_CONFIG =
        TraceConfigs(
            wmTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false),
            layersTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false),
            transitionsTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false),
            transactionsTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false)
        )
}
