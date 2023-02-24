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

import android.tools.common.flicker.assertors.Components
import android.tools.common.flicker.assertors.assertions.AppLayerBecomesInvisible
import android.tools.common.flicker.assertors.assertions.AppLayerBecomesVisible
import android.tools.common.flicker.assertors.assertions.AppLayerCoversFullScreenAtEnd
import android.tools.common.flicker.assertors.assertions.AppLayerCoversFullScreenAtStart
import android.tools.common.flicker.assertors.assertions.AppLayerIsInvisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppLayerIsInvisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppLayerIsVisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppLayerIsVisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowBecomesInvisible
import android.tools.common.flicker.assertors.assertions.AppWindowBecomesTopWindow
import android.tools.common.flicker.assertors.assertions.AppWindowBecomesVisible
import android.tools.common.flicker.assertors.assertions.AppWindowCoversFullScreenAtEnd
import android.tools.common.flicker.assertors.assertions.AppWindowCoversFullScreenAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowIsInvisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppWindowIsInvisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowIsTopWindowAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowIsVisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppWindowIsVisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowOnTopAtEnd
import android.tools.common.flicker.assertors.assertions.AppWindowOnTopAtStart
import android.tools.common.flicker.assertors.assertions.EntireScreenCoveredAlways
import android.tools.common.flicker.assertors.assertions.EntireScreenCoveredAtEnd
import android.tools.common.flicker.assertors.assertions.EntireScreenCoveredAtStart
import android.tools.common.flicker.assertors.assertions.LayerIsVisibleAlways
import android.tools.common.flicker.assertors.assertions.LayerIsVisibleAtEnd
import android.tools.common.flicker.assertors.assertions.LayerIsVisibleAtStart
import android.tools.common.flicker.assertors.assertions.NonAppWindowIsVisibleAlways
import android.tools.common.flicker.assertors.assertions.VisibleLayersShownMoreThanOneConsecutiveEntry
import android.tools.common.flicker.assertors.assertions.VisibleWindowsShownMoreThanOneConsecutiveEntry

object AssertionTemplates {
    val COMMON_ASSERTIONS =
        listOf(
            EntireScreenCoveredAtStart(),
            EntireScreenCoveredAtEnd(),
            EntireScreenCoveredAlways(),
            VisibleWindowsShownMoreThanOneConsecutiveEntry(),
            VisibleLayersShownMoreThanOneConsecutiveEntry(),
        )

    val NAV_BAR_ASSERTIONS =
        listOf(
            LayerIsVisibleAtStart(Components.NAV_BAR),
            LayerIsVisibleAtEnd(Components.NAV_BAR),
            NonAppWindowIsVisibleAlways(Components.NAV_BAR),
        )

    val STATUS_BAR_ASSERTIONS =
        listOf(
            NonAppWindowIsVisibleAlways(Components.STATUS_BAR),
            LayerIsVisibleAlways(Components.STATUS_BAR),
        )

    val APP_LAUNCH_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                AppLayerIsInvisibleAtStart(Components.OPENING_APP),
                AppLayerIsVisibleAtEnd(Components.OPENING_APP),
                AppLayerBecomesVisible(Components.OPENING_APP),
                AppWindowBecomesVisible(Components.OPENING_APP),
                AppWindowBecomesTopWindow(Components.OPENING_APP),
            )

    val APP_CLOSE_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                AppLayerIsVisibleAtStart(Components.CLOSING_APP),
                AppLayerIsInvisibleAtEnd(Components.CLOSING_APP),
                AppWindowIsVisibleAtStart(Components.CLOSING_APP),
                AppWindowIsInvisibleAtEnd(Components.CLOSING_APP),
                AppLayerBecomesInvisible(Components.CLOSING_APP),
                AppWindowBecomesInvisible(Components.CLOSING_APP),
                AppWindowIsTopWindowAtStart(Components.CLOSING_APP),
            )

    val APP_LAUNCH_FROM_HOME_ASSERTIONS =
        APP_LAUNCH_ASSERTIONS +
            listOf(
                AppLayerIsVisibleAtStart(Components.LAUNCHER),
                AppLayerIsInvisibleAtEnd(Components.LAUNCHER),
            )

    val APP_CLOSE_TO_HOME_ASSERTIONS =
        APP_CLOSE_ASSERTIONS +
            listOf(
                AppLayerIsInvisibleAtStart(Components.LAUNCHER),
                AppLayerIsVisibleAtEnd(Components.LAUNCHER),
                AppWindowIsInvisibleAtStart(Components.LAUNCHER),
                AppWindowIsVisibleAtEnd(Components.LAUNCHER),
                AppWindowBecomesTopWindow(Components.LAUNCHER),
            )

    val APP_LAUNCH_FROM_NOTIFICATION_ASSERTIONS =
        COMMON_ASSERTIONS +
            APP_LAUNCH_ASSERTIONS +
            listOf(
                // None specific to opening from notification yet
                )

    val LAUNCHER_QUICK_SWITCH_ASSERTIONS =
        COMMON_ASSERTIONS +
            APP_LAUNCH_ASSERTIONS +
            APP_CLOSE_ASSERTIONS +
            listOf(
                AppWindowCoversFullScreenAtStart(Components.CLOSING_APP),
                AppLayerCoversFullScreenAtStart(Components.CLOSING_APP),
                AppWindowCoversFullScreenAtEnd(Components.OPENING_APP),
                AppLayerCoversFullScreenAtEnd(Components.OPENING_APP),
                AppWindowOnTopAtStart(Components.CLOSING_APP),
                AppWindowOnTopAtEnd(Components.OPENING_APP),
                AppWindowBecomesInvisible(Components.CLOSING_APP),
                AppLayerBecomesInvisible(Components.CLOSING_APP),
                AppWindowBecomesVisible(Components.OPENING_APP),
                AppLayerBecomesVisible(Components.OPENING_APP),
            )
}
