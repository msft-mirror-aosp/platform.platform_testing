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
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import kotlin.js.JsName

interface IComponentMatcher {
    @JsName("packageNames")
    val packageNames: Array<String>
    @JsName("classNames")
    val classNames: Array<String>
    @JsName("components")
    val components: List<ComponentName>

    @JsName("or")
    fun or(other: IComponentMatcher): IComponentMatcher
    @JsName("toActivityRecordFilter")
    fun toActivityRecordFilter(): Regex
    fun windowMatchesAnyOf(window: WindowState): Boolean
    fun windowMatchesAnyOf(windows: Collection<WindowState>): Boolean
    fun windowMatchesAnyOf(windows: Array<WindowState>): Boolean
    fun activityMatchesAnyOf(activity: Activity): Boolean
    fun activityMatchesAnyOf(activities: Collection<Activity>): Boolean
    fun activityMatchesAnyOf(activities: Array<Activity>): Boolean
    fun layerMatchesAnyOf(layer: Layer): Boolean
    fun layerMatchesAnyOf(layers: Collection<Layer>): Boolean
    fun layerMatchesAnyOf(layers: Array<Layer>): Boolean
    @JsName("toActivityName")
    fun toActivityName(): String
    @JsName("toWindowName")
    fun toWindowName(): String
    @JsName("toLayerName")
    fun toLayerName(): String
}
