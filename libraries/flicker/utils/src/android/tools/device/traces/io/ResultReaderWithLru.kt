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

package android.tools.device.traces.io

import android.tools.common.Logger
import android.tools.common.Timestamp
import android.tools.common.io.FLICKER_IO_TAG
import android.tools.common.io.Reader
import android.tools.common.io.ResultArtifactDescriptor
import android.tools.common.io.TraceType
import android.tools.common.io.TransitionTimeRange
import android.tools.common.traces.events.EventLog
import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.common.traces.wm.WindowManagerTrace
import android.tools.device.traces.TraceConfigs
import android.util.LruCache
import java.io.IOException

/**
 * Helper class to read results from a flicker artifact using a LRU
 *
 * @param result to read from
 * @param traceConfig
 */
open class ResultReaderWithLru(
    result: IResultData,
    traceConfig: TraceConfigs,
    private val reader: ResultReader = ResultReader(result, traceConfig)
) : Reader by reader {
    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readWmTrace(): WindowManagerTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.WM)
        val key = CacheKey(reader.artifact.stableId, descriptor, reader.transitionTimeRange)
        val trace =
            wmTraceCache[key]
                ?: reader.readWmTrace().also {
                    Logger.d(FLICKER_IO_TAG, "Cache miss $descriptor, $reader readWmTraceTrace")
                }
        return trace?.also {
            wmTraceCache.put(key, trace)
            Logger.d(FLICKER_IO_TAG, "Add to cache $descriptor, $reader")
        }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readLayersTrace(): LayersTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.SF)
        val key = CacheKey(reader.artifact.stableId, descriptor, reader.transitionTimeRange)
        val trace =
            layersTraceCache[key]
                ?: reader.readLayersTrace().also {
                    Logger.d(FLICKER_IO_TAG, "Cache miss $descriptor, $reader readLayersTrace")
                }
        return trace?.also {
            layersTraceCache.put(key, trace)
            Logger.d(FLICKER_IO_TAG, "Add to cache $descriptor, $reader")
        }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readEventLogTrace(): EventLog? {
        val descriptor = ResultArtifactDescriptor(TraceType.EVENT_LOG)
        val key = CacheKey(reader.artifact.stableId, descriptor, reader.transitionTimeRange)
        val trace =
            eventLogCache[key]
                ?: reader.readEventLogTrace().also {
                    Logger.d(FLICKER_IO_TAG, "Cache miss $descriptor, $reader readEventLogTrace")
                }
        return trace?.also {
            eventLogCache.put(key, trace)
            Logger.d(FLICKER_IO_TAG, "Add to cache $descriptor")
        }
    }

    /** {@inheritDoc} */
    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): ResultReaderWithLru {
        val slicedReader = reader.slice(startTimestamp, endTimestamp)
        return ResultReaderWithLru(slicedReader.result, slicedReader.traceConfig, slicedReader)
    }

    companion object {
        data class CacheKey(
            private val artifact: String,
            private val descriptor: ResultArtifactDescriptor,
            private val transitionTimeRange: TransitionTimeRange
        )

        private val wmTraceCache = LruCache<CacheKey, WindowManagerTrace>(5)
        private val layersTraceCache = LruCache<CacheKey, LayersTrace>(5)
        private val eventLogCache = LruCache<CacheKey, EventLog>(5)
    }
}
