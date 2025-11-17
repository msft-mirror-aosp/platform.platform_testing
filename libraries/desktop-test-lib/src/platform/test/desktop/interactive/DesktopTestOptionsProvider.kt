/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.desktop.interactive

import android.util.Log

/** Interface used to decouple desktop-test-lib from bedstead library */
interface DesktopTestOptionsProvider {
    // LINT.IfChange
    enum class TestOption(val key: String) {
        HOST_DRIVEN_TEST("HOST_DRIVEN_TEST"),
        KEEP_PERIPHERALS_AFTER_TEST("KEEP_PERIPHERALS_AFTER_TEST"),
        KEEP_PERIPHERALS_BEFORE_TEST("KEEP_PERIPHERALS_BEFORE_TEST"),
        ALLOW_DISABLING_DISPLAYS("ALLOW_DISABLING_DISPLAYS"),
        ENABLE_MANUAL("ENABLE_MANUAL"),
        ENABLE_AUTOMATED("ENABLE_AUTOMATED"),
    }

    // LINT.ThenChange(../../../../../mobly/desktop_test_base.py)

    fun isHostDrivenTest(): Boolean

    fun keepPeripheralsAfterTest(): Boolean

    fun keepPeripheralsBeforeTest(): Boolean

    fun isManual(): Boolean

    fun isAutomated(): Boolean

    fun allowDisablingDisplays(): Boolean

    companion object {
        private var instance: DesktopTestOptionsProvider? = null

        fun getInstance(): DesktopTestOptionsProvider {
            if (instance != null) {
                return instance!!
            }
            try {
                val c = Class.forName("platform.test.desktop.interactive.DesktopTestOptions")
                instance = c.getDeclaredConstructor().newInstance() as DesktopTestOptionsProvider
                return instance!!
            } catch (e: ClassNotFoundException) {
                Log.d("DesktopTestOptions", "ClassNotFoundException: " + e.message)
                return DefaultDesktopOptions()
            }
        }
    }

    class DefaultDesktopOptions : DesktopTestOptionsProvider {
        override fun isHostDrivenTest(): Boolean = false

        override fun keepPeripheralsAfterTest(): Boolean = false

        override fun keepPeripheralsBeforeTest(): Boolean = false

        override fun isManual(): Boolean = false

        override fun isAutomated(): Boolean = false

        override fun allowDisablingDisplays(): Boolean = false
    }
}
