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

import android.util.Log
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.errors.Error
import com.android.server.wm.traces.common.errors.ErrorTrace
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.ITransitionAssertor
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/**
 * Class that runs FASS assertions.
 */
class TransitionAssertor(
    private val configModel: AssertorConfigModel,
    private val logger: (String) -> Unit
) : ITransitionAssertor {
    override fun analyzeWmTrace(wmTrace: WindowManagerTrace): ErrorTrace {
        val errorStates = mutableMapOf<Long, MutableList<Error>>()

        errorStates.putAll(runCategoryAssertions(wmTrace, AssertionConfigParser.PRESUBMIT_KEY))
        errorStates.putAll(runCategoryAssertions(wmTrace, AssertionConfigParser.POSTSUBMIT_KEY))
        errorStates.putAll(runCategoryAssertions(wmTrace, AssertionConfigParser.FLAKY_KEY))

        return buildErrorTrace(errorStates)
    }

    override fun analyzeLayersTrace(layersTrace: LayersTrace): ErrorTrace {
        val errorStates = mutableMapOf<Long, MutableList<Error>>()

        errorStates.putAll(runCategoryAssertions(layersTrace, AssertionConfigParser.PRESUBMIT_KEY))
        errorStates.putAll(runCategoryAssertions(layersTrace, AssertionConfigParser.POSTSUBMIT_KEY))
        errorStates.putAll(runCategoryAssertions(layersTrace, AssertionConfigParser.FLAKY_KEY))

        return buildErrorTrace(errorStates)
    }

    private fun runCategoryAssertions(
        trace: ITrace<out ITraceEntry>,
        categoryKey: String
    ): Map<Long, MutableList<Error>> {
        val errors = mutableMapOf<Long, MutableList<Error>>()

        if (trace is WindowManagerTrace) {
            val subject = WindowManagerTraceSubject.assertThat(trace)
            val assertions = configModel.assertions.filter {
                it.trace == AssertionConfigParser.WM_TRACE_KEY &&
                    it.category == categoryKey
            }
            errors.putAll(runAssertionsOnSubject(subject, assertions))
        }

        if (trace is LayersTrace) {
            val subject = LayersTraceSubject.assertThat(trace)
            val assertions = configModel.assertions.filter {
                it.trace == AssertionConfigParser.LAYERS_TRACE_KEY &&
                    it.category == categoryKey
            }
            errors.putAll(runAssertionsOnSubject(subject, assertions))
        }

        return errors
    }

    private fun runAssertionsOnSubject(
        subject: FlickerTraceSubject<out FlickerSubject>,
        assertions: List<AssertionData>
    ): Map<Long, MutableList<Error>> {
        val errors = mutableMapOf<Long, MutableList<Error>>()
        val subjectClass = if (subject is WindowManagerTraceSubject)
            WindowManagerTraceSubject::class.java else LayersTraceSubject::class.java

        val assertionsClass = try {
            Class.forName(configModel.name)
        } catch (e: ClassNotFoundException) {
            Log.e("$FLICKER_TAG-ASSERT", "Assertions class not found", e)
            return errors
        }

        try {
            assertions.forEach { assertion ->
                val assertionMethod = assertionsClass.getDeclaredMethod(
                    assertion.name, subjectClass
                )
                val error = assertionMethod.invoke(
                        assertionsClass.newInstance(), subject
                ) as FlickerSubjectException?
                error?.let { exception ->
                    errors.putIfAbsent(exception.timestamp, mutableListOf())
                    errors.getValue(exception.timestamp).add(createError(subject, exception))
                }
            }
        } catch (e: NoSuchMethodException) {
            Log.e("$FLICKER_TAG-ASSERT", "Assertion method not found", e)
        } catch (e: SecurityException) {
            Log.e("$FLICKER_TAG-ASSERT", "Unable to get assertion method", e)
        }

        return errors
    }
}