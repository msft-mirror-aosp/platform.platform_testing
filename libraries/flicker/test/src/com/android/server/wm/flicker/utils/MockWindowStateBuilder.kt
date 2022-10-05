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

package com.android.server.wm.flicker.utils

import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.ConfigurationContainer
import com.android.server.wm.traces.common.windowmanager.windows.KeyguardControllerState
import com.android.server.wm.traces.common.windowmanager.windows.RootWindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer

class MockWindowStateBuilder() {
    var timestamp = -1L
        private set

    constructor(timestamp: Long) : this() {
        setTimestamp(timestamp)
    }

    init {
        if (timestamp <= 0L) {
            timestamp = ++lastTimestamp
        }
    }

    fun setTimestamp(timestamp: Long): MockWindowStateBuilder = apply {
        require(timestamp > 0) { "Timestamp must be a positive value." }
        this.timestamp = timestamp
        lastTimestamp = timestamp
    }

    fun build(): WindowManagerState {
        return WindowManagerState(
            where = "where",
            policy = null,
            focusedApp = "focusedApp",
            focusedDisplayId = 1,
            _focusedWindow = "focusedWindow",
            inputMethodWindowAppToken = "",
            isHomeRecentsComponent = false,
            isDisplayFrozen = false,
            _pendingActivities = emptyArray(),
            root =
                RootWindowContainer(
                    WindowContainer(
                        title = "root container",
                        token = "",
                        orientation = 1,
                        layerId = 1,
                        _isVisible = true,
                        configurationContainer = ConfigurationContainer(null, null, null),
                        children = emptyArray()
                    )
                ),
            keyguardControllerState =
                KeyguardControllerState.from(
                    isAodShowing = false,
                    isKeyguardShowing = false,
                    keyguardOccludedStates = emptyMap()
                ),
            _timestamp = timestamp.toString()
        )
    }

    companion object {
        private var lastTimestamp = 1L
    }
}
