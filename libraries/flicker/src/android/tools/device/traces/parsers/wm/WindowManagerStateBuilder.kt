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

package android.tools.device.traces.parsers.wm

import android.app.nano.WindowConfigurationProto
import android.content.nano.ConfigurationProto
import android.graphics.nano.RectProto
import android.tools.common.Rotation
import android.tools.common.datatypes.Insets
import android.tools.common.datatypes.Rect
import android.tools.common.datatypes.Size
import android.tools.common.traces.wm.Activity
import android.tools.common.traces.wm.Configuration
import android.tools.common.traces.wm.ConfigurationContainer
import android.tools.common.traces.wm.DisplayArea
import android.tools.common.traces.wm.DisplayContent
import android.tools.common.traces.wm.DisplayCutout
import android.tools.common.traces.wm.KeyguardControllerState
import android.tools.common.traces.wm.RootWindowContainer
import android.tools.common.traces.wm.Task
import android.tools.common.traces.wm.TaskFragment
import android.tools.common.traces.wm.WindowConfiguration
import android.tools.common.traces.wm.WindowContainer
import android.tools.common.traces.wm.WindowLayoutParams
import android.tools.common.traces.wm.WindowManagerPolicy
import android.tools.common.traces.wm.WindowManagerState
import android.tools.common.traces.wm.WindowManagerTraceEntryBuilder
import android.tools.common.traces.wm.WindowState
import android.tools.common.traces.wm.WindowToken
import android.view.Surface
import android.view.nano.DisplayCutoutProto
import android.view.nano.ViewProtoEnums
import android.view.nano.WindowLayoutParamsProto
import com.android.server.wm.nano.ActivityRecordProto
import com.android.server.wm.nano.AppTransitionProto
import com.android.server.wm.nano.ConfigurationContainerProto
import com.android.server.wm.nano.DisplayAreaProto
import com.android.server.wm.nano.DisplayContentProto
import com.android.server.wm.nano.KeyguardControllerProto
import com.android.server.wm.nano.RootWindowContainerProto
import com.android.server.wm.nano.TaskFragmentProto
import com.android.server.wm.nano.TaskProto
import com.android.server.wm.nano.WindowContainerChildProto
import com.android.server.wm.nano.WindowContainerProto
import com.android.server.wm.nano.WindowManagerPolicyProto
import com.android.server.wm.nano.WindowManagerServiceDumpProto
import com.android.server.wm.nano.WindowStateProto
import com.android.server.wm.nano.WindowTokenProto

/** Helper class to create a new WM state */
class WindowManagerStateBuilder {
    private var computedZCounter = 0
    private var realToElapsedTimeOffsetNanos = 0L
    private var where = ""
    private var timestamp = 0L
    private var proto: WindowManagerServiceDumpProto? = null

    fun withRealTimeOffset(value: Long) = apply { realToElapsedTimeOffsetNanos = value }

    fun atPlace(_where: String) = apply { where = _where }

    fun forTimestamp(value: Long) = apply { timestamp = value }

    fun forProto(value: WindowManagerServiceDumpProto) = apply { proto = value }

    fun build(): WindowManagerState {
        val proto = proto
        requireNotNull(proto) { "Proto object not specified" }

        computedZCounter = 0
        return WindowManagerTraceEntryBuilder(
                _elapsedTimestamp = timestamp.toString(),
                policy = createWindowManagerPolicy(proto.policy),
                focusedApp = proto.focusedApp,
                focusedDisplayId = proto.focusedDisplayId,
                focusedWindow = proto.focusedWindow?.title ?: "",
                inputMethodWindowAppToken =
                    if (proto.inputMethodWindow != null) {
                        Integer.toHexString(proto.inputMethodWindow.hashCode)
                    } else {
                        ""
                    },
                isHomeRecentsComponent = proto.rootWindowContainer.isHomeRecentsComponent,
                isDisplayFrozen = proto.displayFrozen,
                pendingActivities =
                    proto.rootWindowContainer.pendingActivities.map { it.title }.toTypedArray(),
                root = createRootWindowContainer(proto.rootWindowContainer),
                keyguardControllerState =
                    createKeyguardControllerState(proto.rootWindowContainer.keyguardController),
                where = where,
                realToElapsedTimeOffsetNs = realToElapsedTimeOffsetNanos.toString()
            )
            .build()
    }

    private fun createWindowManagerPolicy(proto: WindowManagerPolicyProto): WindowManagerPolicy {
        return WindowManagerPolicy.from(
            focusedAppToken = proto.focusedAppToken ?: "",
            forceStatusBar = proto.forceStatusBar,
            forceStatusBarFromKeyguard = proto.forceStatusBarFromKeyguard,
            keyguardDrawComplete = proto.keyguardDrawComplete,
            keyguardOccluded = proto.keyguardOccluded,
            keyguardOccludedChanged = proto.keyguardOccludedChanged,
            keyguardOccludedPending = proto.keyguardOccludedPending,
            lastSystemUiFlags = proto.lastSystemUiFlags,
            orientation = proto.orientation,
            rotation = Rotation.getByValue(proto.rotation),
            rotationMode = proto.rotationMode,
            screenOnFully = proto.screenOnFully,
            windowManagerDrawComplete = proto.windowManagerDrawComplete
        )
    }

    private fun createRootWindowContainer(proto: RootWindowContainerProto): RootWindowContainer {
        return RootWindowContainer(
            createWindowContainer(
                proto.windowContainer,
                proto.windowContainer.children.mapNotNull { p ->
                    createWindowContainerChild(p, isActivityInTree = false)
                }
            )
                ?: error("Window container should not be null")
        )
    }

    private fun createKeyguardControllerState(
        proto: KeyguardControllerProto?
    ): KeyguardControllerState {
        return KeyguardControllerState.from(
            isAodShowing = proto?.aodShowing ?: false,
            isKeyguardShowing = proto?.keyguardShowing ?: false,
            keyguardOccludedStates =
                proto?.keyguardOccludedStates?.associate { it.displayId to it.keyguardOccluded }
                    ?: emptyMap()
        )
    }

    private fun createWindowContainerChild(
        proto: WindowContainerChildProto,
        isActivityInTree: Boolean
    ): WindowContainer? {
        return createDisplayContent(proto.displayContent, isActivityInTree)
            ?: createDisplayArea(proto.displayArea, isActivityInTree)
                ?: createTask(proto.task, isActivityInTree)
                ?: createTaskFragment(proto.taskFragment, isActivityInTree)
                ?: createActivity(proto.activity)
                ?: createWindowToken(proto.windowToken, isActivityInTree)
                ?: createWindowState(proto.window, isActivityInTree)
                ?: createWindowContainer(proto.windowContainer, children = emptyList())
    }

    private fun createDisplayContent(
        proto: DisplayContentProto?,
        isActivityInTree: Boolean
    ): DisplayContent? {
        return if (proto == null) {
            null
        } else {
            DisplayContent(
                id = proto.id,
                focusedRootTaskId = proto.focusedRootTaskId,
                resumedActivity = proto.resumedActivity?.title ?: "",
                singleTaskInstance = proto.singleTaskInstance,
                defaultPinnedStackBounds = proto.pinnedTaskController?.defaultBounds?.toRect()
                        ?: Rect.EMPTY,
                pinnedStackMovementBounds = proto.pinnedTaskController?.movementBounds?.toRect()
                        ?: Rect.EMPTY,
                displayRect =
                    Rect.from(
                        0,
                        0,
                        proto.displayInfo?.logicalWidth ?: 0,
                        proto.displayInfo?.logicalHeight ?: 0
                    ),
                appRect =
                    Rect.from(
                        0,
                        0,
                        proto.displayInfo?.appWidth ?: 0,
                        proto.displayInfo?.appHeight ?: 0
                    ),
                dpi = proto.dpi,
                flags = proto.displayInfo?.flags ?: 0,
                stableBounds = proto.displayFrames?.stableBounds?.toRect() ?: Rect.EMPTY,
                surfaceSize = proto.surfaceSize,
                focusedApp = proto.focusedApp,
                lastTransition =
                    appTransitionToString(proto.appTransition?.lastUsedAppTransition ?: 0),
                appTransitionState = appStateToString(proto.appTransition?.appTransitionState ?: 0),
                rotation =
                    Rotation.getByValue(proto.displayRotation?.rotation ?: Surface.ROTATION_0),
                lastOrientation = proto.displayRotation?.lastOrientation ?: 0,
                cutout = createDisplayCutout(proto.displayInfo?.cutout),
                windowContainer =
                    createWindowContainer(
                        proto.rootDisplayArea.windowContainer,
                        proto.rootDisplayArea.windowContainer.children.mapNotNull { p ->
                            createWindowContainerChild(p, isActivityInTree)
                        },
                        nameOverride = proto.displayInfo?.name ?: ""
                    )
                        ?: error("Window container should not be null")
            )
        }
    }

    private fun createDisplayArea(
        proto: DisplayAreaProto?,
        isActivityInTree: Boolean
    ): DisplayArea? {
        return if (proto == null) {
            null
        } else {
            DisplayArea(
                isTaskDisplayArea = proto.isTaskDisplayArea,
                windowContainer =
                    createWindowContainer(
                        proto.windowContainer,
                        proto.windowContainer.children.mapNotNull { p ->
                            createWindowContainerChild(p, isActivityInTree)
                        }
                    )
                        ?: error("Window container should not be null")
            )
        }
    }

    private fun createTask(proto: TaskProto?, isActivityInTree: Boolean): Task? {
        return if (proto == null) {
            null
        } else {
            Task(
                activityType = proto.taskFragment?.activityType ?: proto.activityType,
                isFullscreen = proto.fillsParent,
                bounds = proto.bounds.toRect(),
                taskId = proto.id,
                rootTaskId = proto.rootTaskId,
                displayId = proto.taskFragment?.displayId ?: proto.displayId,
                lastNonFullscreenBounds = proto.lastNonFullscreenBounds?.toRect() ?: Rect.EMPTY,
                realActivity = proto.realActivity,
                origActivity = proto.origActivity,
                resizeMode = proto.resizeMode,
                _resumedActivity = proto.resumedActivity?.title ?: "",
                animatingBounds = proto.animatingBounds,
                surfaceWidth = proto.surfaceWidth,
                surfaceHeight = proto.surfaceHeight,
                createdByOrganizer = proto.createdByOrganizer,
                minWidth = proto.taskFragment?.minWidth ?: proto.minWidth,
                minHeight = proto.taskFragment?.minHeight ?: proto.minHeight,
                windowContainer =
                    createWindowContainer(
                        proto.taskFragment?.windowContainer ?: proto.windowContainer,
                        if (proto.taskFragment != null) {
                            proto.taskFragment.windowContainer.children.mapNotNull { p ->
                                createWindowContainerChild(p, isActivityInTree)
                            }
                        } else {
                            proto.windowContainer.children.mapNotNull { p ->
                                createWindowContainerChild(p, isActivityInTree)
                            }
                        }
                    )
                        ?: error("Window container should not be null")
            )
        }
    }

    private fun createTaskFragment(
        proto: TaskFragmentProto?,
        isActivityInTree: Boolean
    ): TaskFragment? {
        return if (proto == null) {
            null
        } else {
            TaskFragment(
                activityType = proto.activityType,
                displayId = proto.displayId,
                minWidth = proto.minWidth,
                minHeight = proto.minHeight,
                windowContainer =
                    createWindowContainer(
                        proto.windowContainer,
                        proto.windowContainer.children.mapNotNull { p ->
                            createWindowContainerChild(p, isActivityInTree)
                        }
                    )
                        ?: error("Window container should not be null")
            )
        }
    }

    private fun createActivity(proto: ActivityRecordProto?): Activity? {
        return if (proto == null) {
            null
        } else {
            Activity(
                name = proto.name,
                state = proto.state,
                visible = proto.visible,
                frontOfTask = proto.frontOfTask,
                procId = proto.procId,
                isTranslucent = proto.translucent,
                windowContainer =
                    createWindowContainer(
                        proto.windowToken.windowContainer,
                        proto.windowToken.windowContainer.children.mapNotNull { p ->
                            createWindowContainerChild(p, isActivityInTree = true)
                        }
                    )
                        ?: error("Window container should not be null")
            )
        }
    }

    private fun createWindowToken(
        proto: WindowTokenProto?,
        isActivityInTree: Boolean
    ): WindowToken? {
        return if (proto == null) {
            null
        } else {
            WindowToken(
                createWindowContainer(
                    proto.windowContainer,
                    proto.windowContainer.children.mapNotNull { p ->
                        createWindowContainerChild(p, isActivityInTree)
                    }
                )
                    ?: error("Window container should not be null")
            )
        }
    }

    private fun createWindowState(
        proto: WindowStateProto?,
        isActivityInTree: Boolean
    ): WindowState? {
        return if (proto == null) {
            null
        } else {
            val identifierName = proto.windowContainer.identifier?.title ?: ""
            WindowState(
                attributes = createWindowLayerParams(proto.attributes),
                displayId = proto.displayId,
                stackId = proto.stackId,
                layer = proto.animator?.surface?.layer ?: 0,
                isSurfaceShown = proto.animator?.surface?.shown ?: false,
                windowType =
                    when {
                        identifierName.startsWith(WindowState.STARTING_WINDOW_PREFIX) ->
                            WindowState.WINDOW_TYPE_STARTING
                        proto.animatingExit -> WindowState.WINDOW_TYPE_EXITING
                        identifierName.startsWith(WindowState.DEBUGGER_WINDOW_PREFIX) ->
                            WindowState.WINDOW_TYPE_STARTING
                        else -> 0
                    },
                requestedSize = Size.from(proto.requestedWidth, proto.requestedHeight),
                surfacePosition = proto.surfacePosition?.toRect(),
                frame = proto.windowFrames?.frame?.toRect() ?: Rect.EMPTY,
                containingFrame = proto.windowFrames?.containingFrame?.toRect() ?: Rect.EMPTY,
                parentFrame = proto.windowFrames?.parentFrame?.toRect() ?: Rect.EMPTY,
                contentFrame = proto.windowFrames?.contentFrame?.toRect() ?: Rect.EMPTY,
                contentInsets = proto.windowFrames?.contentInsets?.toRect() ?: Rect.EMPTY,
                surfaceInsets = proto.surfaceInsets?.toRect() ?: Rect.EMPTY,
                givenContentInsets = proto.givenContentInsets?.toRect() ?: Rect.EMPTY,
                crop = proto.animator?.lastClipRect?.toRect() ?: Rect.EMPTY,
                windowContainer =
                    createWindowContainer(
                        proto.windowContainer,
                        proto.windowContainer.children.mapNotNull { p ->
                            createWindowContainerChild(p, isActivityInTree)
                        },
                        nameOverride =
                            when {
                                // Existing code depends on the prefix being removed
                                identifierName.startsWith(WindowState.STARTING_WINDOW_PREFIX) ->
                                    identifierName.substring(
                                        WindowState.STARTING_WINDOW_PREFIX.length
                                    )
                                identifierName.startsWith(WindowState.DEBUGGER_WINDOW_PREFIX) ->
                                    identifierName.substring(
                                        WindowState.DEBUGGER_WINDOW_PREFIX.length
                                    )
                                else -> identifierName
                            }
                    )
                        ?: error("Window container should not be null"),
                isAppWindow = isActivityInTree
            )
        }
    }

    private fun createWindowLayerParams(proto: WindowLayoutParamsProto?): WindowLayoutParams {
        return WindowLayoutParams.from(
            type = proto?.type ?: 0,
            x = proto?.x ?: 0,
            y = proto?.y ?: 0,
            width = proto?.width ?: 0,
            height = proto?.height ?: 0,
            horizontalMargin = proto?.horizontalMargin ?: 0f,
            verticalMargin = proto?.verticalMargin ?: 0f,
            gravity = proto?.gravity ?: 0,
            softInputMode = proto?.softInputMode ?: 0,
            format = proto?.format ?: 0,
            windowAnimations = proto?.windowAnimations ?: 0,
            alpha = proto?.alpha ?: 0f,
            screenBrightness = proto?.screenBrightness ?: 0f,
            buttonBrightness = proto?.buttonBrightness ?: 0f,
            rotationAnimation = proto?.rotationAnimation ?: 0,
            preferredRefreshRate = proto?.preferredRefreshRate ?: 0f,
            preferredDisplayModeId = proto?.preferredDisplayModeId ?: 0,
            hasSystemUiListeners = proto?.hasSystemUiListeners ?: false,
            inputFeatureFlags = proto?.inputFeatureFlags ?: 0,
            userActivityTimeout = proto?.userActivityTimeout ?: 0,
            colorMode = proto?.colorMode ?: 0,
            flags = proto?.flags ?: 0,
            privateFlags = proto?.privateFlags ?: 0,
            systemUiVisibilityFlags = proto?.systemUiVisibilityFlags ?: 0,
            subtreeSystemUiVisibilityFlags = proto?.subtreeSystemUiVisibilityFlags ?: 0,
            appearance = proto?.appearance ?: 0,
            behavior = proto?.behavior ?: 0,
            fitInsetsTypes = proto?.fitInsetsTypes ?: 0,
            fitInsetsSides = proto?.fitInsetsSides ?: 0,
            fitIgnoreVisibility = proto?.fitIgnoreVisibility ?: false
        )
    }

    private fun createConfigurationContainer(
        proto: ConfigurationContainerProto?
    ): ConfigurationContainer {
        return ConfigurationContainer(
            overrideConfiguration = createConfiguration(proto?.overrideConfiguration),
            fullConfiguration = createConfiguration(proto?.fullConfiguration),
            mergedOverrideConfiguration = createConfiguration(proto?.mergedOverrideConfiguration)
        )
    }

    private fun createConfiguration(proto: ConfigurationProto?): Configuration? {
        return if (proto == null) {
            null
        } else {
            Configuration.from(
                windowConfiguration =
                    if (proto.windowConfiguration != null) {
                        createWindowConfiguration(proto.windowConfiguration)
                    } else {
                        null
                    },
                densityDpi = proto.densityDpi,
                orientation = proto.orientation,
                screenHeightDp = proto.screenHeightDp,
                screenWidthDp = proto.screenWidthDp,
                smallestScreenWidthDp = proto.smallestScreenWidthDp,
                screenLayout = proto.screenLayout,
                uiMode = proto.uiMode
            )
        }
    }

    private fun createWindowConfiguration(proto: WindowConfigurationProto): WindowConfiguration {
        return WindowConfiguration.from(
            appBounds = proto.appBounds?.toRect(),
            bounds = proto.bounds?.toRect(),
            maxBounds = proto.maxBounds?.toRect(),
            windowingMode = proto.windowingMode,
            activityType = proto.activityType
        )
    }

    private fun createWindowContainer(
        proto: WindowContainerProto?,
        children: List<WindowContainer>,
        nameOverride: String? = null
    ): WindowContainer? {
        return if (proto == null) {
            null
        } else {
            WindowContainer(
                title = nameOverride ?: proto.identifier?.title ?: "",
                token = proto.identifier?.hashCode?.toString(16) ?: "",
                orientation = proto.orientation,
                _isVisible = proto.visible,
                configurationContainer = createConfigurationContainer(proto.configurationContainer),
                layerId = proto.surfaceControl?.layerId ?: 0,
                children = children.toTypedArray(),
                computedZ = computedZCounter++
            )
        }
    }

    private fun createDisplayCutout(proto: DisplayCutoutProto?): DisplayCutout? {
        return if (proto == null) {
            null
        } else {
            DisplayCutout(
                proto.insets?.toInsets() ?: Insets.EMPTY,
                proto.boundLeft?.toRect() ?: Rect.EMPTY,
                proto.boundTop?.toRect() ?: Rect.EMPTY,
                proto.boundRight?.toRect() ?: Rect.EMPTY,
                proto.boundBottom?.toRect() ?: Rect.EMPTY,
                proto.waterfallInsets?.toInsets() ?: Insets.EMPTY
            )
        }
    }

    private fun appTransitionToString(transition: Int): String {
        return when (transition) {
            ViewProtoEnums.TRANSIT_UNSET -> "TRANSIT_UNSET"
            ViewProtoEnums.TRANSIT_NONE -> "TRANSIT_NONE"
            ViewProtoEnums.TRANSIT_ACTIVITY_OPEN -> TRANSIT_ACTIVITY_OPEN
            ViewProtoEnums.TRANSIT_ACTIVITY_CLOSE -> TRANSIT_ACTIVITY_CLOSE
            ViewProtoEnums.TRANSIT_TASK_OPEN -> TRANSIT_TASK_OPEN
            ViewProtoEnums.TRANSIT_TASK_CLOSE -> TRANSIT_TASK_CLOSE
            ViewProtoEnums.TRANSIT_TASK_TO_FRONT -> "TRANSIT_TASK_TO_FRONT"
            ViewProtoEnums.TRANSIT_TASK_TO_BACK -> "TRANSIT_TASK_TO_BACK"
            ViewProtoEnums.TRANSIT_WALLPAPER_CLOSE -> TRANSIT_WALLPAPER_CLOSE
            ViewProtoEnums.TRANSIT_WALLPAPER_OPEN -> TRANSIT_WALLPAPER_OPEN
            ViewProtoEnums.TRANSIT_WALLPAPER_INTRA_OPEN -> TRANSIT_WALLPAPER_INTRA_OPEN
            ViewProtoEnums.TRANSIT_WALLPAPER_INTRA_CLOSE -> TRANSIT_WALLPAPER_INTRA_CLOSE
            ViewProtoEnums.TRANSIT_TASK_OPEN_BEHIND -> "TRANSIT_TASK_OPEN_BEHIND"
            ViewProtoEnums.TRANSIT_ACTIVITY_RELAUNCH -> "TRANSIT_ACTIVITY_RELAUNCH"
            ViewProtoEnums.TRANSIT_DOCK_TASK_FROM_RECENTS -> "TRANSIT_DOCK_TASK_FROM_RECENTS"
            ViewProtoEnums.TRANSIT_KEYGUARD_GOING_AWAY -> TRANSIT_KEYGUARD_GOING_AWAY
            ViewProtoEnums.TRANSIT_KEYGUARD_GOING_AWAY_ON_WALLPAPER ->
                TRANSIT_KEYGUARD_GOING_AWAY_ON_WALLPAPER
            ViewProtoEnums.TRANSIT_KEYGUARD_OCCLUDE -> TRANSIT_KEYGUARD_OCCLUDE
            ViewProtoEnums.TRANSIT_KEYGUARD_UNOCCLUDE -> TRANSIT_KEYGUARD_UNOCCLUDE
            ViewProtoEnums.TRANSIT_TRANSLUCENT_ACTIVITY_OPEN -> TRANSIT_TRANSLUCENT_ACTIVITY_OPEN
            ViewProtoEnums.TRANSIT_TRANSLUCENT_ACTIVITY_CLOSE -> TRANSIT_TRANSLUCENT_ACTIVITY_CLOSE
            ViewProtoEnums.TRANSIT_CRASHING_ACTIVITY_CLOSE -> "TRANSIT_CRASHING_ACTIVITY_CLOSE"
            else -> error("Invalid lastUsedAppTransition")
        }
    }

    private fun appStateToString(appState: Int): String {
        return when (appState) {
            AppTransitionProto.APP_STATE_IDLE -> "APP_STATE_IDLE"
            AppTransitionProto.APP_STATE_READY -> "APP_STATE_READY"
            AppTransitionProto.APP_STATE_RUNNING -> "APP_STATE_RUNNING"
            AppTransitionProto.APP_STATE_TIMEOUT -> "APP_STATE_TIMEOUT"
            else -> error("Invalid AppTransitionState")
        }
    }

    private fun RectProto.toRect() = Rect.from(this.left, this.top, this.right, this.bottom)

    private fun RectProto.toInsets() = Insets.from(this.left, this.top, this.right, this.bottom)

    companion object {
        private const val TRANSIT_ACTIVITY_OPEN = "TRANSIT_ACTIVITY_OPEN"
        private const val TRANSIT_ACTIVITY_CLOSE = "TRANSIT_ACTIVITY_CLOSE"
        private const val TRANSIT_TASK_OPEN = "TRANSIT_TASK_OPEN"
        private const val TRANSIT_TASK_CLOSE = "TRANSIT_TASK_CLOSE"
        private const val TRANSIT_WALLPAPER_OPEN = "TRANSIT_WALLPAPER_OPEN"
        private const val TRANSIT_WALLPAPER_CLOSE = "TRANSIT_WALLPAPER_CLOSE"
        private const val TRANSIT_WALLPAPER_INTRA_OPEN = "TRANSIT_WALLPAPER_INTRA_OPEN"
        private const val TRANSIT_WALLPAPER_INTRA_CLOSE = "TRANSIT_WALLPAPER_INTRA_CLOSE"
        private const val TRANSIT_KEYGUARD_GOING_AWAY = "TRANSIT_KEYGUARD_GOING_AWAY"
        private const val TRANSIT_KEYGUARD_GOING_AWAY_ON_WALLPAPER =
            "TRANSIT_KEYGUARD_GOING_AWAY_ON_WALLPAPER"
        private const val TRANSIT_KEYGUARD_OCCLUDE = "TRANSIT_KEYGUARD_OCCLUDE"
        private const val TRANSIT_KEYGUARD_UNOCCLUDE = "TRANSIT_KEYGUARD_UNOCCLUDE"
        private const val TRANSIT_TRANSLUCENT_ACTIVITY_OPEN = "TRANSIT_TRANSLUCENT_ACTIVITY_OPEN"
        private const val TRANSIT_TRANSLUCENT_ACTIVITY_CLOSE = "TRANSIT_TRANSLUCENT_ACTIVITY_CLOSE"
    }
}
