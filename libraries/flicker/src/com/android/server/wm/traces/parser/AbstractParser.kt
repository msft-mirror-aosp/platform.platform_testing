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

package com.android.server.wm.traces.parser

import com.android.server.wm.traces.common.Cache

/** Base parser class */
abstract class AbstractParser<InputTypeTrace, OutputTypeTrace> {
    protected abstract val traceName: String
    protected abstract fun doDecodeByteArray(bytes: ByteArray): InputTypeTrace
    protected abstract fun doParse(input: InputTypeTrace): OutputTypeTrace

    /**
     * Uses a [ByteArray] to generates a trace
     *
     * @param bytes Parsed proto data
     * @param clearCache If the caching used while parsing the object should be cleared
     */
    open fun parse(bytes: ByteArray, clearCache: Boolean = true): OutputTypeTrace {
        val input = decodeByteArray(bytes)
        return parse(input, clearCache)
    }

    /**
     * Uses [InputTypeTrace] to generates a trace
     *
     * @param input Parsed proto data
     * @param clearCache If the caching used while parsing the object should be cleared
     */
    open fun parse(input: InputTypeTrace, clearCache: Boolean): OutputTypeTrace {
        return withPerfettoTrace("${this::class.simpleName}#parse") {
            try {
                doParse(input)
            } finally {
                if (clearCache) {
                    Cache.clear()
                }
            }
        }
    }

    protected fun decodeByteArray(input: ByteArray): InputTypeTrace {
        return withPerfettoTrace("${this::class.simpleName}#decodeByteArray") {
            doDecodeByteArray(input)
        }
    }
}
