/*
 * Copyright (C) 2026 The Android Open Source Project
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
package android.platform.uiautomatoraccessibility

import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesElements
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.common.Suppressor
import com.google.android.apps.common.testing.accessibility.framework.matcher.ElementMatchers.withContentDescription
import com.google.android.apps.common.testing.accessibility.framework.matcher.ElementMatchers.withResourceName
import com.google.android.apps.common.testing.accessibility.framework.matcher.ElementMatchers.withTestTag
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.oneOf

/**
 * Configures global suppression matchers to be used on all UI Automator tests, spanning SysUi etc
 */
object GlobalSuppressions {
    /** Add all suppressions to the given suppressor */
    fun addAll(it: Suppressor<AccessibilityHierarchyCheckResult>) {

        // TODO(b/466187499): remove this suppression and fix the root cause
        it.addSuppressingResultMatcher(
            allOf(
                matchesCheck(DuplicateClickableBoundsCheck::class.java),
                matchesElements(
                    withResourceName(
                        anyOf(
                            containsString("search_container_workspace"),
                            containsString("smartspace_card_pager"),
                        )
                    )
                ),
            )
        )

        // TODO(b/466186277): remove this suppression and fix the root cause
        it.addSuppressingResultMatcher(
            allOf(
                matchesCheck(TouchTargetSizeCheck::class.java),
                matchesElements(
                    withResourceName(
                        `is`(
                            oneOf(
                                "com.android.systemui:id/carrier_combo",
                                "com.android.systemui:id/carrier1",
                                "com.android.systemui:id/hover_system_icons_container",
                                "com.android.systemui:id/clock",
                                "com.android.systemui:id/no_carrier_text",
                            )
                        )
                    )
                ),
            )
        )
    }
}
