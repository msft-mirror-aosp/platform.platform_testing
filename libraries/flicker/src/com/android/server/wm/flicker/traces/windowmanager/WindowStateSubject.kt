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

package com.android.server.wm.flicker.traces.windowmanager

import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.assertions.Fact
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.region.RegionSubject
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

/**
 * Subject for [WindowState] objects, used to make assertions over behaviors that occur on a single
 * [WindowState] of a WM state.
 *
 * To make assertions over a layer from a state it is recommended to create a subject using
 * [WindowManagerStateSubject.windowState](windowStateName)
 *
 * Alternatively, it is also possible to use [WindowStateSubject](myWindow).
 *
 * Example:
 * ```
 *    val trace = WindowManagerTraceParser().parse(myTraceFile)
 *    val subject = WindowManagerTraceSubject(trace).first()
 *        .windowState("ValidWindow")
 *        .exists()
 *        { myCustomAssertion(this) }
 * ```
 */
class WindowStateSubject(
    override val parent: WindowManagerStateSubject?,
    override val timestamp: Timestamp,
    val windowState: WindowState?,
    private val windowTitle: String? = null
) : FlickerSubject() {
    val isEmpty: Boolean
        get() = windowState == null
    val isNotEmpty: Boolean
        get() = !isEmpty
    val isVisible: Boolean
        get() = windowState?.isVisible == true
    val isInvisible: Boolean
        get() = windowState?.isVisible == false
    val name: String
        get() = windowState?.name ?: windowTitle ?: ""
    val frame: RegionSubject
        get() = RegionSubject(windowState?.frame, this, timestamp)

    override val selfFacts = listOf(Fact("Window title", "${windowState?.title ?: windowTitle}"))

    /** If the [windowState] exists, executes a custom [assertion] on the current subject */
    operator fun invoke(assertion: Assertion<WindowState>): WindowStateSubject = apply {
        windowState ?: return exists()
        assertion(this.windowState)
    }

    /** Asserts that current subject doesn't exist in the window hierarchy */
    fun doesNotExist(): WindowStateSubject = apply {
        check { "Window '${windowState?.name}' does not exist" }.that(windowState).isEqual(null)
    }

    /** Asserts that current subject exists in the window hierarchy */
    fun exists(): WindowStateSubject = apply {
        check { "Window '$windowTitle' exists" }.that(windowState).isNotEqual(null)
    }

    override fun toString(): String {
        return "WindowState:${windowState?.name}"
    }
}
