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

package com.android.server.wm.flicker

import com.android.internal.annotations.VisibleForTesting
import com.android.server.wm.flicker.FlickerRunResult.Companion.RunStatus
import com.android.server.wm.flicker.TransitionRunner.Companion.ExecutionError
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.google.common.truth.Truth

/**
 * Result of a flicker run, including transitions, errors and create tags
 */
data class FlickerResult(
    /**
     * Result of a transition run
     */
    private val runResult: FlickerRunResult,
) {
    val status: RunStatus get() = runResult.status
    val transitionExecutionError: ExecutionError? get() = runResult.transitionExecutionError
    val faasExecutionError: ExecutionError? get() = runResult.faasExecutionError
    val ranSuccessfully: Boolean get() = runResult.isSuccessfulRun

    /**
     * List of failures during assertion
     */
    private val failures: MutableList<FlickerAssertionError> = mutableListOf()

    /**
     * Run the assertion on the trace
     *
     * @throws AssertionError If the assertion fail or the transition crashed
     */
    @VisibleForTesting
    fun checkAssertion(assertion: AssertionData): FlickerAssertionError? {
        Truth.assertWithMessage("Transition was not executed successful. Can't check assertions")
                .that(runResult.isSuccessfulRun).isTrue()

        val assertionFailure = runResult.checkAssertion(assertion)?.also {
            failures.add(it)
        }
        return assertionFailure
    }

    fun clearFromMemory() {
        runResult.clearFromMemory()
    }
}
