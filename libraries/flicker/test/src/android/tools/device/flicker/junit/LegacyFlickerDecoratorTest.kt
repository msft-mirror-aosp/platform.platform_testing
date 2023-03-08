/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.device.flicker.junit

import android.annotation.SuppressLint
import android.tools.InitRule
import android.tools.common.ScenarioBuilder
import android.tools.device.flicker.datastore.DataStore
import android.tools.device.flicker.legacy.FlickerTest
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.TestWithParameters

/** Tests for [LegacyFlickerDecorator] */
@SuppressLint("VisibleForTests")
class LegacyFlickerDecoratorTest {
    @Before
    fun setup() {
        DataStore.clear()
    }

    @Test
    fun hasNoTestMethods() {
        val scenario =
            ScenarioBuilder().forClass(TestUtils.DummyTestClassValid::class.java.name).build()
        val test =
            TestWithParameters(
                "test",
                TestClass(TestUtils.DummyTestClassValid::class.java),
                listOf(TestUtils.VALID_ARGS_EMPTY)
            )
        val helper = LegacyFlickerDecorator(test.testClass, scenario, inner = null)
        Truth.assertWithMessage("Test method count").that(helper.getTestMethods(Any())).isEmpty()
    }

    @Test
    fun runTransitionAndAddToDatastore() {
        val scenario =
            ScenarioBuilder().forClass(TestUtils.DummyTestClassValid::class.java.name).build()
        val test =
            TestWithParameters(
                "test",
                TestClass(TestUtils.DummyTestClassValid::class.java),
                listOf(TestUtils.VALID_ARGS_EMPTY)
            )
        val helper = LegacyFlickerDecorator(test.testClass, scenario, inner = null)
        TestUtils.executionCount = 0
        val method =
            FrameworkMethod(TestUtils.DummyTestClassValid::class.java.getMethod("dummyExecute"))
        repeat(3) {
            helper
                .getMethodInvoker(
                    method,
                    test = TestUtils.DummyTestClassValid(FlickerTest()),
                )
                .evaluate()
        }

        Truth.assertWithMessage("Executed").that(TestUtils.executionCount).isEqualTo(1)
        Truth.assertWithMessage("In Datastore")
            .that(DataStore.containsResult(TestUtils.DummyTestClassValid.SCENARIO))
            .isTrue()
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
