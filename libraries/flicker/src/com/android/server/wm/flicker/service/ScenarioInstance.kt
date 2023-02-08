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

package com.android.server.wm.flicker.service

import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.config.FaasScenarioType
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.events.CujType
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.transition.Transition

data class ScenarioInstance(
    override val type: FaasScenarioType,
    override val startRotation: PlatformConsts.Rotation,
    override val endRotation: PlatformConsts.Rotation,
    val startTimestamp: Timestamp,
    val endTimestamp: Timestamp,
    override val reader: IReader,
    val associatedCuj: CujType? = null,
    override val associatedTransition: Transition? = null,
) : IScenarioInstance {
    val startTransaction
        get() = associatedTransition?.startTransaction
    val finishTransaction
        get() = associatedTransition?.finishTransaction

    // b/227752705
    override val navBarMode: PlatformConsts.NavBar
        get() = error("Unsupported")

    override val key: String
        get() = "${type.name}_${startRotation}_$endRotation"

    override val description: String = key

    override val isEmpty: Boolean
        get() = false

    override fun toString() = key
}
