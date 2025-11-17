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

import com.android.bedstead.nene.TestApis
import platform.test.desktop.interactive.DesktopTestOptionsProvider.TestOption

/** Implementation of desktop test options relying on bedstead library providing test arguments. */
class DesktopTestOptions : DesktopTestOptionsProvider {
    private val instrumentationArguments = TestApis.instrumentation().arguments()

    override fun isHostDrivenTest(): Boolean =
        instrumentationArguments.getBoolean(TestOption.HOST_DRIVEN_TEST.key, false)

    override fun keepPeripheralsAfterTest(): Boolean =
        instrumentationArguments.getBoolean(TestOption.KEEP_PERIPHERALS_AFTER_TEST.key, false)

    override fun keepPeripheralsBeforeTest(): Boolean =
        instrumentationArguments.getBoolean(TestOption.KEEP_PERIPHERALS_BEFORE_TEST.key, false)

    override fun isManual(): Boolean =
        instrumentationArguments.getBoolean(TestOption.ENABLE_MANUAL.key, false)

    override fun isAutomated(): Boolean =
        instrumentationArguments.getBoolean(TestOption.ENABLE_AUTOMATED.key, false)

    override fun allowDisablingDisplays(): Boolean =
        instrumentationArguments.getBoolean(TestOption.ALLOW_DISABLING_DISPLAYS.key, false)
}
