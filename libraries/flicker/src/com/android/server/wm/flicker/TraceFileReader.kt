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

import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser

class TraceFileReader {
    companion object {
        /**
         * Creates a device trace dump containing the WindowManager and Layers trace
         * obtained from the contents in a regular trace file, already read and passed as bytearray
         * The parsed traces may contain a multiple
         * [WindowManagerState] or [LayerTraceEntry].
         *
         * @param wmTraceByteArray [WindowManagerTrace] content
         * @param layersTraceByteArray [LayersTrace] content
         */
        @JvmStatic
        fun fromTraceByteArray(wmTraceByteArray: ByteArray?, layersTraceByteArray: ByteArray?):
        DeviceTraceDump {
            val wmTrace = wmTraceByteArray?.let {
                WindowManagerTraceParser.parseFromTrace(wmTraceByteArray)
            }
            val layersTrace = layersTraceByteArray?.let {
                LayersTraceParser.parseFromTrace(data = layersTraceByteArray)
            }
            return DeviceTraceDump(wmTrace, layersTrace)
        }
    }
}
