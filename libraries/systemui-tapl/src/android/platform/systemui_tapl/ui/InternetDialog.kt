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

package android.platform.systemui_tapl.ui

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomator_helpers.DeviceHelpers.assertInvisible
import android.platform.uiautomator_helpers.DeviceHelpers.uiDevice
import android.platform.uiautomator_helpers.DeviceHelpers.waitForObj
import android.platform.uiautomator_helpers.WaitResult
import android.platform.uiautomator_helpers.WaitUtils.waitToBecomeTrue
import android.platform.uiautomator_helpers.scrollUntilFound
import android.util.Log
import androidx.test.uiautomator.UiObject2

/** Wrapper representing the InternetDialog that opens when the QS Tile is clicked */
class InternetDialog internal constructor() {
    private val scrollView: UiObject2 =
        waitForObj(sysuiResSelector(SCROLL_VIEW_RES_ID).hasParent(sysuiResSelector(DIALOG_RES_ID)))

    /** Finds the done button, clicks on it and asserts that the dialog has closed. */
    fun clickOnDoneAndClose() {
        val doneButton = scrollView.scrollUntilFound(DONE_BTN) ?: error("Done button not found")
        doneButton.click()
        if (waitToBecomeTrue { !uiDevice.hasObject(DONE_BTN) }.result !is WaitResult.WaitSuccess) {
            Log.d("QuickSettingsTileBase", "Retrying click due to b/339676505")
            doneButton.click()
        }
        DONE_BTN.assertInvisible(errorProvider = { "Internet dialog is dismissed" })
    }

    private companion object {
        const val DIALOG_RES_ID = "internet_connectivity_dialog"
        const val SCROLL_VIEW_RES_ID = "scroll_view"
        val DONE_BTN = sysuiResSelector("done_button")
    }
}
