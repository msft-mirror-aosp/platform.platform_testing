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

import android.platform.systemui_tapl.utils.LAUNCHER_PACKAGE
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector

/**
 * Represents the Taskbar UI component on a specific display.
 *
 * @param displayId The ID of the physical or virtual display to target.
 */
class Taskbar(private val displayId: Int) {
    val selector: BySelector =
        By.displayId(displayId).pkg(LAUNCHER_PACKAGE).res(LAUNCHER_PACKAGE, TASKBAR_RES_ID)

    fun assertVisible() {
        selector.assertVisible()
    }

    companion object {
        private const val TASKBAR_RES_ID = "taskbar_view"

        @JvmStatic
        fun getTaskbarHeight(displayId: Int): Int {
            return uiDevice.findObject(Taskbar(displayId).selector).let {
                it?.visibleBounds?.height() ?: 0
            }
        }
    }
}
