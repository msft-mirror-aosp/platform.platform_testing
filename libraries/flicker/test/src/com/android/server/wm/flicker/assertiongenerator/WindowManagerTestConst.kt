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

package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.common.windowmanager.windows.ConfigurationContainer
import com.android.server.wm.traces.common.windowmanager.windows.KeyguardControllerState
import com.android.server.wm.traces.common.windowmanager.windows.RootWindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowManagerPolicy

class WindowManagerTestConst {
    companion object {
        val wmPolicy_ROTATION_0 =
            WindowManagerPolicy.from(rotation = PlatformConsts.Rotation.ROTATION_0)

        val emptyConfigurationContainer = ConfigurationContainer(null, null, null)
        val emptyWindowContainer =
            WindowContainer(
                "",
                "",
                0,
                0,
                false,
                emptyConfigurationContainer,
                arrayOf(),
                computedZ = 0
            )
        val emptyRootWindowContainer = RootWindowContainer(emptyWindowContainer)
        val emptyKeyguardControllerState = KeyguardControllerState.from(false, false, mapOf())

        val wmTraceState_ROTATION_0 =
            WindowManagerState(
                elapsedTimestamp = 0,
                clockTimestamp = 0,
                where = "",
                policy = wmPolicy_ROTATION_0,
                focusedApp = "",
                focusedDisplayId = 0,
                _focusedWindow = "",
                inputMethodWindowAppToken = "",
                isHomeRecentsComponent = false,
                isDisplayFrozen = false,
                _pendingActivities = arrayOf(),
                root = emptyRootWindowContainer,
                keyguardControllerState = emptyKeyguardControllerState
            )

        val wmTrace_ROTATION_0 = WindowManagerTrace(arrayOf(wmTraceState_ROTATION_0))

        fun createWindowContainer(title: String): WindowContainer {
            return WindowContainer(
                title,
                "",
                0,
                0,
                false,
                emptyConfigurationContainer,
                arrayOf(),
                computedZ = 0
            )
        }
    }
}
