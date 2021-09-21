/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.traces.common.tags.Transition

/**
 * Represents an assertor configuration.
 */
data class AssertorConfigModel(
    var name: String,
    var transition: Transition,
    var assertions: Array<AssertionData> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssertorConfigModel) return false

        if (name != other.name) return false
        if (transition != other.transition) return false
        if (!assertions.contentEquals(other.assertions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + transition.hashCode()
        result = 31 * result + assertions.contentHashCode()
        return result
    }
}