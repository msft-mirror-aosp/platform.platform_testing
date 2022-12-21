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

package com.android.server.wm.traces.parser

import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.NullableDeviceStateDump
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerDumpParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser

/**
 * Represents a state dump containing the [WindowManagerTrace] and the [LayersTrace] both parsed and
 * in raw (byte) data.
 */
class DeviceDumpParser {
    companion object {
        /**
         * Creates a device state dump containing the [WindowManagerTrace] and [LayersTrace]
         * obtained from a `dumpsys` command. The parsed traces will contain a single
         * [WindowManagerState] or [BaseLayerTraceEntry].
         *
         * @param wmTraceData [WindowManagerTrace] content
         * @param layersTraceData [LayersTrace] content
         * @param clearCacheAfterParsing If the caching used while parsing the proto should be
         * ```
         *                               cleared or remain in memory
         * ```
         */
        @JvmStatic
        fun fromNullableDump(
            wmTraceData: ByteArray,
            layersTraceData: ByteArray,
            clearCacheAfterParsing: Boolean
        ): NullableDeviceStateDump {
            return withPerfettoTrace("fromNullableDump") {
                NullableDeviceStateDump(
                    wmState =
                        if (wmTraceData.isNotEmpty()) {
                            WindowManagerDumpParser()
                                .parse(wmTraceData, clearCache = clearCacheAfterParsing)
                                .first()
                        } else {
                            null
                        },
                    layerState =
                        if (layersTraceData.isNotEmpty()) {
                            LayersTraceParser()
                                .parse(layersTraceData, clearCache = clearCacheAfterParsing)
                                .first()
                        } else {
                            null
                        }
                )
            }
        }

        /** See [fromNullableDump] */
        @JvmStatic
        fun fromDump(
            wmTraceData: ByteArray,
            layersTraceData: ByteArray,
            clearCacheAfterParsing: Boolean
        ): DeviceStateDump {
            return withPerfettoTrace("fromDump") {
                val nullableDump =
                    fromNullableDump(wmTraceData, layersTraceData, clearCacheAfterParsing)
                DeviceStateDump(
                    nullableDump.wmState ?: error("WMState dump missing"),
                    nullableDump.layerState ?: error("Layer State dump missing")
                )
            }
        }

        /**
         * Creates a device state dump containing the WindowManager and Layers trace obtained from a
         * regular trace. The parsed traces may contain a multiple [WindowManagerState] or
         * [BaseLayerTraceEntry].
         *
         * @param wmTraceData [WindowManagerTrace] content
         * @param layersTraceData [LayersTrace] content
         * @param clearCache If the caching used while parsing the proto should be
         * ```
         *                               cleared or remain in memory
         * ```
         */
        @JvmStatic
        fun fromTrace(
            wmTraceData: ByteArray,
            layersTraceData: ByteArray,
            clearCache: Boolean
        ): DeviceTraceDump {
            return withPerfettoTrace("fromTrace") {
                DeviceTraceDump(
                    wmTrace =
                        if (wmTraceData.isNotEmpty()) {
                            WindowManagerTraceParser().parse(wmTraceData, clearCache = clearCache)
                        } else {
                            null
                        },
                    layersTrace =
                        if (layersTraceData.isNotEmpty()) {
                            LayersTraceParser().parse(layersTraceData, clearCache = clearCache)
                        } else {
                            null
                        }
                )
            }
        }
    }
}
