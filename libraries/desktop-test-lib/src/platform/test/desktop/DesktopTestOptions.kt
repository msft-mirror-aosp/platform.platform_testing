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

package platform.test.desktop

import com.android.bedstead.nene.TestApis

object DesktopTestOptions {
    // LINT.IfChange
    enum class TestOption(val key: String) {
        HOST_DRIVEN_TEST("HOST_DRIVEN_TEST"),
        KEEP_PERIPHERALS_AFTER_TEST("KEEP_PERIPHERALS_AFTER_TEST"),
        KEEP_PERIPHERALS_BEFORE_TEST("KEEP_PERIPHERALS_BEFORE_TEST"),
        ALLOW_DISABLING_DISPLAYS("ALLOW_DISABLING_DISPLAYS"),
        ENABLE_MANUAL("ENABLE_MANUAL"),
        ENABLE_AUTOMATED("ENABLE_AUTOMATED"),
    }

    // LINT.ThenChange(../../../../mobly/desktop_test_base.py)

    private val instrumentationArguments = TestApis.instrumentation().arguments()
    val isHostDrivenTest =
        instrumentationArguments.getBoolean(TestOption.HOST_DRIVEN_TEST.key, false)
    val keepPeripheralsAfterTest =
        instrumentationArguments.getBoolean(TestOption.KEEP_PERIPHERALS_AFTER_TEST.key, false)
    val keepPeripheralsBeforeTest =
        instrumentationArguments.getBoolean(TestOption.KEEP_PERIPHERALS_BEFORE_TEST.key, false)
    val isManual = instrumentationArguments.getBoolean(TestOption.ENABLE_MANUAL.key, false)
    val isAutomated = instrumentationArguments.getBoolean(TestOption.ENABLE_AUTOMATED.key, false)
    val allowDisablingDisplays =
        instrumentationArguments.getBoolean(TestOption.ALLOW_DISABLING_DISPLAYS.key, false)
}
