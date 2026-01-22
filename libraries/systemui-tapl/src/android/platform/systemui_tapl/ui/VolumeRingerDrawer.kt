/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.platform.systemui_tapl.controller.VolumeController.RingerMode
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.UiObject2

interface VolumeRingerDrawer {

    val selectedMode: RingerMode

    fun selectRingerMode(mode: RingerMode)

    companion object {

        fun get(): VolumeRingerDrawer = VolumeRingerDrawerImpl()
    }
}

private class VolumeRingerDrawerImpl : VolumeRingerDrawer {

    private val container: UiObject2 =
        waitForObj(sysuiResSelector("volume_ringer_drawer")) { "Can't find the ringer drawer." }

    /**
     * Detect the current selected mode by checking the highlighted ringer icon. The highlighted
     * icon is the one with the active icon container on the top.
     */
    override val selectedMode: RingerMode
        get() {
            val selectedIndex =
                container.children
                    .mapIndexedNotNull { index, uiObject2 -> index.takeIf { uiObject2.isSelected } }
                    .single()
            return RingerMode.entries.single { it.getIndex() == selectedIndex }
        }

    /** Click the given ringer icon in the drawer. */
    override fun selectRingerMode(mode: RingerMode) {
        val index = mode.getIndex() ?: error("ringer mode is unavailable")
        container.children[index].click()
    }

    private fun RingerMode.getIndex(): Int? {
        return if (!RingerMode.VIBRATE.isAvailable) {
            when (this) {
                RingerMode.NORMAL -> 2
                RingerMode.SILENT -> 1
                RingerMode.VIBRATE -> null
            }
        } else {
            when (this) {
                RingerMode.NORMAL -> 3
                RingerMode.SILENT -> 2
                RingerMode.VIBRATE -> 1
            }
        }
    }
}
