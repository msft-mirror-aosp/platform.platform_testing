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

package com.android.server.wm.traces.common.windowmanager.windows

import com.android.server.wm.traces.common.Rect

/**
 * Represents a task in the window manager hierarchy
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot
 * access internal Java/Android functionality
 *
 */
open class ActivityTask(
    override val activityType: Int,
    override val isFullscreen: Boolean,
    override val bounds: Rect,
    val taskId: Int,
    val rootTaskId: Int,
    val displayId: Int,
    val lastNonFullscreenBounds: Rect,
    val realActivity: String,
    val origActivity: String,
    val resizeMode: Int,
    val resumedActivity: String,
    var animatingBounds: Boolean,
    val surfaceWidth: Int,
    val surfaceHeight: Int,
    val createdByOrganizer: Boolean,
    val minWidth: Int,
    val minHeight: Int,
    windowContainer: WindowContainer
) : WindowContainer(windowContainer) {
    override val kind: String = "Task"
    override val name: String = taskId.toString()
    override val isEmpty: Boolean by lazy { tasks.isEmpty() && activities.isEmpty() }

    val isRootTask: Boolean by lazy { taskId == rootTaskId }
    val tasks: List<ActivityTask>
        by lazy { this.childrenWindows.reversed().filterIsInstance<ActivityTask>() }
    val activities: List<Activity>
        by lazy { this.childrenWindows.reversed().filterIsInstance<Activity>() }

    /** The top task in the stack.
     */
    // NOTE: Unlike the WindowManager internals, we dump the state from top to bottom,
    //       so the indices are inverted
    val topTask: ActivityTask? by lazy { tasks.firstOrNull() }

    fun getTask(predicate: (ActivityTask) -> Boolean) =
        tasks.firstOrNull { predicate(it) } ?: if (predicate(this)) this else null

    fun getTask(taskId: Int) = getTask { t -> t.taskId == taskId }

    fun forAllTasks(consumer: (ActivityTask) -> Any) {
        tasks.forEach { consumer(it) }
    }

    fun getActivity(predicate: (Activity) -> Boolean): Activity? {
        return activities.firstOrNull { predicate(it) }
            ?: tasks.flatMap { it.activities }
                .firstOrNull { predicate(it) }
    }

    fun getActivity(activityName: String): Activity? {
        return getActivity { activity -> activity.title == activityName }
    }

    fun containsActivity(activityName: String) = getActivity(activityName) != null

    override fun toString(): String {
        return "$kind: {$token $title} id=$taskId bounds=$bounds"
    }
}