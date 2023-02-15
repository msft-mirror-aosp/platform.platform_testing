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

package com.android.server.wm.flicker.service

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.assertors.IFaasAssertion
import com.android.server.wm.flicker.service.assertors.factories.IAssertionFactory
import com.android.server.wm.flicker.service.assertors.runners.IAssertionRunner
import com.android.server.wm.flicker.service.extractors.IScenarioExtractor
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.mockito.ArgumentCaptor
import org.mockito.Mockito

/** Contains [FlickerService] tests. To run this test: `atest FlickerLibTest:FlickerServiceTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerServiceTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val wmHelper = WindowManagerStateHelper(instrumentation, clearCacheAfterParsing = false)

    @Test
    fun generatesAssertionsFromExtractedScenarios() {
        val mockReader = Mockito.mock(IReader::class.java)
        val mockScenarioExtractor = Mockito.mock(IScenarioExtractor::class.java)
        val mockAssertionFactory = Mockito.mock(IAssertionFactory::class.java)
        val mockAssertionRunner = Mockito.mock(IAssertionRunner::class.java)

        val scenarioInstance = Mockito.mock(IScenarioInstance::class.java)
        val assertions = listOf(Mockito.mock(IFaasAssertion::class.java))

        Mockito.`when`(mockScenarioExtractor.extract(mockReader))
            .thenReturn(listOf(scenarioInstance))
        Mockito.`when`(mockAssertionFactory.generateAssertionsFor(scenarioInstance))
            .thenReturn(assertions)

        val service =
            FlickerService(
                scenarioExtractor = mockScenarioExtractor,
                assertionFactory = mockAssertionFactory,
                assertionRunner = mockAssertionRunner
            )
        service.process(mockReader)

        Mockito.verify(mockScenarioExtractor).extract(mockReader)
        Mockito.verify(mockAssertionFactory).generateAssertionsFor(scenarioInstance)
    }

    @Test
    fun executesAssertionsReturnedByAssertionFactories() {
        val mockReader = Mockito.mock(IReader::class.java)
        val mockScenarioExtractor = Mockito.mock(IScenarioExtractor::class.java)
        val mockAssertionFactory = Mockito.mock(IAssertionFactory::class.java)
        val mockAssertionRunner = Mockito.mock(IAssertionRunner::class.java)

        val scenarioInstance = Mockito.mock(IScenarioInstance::class.java)
        val assertions = listOf(Mockito.mock(IFaasAssertion::class.java))

        Mockito.`when`(mockScenarioExtractor.extract(mockReader))
            .thenReturn(listOf(scenarioInstance))
        Mockito.`when`(mockAssertionFactory.generateAssertionsFor(scenarioInstance))
            .thenReturn(assertions)

        val service =
            FlickerService(
                scenarioExtractor = mockScenarioExtractor,
                assertionFactory = mockAssertionFactory,
                assertionRunner = mockAssertionRunner
            )
        service.process(mockReader)

        Mockito.verify(mockScenarioExtractor).extract(mockReader)
        Mockito.verify(mockAssertionRunner).execute(assertions)
    }

    fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

    inline fun <reified T : Any> argumentCaptor() = ArgumentCaptor.forClass(T::class.java)
}
