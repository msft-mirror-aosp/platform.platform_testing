/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.readTestFile
import com.android.server.wm.traces.common.tags.Transition
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [AssertionConfigParser] tests. To run this test:
 * `atest FlickerLibTest:AssertionConfigParserTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AssertionConfigParserTest {
    @Test
    fun canParseConfigFile() {
        val jsonByteArray = readTestFile("assertors/assertionsConfig.json")
        val assertions = AssertionConfigParser.parseConfigFile(String(jsonByteArray))

        val rotationAssertions = arrayOf(
            AssertionData("navBarWindowIsVisible", "wmTrace", "presubmit"),
            AssertionData("navBarLayerIsVisible", "layersTrace", "presubmit"),
            AssertionData("entireScreenCovered", "layersTrace", "postsubmit"),
            AssertionData("launcherWindowBecomesInvisible", "wmTrace", "flaky"),
            AssertionData("visibleLayersShownMoreThanOneConsecutiveEntry", "layersTrace", "flaky")
        )
        Truth.assertThat(assertions.size).isEqualTo(2)
        Truth.assertThat(assertions[0].name).isEqualTo("RotationAssertions")
        Truth.assertThat(assertions[0].transition).isEqualTo(Transition.ROTATION)
        Truth.assertThat(assertions[0].assertions.size).isEqualTo(5)
        Truth.assertThat(assertions[0].assertions).isEqualTo(rotationAssertions)

        val appLaunchAssertion = AssertionData("entireScreenCovered", "wmTrace", "flaky")
        Truth.assertThat(assertions[1].assertions.size).isEqualTo(10)
        Truth.assertThat(assertions[1].assertions[9]).isEqualTo(appLaunchAssertion)
    }
}