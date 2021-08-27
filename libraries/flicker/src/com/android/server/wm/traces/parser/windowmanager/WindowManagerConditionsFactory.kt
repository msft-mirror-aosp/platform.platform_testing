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

package com.android.server.wm.traces.parser.windowmanager

import android.content.ComponentName
import android.view.Display
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.parser.ConditionList
import com.android.server.wm.traces.parser.Condition
import com.android.server.wm.traces.parser.toActivityName
import com.android.server.wm.traces.parser.toLayerName
import com.android.server.wm.traces.parser.toWindowName
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.BLACK_SURFACE_COMPONENT
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.IME_COMPONENT
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.ROTATION_COMPONENT

object WindowManagerConditionsFactory {
    private val navBarWindowName =
        WindowManagerStateHelper.NAV_BAR_COMPONENT.toWindowName()
    private val navBarLayerName =
        WindowManagerStateHelper.NAV_BAR_COMPONENT.toLayerName()
    private val statusBarWindowName =
        WindowManagerStateHelper.STATUS_BAR_COMPONENT.toWindowName()
    private val statusBarLayerName =
        WindowManagerStateHelper.STATUS_BAR_COMPONENT.toLayerName()

    /**
     * Condition to check if the nav bar window is visible
     */
    fun isNavBarVisible(): Condition<WindowManagerStateHelper.Dump> =
        ConditionList(listOf(
            isNavBarWindowVisible(), isNavBarLayerVisible(), isNavBarLayerOpaque()))

    /**
     * Condition to check if the nav bar window is visible
     */
    fun isNavBarWindowVisible(): Condition<WindowManagerStateHelper.Dump> =
        Condition("isNavBarWindowVisible") {
            it.wmState.isWindowVisible(navBarWindowName)
        }

    /**
     * Condition to check if the nav bar layer is visible
     */
    fun isNavBarLayerVisible(): Condition<WindowManagerStateHelper.Dump> =
        isLayerVisible(navBarLayerName)

    /**
     * Condition to check if the nav bar layer is opaque
     */
    fun isNavBarLayerOpaque(): Condition<WindowManagerStateHelper.Dump> =
        Condition("isNavBarLayerOpaque") {
            it.layerState.getLayerWithBuffer(navBarLayerName)
                ?.color?.a ?: 0f == 1f
        }

    /**
     * Condition to check if the status bar window is visible
     */
    fun isStatusBarVisible(): Condition<WindowManagerStateHelper.Dump> =
        ConditionList(listOf(
            isStatusBarWindowVisible(), isStatusBarLayerVisible(), isStatusBarLayerOpaque()))

    /**
     * Condition to check if the nav bar window is visible
     */
    fun isStatusBarWindowVisible(): Condition<WindowManagerStateHelper.Dump> =
        Condition("isStatusBarWindowVisible") {
            it.wmState.isWindowVisible(statusBarWindowName)
        }

    /**
     * Condition to check if the nav bar layer is visible
     */
    fun isStatusBarLayerVisible(): Condition<WindowManagerStateHelper.Dump> =
        isLayerVisible(statusBarLayerName)

    /**
     * Condition to check if the nav bar layer is opaque
     */
    fun isStatusBarLayerOpaque(): Condition<WindowManagerStateHelper.Dump> =
        Condition("isStatusBarLayerOpaque") {
            it.layerState.getLayerWithBuffer(statusBarLayerName)
                ?.color?.a ?: 0f == 1f
        }

    fun isHomeActivityVisible(): Condition<WindowManagerStateHelper.Dump> =
        Condition("isHomeActivityVisible") {
            it.wmState.homeActivity?.isVisible == true
        }

    @JvmOverloads
    fun isAppTransitionIdle(
        displayId: Int = Display.DEFAULT_DISPLAY
    ): Condition<WindowManagerStateHelper.Dump> =
        Condition("isAppTransitionIdle[$displayId]") {
            it.wmState.getDisplay(displayId)
                ?.appTransitionState == WindowManagerState.APP_STATE_IDLE
        }

    fun containsActivity(
        component: ComponentName
    ): Condition<WindowManagerStateHelper.Dump> =
        Condition("containsActivity[${component.toActivityName()}]") {
            it.wmState.containsActivity(component.toActivityName())
        }

    fun containsWindow(
        component: ComponentName
    ): Condition<WindowManagerStateHelper.Dump> =
        Condition("containsWindow[${component.toWindowName()}]") {
            it.wmState.containsWindow(component.toWindowName())
        }

    fun isWindowSurfaceShown(
        windowName: String
    ): Condition<WindowManagerStateHelper.Dump> =
        Condition("isWindowSurfaceShown[$windowName]") {
            it.wmState.isWindowSurfaceShown(windowName)
        }

    fun isWindowSurfaceShown(
        component: ComponentName
    ): Condition<WindowManagerStateHelper.Dump> =
        isWindowSurfaceShown(component.toWindowName())

    fun isActivityVisible(
        component: ComponentName
    ): Condition<WindowManagerStateHelper.Dump> =
        Condition("isActivityVisible") {
            it.wmState.isActivityVisible(component.toActivityName())
        }

    fun isWMStateComplete(): Condition<WindowManagerStateHelper.Dump> =
        Condition("isWMStateComplete") {
            it.wmState.isComplete()
        }

    @JvmOverloads
    fun hasRotation(
        expectedRotation: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): Condition<WindowManagerStateHelper.Dump> {
        val hasRotationCondition = Condition<WindowManagerStateHelper.Dump>(
            "hasRotation[$expectedRotation, display=$displayId]") {
            val currRotation = it.wmState.getRotation(displayId)
            currRotation == expectedRotation
        }
        return ConditionList(listOf(
            hasRotationCondition,
            isLayerVisible(ROTATION_COMPONENT).negate(),
            isLayerVisible(BLACK_SURFACE_COMPONENT).negate(),
            hasLayersAnimating().negate()
        ))
    }

    fun isLayerVisible(
        layerName: String
    ): Condition<WindowManagerStateHelper.Dump> =
        Condition("isLayerVisible[$layerName]") {
            it.layerState.isVisible(layerName)
        }

    fun isLayerVisible(
        component: ComponentName
    ): Condition<WindowManagerStateHelper.Dump> = isLayerVisible(component.toLayerName())

    fun hasLayersAnimating(): Condition<WindowManagerStateHelper.Dump> =
        Condition("hasLayersAnimating") {
            it.layerState.isAnimating()
        }

    @JvmOverloads
    fun isImeShown(
        displayId: Int = Display.DEFAULT_DISPLAY
    ): Condition<WindowManagerStateHelper.Dump> =
        ConditionList(listOf(
            isImeOnDisplay(displayId), isLayerVisible(IME_COMPONENT), isImeSurfaceShown(),
            isWindowSurfaceShown(IME_COMPONENT)))

    private fun isImeOnDisplay(
        displayId: Int = Display.DEFAULT_DISPLAY
    ): Condition<WindowManagerStateHelper.Dump> =
        Condition("isImeOnDisplay[$displayId]") {
            it.wmState.inputMethodWindowState?.displayId == displayId
        }

    private fun isImeSurfaceShown(): Condition<WindowManagerStateHelper.Dump> =
        Condition("isImeSurfaceShown") {
            it.wmState.inputMethodWindowState?.isSurfaceShown == true
        }
}