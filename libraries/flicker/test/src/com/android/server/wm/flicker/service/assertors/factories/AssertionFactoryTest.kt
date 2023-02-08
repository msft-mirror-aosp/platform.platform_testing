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

package com.android.server.wm.flicker.service.assertors.factories

import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.ScenarioInstance
import com.android.server.wm.flicker.service.config.FaasScenarioType
import com.android.server.wm.flicker.service.config.FlickerServiceConfig
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.service.PlatformConsts
import com.google.common.truth.Truth
import org.junit.Test
import org.mockito.Mockito

/**
 * Contains tests for the [AssertionFactory]. To run this test: `atest
 * FlickerLibTest:AssertionFactoryTest`
 */
class AssertionFactoryTest {

    @Test
    fun getsAssertionsFromConfig() {
        val factory = AssertionFactory()

        val type = FaasScenarioType.LAUNCHER_APP_LAUNCH_FROM_ICON

        val scenarioInstance =
            ScenarioInstance(
                type = type,
                startRotation = PlatformConsts.Rotation.ROTATION_0,
                endRotation = PlatformConsts.Rotation.ROTATION_0,
                startTimestamp = Timestamp.MIN,
                endTimestamp = Timestamp.MAX,
                reader = Mockito.mock(IReader::class.java)
            )
        val assertions = factory.generateAssertionsFor(scenarioInstance)

        Truth.assertThat(assertions.map { it.name })
            .containsExactlyElementsIn(
                FlickerServiceConfig.getScenarioConfigFor(type = type).assertionTemplates.map {
                    it.assertionName
                }
            )
    }
}
