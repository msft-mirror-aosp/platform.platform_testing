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

package android.tools.common.flicker.assertors.assertions

import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertions.FlickerTest
import android.tools.common.flicker.assertors.ComponentTemplate

/**
 * Checks that the app layer doesn't exist or is invisible at the start of the transition, but is
 * created and/or becomes visible during the transition.
 */
class HasAtMostOneWindowMatching(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        flicker.assertWm {
            invoke("HasAtMostOneWindowMatching") {
                val matcher = component.build(scenarioInstance)
                val windowCount =
                    it.wmState.windowStates.count { window -> matcher.windowMatchesAnyOf(window) }
                require(windowCount <= 1) { "Matched more than 1 $matcher" }
            }
        }
    }
}
