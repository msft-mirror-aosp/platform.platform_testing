/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.ComponentName
import android.view.Display
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.parser.toActivityName
import com.android.server.wm.traces.parser.toWindowName
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

class WindowManagerStateSubject private constructor(
    fm: FailureMetadata,
    private val wmState: WindowManagerState
) : Subject(fm, wmState) {
    @JvmOverloads
    fun isRotation(
        rotation: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Rotation should be $rotation")
            .that(rotation)
            .isEqualTo(wmState.getRotation(displayId))
    }

    @JvmOverloads
    fun isNotRotation(
        rotation: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Rotation should not be $rotation")
            .that(rotation)
            .isNotEqualTo(wmState.getRotation(displayId))
    }

    fun contains(activity: ComponentName): WindowManagerStateSubject = apply {
        val windowName = activity.toWindowName()
        val activityName = activity.toActivityName()
        Truth.assertWithMessage("Activity=$activityName must exist.")
                .that(wmState.containsActivity(activityName)).isTrue()
        Truth.assertWithMessage("Window=$windowName must exits.")
                .that(wmState.containsWindow(windowName)).isTrue()
    }

    fun notContains(activity: ComponentName): WindowManagerStateSubject = apply {
        val windowName = activity.toWindowName()
        val activityName = activity.toActivityName()
        Truth.assertWithMessage("Activity=$activityName must NOT exist.")
            .that(wmState.containsActivity(activityName)).isFalse()
        Truth.assertWithMessage("Window=$windowName must NOT exits.")
            .that(wmState.containsWindow(windowName)).isFalse()
    }

    @JvmOverloads
    fun isRecentsActivityVisible(visible: Boolean = true): WindowManagerStateSubject = apply {
        if (wmState.isHomeRecentsComponent) {
            isHomeActivityVisible()
        } else {
            Truth.assertWithMessage("Recents activity is ${if (visible) "" else "not"} visible")
                    .that(wmState.isRecentsActivityVisible)
                    .isEqualTo(visible)
        }
    }

    fun isValid(): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Must have stacks").that(wmState.stackCount).isGreaterThan(0)
        // TODO: Update when keyguard will be shown on multiple displays
        if (!wmState.keyguardControllerState.isKeyguardShowing) {
            Truth.assertWithMessage("There should be at least one resumed activity in the system.")
                .that(wmState.resumedActivitiesCount).isGreaterThan(0)
        }
        Truth.assertWithMessage("Must have focus activity.")
            .that(wmState.focusedActivity).isNotEmpty()
        wmState.rootTasks.forEach { aStack ->
            val stackId = aStack.rootTaskId
            aStack.tasks.forEach { aTask ->
                Truth.assertWithMessage("Stack can only contain its own tasks")
                    .that(stackId).isEqualTo(aTask.rootTaskId)
            }
        }
        Truth.assertWithMessage("Must have front window.")
            .that(wmState.frontWindow).isNotEmpty()
        Truth.assertWithMessage("Must have focused window.")
            .that(wmState.focusedWindow).isNotEmpty()
        Truth.assertWithMessage("Must have app.")
            .that(wmState.focusedApp).isNotEmpty()
    }

    fun hasFocusedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Focused activity invalid")
            .that(activityComponentName)
            .isEqualTo(wmState.focusedActivity)
        Truth.assertWithMessage("Focused app invalid")
            .that(activityComponentName)
            .isEqualTo(wmState.focusedApp)
    }

    fun hasNotFocusedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Has focused activity")
            .that(wmState.focusedActivity)
            .isNotEqualTo(activityComponentName)
        Truth.assertWithMessage("Has focused app")
            .that(wmState.focusedApp)
            .isNotEqualTo(activityComponentName)
    }

    @JvmOverloads
    fun hasFocusedApp(
        activity: ComponentName,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Focused app invalid")
            .that(activityComponentName)
            .isEqualTo(wmState.getDisplay(displayId)?.focusedApp)
    }

    fun hasResumedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Invalid resumed activity")
            .that(activityComponentName)
            .isEqualTo(wmState.focusedActivity)
    }

    fun hasNotResumedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Has resumed activity")
            .that(wmState.focusedActivity)
            .isNotEqualTo(activityComponentName)
    }

    fun isWindowFocused(windowName: String): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Invalid focused window")
            .that(windowName)
            .isEqualTo(wmState.focusedWindow)
    }

    fun isWindowNotFocused(windowName: String): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Has focused window")
            .that(wmState.focusedWindow)
            .isNotEqualTo(windowName)
    }

    fun isVisible(activity: ComponentName): WindowManagerStateSubject =
        hasActivityAndWindowVisibility(activity, visible = true)

    fun isInvisible(activity: ComponentName): WindowManagerStateSubject =
        hasActivityAndWindowVisibility(activity, visible = false)

    private fun hasActivityAndWindowVisibility(
        activity: ComponentName,
        visible: Boolean
    ): WindowManagerStateSubject = apply {
        // Check existence of activity and window.
        val windowName = activity.toWindowName()
        val activityName = activity.toActivityName()
        Truth.assertWithMessage("Activity=$activityName must exist.")
            .that(wmState.containsActivity(activityName)).isTrue()
        Truth.assertWithMessage("Window=$windowName must exist.")
            .that(wmState.containsWindow(windowName)).isTrue()

        // Check visibility of activity and window.
        Truth.assertWithMessage("Activity=$activityName must ${if (visible) "" else " NOT"}" +
            " be visible.")
            .that(visible).isEqualTo(wmState.isActivityVisible(activityName))
        Truth.assertWithMessage("Window=$windowName must ${if (visible) "" else " NOT"}" +
            " have shown surface.")
            .that(visible).isEqualTo(wmState.isWindowSurfaceShown(windowName))
    }

    @JvmOverloads
    fun isHomeActivityVisible(visible: Boolean = true): WindowManagerStateSubject = apply {
        val homeActivity = wmState.homeActivityName
        require(homeActivity != null)
        hasActivityAndWindowVisibility(homeActivity, visible)
    }

    @JvmOverloads
    fun isImeWindowShown(
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        val imeWinState = wmState.inputMethodWindowState
        Truth.assertWithMessage("IME window must exist")
            .that(imeWinState).isNotNull()
        Truth.assertWithMessage("IME window must be shown")
            .that(imeWinState?.isSurfaceShown ?: false).isTrue()
        Truth.assertWithMessage("IME window must be on the given display")
            .that(displayId).isEqualTo(imeWinState?.displayId ?: -1)
    }

    @JvmOverloads
    fun isImeWindowNotShown(
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        val imeWinState = wmState.inputMethodWindowState
        Truth.assertWithMessage("IME window must not be shown")
            .that(imeWinState?.isSurfaceShown ?: false).isFalse()
        if (imeWinState?.isSurfaceShown == true) {
            Truth.assertWithMessage("IME window must not be on the given display")
                .that(displayId).isNotEqualTo(imeWinState.displayId)
        }
    }

    private val WindowManagerState.homeActivityName: ComponentName?
        get() {
            val activity = homeActivity ?: return null
            return ComponentName.unflattenFromString(activity.name)
        }

    companion object {
        /**
         * Boiler-plate Subject.Factory for WindowManagerStateSubject
         */
        private val FACTORY = Factory { fm: FailureMetadata, subject: WindowManagerState ->
            WindowManagerStateSubject(fm, subject)
        }

        /**
         * User-defined entry point
         */
        @JvmStatic
        fun assertThat(entry: WindowManagerState?) =
            Truth.assertAbout(FACTORY).that(entry) as WindowManagerStateSubject
    }
}