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

package com.android.server.wm.flicker.service.assertors.assertions

import com.android.server.wm.flicker.service.assertors.ComponentBuilder
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.transition.Transition

/**
 * Checks that [getWindowState] window remains inside the display bounds throughout the whole
 * animation
 */
class AppWindowRemainInsideDisplayBounds(component: ComponentBuilder) :
    BaseAssertionBuilderWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(transition: Transition, wmSubject: WindowManagerTraceSubject) {
        wmSubject
            .invoke("appWindowRemainInsideDisplayBounds") { entry ->
                val displays = entry.wmState.displays
                if (displays.isEmpty()) {
                    entry.fail("No displays found")
                }
                val display = entry.wmState.displays.sortedBy { it.id }.first()
                entry.visibleRegion(component.build(transition)).coversAtMost(display.displayRect)
            }
            .forAllEntries()
    }
}
