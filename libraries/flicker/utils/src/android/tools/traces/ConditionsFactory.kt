/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.traces

import android.content.Context
import android.content.res.Resources
import android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY
import android.hardware.devicestate.DeviceStateManager
import android.hardware.devicestate.feature.flags.Flags as DeviceStateManagerFlags
import android.tools.PlatformConsts
import android.tools.Rotation
import android.tools.traces.component.ComponentNameMatcher
import android.tools.traces.component.ComponentNameMatcher.Companion.BUBBLE
import android.tools.traces.component.IComponentMatcher
import android.tools.traces.surfaceflinger.Layer
import android.tools.traces.surfaceflinger.Transform
import android.tools.traces.surfaceflinger.Transform.Companion.isFlagSet
import android.tools.traces.wm.WindowState
import androidx.test.platform.app.InstrumentationRegistry

object ConditionsFactory {

    /** Check if this is a phone device instead of a folded foldable. */
    fun isPhoneNavBar(): Boolean {
        val isPhone: Boolean
        if (DeviceStateManagerFlags.deviceStatePropertyMigration()) {
            val context = InstrumentationRegistry.getInstrumentation().context
            val deviceStateManager =
                context.getSystemService(Context.DEVICE_STATE_SERVICE) as DeviceStateManager?
            isPhone =
                deviceStateManager?.supportedDeviceStates?.any {
                    it.hasProperty(PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY)
                } ?: true
        } else {
            val foldedDeviceStatesId: Int =
                Resources.getSystem().getIdentifier("config_foldedDeviceStates", "array", "android")
            isPhone =
                if (foldedDeviceStatesId != 0) {
                    Resources.getSystem().getIntArray(foldedDeviceStatesId).isEmpty()
                } else {
                    true
                }
        }
        return isPhone
    }

    fun getNavBarComponentOrLegacy(): IComponentMatcher {
        // FlickerLib tests may still use legacy navigation bar.
        return ComponentNameMatcher.NAV_BAR_LEGACY.or(ComponentNameMatcher.NAV_BAR)
    }

    /**
     * Condition to check if the [ComponentNameMatcher.NAV_BAR] or [ComponentNameMatcher.TASK_BAR]
     * windows are visible
     */
    @JvmOverloads
    fun isNavOrTaskBarVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        ConditionList(
            listOf(
                isNavOrTaskBarWindowVisible(displayId),
                isNavOrTaskBarLayerVisible(),
                isNavOrTaskBarLayerOpaque(),
            )
        )

    /**
     * Condition to check if the [ComponentNameMatcher.NAV_BAR] or [ComponentNameMatcher.TASK_BAR]
     * windows are visible
     */
    @JvmOverloads
    fun isNavOrTaskBarWindowVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isNavBarOrTaskBarWindowVisible[display=$displayId]") {
            val component = getNavBarComponentOrLegacy()
            it.wmState.isWindowSurfaceShown(component, displayId)
        }

    /**
     * Condition to check if the [ComponentNameMatcher.NAV_BAR] or [ComponentNameMatcher.TASK_BAR]
     * layers are visible
     */
    @JvmOverloads
    fun isNavOrTaskBarLayerVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isNavBarOrTaskBarLayerVisible[display=$displayId]") {
            val component = getNavBarComponentOrLegacy()
            it.layerState.isVisible(component, displayId)
        }

    /** Condition to check if the [ComponentNameMatcher.NAV_BAR] layer is opaque */
    @JvmOverloads
    fun isNavOrTaskBarLayerOpaque(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isNavOrTaskBarLayerOpaque[display=$displayId]") {
            val component = getNavBarComponentOrLegacy()
            it.layerState.getLayerWithBuffer(component, displayId)?.color?.alpha() == 1.0f
        }

    /** Condition to check if the [ComponentNameMatcher.NAV_BAR] window is visible */
    @JvmOverloads
    fun isNavBarVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        ConditionList(
            listOf(
                isNavBarWindowVisible(displayId),
                isNavBarLayerVisible(displayId),
                isNavBarLayerOpaque(displayId),
            )
        )

    /**
     * Condition to check if the [ComponentNameMatcher.NAV_BAR] window is visible on a specific
     * display
     */
    @JvmOverloads
    fun isNavBarWindowVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isNavBarWindowVisible[$displayId]") {
            it.wmState.isWindowSurfaceShown(ComponentNameMatcher.NAV_BAR, displayId)
        }

    /** Condition to check if the [ComponentNameMatcher.NAV_BAR] layer is visible */
    @JvmOverloads
    fun isNavBarLayerVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        isLayerVisible(ComponentNameMatcher.NAV_BAR, displayId)

    /** Condition to check if the [ComponentNameMatcher.NAV_BAR] layer is opaque */
    @JvmOverloads
    fun isNavBarLayerOpaque(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isNavBarLayerOpaque[display=$displayId]") {
            it.layerState
                .getLayerWithBuffer(ComponentNameMatcher.NAV_BAR, displayId)
                ?.color
                ?.alpha() == 1.0f
        }

    /** Condition to check if the [ComponentNameMatcher.TASK_BAR] window is visible */
    @JvmOverloads
    fun isTaskBarVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        ConditionList(
            listOf(
                isTaskBarWindowVisible(displayId),
                isTaskBarLayerVisible(displayId),
                isTaskBarLayerOpaque(displayId),
            )
        )

    /**
     * Condition to check if the [ComponentNameMatcher.TASK_BAR] window is visible on a specific
     * display
     */
    @JvmOverloads
    fun isTaskBarWindowVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isTaskBarWindowVisible[$displayId]") {
            it.wmState.isWindowSurfaceShown(ComponentNameMatcher.TASK_BAR, displayId)
        }

    /** Condition to check if the [ComponentNameMatcher.TASK_BAR] layer is visible */
    @JvmOverloads
    fun isTaskBarLayerVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        isLayerVisible(ComponentNameMatcher.TASK_BAR, displayId)

    /** Condition to check if the [ComponentNameMatcher.TASK_BAR] layer is opaque */
    @JvmOverloads
    fun isTaskBarLayerOpaque(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isTaskBarLayerOpaque[display=$displayId]") {
            it.layerState
                .getLayerWithBuffer(ComponentNameMatcher.TASK_BAR, displayId)
                ?.color
                ?.alpha() == 1.0f
        }

    /** Condition to check if the [ComponentNameMatcher.STATUS_BAR] window is visible */
    @JvmOverloads
    fun isStatusBarVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        ConditionList(
            listOf(
                isStatusBarWindowVisible(displayId),
                isStatusBarLayerVisible(displayId),
                isStatusBarLayerOpaque(displayId),
            )
        )

    /**
     * Condition to check if the [ComponentNameMatcher.STATUS_BAR] window is visible on a specific
     * display
     */
    @JvmOverloads
    fun isStatusBarWindowVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isStatusBarWindowVisible[$displayId]") {
            it.wmState.isWindowSurfaceShown(ComponentNameMatcher.STATUS_BAR, displayId)
        }

    /** Condition to check if the [ComponentNameMatcher.STATUS_BAR] layer is visible */
    @JvmOverloads
    fun isStatusBarLayerVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        isLayerVisible(ComponentNameMatcher.STATUS_BAR, displayId)

    /** Condition to check if the [ComponentNameMatcher.STATUS_BAR] layer is opaque */
    @JvmOverloads
    fun isStatusBarLayerOpaque(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isStatusBarLayerOpaque[display=$displayId]") {
            it.layerState
                .getLayerWithBuffer(ComponentNameMatcher.STATUS_BAR, displayId)
                ?.color
                ?.alpha() == 1.0f
        }

    @JvmOverloads
    fun isHomeActivityVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isHomeActivityVisible[display=$displayId]") {
            it.wmState.isHomeActivityVisible(displayId)
        }

    /**
     * Condition to check if the [ComponentNameMatcher.IMAGE_WALLPAPER] window is visible on a
     * specific display
     */
    @JvmOverloads
    fun isImageWallpaperWindowVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isWallpaperWindowVisible[$displayId]") {
            it.wmState.isWindowSurfaceShown(ComponentNameMatcher.IMAGE_WALLPAPER, displayId)
        }

    @JvmOverloads
    fun isRecentsActivityVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isRecentsActivityVisible[display=$displayId]") {
            it.wmState.isRecentsActivityVisible(displayId)
        }

    @JvmOverloads
    fun isLauncherLayerVisible(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isLauncherLayerVisible[display=$displayId]") {
            it.layerState.isVisible(ComponentNameMatcher.LAUNCHER, displayId) ||
                it.layerState.isVisible(ComponentNameMatcher.AOSP_LAUNCHER, displayId)
        }

    /**
     * Condition to check if WM app transition is idle
     *
     * Because in shell transitions, active recents animation is running transition (never idle)
     * this method always assumed recents are idle
     */
    @JvmOverloads
    fun isAppTransitionIdle(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("isAppTransitionIdle[$displayId]") {
            (it.wmState.isHomeRecentsComponent && it.wmState.isHomeActivityVisible) ||
                it.wmState.isRecentsActivityVisible ||
                (displayId == null &&
                    it.wmState.displays.all { d ->
                        d.appTransitionState == PlatformConsts.APP_STATE_IDLE
                    }) ||
                (displayId != null &&
                    it.wmState.getDisplay(displayId)?.appTransitionState ==
                        PlatformConsts.APP_STATE_IDLE)
        }

    @JvmOverloads
    fun containsActivity(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition(
            "containsActivity[${componentMatcher.toActivityIdentifier()}, display=$displayId]"
        ) {
            it.wmState.containsActivity(componentMatcher, displayId)
        }

    /** Condition to check if a specific display contains no activity */
    @JvmOverloads
    fun hasNoActivityOnDisplay(displayId: Int? = null): Condition<DeviceStateDump> =
        Condition("hasNoActivityOnDisplay[$displayId]") {
            it.wmState.hasNoActivityOnDisplay(displayId)
        }

    @JvmOverloads
    fun containsWindow(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition("containsWindow[${componentMatcher.toWindowIdentifier()}, display=$displayId]") {
            it.wmState.containsWindow(componentMatcher, displayId)
        }

    @JvmOverloads
    fun isWindowSurfaceShown(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition(
            "isWindowSurfaceShown[${componentMatcher.toWindowIdentifier()}, display=$displayId]"
        ) {
            it.wmState.isWindowSurfaceShown(componentMatcher, displayId)
        }

    @JvmOverloads
    fun isActivityVisible(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition(
            "isActivityVisible[${componentMatcher.toActivityIdentifier()}, display=$displayId]"
        ) {
            it.wmState.isActivityVisible(componentMatcher, displayId)
        }

    fun isWMStateComplete(): Condition<DeviceStateDump> =
        Condition("isWMStateComplete") { it.wmState.isComplete() }

    fun hasRotation(expectedRotation: Rotation, displayId: Int): Condition<DeviceStateDump> {
        val hasRotationCondition =
            Condition<DeviceStateDump>("hasRotation[$expectedRotation, display=$displayId]") {
                val currRotation = it.wmState.getRotation(displayId)
                currRotation == expectedRotation
            }
        return ConditionList(
            listOf(
                hasRotationCondition,
                isLayerVisible(ComponentNameMatcher.ROTATION, displayId).negate(),
                isLayerVisible(ComponentNameMatcher.BACK_SURFACE, displayId).negate(),
                hasLayersAnimating(displayId).negate(),
            )
        )
    }

    @JvmOverloads
    fun isWindowVisible(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        ConditionList(
            containsActivity(componentMatcher, displayId),
            containsWindow(componentMatcher, displayId),
            isActivityVisible(componentMatcher, displayId),
            isWindowSurfaceShown(componentMatcher, displayId),
            isAppTransitionIdle(displayId),
        )

    @JvmOverloads
    fun isLayerVisible(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition("isLayerVisible[${componentMatcher.toLayerIdentifier()}, display=$displayId]") {
            it.layerState.isVisible(componentMatcher, displayId)
        }

    fun isLayerVisible(layerId: Int): Condition<DeviceStateDump> =
        Condition("isLayerVisible[layerId=$layerId]") {
            it.layerState.getLayerById(layerId)?.isVisible ?: false
        }

    /** Condition to check if the given layer is opaque */
    @JvmOverloads
    fun isLayerOpaque(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition("isLayerOpaque[${componentMatcher.toLayerIdentifier()}, display=$displayId]") {
            it.layerState.getLayerWithBuffer(componentMatcher, displayId)?.color?.alpha() == 1.0f
        }

    @JvmOverloads
    fun isLayerColorAlphaOne(
        componentMatcher: IComponentMatcher,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition(
            "isLayerColorAlphaOne[${componentMatcher.toLayerIdentifier()}, display=$displayId]"
        ) {
            it.layerState
                .getLayersForDisplay(displayId)
                .filter { layer -> layer.isVisible && componentMatcher.layerMatchesAnyOf(layer) }
                .any { layer -> layer.color.alpha() == 1.0f }
        }

    fun isLayerColorAlphaOne(layerId: Int): Condition<DeviceStateDump> =
        Condition("isLayerColorAlphaOne[$layerId]") {
            val layer = it.layerState.getLayerById(layerId)
            layer?.color?.alpha() == 1.0f
        }

    @JvmOverloads
    fun isLayerTransformFlagSet(
        componentMatcher: IComponentMatcher,
        transform: Int,
        displayId: Int? = null,
    ): Condition<DeviceStateDump> =
        Condition(
            "isLayerTransformFlagSet[" +
                "${componentMatcher.toLayerIdentifier()}," +
                "transform=$transform, display=$displayId]"
        ) {
            it.layerState
                .getLayersForDisplay(displayId)
                .filter { layer -> layer.isVisible && componentMatcher.layerMatchesAnyOf(layer) }
                .any { layer -> isTransformFlagSet(layer, transform) }
        }

    fun isLayerTransformFlagSet(layerId: Int, transform: Int): Condition<DeviceStateDump> =
        Condition("isLayerTransformFlagSet[$layerId, $transform]") {
            val layer = it.layerState.getLayerById(layerId)
            layer?.transform?.type?.isFlagSet(transform) ?: false
        }

    fun isLayerTransformIdentity(layerId: Int): Condition<DeviceStateDump> =
        ConditionList(
            listOf(
                isLayerTransformFlagSet(layerId, Transform.SCALE_VAL).negate(),
                isLayerTransformFlagSet(layerId, Transform.TRANSLATE_VAL).negate(),
                isLayerTransformFlagSet(layerId, Transform.ROTATE_VAL).negate(),
            )
        )

    private fun isTransformFlagSet(layer: Layer, transform: Int): Boolean =
        layer.transform.type?.isFlagSet(transform) ?: false

    @JvmOverloads
    fun hasLayersAnimating(displayId: Int? = null): Condition<DeviceStateDump> {
        var prevState: DeviceStateDump? = null
        return ConditionList(
            Condition("hasLayersAnimating[display=$displayId]") {
                val result = it.layerState.isAnimating(prevState?.layerState, displayId = displayId)
                prevState = it
                result
            },
            isLayerVisible(ComponentNameMatcher.SNAPSHOT, displayId).negate(),
            isLayerVisible(ComponentNameMatcher.SPLASH_SCREEN, displayId).negate(),
        )
    }

    fun isPipWindowLayerSizeMatch(layerId: Int): Condition<DeviceStateDump> =
        Condition("isPipWindowLayerSizeMatch[layerId=$layerId]") {
            val pipWindow =
                it.wmState.pinnedWindows.firstOrNull { pinnedWindow ->
                    pinnedWindow.layerId == layerId
                } ?: error("Unable to find window with layerId $layerId")
            val windowHeight = pipWindow.frame.height().toFloat()
            val windowWidth = pipWindow.frame.width().toFloat()

            val pipLayer = it.layerState.getLayerById(layerId)
            val layerHeight =
                pipLayer?.screenBounds?.height() ?: error("Unable to find layer with id $layerId")
            val layerWidth = pipLayer.screenBounds.width()

            windowHeight == layerHeight && windowWidth == layerWidth
        }

    fun hasPipWindow(): Condition<DeviceStateDump> =
        Condition("hasPipWindow") { it.wmState.hasPipWindow() }

    /** Checks whether the [BUBBLE] window exists. */
    fun hasBubbleWindow(): Condition<DeviceStateDump> =
        Condition("hasBubbleWindow") { it.wmState.containsWindow(BUBBLE) }

    /** Checks whether the given component is in fullscreen mode. */
    fun isInFullscreenMode(componentMatcher: IComponentMatcher): Condition<DeviceStateDump> =
        Condition("isInFullscreenMode") { it.wmState.isInFullscreenMode(componentMatcher) }

    @JvmOverloads
    fun isImeShown(displayId: Int? = null): Condition<DeviceStateDump> =
        ConditionList(
            listOf(
                isImeOnDisplay(displayId),
                isLayerVisible(ComponentNameMatcher.IME),
                isLayerOpaque(ComponentNameMatcher.IME),
                isImeSurfaceShown(),
                isWindowSurfaceShown(ComponentNameMatcher.IME, displayId),
            )
        )

    private fun isImeOnDisplay(displayId: Int?): Condition<DeviceStateDump> =
        Condition("isImeOnDisplay[$displayId]") {
            val imeDisplayId = it.wmState.inputMethodWindowState?.displayId
            displayId == null || imeDisplayId == displayId
        }

    private fun isImeSurfaceShown(): Condition<DeviceStateDump> =
        Condition("isImeSurfaceShown") {
            it.wmState.inputMethodWindowState?.isSurfaceShown == true &&
                it.wmState.inputMethodWindowState?.isVisible == true
        }

    fun isAppLaunchEnded(taskId: Int): Condition<DeviceStateDump> =
        Condition("containsVisibleAppLaunchWindow[taskId=$taskId]") { dump ->
            val windowStates =
                dump.wmState.getRootTask(taskId)?.activities?.flatMap {
                    it.children.filterIsInstance<WindowState>()
                }
            windowStates != null &&
                windowStates.none {
                    it.attributes.type == PlatformConsts.TYPE_APPLICATION_STARTING && it.isVisible
                }
        }
}
