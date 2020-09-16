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

package com.android.server.wm.flicker.dsl

/**
 * Identify a trace location. By default all traces have: [START], [END] and [ALL] locations,
 * representing inital, final and all trace states.
 *
 * In addition, it is possible to create custom trace locations (tags).
 *
 * @param tag Tag identifier containing only letters and digits
 * @throws IllegalArgumentException If [tag] contains anything besides letters and digits
 */
data class AssertionTag(val tag: String) {
    init {
        require(!tag.contains(" ")) {
            "The test tag $tag can not contain spaces since it is a part of the file name"
        }
    }

    companion object {
        /**
         * Assert only the initial trace entry (initial state)
         */
        @JvmField
        val START = AssertionTag("start")
        /**
         * Assert only the final trace entry (final state)
         */
        @JvmField
        val END = AssertionTag("end")
        /**
         * Assert all trace entries
         */
        @JvmField
        val ALL = AssertionTag("all")
        /**
         * Default assertions available for all flicker tests
         */
        val DEFAULT = listOf(START, END, ALL)
    }
}