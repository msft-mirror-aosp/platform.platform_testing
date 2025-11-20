/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.platform.systemui_tapl.ui

import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.uiautomator.By

/** Wrapper representing the Cast Details View that opens when the QS Tile is clicked */
class CastDetailsView internal constructor(val displayId: Int = DEFAULT_DISPLAY) {
    private val viewSelector = By.text(TITLE_TEXT)
    private val backButtonSelector = By.desc(BACK_BUTTON_DESCRIPTION)

    fun assertVisible() {
        waitForObj(viewSelector)
        viewSelector.assertVisible()
    }

    /** Finds the back button, clicks on it and asserts that the details view has closed. */
    fun clickBackButton() {
        backButtonSelector.click()
        viewSelector.assertInvisible(errorProvider = { "Cast details view is dismissed" })
    }

    private companion object {
        private const val TITLE_TEXT = "Cast screen to device"
        private const val BACK_BUTTON_DESCRIPTION = "Back to QS panel"
    }
}
