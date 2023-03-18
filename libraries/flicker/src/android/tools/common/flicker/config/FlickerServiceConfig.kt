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

package android.tools.common.flicker.config

import android.tools.common.flicker.assertors.IAssertionTemplate
import android.tools.common.flicker.config.AssertionTemplates.APP_CLOSE_TO_HOME_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.APP_CLOSE_TO_PIP_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.APP_LAUNCH_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.APP_LAUNCH_FROM_HOME_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.APP_LAUNCH_FROM_LOCK_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.APP_LAUNCH_FROM_NOTIFICATION_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.APP_SWIPE_TO_RECENTS_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.COMMON_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.ENTER_SPLITSCREEN_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.EXIT_SPLITSCREEN_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.LAUNCHER_QUICK_SWITCH_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.LOCKSCREEN_TRANSITION_FROM_AOD_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.LOCKSCREEN_TRANSITION_TO_AOD_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.LOCKSCREEN_UNLOCK_ANIMATION_ASSERTIONS
import android.tools.common.flicker.config.AssertionTemplates.RESIZE_SPLITSCREEN_ASSERTIONS
import android.tools.common.flicker.config.TransitionFilters.APP_CLOSE_TO_PIP_TRANSITION_FILTER
import android.tools.common.flicker.config.TransitionFilters.CLOSE_APP_TO_LAUNCHER_FILTER
import android.tools.common.flicker.config.TransitionFilters.ENTER_SPLIT_SCREEN_FILTER
import android.tools.common.flicker.config.TransitionFilters.EXIT_SPLIT_SCREEN_FILTER
import android.tools.common.flicker.config.TransitionFilters.OPEN_APP_TRANSITION_FILTER
import android.tools.common.flicker.config.TransitionFilters.QUICK_SWITCH_TRANSITION_FILTER
import android.tools.common.flicker.config.TransitionFilters.QUICK_SWITCH_TRANSITION_MERGE
import android.tools.common.flicker.config.TransitionFilters.RESIZE_SPLIT_SCREEN_FILTER
import android.tools.common.flicker.extractors.EntireTraceExtractor
import android.tools.common.flicker.extractors.IScenarioExtractor
import android.tools.common.flicker.extractors.TaggedScenarioExtractor
import android.tools.common.flicker.extractors.TransitionMatcher
import android.tools.common.traces.events.Cuj
import android.tools.common.traces.events.CujType

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
            FaasScenarioType.LAUNCHER_APP_CLOSE_TO_HOME ->
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
            FaasScenarioType.LOCKSCREEN_LAUNCH_CAMERA ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_LAUNCH_CAMERA,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_FROM_LOCK_ASSERTIONS
                )
            FaasScenarioType.LAUNCHER_APP_CLOSE_TO_PIP ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_APP_CLOSE_TO_PIP,
                            type,
                            transitionMatcher =
                                TransitionMatcher(APP_CLOSE_TO_PIP_TRANSITION_FILTER),
                            adjustCuj = { cuj, reader ->
                                val cujs = reader.readCujTrace() ?: error("Missing CUJ trace")
                                val closeToHomeCuj =
                                    cujs.entries.firstOrNull {
                                        it.cuj == CujType.CUJ_LAUNCHER_APP_CLOSE_TO_HOME &&
                                            it.startTimestamp <= cuj.startTimestamp &&
                                            cuj.startTimestamp <= it.endTimestamp
                                    }

                                if (closeToHomeCuj == null) {
                                    cuj
                                } else {
                                    Cuj(
                                        cuj.cuj,
                                        closeToHomeCuj.startTimestamp,
                                        cuj.endTimestamp,
                                        cuj.canceled
                                    )
                                }
                            }
                        ),
                    assertionTemplates = APP_CLOSE_TO_PIP_ASSERTIONS
                )
            FaasScenarioType.LAUNCHER_APP_LAUNCH_FROM_WIDGET ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_APP_LAUNCH_FROM_WIDGET,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_FROM_HOME_ASSERTIONS
                )
            FaasScenarioType.LAUNCHER_APP_SWIPE_TO_RECENTS ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_APP_SWIPE_TO_RECENTS,
                            type,
                            transitionMatcher = TransitionMatcher(CLOSE_APP_TO_LAUNCHER_FILTER)
                        ),
                    assertionTemplates = APP_SWIPE_TO_RECENTS_ASSERTIONS
                )
            FaasScenarioType.LAUNCHER_CLOSE_ALL_APPS_TO_HOME ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_CLOSE_ALL_APPS_TO_HOME,
                            type,
                            transitionMatcher = TransitionMatcher(CLOSE_APP_TO_LAUNCHER_FILTER)
                        ),
                    assertionTemplates = APP_CLOSE_TO_HOME_ASSERTIONS
                )
            FaasScenarioType.SPLIT_SCREEN_ENTER ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SPLIT_SCREEN_ENTER,
                            type,
                            transitionMatcher = TransitionMatcher(ENTER_SPLIT_SCREEN_FILTER)
                        ),
                    assertionTemplates = ENTER_SPLITSCREEN_ASSERTIONS
                )
            FaasScenarioType.SPLIT_SCREEN_EXIT ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SPLIT_SCREEN_EXIT,
                            type,
                            transitionMatcher = TransitionMatcher(EXIT_SPLIT_SCREEN_FILTER)
                        ),
                    assertionTemplates = EXIT_SPLITSCREEN_ASSERTIONS
                )
            FaasScenarioType.SPLIT_SCREEN_RESIZE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SPLIT_SCREEN_RESIZE,
                            type,
                            transitionMatcher = TransitionMatcher(RESIZE_SPLIT_SCREEN_FILTER)
                        ),
                    assertionTemplates = RESIZE_SPLITSCREEN_ASSERTIONS
                )
            FaasScenarioType.SHADE_APP_LAUNCH_FROM_HISTORY_BUTTON ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SHADE_APP_LAUNCH_FROM_HISTORY_BUTTON,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_ASSERTIONS
                )
            FaasScenarioType.SHADE_APP_LAUNCH_FROM_MEDIA_PLAYER ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SHADE_APP_LAUNCH_FROM_MEDIA_PLAYER,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_ASSERTIONS
                )
            FaasScenarioType.SHADE_APP_LAUNCH_FROM_QS_TILE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SHADE_APP_LAUNCH_FROM_QS_TILE,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_ASSERTIONS
                )
            FaasScenarioType.SHADE_APP_LAUNCH_FROM_SETTINGS_BUTTON ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SHADE_APP_LAUNCH_FROM_SETTINGS_BUTTON,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_ASSERTIONS
                )
            FaasScenarioType.STATUS_BAR_APP_LAUNCH_FROM_CALL_CHIP ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_STATUS_BAR_APP_LAUNCH_FROM_CALL_CHIP,
                            type,
                            transitionMatcher = TransitionMatcher(OPEN_APP_TRANSITION_FILTER)
                        ),
                    assertionTemplates = APP_LAUNCH_ASSERTIONS
                )
            FaasScenarioType.LOCKSCREEN_TRANSITION_FROM_AOD ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_TRANSITION_FROM_AOD,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = LOCKSCREEN_TRANSITION_FROM_AOD_ASSERTIONS
                )
            FaasScenarioType.LOCKSCREEN_TRANSITION_TO_AOD ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_TRANSITION_TO_AOD,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = LOCKSCREEN_TRANSITION_TO_AOD_ASSERTIONS
                )
            FaasScenarioType.LOCKSCREEN_UNLOCK_ANIMATION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_UNLOCK_ANIMATION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = LOCKSCREEN_UNLOCK_ANIMATION_ASSERTIONS
                )
            FaasScenarioType.NOTIFICATION_SHADE_EXPAND_COLLAPSE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_SHADE_EXPAND_COLLAPSE_LOCK ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_SHADE_EXPAND_COLLAPSE_LOCK,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_SHADE_SCROLL_FLING ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_SHADE_SCROLL_FLING,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_SHADE_ROW_EXPAND ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_SHADE_ROW_EXPAND,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_SHADE_ROW_SWIPE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_SHADE_ROW_SWIPE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_SHADE_QS_EXPAND_COLLAPSE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_SHADE_QS_EXPAND_COLLAPSE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_SHADE_QS_SCROLL_SWIPE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_SHADE_QS_SCROLL_SWIPE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_HEADS_UP_APPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_HEADS_UP_APPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_HEADS_UP_DISAPPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_HEADS_UP_DISAPPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_ADD ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_ADD,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.NOTIFICATION_REMOVE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_NOTIFICATION_REMOVE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LOCKSCREEN_PASSWORD_APPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_PASSWORD_APPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LOCKSCREEN_PATTERN_APPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_PATTERN_APPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LOCKSCREEN_PIN_APPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_PIN_APPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LOCKSCREEN_PASSWORD_DISAPPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_PASSWORD_DISAPPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LOCKSCREEN_PATTERN_DISAPPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_PATTERN_DISAPPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LOCKSCREEN_PIN_DISAPPEAR ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_PIN_DISAPPEAR,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LAUNCHER_OPEN_ALL_APPS ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_OPEN_ALL_APPS,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LAUNCHER_ALL_APPS_SCROLL ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SETTINGS_PAGE_SCROLL ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SETTINGS_PAGE_SCROLL,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.PIP_TRANSITION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_PIP_TRANSITION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.WALLPAPER_TRANSITION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_WALLPAPER_TRANSITION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.USER_SWITCH ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_USER_SWITCH,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SPLASHSCREEN_AVD ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SPLASHSCREEN_AVD,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SPLASHSCREEN_EXIT_ANIM ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SPLASHSCREEN_EXIT_ANIM,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SCREEN_OFF ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SCREEN_OFF,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SCREEN_OFF_SHOW_AOD ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SCREEN_OFF_SHOW_AOD,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.ONE_HANDED_ENTER_TRANSITION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_ONE_HANDED_ENTER_TRANSITION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.ONE_HANDED_EXIT_TRANSITION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_ONE_HANDED_EXIT_TRANSITION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.UNFOLD_ANIM ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_UNFOLD_ANIM,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SUW_LOADING_TO_SHOW_INFO_WITH_ACTIONS ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SUW_LOADING_TO_SHOW_INFO_WITH_ACTIONS,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SUW_SHOW_FUNCTION_SCREEN_WITH_ACTIONS ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SUW_SHOW_FUNCTION_SCREEN_WITH_ACTIONS,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SUW_LOADING_TO_NEXT_FLOW ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SUW_LOADING_TO_NEXT_FLOW,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SUW_LOADING_SCREEN_FOR_STATUS ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SUW_LOADING_SCREEN_FOR_STATUS,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SETTINGS_SLIDER ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SETTINGS_SLIDER,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.TAKE_SCREENSHOT ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_TAKE_SCREENSHOT,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.VOLUME_CONTROL ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_VOLUME_CONTROL,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.BIOMETRIC_PROMPT_TRANSITION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_BIOMETRIC_PROMPT_TRANSITION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SETTINGS_TOGGLE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SETTINGS_TOGGLE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SHADE_DIALOG_OPEN ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SHADE_DIALOG_OPEN,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.USER_DIALOG_OPEN ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_USER_DIALOG_OPEN,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.TASKBAR_EXPAND ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_TASKBAR_EXPAND,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.TASKBAR_COLLAPSE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_TASKBAR_COLLAPSE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.SHADE_CLEAR_ALL ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_SHADE_CLEAR_ALL,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LAUNCHER_UNLOCK_ENTRANCE_ANIMATION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_UNLOCK_ENTRANCE_ANIMATION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LOCKSCREEN_OCCLUSION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LOCKSCREEN_OCCLUSION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.RECENTS_SCROLLING ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_RECENTS_SCROLLING,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.LAUNCHER_CLOSE_ALL_APPS_SWIPE ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_LAUNCHER_CLOSE_ALL_APPS_SWIPE,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
            FaasScenarioType.IME_INSETS_ANIMATION ->
                ScenarioConfig(
                    extractor =
                        TaggedScenarioExtractor(
                            targetTag = CujType.CUJ_IME_INSETS_ANIMATION,
                            type,
                            transitionMatcher =
                                TransitionMatcher(associatedTransitionRequired = false),
                        ),
                    assertionTemplates = COMMON_ASSERTIONS, // TODO: Add specific assertions
                    enabled = false,
                )
        }

    fun getExtractors(enabledOnly: Boolean = true): List<IScenarioExtractor> {
        val scenarios: Collection<FaasScenarioType> =
            if (enabledOnly) {
                FaasScenarioType.values().filter { getScenarioConfigFor(it).enabled }
            } else {
                FaasScenarioType.values().asList()
            }
        return scenarios.map { getScenarioConfigFor(it).extractor }
    }
}

data class ScenarioConfig(
    val extractor: IScenarioExtractor,
    val assertionTemplates: Collection<IAssertionTemplate>,
    val enabled: Boolean = true
)
