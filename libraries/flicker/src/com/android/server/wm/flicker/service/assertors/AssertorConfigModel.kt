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

/**
 * Represents an assertor configuration.
 */
data class AssertorConfigModel(
    var name: String,
    var presubmit: Array<AssertionData>,
    var postsubmit: Array<AssertionData>,
    var flaky: Array<AssertionData>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssertorConfigModel) return false

        if (name != other.name) return false
        if (!presubmit.contentEquals(other.presubmit)) return false
        if (!postsubmit.contentEquals(other.postsubmit)) return false
        if (!flaky.contentEquals(other.flaky)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + presubmit.contentHashCode()
        result = 31 * result + postsubmit.contentHashCode()
        result = 31 * result + flaky.contentHashCode()
        return result
    }
}