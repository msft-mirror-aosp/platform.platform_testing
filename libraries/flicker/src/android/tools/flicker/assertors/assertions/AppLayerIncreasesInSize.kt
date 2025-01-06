/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.tools.flicker.assertors.assertions

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.ComponentTemplate

/** Checks that the visible region of [component] always increases during the animation */
class AppLayerIncreasesInSize(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        val layerMatcher = component.get(scenarioInstance)
        flicker.assertLayers {
            val layerList = layers { layerMatcher.layerMatchesAnyOf(it) && it.isVisible }
            layerList.zipWithNext { previous, current ->
                current.visibleRegion.coversAtLeast(previous.visibleRegion.region)
            }
        }
    }
}
