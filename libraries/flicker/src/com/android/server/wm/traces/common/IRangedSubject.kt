/*
 * Copyright (C) 2020 The Android Open Source Project
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

interface IRangedSubject<Entry> {
    /**
     * Run the assertions for all entries.
     */
    fun forAllEntries()

    /**
     * Run the assertions for entries within the specified time range.
     */
    fun forRange(startTime: Long, endTime: Long) { throw UnsupportedOperationException() }

    /**
     * Run the assertions only in the first entry.
     */
    fun inTheBeginning() { throw UnsupportedOperationException() }

    /**
     * Run the assertions only in the last entry.
     */
    fun atTheEnd() { throw UnsupportedOperationException() }
}