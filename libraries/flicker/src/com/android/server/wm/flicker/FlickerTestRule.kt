/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule that runs the flicker tests.
 *
 * @param flicker Flicker test
 * @param cleanUp If the rule should cleanup instead of executing the test
 */
class FlickerTestRule @JvmOverloads constructor(
    val flicker: Flicker,
    val cleanUp: Boolean = false
) : TestRule {
    /**
     * JUnit rule that runs the flicker tests.
     *
     * @param testSpec Flicker test specification
     */
    constructor(testSpec: FlickerTestRunnerFactory.TestSpec): this(testSpec.test, testSpec.cleanUp)

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                if (cleanUp) {
                    flicker.clear()
                } else {
                    flicker.execute()
                    base.evaluate()
                }
            }
        }
}
