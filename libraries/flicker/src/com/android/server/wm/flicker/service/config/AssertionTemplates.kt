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

import com.android.server.wm.flicker.service.assertors.Components
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerBecomesVisible
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsInvisibleAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsInvisibleAtStart
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsVisibleAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsVisibleAtStart
import com.android.server.wm.flicker.service.assertors.assertions.AppWindowBecomesTopWindow
import com.android.server.wm.flicker.service.assertors.assertions.AppWindowBecomesVisible
import com.android.server.wm.flicker.service.assertors.assertions.EntireScreenCoveredAlways
import com.android.server.wm.flicker.service.assertors.assertions.EntireScreenCoveredAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.EntireScreenCoveredAtStart
import com.android.server.wm.flicker.service.assertors.assertions.LayerIsVisibleAlways
import com.android.server.wm.flicker.service.assertors.assertions.LayerIsVisibleAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.LayerIsVisibleAtStart
import com.android.server.wm.flicker.service.assertors.assertions.NonAppWindowIsVisibleAlways
import com.android.server.wm.flicker.service.assertors.assertions.VisibleLayersShownMoreThanOneConsecutiveEntry
import com.android.server.wm.flicker.service.assertors.assertions.VisibleWindowsShownMoreThanOneConsecutiveEntry

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

    val APP_LAUNCH_FROM_HOME_ASSERTIONS =
        APP_LAUNCH_ASSERTIONS +
            listOf(
                AppLayerIsVisibleAtStart(Components.LAUNCHER),
                AppLayerIsInvisibleAtEnd(Components.LAUNCHER),
            )

    val APP_CLOSE_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                AppLayerIsVisibleAtStart(Components.CLOSING_APP),
                AppLayerIsInvisibleAtStart(Components.LAUNCHER),
                AppLayerIsInvisibleAtEnd(Components.CLOSING_APP),
                AppLayerIsVisibleAtEnd(Components.LAUNCHER),
            )

    val APP_LAUNCH_FROM_NOTIFICATION_ASSERTIONS =
        COMMON_ASSERTIONS +
            APP_LAUNCH_ASSERTIONS +
            listOf(
                // None specific to opening from notification yet
                )
}
