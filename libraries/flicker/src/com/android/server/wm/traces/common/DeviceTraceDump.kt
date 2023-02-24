/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.traces.common

import com.android.server.wm.traces.common.events.EventLog
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import kotlin.js.JsName

/**
 * Represents a state dump containing the [WindowManagerTrace] and the [LayersTrace] both parsed and
 * in raw (byte) data.
 *
 * @param wmTrace Parsed [WindowManagerTrace]
 * @param layersTrace Parsed [LayersTrace]
 * @param transactionsTrace Parse [TransactionsTrace]
 * @param transitionsTrace Parsed [TransitionsTrace]
 * @param eventLog Parsed [EventLog]
 */
class DeviceTraceDump(
    @JsName("wmTrace") val wmTrace: WindowManagerTrace?,
    @JsName("layersTrace") val layersTrace: LayersTrace?,
    @JsName("transactionsTrace") val transactionsTrace: TransactionsTrace? = null,
    @JsName("transitionsTrace") val transitionsTrace: TransitionsTrace? = null,
    @JsName("eventLog") val eventLog: EventLog? = null,
) {
    /** A deviceTraceDump is considered valid if at least one of the layers/wm traces is non-null */
    val isValid: Boolean
        get() {
            if (wmTrace == null && layersTrace == null) {
                return false
            }
            return true
        }
}
