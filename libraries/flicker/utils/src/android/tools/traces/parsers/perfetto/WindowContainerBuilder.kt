/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.graphics.Insets
import android.graphics.Rect
import android.tools.PlatformConsts
import android.tools.Rotation
import android.tools.datatypes.Size
import android.tools.traces.wm.Activity
import android.tools.traces.wm.ColorMode
import android.tools.traces.wm.Configuration
import android.tools.traces.wm.ConfigurationContainer
import android.tools.traces.wm.ConfigurationContainerImpl
import android.tools.traces.wm.DisplayArea
import android.tools.traces.wm.DisplayContent
import android.tools.traces.wm.DisplayCutout
import android.tools.traces.wm.InsetsSource
import android.tools.traces.wm.InsetsSourceProvider
import android.tools.traces.wm.PixelFormat
import android.tools.traces.wm.RootWindowContainer
import android.tools.traces.wm.RotationAnimation
import android.tools.traces.wm.Task
import android.tools.traces.wm.TaskFragment
import android.tools.traces.wm.WindowConfiguration
import android.tools.traces.wm.WindowContainer
import android.tools.traces.wm.WindowContainerImpl
import android.tools.traces.wm.WindowLayoutParams
import android.tools.traces.wm.WindowState
import android.tools.traces.wm.WindowToken
import androidx.annotation.VisibleForTesting

class WindowContainerBuilder {
    private var args: Args? = null
    private var title: String? = null
    private var token: Int? = null
    private var parentToken: Int? = null
    private var isVisible: Boolean? = null
    private var containerType: String? = null
    private var nameOverride: String? = null

    fun setArgs(value: Args): WindowContainerBuilder = apply { this.args = value }

    fun setTitle(value: String): WindowContainerBuilder = apply { this.title = value }

    fun setToken(value: Int): WindowContainerBuilder = apply { this.token = value }

    fun setParentToken(value: Int?): WindowContainerBuilder = apply { this.parentToken = value }

    fun setIsVisible(value: Boolean): WindowContainerBuilder = apply { this.isVisible = value }

    fun setContainerType(value: String): WindowContainerBuilder = apply {
        this.containerType = value
    }

    fun setNameOverride(value: String?): WindowContainerBuilder = apply {
        this.nameOverride = value
    }

    fun build(): WindowContainer {
        if (args == null) {
            error("args not set")
        }
        if (title == null) {
            error("title not set")
        }
        if (token == null) {
            error("token not set")
        }
        if (isVisible == null) {
            error("isVisible not set")
        }
        if (containerType == null) {
            error("containerType not set")
        }

        if (containerType == ROOT_WINDOW_CONTAINER) {
            return buildRootWindowContainer()
        }
        if (containerType == DISPLAY_CONTENT) {
            return buildDisplayContent(args?.getChild("display_content"))
        }
        if (containerType == DISPLAY_AREA) {
            return buildDisplayArea(args?.getChild("display_area"))
        }
        if (containerType == TASK) {
            return buildTask(args?.getChild("task"))
        }
        if (containerType == TASK_FRAGMENT) {
            return buildTaskFragment(args?.getChild("task_fragment"))
        }
        if (containerType == ACTIVITY) {
            return buildActivity(args?.getChild("activity"))
        }
        if (containerType == WINDOW_TOKEN) {
            return buildWindowToken(args?.getChild("window_token"))
        }
        if (containerType == WINDOW_STATE) {
            return buildWindowState(args?.getChild("window"))
        }
        if (containerType == WINDOW_CONTAINER) {
            return buildWindowContainer(args?.getChild("window_container"))
        }
        throw error("unable to build container with token $token and type $containerType")
    }

    private fun buildRootWindowContainer(): RootWindowContainer {
        val windowContainer = buildWindowContainer(args?.getChild("window_container"))

        return RootWindowContainer(
            isHomeRecentsComponent =
                args?.getChild("is_home_recents_component")?.getBoolean() ?: false,
            pendingActivities =
                args?.getChildren("pending_activities")?.map {
                    it.getChild("title")?.getString() ?: ""
                } ?: emptyList(),
            windowContainer,
        )
    }

    private fun buildDisplayContent(displayContentProto: Args?): DisplayContent {
        if (displayContentProto == null) {
            error("unable to build display content")
        }

        return DisplayContent(
            displayId = displayContentProto?.getChild("id")?.getInt() ?: 0,
            focusedRootTaskId =
                displayContentProto?.getChild("focused_root_task_id")?.getInt() ?: 0,
            resumedActivity =
                displayContentProto?.getChild("resumed_activity")?.getChild("title")?.getString()
                    ?: "",
            singleTaskInstance =
                displayContentProto?.getChild("single_task_instance")?.getBoolean() ?: false,
            defaultPinnedStackBounds =
                buildRect(
                    displayContentProto
                        ?.getChild("pinned_task_controller")
                        ?.getChild("default_bounds")
                ),
            pinnedStackMovementBounds =
                buildRect(
                    displayContentProto
                        ?.getChild("pinned_task_controller")
                        ?.getChild("movement_bounds")
                ),
            displayRect =
                Rect(
                    0,
                    0,
                    displayContentProto
                        ?.getChild("display_info")
                        ?.getChild("logical_width")
                        ?.getInt() ?: 0,
                    displayContentProto
                        ?.getChild("display_info")
                        ?.getChild("logical_height")
                        ?.getInt() ?: 0,
                ),
            appRect =
                Rect(
                    0,
                    0,
                    displayContentProto?.getChild("display_info")?.getChild("app_width")?.getInt()
                        ?: 0,
                    displayContentProto?.getChild("display_info")?.getChild("app_height")?.getInt()
                        ?: 0,
                ),
            dpi = displayContentProto?.getChild("dpi")?.getInt() ?: 0,
            flags = displayContentProto?.getChild("display_info")?.getChild("flags")?.getInt() ?: 0,
            stableBounds =
                buildRect(
                    displayContentProto?.getChild("display_frames")?.getChild("stable_bounds")
                ),
            surfaceSize = displayContentProto?.getChild("surface_size")?.getInt() ?: 0,
            focusedApp = displayContentProto?.getChild("focused_app")?.getString() ?: "",
            lastTransition =
                displayContentProto
                    ?.getChild("app_transition")
                    ?.getChild("last_used_app_transition")
                    ?.getString() ?: DEFAULT_TRANSITION_TYPE,
            appTransitionState =
                displayContentProto
                    ?.getChild("app_transition")
                    ?.getChild("app_transition_state")
                    ?.getString() ?: DEFAULT_APP_STATE,
            rotation =
                Rotation.getByValue(
                    displayContentProto
                        ?.getChild("display_rotation")
                        ?.getChild("rotation")
                        ?.getInt() ?: 0
                ),
            lastOrientation =
                displayContentProto
                    ?.getChild("display_rotation")
                    ?.getChild("last_orientation")
                    ?.getInt() ?: 0,
            cutout =
                buildDisplayCutout(
                    displayContentProto?.getChild("display_info")?.getChild("cutout")
                ),
            insetsSourceProviders =
                buildInsetsSourceProviders(
                    displayContentProto?.getChildren("insets_source_providers")
                ),
            windowContainer =
                buildWindowContainer(
                    containerProto =
                        displayContentProto
                            ?.getChild("root_display_area")
                            ?.getChild("window_container")
                ),
        )
    }

    private fun buildDisplayArea(displayAreaProto: Args?): DisplayArea {
        if (displayAreaProto == null) {
            error("unable to build display area")
        }

        return DisplayArea(
            isTaskDisplayArea =
                displayAreaProto?.getChild("is_task_display_area")?.getBoolean() ?: false,
            windowContainer = buildWindowContainer(displayAreaProto?.getChild("window_container")),
        )
    }

    private fun buildTask(taskProto: Args?): Task {
        if (taskProto == null) {
            error("unable to build task")
        }

        return Task(
            activityType =
                taskProto?.getChild("task_fragment")?.getChild("activity_type")?.getInt()
                    ?: taskProto?.getChild("activity_type")?.getInt()
                    ?: 0,
            isFullscreen = taskProto?.getChild("fills_parent")?.getBoolean() ?: false,
            bounds = buildRect(taskProto?.getChild("bounds")),
            taskId = taskProto?.getChild("id")?.getInt() ?: 0,
            rootTaskId = taskProto?.getChild("root_task_id")?.getInt() ?: 0,
            displayId =
                taskProto?.getChild("task_fragment")?.getChild("display_id")?.getInt()
                    ?: taskProto?.getChild("display_id")?.getInt()
                    ?: 0,
            lastNonFullscreenBounds = buildRect(taskProto?.getChild("last_non_fullscreen_bounds")),
            realActivity = taskProto?.getChild("real_activity")?.getString() ?: "",
            origActivity = taskProto?.getChild("orig_activity")?.getString() ?: "",
            resizeMode = taskProto?.getChild("resize_mode")?.getInt() ?: 0,
            _resumedActivity =
                taskProto?.getChild("resumed_activity")?.getChild("title")?.getString() ?: "",
            animatingBounds = taskProto?.getChild("animating_bounds")?.getBoolean() ?: false,
            surfaceWidth = taskProto?.getChild("surface_width")?.getInt() ?: 0,
            surfaceHeight = taskProto?.getChild("surface_height")?.getInt() ?: 0,
            createdByOrganizer = taskProto?.getChild("created_by_organizer")?.getBoolean() ?: false,
            minWidth =
                taskProto?.getChild("task_fragment")?.getChild("min_width")?.getInt()
                    ?: taskProto?.getChild("min_width")?.getInt()
                    ?: 0,
            minHeight =
                taskProto?.getChild("task_fragment")?.getChild("min_height")?.getInt()
                    ?: taskProto?.getChild("min_height")?.getInt()
                    ?: 0,
            windowContainer =
                buildWindowContainer(
                    taskProto?.getChild("task_fragment")?.getChild("window_container")
                        ?: taskProto?.getChild("window_container")
                ),
        )
    }

    private fun buildTaskFragment(taskFragmentProto: Args?): TaskFragment {
        if (taskFragmentProto == null) {
            error("unable to build task fragment")
        }

        return TaskFragment(
            activityType = taskFragmentProto?.getChild("activity_type")?.getInt() ?: 0,
            displayId = taskFragmentProto?.getChild("display_id")?.getInt() ?: 0,
            minWidth = taskFragmentProto?.getChild("min_width")?.getInt() ?: 0,
            minHeight = taskFragmentProto?.getChild("min_height")?.getInt() ?: 0,
            windowContainer = buildWindowContainer(taskFragmentProto?.getChild("window_container")),
        )
    }

    private fun buildActivity(activityRecordProto: Args?): Activity {
        if (activityRecordProto == null) {
            error("unable to build activity")
        }

        return Activity(
            state = activityRecordProto?.getChild("state")?.getString() ?: "",
            frontOfTask = activityRecordProto?.getChild("front_of_task")?.getBoolean() ?: false,
            procId = activityRecordProto?.getChild("proc_id")?.getInt() ?: 0,
            isTranslucent = activityRecordProto?.getChild("translucent")?.getBoolean() ?: false,
            windowContainer =
                buildWindowContainer(
                    activityRecordProto?.getChild("window_token")?.getChild("window_container")
                ),
        )
    }

    private fun buildWindowToken(windowTokenProto: Args?): WindowToken {
        if (windowTokenProto == null) {
            error("unable to build window token")
        }

        return WindowToken(buildWindowContainer(windowTokenProto?.getChild("window_container")))
    }

    private fun buildWindowState(windowStateProto: Args?): WindowState {
        if (windowStateProto == null) {
            error("unable to build window state")
        }

        val identifierName =
            windowStateProto
                ?.getChild("window_container")
                ?.getChild("identifier")
                ?.getChild("title")
                ?.getString() ?: ""
        return WindowState(
            attributes = buildWindowLayoutParams(windowStateProto?.getChild("attributes")),
            displayId = windowStateProto?.getChild("display_id")?.getInt() ?: 0,
            stackId = windowStateProto?.getChild("stack_id")?.getInt() ?: 0,
            layer =
                windowStateProto
                    ?.getChild("animator")
                    ?.getChild("surface")
                    ?.getChild("layer")
                    ?.getInt() ?: 0,
            isSurfaceShown =
                windowStateProto
                    ?.getChild("animator")
                    ?.getChild("surface")
                    ?.getChild("shown")
                    ?.getBoolean() ?: false,
            windowType =
                when {
                    identifierName.startsWith(PlatformConsts.STARTING_WINDOW_PREFIX) ->
                        PlatformConsts.WINDOW_TYPE_STARTING
                    windowStateProto?.getChild("animating_exit")?.getBoolean() ?: false ->
                        PlatformConsts.WINDOW_TYPE_EXITING
                    identifierName.startsWith(PlatformConsts.DEBUGGER_WINDOW_PREFIX) ->
                        PlatformConsts.WINDOW_TYPE_STARTING
                    else -> 0
                },
            requestedSize =
                Size.from(
                    windowStateProto?.getChild("requested_width")?.getInt() ?: 0,
                    windowStateProto?.getChild("requested_height")?.getInt() ?: 0,
                ),
            surfacePosition = buildRect(windowStateProto?.getChild("surface_position")),
            frame = buildRect(windowStateProto?.getChild("window_frames")?.getChild("frame")),
            containingFrame =
                buildRect(
                    windowStateProto?.getChild("window_frames")?.getChild("containing_frame")
                ),
            parentFrame =
                buildRect(windowStateProto?.getChild("window_frames")?.getChild("parent_frame")),
            contentFrame =
                buildRect(windowStateProto?.getChild("window_frames")?.getChild("content_frame")),
            contentInsets =
                buildRect(windowStateProto?.getChild("window_frames")?.getChild("content_insets")),
            surfaceInsets = buildRect(windowStateProto?.getChild("surface_insets")),
            givenContentInsets = buildRect(windowStateProto?.getChild("given_content_insets")),
            crop = buildRect(windowStateProto?.getChild("animator")?.getChild("last_clip_rect")),
            windowContainer = buildWindowContainer(windowStateProto?.getChild("window_container")),
        )
    }

    private fun buildWindowLayoutParams(windowLayoutParamsProto: Args?): WindowLayoutParams {
        return WindowLayoutParams.from(
            type = windowLayoutParamsProto?.getChild("type")?.getInt() ?: 0,
            x = windowLayoutParamsProto?.getChild("x")?.getInt() ?: 0,
            y = windowLayoutParamsProto?.getChild("y")?.getInt() ?: 0,
            width = windowLayoutParamsProto?.getChild("width")?.getInt() ?: 0,
            height = windowLayoutParamsProto?.getChild("height")?.getInt() ?: 0,
            horizontalMargin =
                windowLayoutParamsProto?.getChild("horizontal_margin")?.getFloat() ?: 0f,
            verticalMargin = windowLayoutParamsProto?.getChild("vertical_margin")?.getFloat() ?: 0f,
            gravity = windowLayoutParamsProto?.getChild("gravity")?.getInt() ?: 0,
            softInputMode = windowLayoutParamsProto?.getChild("soft_input_mode")?.getInt() ?: 0,
            format =
                PixelFormat.fromName(windowLayoutParamsProto?.getChild("format")?.getString())
                    ?.value ?: 0,
            windowAnimations =
                windowLayoutParamsProto?.getChild("window_animations")?.getInt() ?: 0,
            alpha = windowLayoutParamsProto?.getChild("alpha")?.getFloat() ?: 0f,
            screenBrightness =
                windowLayoutParamsProto?.getChild("screen_brightness")?.getFloat() ?: 0f,
            buttonBrightness =
                windowLayoutParamsProto?.getChild("button_brightness")?.getFloat() ?: 0f,
            rotationAnimation =
                RotationAnimation.fromName(
                        windowLayoutParamsProto?.getChild("rotation_animation")?.getString()
                    )
                    ?.value ?: 0,
            preferredRefreshRate =
                windowLayoutParamsProto?.getChild("preferred_refresh_rate")?.getFloat() ?: 0f,
            preferredDisplayModeId =
                windowLayoutParamsProto?.getChild("preferred_display_mode_id")?.getInt() ?: 0,
            hasSystemUiListeners =
                windowLayoutParamsProto?.getChild("has_system_ui_listeners")?.getBoolean() ?: false,
            inputFeatureFlags =
                windowLayoutParamsProto?.getChild("input_feature_flags")?.getInt() ?: 0,
            userActivityTimeout =
                windowLayoutParamsProto?.getChild("user_activity_timeout")?.getLong() ?: 0,
            colorMode =
                ColorMode.fromName(windowLayoutParamsProto?.getChild("color_mode")?.getString())
                    ?.value ?: 0,
            flags = windowLayoutParamsProto?.getChild("flags")?.getInt() ?: 0,
            privateFlags = windowLayoutParamsProto?.getChild("private_flags")?.getInt() ?: 0,
            systemUiVisibilityFlags =
                windowLayoutParamsProto?.getChild("system_ui_visibility_flags")?.getInt() ?: 0,
            subtreeSystemUiVisibilityFlags =
                windowLayoutParamsProto?.getChild("subtree_system_ui_visibility_flags")?.getInt()
                    ?: 0,
            appearance = windowLayoutParamsProto?.getChild("appearance")?.getInt() ?: 0,
            behavior = windowLayoutParamsProto?.getChild("behavior")?.getInt() ?: 0,
            fitInsetsTypes = windowLayoutParamsProto?.getChild("fit_insets_types")?.getInt() ?: 0,
            fitInsetsSides = windowLayoutParamsProto?.getChild("fit_insets_sides")?.getInt() ?: 0,
            fitIgnoreVisibility =
                windowLayoutParamsProto?.getChild("fit_ignore_visibility")?.getBoolean() ?: false,
        )
    }

    private fun buildWindowContainer(containerProto: Args?): WindowContainer {
        if (containerProto == null) {
            error("unable to build container")
        }
        return WindowContainerImpl(
            title = nameOverride ?: title!!,
            token = Integer.toHexString(token!!),
            orientation = containerProto?.getChild("orientation")?.getInt() ?: 0,
            layerId =
                containerProto?.getChild("surface_control")?.getChild("layer_id")?.getInt() ?: 0,
            isVisible = isVisible!!,
            parentToken = parentToken,
            configurationContainer =
                buildConfigurationContainer(containerProto?.getChild("configuration_container")),
        )
    }

    private fun buildConfigurationContainer(
        configurationContainerProto: Args?
    ): ConfigurationContainer {
        return ConfigurationContainerImpl.from(
            overrideConfiguration =
                buildConfiguration(configurationContainerProto?.getChild("override_configuration")),
            fullConfiguration =
                buildConfiguration(configurationContainerProto?.getChild("full_configuration")),
            mergedOverrideConfiguration =
                buildConfiguration(
                    configurationContainerProto?.getChild("merged_override_configuration")
                ),
        )
    }

    private fun buildConfiguration(configurationProto: Args?): Configuration? {
        if (configurationProto == null) {
            return null
        }

        return Configuration.from(
            windowConfiguration =
                buildWindowConfiguration(configurationProto.getChild("window_configuration")),
            densityDpi = configurationProto.getChild("density_dpi")?.getInt() ?: 0,
            orientation = configurationProto.getChild("orientation")?.getInt() ?: 0,
            screenHeightDp = configurationProto.getChild("screen_height_dp")?.getInt() ?: 0,
            screenWidthDp = configurationProto.getChild("screen_width_dp")?.getInt() ?: 0,
            smallestScreenWidthDp =
                configurationProto.getChild("smallest_screen_width_dp")?.getInt() ?: 0,
            screenLayout = configurationProto.getChild("screen_layout")?.getInt() ?: 0,
            uiMode = configurationProto.getChild("ui_mode")?.getInt() ?: 0,
        )
    }

    private fun buildWindowConfiguration(windowConfigurationProto: Args?): WindowConfiguration? {
        if (windowConfigurationProto == null) {
            return null
        }

        return WindowConfiguration.from(
            appBounds = buildRect(windowConfigurationProto.getChild("app_bounds")),
            bounds = buildRect(windowConfigurationProto.getChild("bounds")),
            maxBounds = buildRect(windowConfigurationProto.getChild("max_bounds")),
            windowingMode = windowConfigurationProto.getChild("windowing_mode")?.getInt() ?: 0,
            activityType = windowConfigurationProto.getChild("activity_type")?.getInt() ?: 0,
        )
    }

    private fun buildDisplayCutout(displayCutoutProto: Args?): DisplayCutout? {
        if (displayCutoutProto == null) {
            return null
        }

        return DisplayCutout.from(
            buildInsets(displayCutoutProto.getChild("insets")),
            buildRect(displayCutoutProto.getChild("bound_left")),
            buildRect(displayCutoutProto.getChild("bound_top")),
            buildRect(displayCutoutProto.getChild("bound_right")),
            buildRect(displayCutoutProto.getChild("bound_bottom")),
            buildInsets(displayCutoutProto.getChild("waterfall_insets")),
        )
    }

    private fun buildInsetsSourceProviders(
        insetsProvidersProto: List<Args>?
    ): Array<InsetsSourceProvider> {
        return insetsProvidersProto
            ?.map {
                InsetsSourceProvider(
                    buildRect(it.getChild("frame")),
                    buildInsetsSource(it.getChild("source")),
                )
            }
            ?.toTypedArray() ?: emptyArray<InsetsSourceProvider>()
    }

    private fun buildInsetsSource(insetsSourceProto: Args?): InsetsSource? {
        if (insetsSourceProto == null) {
            return null
        }

        return InsetsSource.from(
            type = insetsSourceProto.getChild("type_number")?.getInt() ?: -1,
            frame = buildRect(insetsSourceProto.getChild("frame")),
            visible = insetsSourceProto.getChild("visible")?.getBoolean() ?: false,
        )
    }

    private fun buildInsets(rectProto: Args?): Insets {
        if (rectProto == null) {
            return Insets.NONE
        }

        return Insets.of(
            rectProto.getChild("left")?.getInt() ?: 0,
            rectProto.getChild("top")?.getInt() ?: 0,
            rectProto.getChild("right")?.getInt() ?: 0,
            rectProto.getChild("bottom")?.getInt() ?: 0,
        )
    }

    private fun buildRect(rectProto: Args?): Rect =
        Rect(
            rectProto?.getChild("left")?.getInt() ?: 0,
            rectProto?.getChild("top")?.getInt() ?: 0,
            rectProto?.getChild("right")?.getInt() ?: 0,
            rectProto?.getChild("bottom")?.getInt() ?: 0,
        )

    companion object {
        @VisibleForTesting const val ROOT_WINDOW_CONTAINER = "RootWindowContainer"
        @VisibleForTesting const val DISPLAY_CONTENT = "DisplayContent"
        @VisibleForTesting const val DISPLAY_AREA = "DisplayArea"
        @VisibleForTesting const val TASK = "Task"
        @VisibleForTesting const val TASK_FRAGMENT = "TaskFragment"
        @VisibleForTesting const val ACTIVITY = "Activity"
        @VisibleForTesting const val WINDOW_TOKEN = "WindowToken"
        @VisibleForTesting const val WINDOW_STATE = "WindowState"
        @VisibleForTesting const val WINDOW_CONTAINER = "WindowContainer"

        @VisibleForTesting const val DEFAULT_TRANSITION_TYPE = "TRANSIT_NONE"
        @VisibleForTesting const val DEFAULT_APP_STATE = "APP_STATE_IDLE"
    }
}
