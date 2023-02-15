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

package com.android.server.wm.flicker.service.config

import com.android.server.wm.flicker.service.assertors.IAssertionTemplate
import com.android.server.wm.flicker.service.config.AssertionTemplates.APP_CLOSE_TO_HOME_ASSERTIONS
import com.android.server.wm.flicker.service.config.AssertionTemplates.APP_LAUNCH_FROM_HOME_ASSERTIONS
import com.android.server.wm.flicker.service.config.AssertionTemplates.APP_LAUNCH_FROM_NOTIFICATION_ASSERTIONS
import com.android.server.wm.flicker.service.config.AssertionTemplates.COMMON_ASSERTIONS
import com.android.server.wm.flicker.service.config.AssertionTemplates.LAUNCHER_QUICK_SWITCH_ASSERTIONS
import com.android.server.wm.flicker.service.config.TransitionFilters.CLOSE_APP_TO_LAUNCHER_FILTER
import com.android.server.wm.flicker.service.config.TransitionFilters.OPEN_APP_TRANSITION_FILTER
import com.android.server.wm.flicker.service.config.TransitionFilters.QUICK_SWITCH_TRANSITION_FILTER
import com.android.server.wm.flicker.service.config.TransitionFilters.QUICK_SWITCH_TRANSITION_MERGE
import com.android.server.wm.flicker.service.extractors.EntireTraceExtractor
import com.android.server.wm.flicker.service.extractors.IScenarioExtractor
import com.android.server.wm.flicker.service.extractors.TaggedScenarioExtractor
import com.android.server.wm.flicker.service.extractors.TransitionMatcher
import com.android.server.wm.traces.common.events.CujType

object FlickerServiceConfig {
    /** EDIT THIS CONFIG TO ADD SCENARIOS TO FAAS */
    fun getScenarioConfigFor(type: FaasScenarioType): ScenarioConfig =
        when (type) {
            FaasScenarioType.COMMON ->
                ScenarioConfig(
                    extractor = EntireTraceExtractor(FaasScenarioType.COMMON),
                    assertionTemplates = COMMON_ASSERTIONS
                )
            FaasScenarioType.LAUNCHER_APP_LAUNCH_FROM_ICON ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_APP_LAUNCH_FROM_ICON,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_FROM_HOME_ASSERTIONS
                )
            FaasScenarioType.APP_CLOSE_TO_HOME ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_APP_CLOSE_TO_HOME,
                            type,
                            transitionMatcher = TransitionMatcher(CLOSE_APP_TO_LAUNCHER_FILTER)
                        ),
                    assertionTemplates = APP_CLOSE_TO_HOME_ASSERTIONS
                )
            FaasScenarioType.NOTIFICATION_APP_START ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_APP_START,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_FROM_NOTIFICATION_ASSERTIONS
                )
            FaasScenarioType.LAUNCHER_QUICK_SWITCH ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_QUICK_SWITCH,
                            type,
                            transitionMatcher =
                                TransitionMatcher(
                                    QUICK_SWITCH_TRANSITION_FILTER,
                                    finalTransform = QUICK_SWITCH_TRANSITION_MERGE
                                )
                        ),
                    assertionTemplates = LAUNCHER_QUICK_SWITCH_ASSERTIONS
                )
            FaasScenarioType.LAUNCHER_APP_LAUNCH_FROM_RECENTS ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_APP_LAUNCH_FROM_RECENTS,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_FROM_HOME_ASSERTIONS
                )
        }

    fun getExtractors(): List<IScenarioExtractor> {
        return FaasScenarioType.values().map { getScenarioConfigFor(it).extractor }
    }
}

data class ScenarioConfig(
    val extractor: IScenarioExtractor,
    val assertionTemplates: Collection<IAssertionTemplate>
)
