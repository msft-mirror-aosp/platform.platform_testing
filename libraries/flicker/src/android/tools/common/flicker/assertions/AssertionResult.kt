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

package android.tools.common.flicker.assertions

import android.tools.common.flicker.AssertionInvocationGroup

interface AssertionResult {
    val assertionData: Collection<AssertionData>
    val assertionErrors: Collection<Throwable>
    val stabilityGroup: AssertionInvocationGroup
    val passed: Boolean
        get() = assertionErrors.isEmpty()
    val failed: Boolean
        get() = !passed

    val assertionName: String
        get() {
            require(assertionData.all { it.name == assertionData.first().name }) {
                "Failed to get assertion name because not all assertion data had the same name."
            }
            return assertionData.first().name
        }
}