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

package com.android.server.wm.traces.common.service

import com.android.server.wm.traces.common.IScenario

class FlickerServiceScenario(
    val scenarioType: ScenarioType,
    override val startRotation: PlatformConsts.Rotation
) : IScenario {
    // b/227752705
    override val navBarMode: PlatformConsts.NavBar
        get() = error("Unsupported")

    override val key: String
        get() = "${scenarioType.description}_$startRotation"

    override val description: String = key

    override val isEmpty: Boolean
        get() = false

    override fun toString() = key

    override fun equals(other: Any?): Boolean {
        return other is FlickerServiceScenario &&
            scenarioType == other.scenarioType &&
            startRotation == other.startRotation
    }

    override fun hashCode(): Int {
        var result = scenarioType.hashCode()
        result = 31 * result + startRotation.hashCode()
        return result
    }
}
