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

package android.tools.traces.parsers.perfetto

import android.tools.Rotation
import android.tools.traces.wm.Activity
import android.tools.traces.wm.KeyguardControllerState
import android.tools.traces.wm.RootWindowContainer
import android.tools.traces.wm.ScreenOrientation
import android.tools.traces.wm.UserRotationMode
import android.tools.traces.wm.WindowContainer
import android.tools.traces.wm.WindowManagerPolicy
import android.tools.traces.wm.WindowManagerState
import android.tools.traces.wm.WindowState

/** Builder for [WindowManagerState] objects */
class WindowManagerStateBuilder {
    private var realToElapsedTimeOffsetNs: Long = 0L
    private var entry: Args? = null
    private var tokenToContainers: MutableMap<Int, WindowContainer> = mutableMapOf()

    fun setRealToElapsedTimeOffsetNs(value: Long): WindowManagerStateBuilder = apply {
        this.realToElapsedTimeOffsetNs = value
    }

    fun setEntry(value: Args): WindowManagerStateBuilder = apply { this.entry = value }

    fun setContainers(value: Collection<WindowContainer>): WindowManagerStateBuilder = apply {
        val result = mutableMapOf<Int, WindowContainer>()
        value.forEach { container -> result[container.token.hexToInt()] = container }
        this.tokenToContainers = result
    }

    fun build(): WindowManagerState {
        val service =
            entry?.getChild("window_manager_service")
                ?: error("window_manager_service field should not be null")
        val elapsedTimestampNs =
            entry?.getChild("elapsed_realtime_nanos")?.getLong()
                ?: error("elapsed_realtime_nanos field should not be null")
        val realTimestampNs = elapsedTimestampNs + realToElapsedTimeOffsetNs

        updateParents()

        var rootWindowContainer: RootWindowContainer? = null

        tokenToContainers.values.forEach { container ->
            if (container is RootWindowContainer) {
                rootWindowContainer = container
            }
            if (container is WindowState) {
                container.isAppWindow = getIsAppWindow(container)
            }
        }
        if (rootWindowContainer == null) {
            val size = tokenToContainers.size
            error("Root window container not found $size")
        }

        return WindowManagerState(
            elapsedTimestamp = elapsedTimestampNs,
            clockTimestamp = realTimestampNs,
            where = entry?.getChild("where")?.getString() ?: "",
            policy = buildPolicy(service.getChild("policy")!!),
            focusedApp = service.getChild("focused_app")?.getString() ?: "",
            focusedDisplayId = service.getChild("focused_display_id")?.getInt() ?: 0,
            _focusedWindow =
                service.getChild("focused_window")?.getChild("title")?.getString() ?: "",
            inputMethodWindowAppToken =
                service.getChild("input_method_window")?.getChild("hash_code")?.getInt()?.let {
                    Integer.toHexString(it)
                } ?: "",
            isHomeRecentsComponent = rootWindowContainer!!.isHomeRecentsComponent,
            isDisplayFrozen = service.getChild("display_frozen")?.getBoolean() ?: false,
            _pendingActivities = rootWindowContainer!!.pendingActivities,
            root = rootWindowContainer!!,
            keyguardControllerState =
                buildKeyguardControllerState(
                    service.getChild("root_window_container")?.getChild("keyguard_controller")
                ),
        )
    }

    /** Update the parent containers for each trace */
    private fun updateParents() {
        for (container in tokenToContainers.values) {
            val parentToken = container.parentToken ?: continue
            val parentContainer = tokenToContainers[parentToken]
            if (parentContainer == null) {
                error("could not find parent container with token $parentToken")
            }
            parentContainer?.addChild(container)
            container.parent = parentContainer
        }
    }

    private fun getIsAppWindow(container: WindowContainer?): Boolean {
        if (container == null) {
            return false
        }
        if (container is Activity) {
            return true
        }
        return getIsAppWindow(container.parent)
    }

    private fun buildPolicy(windowManagerPolicyProto: Args): WindowManagerPolicy {
        return WindowManagerPolicy.from(
            focusedAppToken =
                windowManagerPolicyProto.getChild("focused_app_token")?.getString() ?: "",
            forceStatusBar =
                windowManagerPolicyProto.getChild("force_status_bar")?.getBoolean() ?: false,
            forceStatusBarFromKeyguard =
                windowManagerPolicyProto.getChild("force_status_bar_from_keyguard")?.getBoolean()
                    ?: false,
            keyguardDrawComplete =
                windowManagerPolicyProto.getChild("keyguard_draw_complete")?.getBoolean() ?: false,
            keyguardOccluded =
                windowManagerPolicyProto.getChild("keyguard_occluded")?.getBoolean() ?: false,
            keyguardOccludedChanged =
                windowManagerPolicyProto.getChild("keyguard_occluded_changed")?.getBoolean()
                    ?: false,
            keyguardOccludedPending =
                windowManagerPolicyProto.getChild("keyguard_occluded_pending")?.getBoolean()
                    ?: false,
            lastSystemUiFlags =
                windowManagerPolicyProto.getChild("last_system_ui_flags")?.getInt() ?: 0,
            orientation =
                ScreenOrientation.fromName(
                        windowManagerPolicyProto.getChild("orientation")?.getString()
                    )
                    ?.value ?: 0,
            rotation =
                Rotation.getByName(windowManagerPolicyProto.getChild("rotation")?.getString())
                    ?: Rotation.ROTATION_0,
            rotationMode =
                UserRotationMode.fromName(
                        windowManagerPolicyProto.getChild("rotation_mode")?.getString()
                    )
                    ?.value ?: 0,
            screenOnFully =
                windowManagerPolicyProto.getChild("screen_on_fully")?.getBoolean() ?: false,
            windowManagerDrawComplete =
                windowManagerPolicyProto.getChild("window_manager_draw_complete")?.getBoolean()
                    ?: false,
        )
    }

    private fun buildKeyguardControllerState(
        keyguardControllerProto: Args?
    ): KeyguardControllerState {
        return KeyguardControllerState.from(
            isAodShowing = keyguardControllerProto?.getChild("aod_showing")?.getBoolean() ?: false,
            isKeyguardShowing =
                keyguardControllerProto?.getChild("keyguard_showing")?.getBoolean() ?: false,
            keyguardOccludedStates =
                keyguardControllerProto?.getChildren("keyguard_occluded_states")?.associate {
                    val displayId = it.getChild("display_id")?.getInt() ?: 0
                    val keyguardOccluded = it.getChild("keyguard_occluded")?.getBoolean() ?: false
                    displayId to keyguardOccluded
                } ?: emptyMap(),
        )
    }
}
