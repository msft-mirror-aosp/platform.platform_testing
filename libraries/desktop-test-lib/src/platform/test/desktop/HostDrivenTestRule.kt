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

package platform.test.desktop

import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.desktop.DesktopTestOptions.TestOption
import platform.test.desktop.DesktopTestOptions.isHostDrivenTest

/**
 * Mark that a test is run as part of a host test suite e.g. mobly.
 *
 * This will skip the test during direct atest runs.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HostDrivenTest()

/**
 * Test rule which processes possible [HostDrivenTest] annotations on test methods. In case
 * annotation is found, the test will be run only as part of host test suite, otherwise skipped with
 * assumption failed.
 */
class HostDrivenTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                if (description.getAnnotation(HostDrivenTest::class.java) != null) {
                    assumeTrue(
                        "Test is required to be run with ${TestOption.HOST_DRIVEN_TEST.key} arg",
                        isHostDrivenTest,
                    )
                }
                base.evaluate()
            }
        }
}
