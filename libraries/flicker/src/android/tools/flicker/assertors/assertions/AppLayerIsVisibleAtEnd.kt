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

package android.tools.flicker.assertors.assertions

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.ComponentTemplate
import android.tools.traces.component.ComponentNameMatcher

/** Checks if the [component] layer is visible at the end of the transition */
class AppLayerIsVisibleAtEnd(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        // The app launch transition can finish when the splashscreen or SnapshotStartingWindows are
        // shown before the app window and layers are actually shown. (b/284302118)
        flicker.assertLayersEnd {
            isVisible(
                ComponentNameMatcher.SNAPSHOT.or(ComponentNameMatcher.SPLASH_SCREEN)
                    .or(component.get(scenarioInstance))
            )
        }
    }
}
