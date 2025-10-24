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

package android.tools.traces.wm

import android.tools.Cache
import android.tools.testutils.assertThrows
import android.tools.traces.parsers.perfetto.Args
import android.tools.traces.parsers.perfetto.WindowContainerBuilder
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [WindowContainerBuilder] tests. To run this test: `atest
 * FlickerLibTest:WindowContainerBuilderTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WindowContainerBuilderTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun createsContainerWithoutParentToken() {
        val args = getWindowContainerArgs()
        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("Container1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_CONTAINER)
                .build()
        checkCommonWindowContainerProperties(container)
    }

    @Test
    fun createsContainerWithParentToken() {
        val args = getWindowContainerArgs()
        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("Container1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_CONTAINER)
                .setParentToken(321)
                .build()
        checkCommonWindowContainerProperties(container, parentToken = 321)
    }

    @Test
    fun createsContainerWithNameOverride() {
        val nameOverride = "OverrideName"
        val args = getWindowContainerArgs()
        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("Container1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_CONTAINER)
                .setNameOverride(nameOverride)
                .build()
        checkCommonWindowContainerProperties(container, name = nameOverride, title = nameOverride)
    }

    @Test
    fun createsContainerWithConfigurationContainers() {
        val args = getWindowContainerArgs(withConfiguration = true)
        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("Container1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_CONTAINER)
                .build()

        checkCommonWindowContainerProperties(container)
        container.overrideConfiguration.let { config ->
            Truth.assertThat(config).isNotNull()
            Truth.assertThat(config?.densityDpi).isEqualTo(160)
            Truth.assertThat(config?.windowConfiguration?.windowingMode).isEqualTo(1)
            Truth.assertThat(config?.windowConfiguration?.activityType).isEqualTo(2)
        }
        Truth.assertThat(container.fullConfiguration).isNull()
        Truth.assertThat(container.mergedOverrideConfiguration).isNull()
    }

    @Test
    fun failsToCreateContainerWithUnknownContainerType() {
        val args = getWindowContainerArgs()
        val builder =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("Container1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType("UnknownType")

        val failure = assertThrows<IllegalStateException> { builder.build() }
        Truth.assertThat(failure)
            .hasMessageThat()
            .contains("unable to build container with token 123 and type UnknownType")
    }

    @Test
    fun createsRootWindowContainer() {
        val title = "RootContainer"
        val args = getWindowContainerArgs()
        args.add("is_home_recents_component", "true", "bool")
        args.add(makeKey("pending_activities[0]", "title"), "PendingActivity1", "string")
        args.add(makeKey("pending_activities[1]", "title"), "PendingActivity2", "string")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle(title)
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.ROOT_WINDOW_CONTAINER)
                .build()

        Truth.assertThat(container).isInstanceOf(RootWindowContainer::class.java)
        val rootContainer = container as RootWindowContainer
        checkCommonWindowContainerProperties(rootContainer, title = title, name = title)
        Truth.assertThat(rootContainer.isHomeRecentsComponent).isTrue()
        Truth.assertThat(rootContainer.pendingActivities)
            .containsExactly("PendingActivity1", "PendingActivity2")
    }

    @Test
    fun createsDisplayContent() {
        val argKeyRoot = "display_content"
        val args = getWindowContainerArgs(prefix = makeKey(argKeyRoot, "root_display_area"))
        args.add(makeKey(argKeyRoot, "id"), "123456", "int")
        args.add(makeKey(argKeyRoot, "focused_root_task_id"), "10", "int")
        args.add(makeKey(argKeyRoot, "resumed_activity", "title"), "ResumedActivity", "string")
        args.add(makeKey(argKeyRoot, "single_task_instance"), "true", "bool")
        args.add(makeKey(argKeyRoot, "display_info", "logical_width"), "1080", "int")
        args.add(makeKey(argKeyRoot, "display_info", "logical_height"), "1920", "int")
        args.add(makeKey(argKeyRoot, "display_info", "app_width"), "1000", "int")
        args.add(makeKey(argKeyRoot, "display_info", "app_height"), "1800", "int")
        args.add(makeKey(argKeyRoot, "dpi"), "420", "int")
        args.add(makeKey(argKeyRoot, "surface_size"), "2500", "int")
        args.add(makeKey(argKeyRoot, "focused_app"), "FocusedApp", "string")
        args.add(
            makeKey(argKeyRoot, "app_transition", "last_used_app_transition"),
            "TRANSIT_OPEN",
            "string",
        )
        args.add(makeKey(argKeyRoot, "display_rotation", "rotation"), "1", "int")
        args.add(makeKey(argKeyRoot, "display_rotation", "last_orientation"), "3", "int")
        args.add(makeKey(argKeyRoot, "display_frames", "stable_bounds", "left"), "10", "int")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("DisplayContent")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.DISPLAY_CONTENT)
                .build()

        Truth.assertThat(container).isInstanceOf(DisplayContent::class.java)
        val displayContent = container as DisplayContent
        checkCommonWindowContainerProperties(
            displayContent,
            name = "123456",
            title = "DisplayContent",
            isVisible = false,
        )
        Truth.assertThat(displayContent.displayId).isEqualTo(123456)
        Truth.assertThat(displayContent.focusedRootTaskId).isEqualTo(10)
        Truth.assertThat(displayContent.resumedActivity).isEqualTo("ResumedActivity")
        Truth.assertThat(displayContent.singleTaskInstance).isTrue()
        Truth.assertThat(displayContent.displayRect.width()).isEqualTo(1080)
        Truth.assertThat(displayContent.displayRect.height()).isEqualTo(1920)
        Truth.assertThat(displayContent.appRect.width()).isEqualTo(1000)
        Truth.assertThat(displayContent.dpi).isEqualTo(420)
        Truth.assertThat(displayContent.stableBounds.left).isEqualTo(10)
        Truth.assertThat(displayContent.focusedApp).isEqualTo("FocusedApp")
        Truth.assertThat(displayContent.surfaceSize).isEqualTo(2500)
        Truth.assertThat(displayContent.lastTransition).isEqualTo("TRANSIT_OPEN")
        Truth.assertThat(displayContent.appTransitionState)
            .isEqualTo(WindowContainerBuilder.DEFAULT_APP_STATE)
        Truth.assertThat(displayContent.rotation.value).isEqualTo(1)
        Truth.assertThat(displayContent.lastOrientation).isEqualTo(3)
    }

    @Test
    fun createsDisplayArea() {
        val args = getWindowContainerArgs(prefix = "display_area")
        args.add("display_area.is_task_display_area", "true", "bool")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("DisplayArea")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.DISPLAY_AREA)
                .build()

        Truth.assertThat(container).isInstanceOf(DisplayArea::class.java)
        val displayArea = container as DisplayArea
        checkCommonWindowContainerProperties(
            displayArea,
            name = "DisplayArea",
            title = "DisplayArea",
        )
        Truth.assertThat(displayArea.isTaskDisplayArea).isTrue()
    }

    @Test
    fun createsTask() {
        val argKeyRoot = "task"
        val args = getWindowContainerArgs(prefix = "task")
        args.add(makeKey(argKeyRoot, "id"), "10", "int")
        args.add(makeKey(argKeyRoot, "activity_type"), "2", "int")
        args.add(makeKey(argKeyRoot, "fills_parent"), "true", "bool")
        args.add(makeKey(argKeyRoot, "root_task_id"), "100", "int")
        args.add(makeKey(argKeyRoot, "display_id"), "1", "int")
        args.add(makeKey(argKeyRoot, "real_activity"), "com.example.RealActivity", "string")
        args.add(makeKey(argKeyRoot, "resize_mode"), "3", "int")
        args.add(makeKey(argKeyRoot, "resumed_activity", "title"), "ResumedActivity", "string")
        args.add(makeKey(argKeyRoot, "animating_bounds"), "true", "bool")
        args.add(makeKey(argKeyRoot, "surface_width"), "100", "int")
        args.add(makeKey(argKeyRoot, "surface_height"), "200", "int")
        args.add(makeKey(argKeyRoot, "created_by_organizer"), "true", "bool")
        args.add(makeKey(argKeyRoot, "bounds", "left"), "0", "int")
        args.add(makeKey(argKeyRoot, "bounds", "top"), "0", "int")
        args.add(makeKey(argKeyRoot, "bounds", "right"), "100", "int")
        args.add(makeKey(argKeyRoot, "bounds", "bottom"), "200", "int")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("Task1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.TASK)
                .build()

        Truth.assertThat(container).isInstanceOf(Task::class.java)
        val task = container as Task
        checkCommonWindowContainerProperties(task, name = "10", title = "Task1", isVisible = false)
        Truth.assertThat(task.taskId).isEqualTo(10)
        Truth.assertThat(task.activityType).isEqualTo(2)
        Truth.assertThat(task.isFullscreen).isTrue()
        Truth.assertThat(task.rootTaskId).isEqualTo(100)
        Truth.assertThat(task.displayId).isEqualTo(1)
        Truth.assertThat(task.realActivity).isEqualTo("com.example.RealActivity")
        Truth.assertThat(task.resizeMode).isEqualTo(3)
        Truth.assertThat(task.resumedActivities).containsExactly("ResumedActivity")
        Truth.assertThat(task.animatingBounds).isTrue()
        Truth.assertThat(task.surfaceWidth).isEqualTo(100)
        Truth.assertThat(task.surfaceHeight).isEqualTo(200)
        Truth.assertThat(task.createdByOrganizer).isTrue()
    }

    @Test
    fun createsTaskFragment() {
        val argKeyRoot = "task_fragment"
        val args = getWindowContainerArgs(prefix = argKeyRoot)
        args.add(makeKey(argKeyRoot, "activity_type"), "1", "int")
        args.add(makeKey(argKeyRoot, "display_id"), "0", "int")
        args.add(makeKey(argKeyRoot, "min_width"), "100", "int")
        args.add(makeKey(argKeyRoot, "min_height"), "200", "int")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("TaskFragment1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.TASK_FRAGMENT)
                .build()

        Truth.assertThat(container).isInstanceOf(TaskFragment::class.java)
        val taskFragment = container as TaskFragment
        checkCommonWindowContainerProperties(
            taskFragment,
            name = "TaskFragment1",
            title = "TaskFragment1",
        )
        Truth.assertThat(taskFragment.activityType).isEqualTo(1)
        Truth.assertThat(taskFragment.displayId).isEqualTo(0)
        Truth.assertThat(taskFragment.minWidth).isEqualTo(100)
        Truth.assertThat(taskFragment.minHeight).isEqualTo(200)
    }

    @Test
    fun createsActivity() {
        val argKeyRoot = "activity"
        val args = getWindowContainerArgs(prefix = makeKey(argKeyRoot, "window_token"))
        args.add(makeKey(argKeyRoot, "state"), "RESUMED", "string")
        args.add(makeKey(argKeyRoot, "front_of_task"), "true", "bool")
        args.add(makeKey(argKeyRoot, "proc_id"), "999", "int")
        args.add(makeKey(argKeyRoot, "translucent"), "false", "bool")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("Activity1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.ACTIVITY)
                .build()

        Truth.assertThat(container).isInstanceOf(Activity::class.java)
        val activity = container as Activity
        checkCommonWindowContainerProperties(activity, name = "Activity1", title = "Activity1")
        Truth.assertThat(activity.state).isEqualTo("RESUMED")
        Truth.assertThat(activity.frontOfTask).isTrue()
        Truth.assertThat(activity.procId).isEqualTo(999)
        Truth.assertThat(activity.isTranslucent).isFalse()
    }

    @Test
    fun createsWindowToken() {
        val argKeyRoot = "window_token"
        val args = getWindowContainerArgs(prefix = argKeyRoot)

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("WindowToken1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_TOKEN)
                .build()

        Truth.assertThat(container).isInstanceOf(WindowToken::class.java)
        val windowToken = container as WindowToken
        checkCommonWindowContainerProperties(
            windowToken,
            name = "WindowToken1",
            title = "WindowToken1",
            isVisible = false,
        )
    }

    @Test
    fun createsWindowState() {
        val argKeyRoot = "window"
        val args = getWindowContainerArgs(prefix = argKeyRoot)
        args.add(makeKey(argKeyRoot, "display_id"), "0", "int")
        args.add(makeKey(argKeyRoot, "stack_id"), "1", "int")
        args.add(makeKey(argKeyRoot, "requested_width"), "500", "int")
        args.add(makeKey(argKeyRoot, "requested_height"), "800", "int")
        args.add(makeKey(argKeyRoot, "animator", "surface", "layer"), "1000", "int")
        args.add(makeKey(argKeyRoot, "animator", "surface", "shown"), "true", "bool")
        args.add(makeKey(argKeyRoot, "window_frames", "frame", "left"), "10", "int")
        args.add(makeKey(argKeyRoot, "window_frames", "content_insets", "top"), "20", "int")
        args.add(makeKey(argKeyRoot, "attributes", "type"), "2", "int")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("WindowState1")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_STATE)
                .build()

        Truth.assertThat(container).isInstanceOf(WindowState::class.java)
        val windowState = container as WindowState
        checkCommonWindowContainerProperties(
            windowState,
            name = "WindowState1",
            title = "WindowState1",
        )
        Truth.assertThat(windowState.displayId).isEqualTo(0)
        Truth.assertThat(windowState.stackId).isEqualTo(1)
        Truth.assertThat(windowState.layer).isEqualTo(1000)
        Truth.assertThat(windowState.isSurfaceShown).isTrue()
        Truth.assertThat(windowState.requestedSize.width).isEqualTo(500)
        Truth.assertThat(windowState.frame.left).isEqualTo(10)
        Truth.assertThat(windowState.contentInsets.top).isEqualTo(20)
        Truth.assertThat(windowState.attributes.type).isEqualTo(2)
        Truth.assertThat(windowState.windowType).isEqualTo(0) // Default type
    }

    @Test
    fun createsStartingWindowStateWithWindowTypeStarting() {
        val args = getWindowContainerArgs(prefix = "window")
        args.add(
            makeKey("window", "window_container", "identifier", "title"),
            "Starting AppWindow",
            "string",
        )

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("DebuggerWindow")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_STATE)
                .build()

        val windowState = container as WindowState
        Truth.assertThat(windowState.windowType).isEqualTo(1)
    }

    @Test
    fun createsDebuggerWindowStateWithWindowTypeStarting() {
        val args = getWindowContainerArgs(prefix = "window")
        args.add(
            makeKey("window", "window_container", "identifier", "title"),
            "Waiting For Debugger: AppWindow",
            "string",
        )

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("DebuggerWindow")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_STATE)
                .build()

        val windowState = container as WindowState
        Truth.assertThat(windowState.windowType).isEqualTo(1)
    }

    @Test
    fun createsWindowStateWithWindowTypeExiting() {
        val args = getWindowContainerArgs(prefix = "window")
        args.add(makeKey("window", "animating_exit"), "true", "bool")

        val container =
            WindowContainerBuilder()
                .setArgs(args)
                .setTitle("ExitingWindow")
                .setToken(123)
                .setIsVisible(true)
                .setContainerType(WindowContainerBuilder.WINDOW_STATE)
                .build()

        val windowState = container as WindowState
        Truth.assertThat(windowState.windowType).isEqualTo(2)
    }

    companion object {

        private fun checkCommonWindowContainerProperties(
            container: WindowContainer,
            name: String = "Container1",
            title: String = "Container1",
            parentToken: Int? = null,
            isVisible: Boolean = true,
        ) {
            Truth.assertThat(container.title).isEqualTo(title)
            Truth.assertThat(container.name).isEqualTo(name)
            Truth.assertThat(container.id).isEqualTo(123)
            Truth.assertThat(container.token).isEqualTo("7b")
            Truth.assertThat(container.orientation).isEqualTo(2)
            Truth.assertThat(container.layerId).isEqualTo(45)
            Truth.assertThat(container.children.size).isEqualTo(0)
            Truth.assertThat(container.isVisible).isEqualTo(isVisible)
            Truth.assertThat(container.parentToken).isEqualTo(parentToken)
        }

        private fun getWindowContainerArgs(
            withConfiguration: Boolean = false,
            prefix: String = "",
        ): Args {
            val args = Args()
            val windowContainerPrefix = makeKey(prefix, "window_container")
            args.add(makeKey(windowContainerPrefix, "orientation"), "2", "int")
            args.add(makeKey(windowContainerPrefix, "surface_control", "layer_id"), "45", "int")
            if (withConfiguration) addConfigurationContainerArgs(args, windowContainerPrefix)
            return args
        }

        private fun addConfigurationContainerArgs(args: Args, prefix: String) {
            val configurationPrefix =
                makeKey(prefix, "configuration_container", "override_configuration")
            args.add(makeKey(configurationPrefix, "density_dpi"), "160", "int")
            args.add(
                makeKey(configurationPrefix, "window_configuration", "windowing_mode"),
                "1",
                "int",
            )
            args.add(
                makeKey(configurationPrefix, "window_configuration", "activity_type"),
                "2",
                "int",
            )
        }

        private fun makeKey(vararg parts: String): String {
            return parts.filter { it.isNotEmpty() }.joinToString(".")
        }
    }
}
