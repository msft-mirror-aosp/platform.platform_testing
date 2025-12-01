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

package android.platform.test.rule

import android.platform.uiautomatorhelpers.DeviceHelpers.context
import android.platform.uiautomatorhelpers.DeviceHelpers.shell
import android.provider.Settings

/** Base rule to set values in [Settings.Global]. The value is then reset at the end of the test. */
class GlobalSettingRule<T : Any>(private val settingName: String, initialValue: T? = null) :
    SettingRule<T>(initialValue) {

    override fun getSettingValueAsString(): String? =
        Settings.Global.getString(context.contentResolver, settingName)

    override fun setSettingValueAsString(value: String?) {
        // We don't have permission from the test to write Global Settings. Using shell command
        // instead.
        if (value == null) {
            shell("settings delete global $settingName")
        } else {
            shell("settings put global $settingName $value")
        }
    }
}
