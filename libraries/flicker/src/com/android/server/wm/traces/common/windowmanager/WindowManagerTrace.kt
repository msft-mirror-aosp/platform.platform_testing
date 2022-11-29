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

package com.android.server.wm.traces.common.windowmanager

import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.Utils.Companion.sliceEntriesByTimestamp
import com.android.server.wm.traces.common.service.PlatformConsts
import kotlin.js.JsName

/**
 * Contains a collection of parsed WindowManager trace entries and assertions to apply over a single
 * entry.
 *
 * Each entry is parsed into a list of [WindowManagerState] objects.
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot access internal
 * Java/Android functionality
 */
data class WindowManagerTrace(override val entries: Array<WindowManagerState>) :
    ITrace<WindowManagerState>, List<WindowManagerState> by entries.toList() {

    @JsName("isTablet")
    val isTablet: Boolean
        get() = entries.any { it.isTablet }

    override fun toString(): String {
        return "WindowManagerTrace(Start: ${entries.firstOrNull()}, " +
            "End: ${entries.lastOrNull()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WindowManagerTrace) return false

        if (!entries.contentEquals(other.entries)) return false

        return true
    }

    override fun hashCode(): Int {
        return entries.contentHashCode()
    }

    /**
     * Split the trace by the start and end timestamp.
     *
     * @param from the start elapsed timestamp
     * @param to the end elapsed timestamp
     * @return the subtrace trace(from, to)
     */
    @JsName("sliceUsingElapsedTimestamp")
    fun sliceUsingElapsedTimestamp(
        from: Long,
        to: Long,
        addInitialEntry: Boolean = false
    ): WindowManagerTrace {
        return WindowManagerTrace(
            sliceEntriesByTimestamp(this.entries, from, to, addInitialEntry) { it.elapsedTimestamp }
        )
    }

    /** Get the initial rotation */
    fun getInitialRotation(): PlatformConsts.Rotation {
        if (entries.isEmpty()) {
            throw RuntimeException("WindowManager Trace has no entries")
        }
        val firstWmState = entries[0]
        return firstWmState.policy?.rotation?.let { PlatformConsts.Rotation.getByValue(it) }
            ?: run { throw RuntimeException("Wm state has no policy") }
    }
}
