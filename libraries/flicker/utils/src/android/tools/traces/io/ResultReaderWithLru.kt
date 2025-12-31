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

package android.tools.traces.io

import android.tools.Timestamp
import android.tools.io.FLICKER_IO_TAG
import android.tools.io.Reader
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.TraceType
import android.tools.io.TransitionTimeRange
import android.tools.traces.events.CujTrace
import android.tools.traces.events.EventLog
import android.tools.traces.protolog.ProtoLogTrace
import android.tools.traces.surfaceflinger.LayersTrace
import android.tools.traces.surfaceflinger.TransactionsTrace
import android.tools.traces.wm.TransitionsTrace
import android.tools.traces.wm.WindowManagerTrace
import android.tools.withTracing
import android.util.Log
import android.util.LruCache
import java.io.IOException

/**
 * Helper class to read results from a flicker artifact using a LRU
 *
 * @param result to read from
 */
open class ResultReaderWithLru(
    result: IResultData,
    private val reader: ResultReader = ResultReader(result),
) : Reader by reader {
    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readWmTrace(): WindowManagerTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return wmTraceCache.logAndReadTrace(key) { reader.readWmTrace() }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readLayersTrace(): LayersTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return layersTraceCache.logAndReadTrace(key) { reader.readLayersTrace() }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readEventLogTrace(): EventLog? {
        val descriptor = ResultArtifactDescriptor(TraceType.EVENT_LOG)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return eventLogCache.logAndReadTrace(key) { reader.readEventLogTrace() }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readWmState(tag: String): WindowManagerTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO, tag)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return wmTraceCache.logAndReadTrace(key) { reader.readWmState(tag) }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readLayersDump(tag: String): LayersTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO, tag)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return layersTraceCache.logAndReadTrace(key) { reader.readLayersDump(tag) }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readTransactionsTrace(): TransactionsTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return transactionsTraceCache.logAndReadTrace(key) { reader.readTransactionsTrace() }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readTransitionsTrace(): TransitionsTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return transitionsTraceCache.logAndReadTrace(key) { reader.readTransitionsTrace() }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readProtoLogTrace(): ProtoLogTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return protoLogTraceCache.logAndReadTrace(key) { reader.readProtoLogTrace() }
    }

    /** {@inheritDoc} */
    @Throws(IOException::class)
    override fun readCujTrace(): CujTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
        val artifact = reader.artifacts.firstOrNull { it.hasTrace(descriptor) } ?: return null
        val key = CacheKey(artifact.stableId, descriptor, reader.transitionTimeRange)
        return cujTraceCache.logAndReadTrace(key) { reader.readCujTrace() }
    }

    /** {@inheritDoc} */
    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): ResultReaderWithLru {
        val slicedReader = reader.slice(startTimestamp, endTimestamp)
        return ResultReaderWithLru(slicedReader.result, slicedReader)
    }

    private fun <TraceType> LruCache<CacheKey, TraceType>.logAndReadTrace(
        key: CacheKey,
        predicate: () -> TraceType?,
    ): TraceType? {
        return withTracing("logAndReadTrace") {
            var value = this[key]
            if (value == null) {
                value =
                    withTracing("cache miss") {
                        Log.d(FLICKER_IO_TAG, "Cache miss $key, $reader")
                        predicate()
                    }
            }

            if (value != null) {
                this.put(key, value)
                Log.d(FLICKER_IO_TAG, "Add to cache $key, $reader")
            }
            value
        }
    }

    companion object {
        data class CacheKey(
            private val artifact: String,
            internal val descriptor: ResultArtifactDescriptor,
            private val transitionTimeRange: TransitionTimeRange,
        )

        private val wmTraceCache = LruCache<CacheKey, WindowManagerTrace>(5)
        private val layersTraceCache = LruCache<CacheKey, LayersTrace>(5)
        private val eventLogCache = LruCache<CacheKey, EventLog>(5)
        private val transactionsTraceCache = LruCache<CacheKey, TransactionsTrace>(5)
        private val transitionsTraceCache = LruCache<CacheKey, TransitionsTrace>(5)
        private val protoLogTraceCache = LruCache<CacheKey, ProtoLogTrace>(5)
        private val cujTraceCache = LruCache<CacheKey, CujTrace>(5)
    }
}
