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

package com.android.server.wm.traces.common.tags

import com.android.server.wm.flicker.service.config.common.Scenario

/**
 * Tag Class relating to a particular transition event in a WindowManager
 * or SurfaceFlinger trace state.
 * @param id The id to match the end and start tags
 * @param scenario The scenario this tag represents
 * @param isStartTag Tag represents the start or end moment in transition
 * @param layerId The Layer the tag is associated with (or 0 if no taskId associated with it)
 * @param windowToken The Window the tag is associated
 * with (or empty string if no taskId associated with it)
 * @param taskId The Task the tag is associated with (or 0 if no taskId associated with it)
 */
data class Tag(
    val id: Int,
    val scenario: Scenario,
    val isStartTag: Boolean,
    val layerId: Int = 0,
    val windowToken: String = "",
    val taskId: Int = 0
) {
    override fun toString() = if (isStartTag) "Start of $scenario" else "End of $scenario"
}
