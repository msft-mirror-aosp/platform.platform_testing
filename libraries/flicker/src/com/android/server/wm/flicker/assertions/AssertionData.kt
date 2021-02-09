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

package com.android.server.wm.flicker.assertions

import android.annotation.IntDef
import android.platform.test.annotations.Presubmit
import androidx.test.filters.FlakyTest
import com.android.server.wm.flicker.FlickerRunResult
import com.android.server.wm.flicker.FlickerTestRunner
import com.android.server.wm.flicker.FlickerTestRunnerFactory
import com.android.server.wm.flicker.assertions.AssertionBlock.Companion.FLAKY
import com.android.server.wm.flicker.assertions.AssertionBlock.Companion.PRESUBMIT
import com.android.server.wm.flicker.assertions.AssertionBlock.Companion.POSTSUBMIT
import kotlin.reflect.KClass
import org.junit.Test

/**
 * Class containing basic data about a trace assertion for Flicker DSL
 */
data class AssertionData internal constructor(
    /**
     * Segment of the trace where the assertion will be applied (e.g., start, end).
     */
    @JvmField val tag: String,
    /**
     * Name of the assertion to appear on errors
     */
    @JvmField val name: String,
    /**
     * If the assertion is disabled because of a bug, which bug is it.
      */
    @JvmField val bugId: Int,
    /**
     * Expected run result type
     */
    @JvmField val expectedSubjectClass: KClass<out FlickerSubject>,
    /**
     * Moment where the assertion should run
     */
    @JvmField @AssertionBlock val block: Int,
    /**
     * Assertion command
     */
    @JvmField val assertion: FlickerSubject.() -> Unit
) {
    /**
     * Extracts the data from the result and executes the assertion
     *
     * @param run Run to be asserted
     */
    fun checkAssertion(run: FlickerRunResult) {
        val correctTag = tag == run.assertionTag
        if (correctTag) {
            val subjects = run.getSubjects()
            subjects.forEach { subject ->
                if (expectedSubjectClass.isInstance(subject)) {
                    assertion(subject)
                }
            }
        }
    }

    override fun toString(): String {
        return name
    }
}

/**
 * Moments where the assertions should be executed
 *
 * This information is used by [FlickerTestRunner] to execute tests created through the
 * [FlickerTestRunnerFactory].
 *
 * Assertions using the [PRESUBMIT] are translated into tests annotated with [Test] and [Presubmit].
 * Assertions using the [POSTSUBMIT] are translated into tests annotated with [Test].
 * Assertions using the [FLAKY] are translated into tests annotated with [Test] and [FlakyTest].
 */
@IntDef(value = [PRESUBMIT, POSTSUBMIT, FLAKY])
annotation class AssertionBlock {
    companion object {
        const val PRESUBMIT = 1
        const val POSTSUBMIT = 2
        const val FLAKY = 4
    }
}