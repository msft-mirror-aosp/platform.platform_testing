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

package com.android.server.wm.traces.common

import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.layers.Transform.Companion.isFlagSet
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import kotlin.js.JsName

object WindowManagerConditionsFactory {
    private fun getNavBarComponent(wmState: WindowManagerState) =
        if (wmState.isTablet) ComponentMatcher.TASK_BAR else ComponentMatcher.NAV_BAR

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] or [ComponentMatcher.TASK_BAR]
     * windows are visible
     */
    @JsName("isNavOrTaskBarVisible")
    fun isNavOrTaskBarVisible(): Condition<DeviceStateDump> = ConditionList(
        listOf(
            isNavOrTaskBarWindowVisible(),
            isNavOrTaskBarLayerVisible(),
            isNavOrTaskBarLayerOpaque()
        )
    )

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] or [ComponentMatcher.TASK_BAR]
     * windows are visible
     */
    @JsName("isNavOrTaskBarWindowVisible")
    fun isNavOrTaskBarWindowVisible(): Condition<DeviceStateDump> =
        Condition("isNavBarOrTaskBarWindowVisible") {
            val component = getNavBarComponent(it.wmState)
            it.wmState.isWindowSurfaceShown(component)
        }

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] or [ComponentMatcher.TASK_BAR]
     * layers are visible
     */
    @JsName("isNavOrTaskBarLayerVisible")
    fun isNavOrTaskBarLayerVisible(): Condition<DeviceStateDump> =
        Condition("isNavBarOrTaskBarLayerVisible") {
            val component = getNavBarComponent(it.wmState)
            it.layerState.isVisible(component)
        }

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] layer is opaque
     */
    @JsName("isNavOrTaskBarLayerOpaque")
    fun isNavOrTaskBarLayerOpaque(): Condition<DeviceStateDump> =
        Condition("isNavOrTaskBarLayerOpaque") {
            val component = getNavBarComponent(it.wmState)
            it.layerState.getLayerWithBuffer(component)?.color?.isOpaque ?: false
        }

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] window is visible
     */
    @JsName("isNavBarVisible")
    fun isNavBarVisible(): Condition<DeviceStateDump> = ConditionList(
        listOf(
            isNavBarWindowVisible(),
            isNavBarLayerVisible(),
            isNavBarLayerOpaque()
        )
    )

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] window is visible
     */
    @JsName("isNavBarWindowVisible")
    fun isNavBarWindowVisible(): Condition<DeviceStateDump> =
        Condition("isNavBarWindowVisible") {
            it.wmState.isWindowSurfaceShown(ComponentMatcher.NAV_BAR)
        }

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] layer is visible
     */
    @JsName("isNavBarLayerVisible")
    fun isNavBarLayerVisible(): Condition<DeviceStateDump> =
        isLayerVisible(ComponentMatcher.NAV_BAR)

    /**
     * Condition to check if the [ComponentMatcher.NAV_BAR] layer is opaque
     */
    @JsName("isNavBarLayerOpaque")
    fun isNavBarLayerOpaque(): Condition<DeviceStateDump> =
        Condition("isNavBarLayerOpaque") {
            it.layerState.getLayerWithBuffer(ComponentMatcher.NAV_BAR)?.color?.isOpaque ?: false
        }

    /**
     * Condition to check if the [ComponentMatcher.TASK_BAR] window is visible
     */
    @JsName("isTaskBarVisible")
    fun isTaskBarVisible(): Condition<DeviceStateDump> = ConditionList(
        listOf(
            isTaskBarWindowVisible(), isTaskBarLayerVisible(), isTaskBarLayerOpaque()
        )
    )

    /**
     * Condition to check if the [ComponentMatcher.TASK_BAR] window is visible
     */
    @JsName("isTaskBarWindowVisible")
    fun isTaskBarWindowVisible(): Condition<DeviceStateDump> =
        Condition("isTaskBarWindowVisible") {
            it.wmState.isWindowSurfaceShown(ComponentMatcher.TASK_BAR)
        }

    /**
     * Condition to check if the [ComponentMatcher.TASK_BAR] layer is visible
     */
    @JsName("isTaskBarLayerVisible")
    fun isTaskBarLayerVisible(): Condition<DeviceStateDump> =
        isLayerVisible(ComponentMatcher.TASK_BAR)

    /**
     * Condition to check if the [ComponentMatcher.TASK_BAR] layer is opaque
     */
    @JsName("isTaskBarLayerOpaque")
    fun isTaskBarLayerOpaque(): Condition<DeviceStateDump> =
        Condition("isTaskBarLayerOpaque") {
            it.layerState.getLayerWithBuffer(ComponentMatcher.TASK_BAR)?.color?.isOpaque ?: false
        }

    /**
     * Condition to check if the [ComponentMatcher.STATUS_BAR] window is visible
     */
    @JsName("isStatusBarVisible")
    fun isStatusBarVisible(): Condition<DeviceStateDump> = ConditionList(
        listOf(
            isStatusBarWindowVisible(), isStatusBarLayerVisible(), isStatusBarLayerOpaque()
        )
    )

    /**
     * Condition to check if the [ComponentMatcher.STATUS_BAR] window is visible
     */
    @JsName("isStatusBarWindowVisible")
    fun isStatusBarWindowVisible(): Condition<DeviceStateDump> =
        Condition("isStatusBarWindowVisible") {
            it.wmState.isWindowSurfaceShown(ComponentMatcher.STATUS_BAR)
        }

    /**
     * Condition to check if the [ComponentMatcher.STATUS_BAR] layer is visible
     */
    @JsName("isStatusBarLayerVisible")
    fun isStatusBarLayerVisible(): Condition<DeviceStateDump> =
        isLayerVisible(ComponentMatcher.STATUS_BAR)

    /**
     * Condition to check if the [ComponentMatcher.STATUS_BAR] layer is opaque
     */
    @JsName("isStatusBarLayerOpaque")
    fun isStatusBarLayerOpaque(): Condition<DeviceStateDump> =
        Condition("isStatusBarLayerOpaque") {
            it.layerState.getLayerWithBuffer(ComponentMatcher.STATUS_BAR)?.color?.isOpaque ?: false
        }

    @JsName("isHomeActivityVisible")
    fun isHomeActivityVisible(): Condition<DeviceStateDump> =
        Condition("isHomeActivityVisible") { it.wmState.isHomeActivityVisible }

    @JsName("isRecentsActivityVisible")
    fun isRecentsActivityVisible(): Condition<DeviceStateDump> =
        Condition("isRecentsActivityVisible") {
            it.wmState.isHomeActivityVisible || it.wmState.isRecentsActivityVisible
        }

    /**
     * Condition to check if WM app transition is idle
     *
     * Because in shell transitions, active recents animation is running transition (never idle)
     * this method always assumed recents are idle
     */
    @JsName("isAppTransitionIdle")
    fun isAppTransitionIdle(
        displayId: Int
    ): Condition<DeviceStateDump> = Condition("isAppTransitionIdle[$displayId]") {
        (it.wmState.isHomeRecentsComponent && it.wmState.isHomeActivityVisible) ||
            it.wmState.isRecentsActivityVisible ||
            it.wmState.getDisplay(displayId)
                ?.appTransitionState == WindowManagerState.APP_STATE_IDLE
    }

    @JsName("containsActivity")
    fun containsActivity(
        componentMatcher: IComponentMatcher
    ): Condition<DeviceStateDump> =
        Condition("containsActivity[${componentMatcher.toActivityName()}]") {
            it.wmState.containsActivity(componentMatcher)
        }

    @JsName("containsWindow")
    fun containsWindow(componentMatcher: IComponentMatcher): Condition<DeviceStateDump> =
        Condition("containsWindow[${componentMatcher.toWindowName()}]") {
            it.wmState.containsWindow(componentMatcher)
        }

    @JsName("isWindowSurfaceShown")
    fun isWindowSurfaceShown(componentMatcher: IComponentMatcher): Condition<DeviceStateDump> =
        Condition("isWindowSurfaceShown[${componentMatcher.toWindowName()}]") {
            it.wmState.isWindowSurfaceShown(componentMatcher)
        }

    @JsName("isActivityVisible")
    fun isActivityVisible(componentMatcher: IComponentMatcher): Condition<DeviceStateDump> =
        Condition("isActivityVisible[${componentMatcher.toActivityName()}]") {
            it.wmState.isActivityVisible(componentMatcher)
        }

    @JsName("isWMStateComplete")
    fun isWMStateComplete(): Condition<DeviceStateDump> = Condition("isWMStateComplete") {
        it.wmState.isComplete()
    }

    @JsName("hasRotation")
    fun hasRotation(
        expectedRotation: Int,
        displayId: Int
    ): Condition<DeviceStateDump> {
        val hasRotationCondition = Condition<DeviceStateDump>(
            "hasRotation[$expectedRotation, display=$displayId]"
        ) {
            val currRotation = it.wmState.getRotation(displayId)
            currRotation == expectedRotation
        }
        return ConditionList(
            listOf(
                hasRotationCondition,
                isLayerVisible(ComponentMatcher.ROTATION).negate(),
                isLayerVisible(ComponentMatcher.BACK_SURFACE).negate(),
                hasLayersAnimating().negate()
            )
        )
    }

    @JsName("isWindowVisible")
    fun isWindowVisible(
        componentMatcher: IComponentMatcher,
        displayId: Int = 0
    ): Condition<DeviceStateDump> = ConditionList(
        containsActivity(componentMatcher),
        containsWindow(componentMatcher),
        isActivityVisible(componentMatcher),
        isWindowSurfaceShown(componentMatcher),
        isAppTransitionIdle(displayId)
    )

    @JsName("isLayerVisible")
    fun isLayerVisible(componentMatcher: IComponentMatcher): Condition<DeviceStateDump> =
        Condition("isLayerVisible[${componentMatcher.toLayerName()}]") {
            it.layerState.isVisible(componentMatcher)
        }

    @JsName("isLayerVisibleForLayerId")
    fun isLayerVisible(layerId: Int): Condition<DeviceStateDump> =
        Condition("isLayerVisible[layerId=$layerId]") {
            it.layerState.getLayerById(layerId)?.isVisible ?: false
        }

    @JsName("isLayerColorAlphaOne")
    fun isLayerColorAlphaOne(componentMatcher: IComponentMatcher): Condition<DeviceStateDump> =
        Condition("isLayerColorAlphaOne[${componentMatcher.toLayerName()}]") {
            it.layerState.visibleLayers
                .filter { layer -> componentMatcher.layerMatchesAnyOf(layer) }
                .any { layer -> layer.color.isOpaque }
        }

    @JsName("isLayerColorAlphaOneForLayerId")
    fun isLayerColorAlphaOne(layerId: Int): Condition<DeviceStateDump> =
        Condition("isLayerColorAlphaOne[$layerId]") {
            val layer = it.layerState.getLayerById(layerId)
            layer?.color?.a == 1.0f
        }

    @JsName("isLayerTransformFlagSet")
    fun isLayerTransformFlagSet(
        componentMatcher: IComponentMatcher,
        transform: Int
    ): Condition<DeviceStateDump> =
        Condition(
            "isLayerTransformFlagSet[" + "${componentMatcher.toLayerName()},transform=$transform]"
        ) {
            it.layerState.visibleLayers
                .filter { layer -> componentMatcher.layerMatchesAnyOf(layer) }
                .any { layer -> isTransformFlagSet(layer, transform) }
        }

    @JsName("isLayerTransformFlagSetForLayerId")
    fun isLayerTransformFlagSet(layerId: Int, transform: Int): Condition<DeviceStateDump> =
        Condition("isLayerTransformFlagSet[$layerId, $transform]") {
            val layer = it.layerState.getLayerById(layerId)
            layer?.transform?.type?.isFlagSet(transform) ?: false
        }

    @JsName("isLayerTransformIdentity")
    fun isLayerTransformIdentity(layerId: Int): Condition<DeviceStateDump> = ConditionList(
        listOf(
            isLayerTransformFlagSet(layerId, Transform.SCALE_VAL).negate(),
            isLayerTransformFlagSet(layerId, Transform.TRANSLATE_VAL).negate(),
            isLayerTransformFlagSet(layerId, Transform.ROTATE_VAL).negate()
        )
    )

    @JsName("isTransformFlagSet")
    private fun isTransformFlagSet(layer: Layer, transform: Int): Boolean =
        layer.transform.type?.isFlagSet(transform) ?: false

    @JsName("hasLayersAnimating")
    fun hasLayersAnimating(): Condition<DeviceStateDump> = ConditionList(
        Condition("hasLayersAnimating") {
            it.layerState.isAnimating()
        },
        isLayerVisible(ComponentMatcher.SNAPSHOT).negate(),
        isLayerVisible(ComponentMatcher.SPLASH_SCREEN).negate()
    )

    @JsName("isPipWindowLayerSizeMatch")
    fun isPipWindowLayerSizeMatch(layerId: Int): Condition<DeviceStateDump> =
        Condition("isPipWindowLayerSizeMatch[layerId=$layerId]") {
            val pipWindow =
                it.wmState.pinnedWindows
                    .firstOrNull { pinnedWindow -> pinnedWindow.layerId == layerId }
                    ?: error("Unable to find window with layerId $layerId")
            val windowHeight = pipWindow.frame.height.toFloat()
            val windowWidth = pipWindow.frame.width.toFloat()

            val pipLayer = it.layerState.getLayerById(layerId)
            val layerHeight =
                pipLayer?.sourceBounds?.height ?: error("Unable to find layer with id $layerId")
            val layerWidth = pipLayer.sourceBounds.width

            windowHeight == layerHeight && windowWidth == layerWidth
        }

    @JsName("hasPipWindow")
    fun hasPipWindow(): Condition<DeviceStateDump> = Condition("hasPipWindow") {
        it.wmState.hasPipWindow()
    }

    @JsName("isImeShown")
    fun isImeShown(
        displayId: Int
    ): Condition<DeviceStateDump> = ConditionList(
        listOf(
            isImeOnDisplay(displayId),
            isLayerVisible(ComponentMatcher.IME),
            isImeSurfaceShown(),
            isWindowSurfaceShown(ComponentMatcher.IME)
        )
    )

    @JsName("isImeOnDisplay")
    private fun isImeOnDisplay(
        displayId: Int
    ): Condition<DeviceStateDump> = Condition("isImeOnDisplay[$displayId]") {
        it.wmState.inputMethodWindowState?.displayId == displayId
    }

    @JsName("isImeSurfaceShown")
    private fun isImeSurfaceShown(): Condition<DeviceStateDump> =
        Condition("isImeSurfaceShown") {
            it.wmState.inputMethodWindowState?.isSurfaceShown == true
        }

    @JsName("isAppLaunchEnded")
    fun isAppLaunchEnded(taskId: Int): Condition<DeviceStateDump> =
        Condition("containsVisibleAppLaunchWindow[taskId=$taskId]") { dump ->
            val windowStates = dump.wmState.getRootTask(taskId)?.activities?.flatMap {
                it.children.filterIsInstance<WindowState>()
            }
            windowStates != null && windowStates.none {
                it.attributes.type == PlatformConsts.TYPE_APPLICATION_STARTING && it.isVisible
            }
        }
}
