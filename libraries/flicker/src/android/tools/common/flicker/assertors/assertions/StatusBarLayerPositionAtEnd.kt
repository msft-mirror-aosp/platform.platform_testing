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

package android.tools.common.flicker.assertors.assertions

import android.tools.common.PlatformConsts
import android.tools.common.datatypes.Region
import android.tools.common.flicker.IScenarioInstance
import android.tools.common.flicker.assertors.AssertionTemplate
import android.tools.common.flicker.subject.layers.LayersTraceSubject
import android.tools.common.traces.component.ComponentNameMatcher

/**
 * Checks if the [ComponentNameMatcher.STATUS_BAR] layer is placed at the correct position at the
 * end of the transition
 */
class StatusBarLayerPositionAtEnd : AssertionTemplate() {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: IScenarioInstance, layerSubject: LayersTraceSubject) {
        layerSubject
            .last()
            .visibleRegion(ComponentNameMatcher.STATUS_BAR)
            .coversExactly(getExpectedStatusbarPosition(scenarioInstance))
    }

    // TODO: Maybe find another way to get the expected position that doesn't rely on use the data
    // from the WM trace
    // can we maybe dump another trace that just has system info for this purpose?
    private fun getExpectedStatusbarPosition(scenarioInstance: IScenarioInstance): Region {
        val wmState =
            scenarioInstance.reader.readWmTrace()?.entries?.last()
                ?: error("Missing wm trace entries")
        val display =
            wmState.getDisplay(PlatformConsts.DEFAULT_DISPLAY) ?: error("Display not found")
        TODO("return WindowUtils.getExpectedStatusBarPosition(display)")
    }
}
