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

package android.platform.systemui_tapl.ui

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import androidx.test.uiautomator.BySelector

/**
 * Represents the slider component within the brightness dialog, which is typically displayed after
 * changing the brightness via hardware keys.
 */
class BrightnessDialogSlider {
    init {
        SELECTOR.assertVisible()
    }

    companion object {
        private val SELECTOR: BySelector
            get() = sysuiResSelector("brightness_dialog_slider")

        fun assertInvisible() {
            SELECTOR.assertInvisible()
        }
    }
}
