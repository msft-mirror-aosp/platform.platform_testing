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
import com.android.server.wm.flicker.service.detectors.AssertionConfigParser
import com.android.server.wm.flicker.service.detectors.AssertionData
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

        val presubmitAssertions = arrayOf(
            AssertionData("navBarWindowIsVisible", "wmTrace"),
            AssertionData("navBarLayerIsVisible", "layersTrace")
        )
        Truth.assertThat(assertions.size).isEqualTo(2)
        Truth.assertThat(assertions[0].name).isEqualTo("RotationAssertor")
        Truth.assertThat(assertions[0].presubmit).isEqualTo(presubmitAssertions)
        Truth.assertThat(assertions[0].postsubmit).isEmpty()
        Truth.assertThat(assertions[0].flaky.size).isEqualTo(3)

        val flakyAssertion = AssertionData("entireScreenCovered", "wmTrace")
        Truth.assertThat(assertions[1].flaky.size).isEqualTo(1)
        Truth.assertThat(assertions[1].flaky[0]).isEqualTo(flakyAssertion)
    }
}