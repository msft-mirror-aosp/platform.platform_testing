/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
import com.android.server.wm.flicker.AssertionTag
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.assertExceptionMessage
import com.android.server.wm.flicker.io.ResultData
import com.android.server.wm.flicker.monitor.EventLogMonitor
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.google.common.truth.Truth
import java.nio.file.Files
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ArtifactAssertionRunner]
 *
 * run with `atest FlickerLibTest:ArtifactAssertionRunnerTest`
 */
@SuppressLint("VisibleForTests")
class ArtifactAssertionRunnerTest {
    private var executionCount = 0

    private val assertionSuccess = newAssertionData { executionCount++ }
    private val assertionFailure = newAssertionData {
        executionCount++
        error(Consts.FAILURE)
    }

    @Before
    fun setup() {
        executionCount = 0
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        Files.deleteIfExists(outputFileName(RunStatus.ASSERTION_FAILED))
        Files.deleteIfExists(outputFileName(RunStatus.ASSERTION_SUCCESS))
    }

    @Test
    fun executes() {
        val result = newResultReaderWithEmptySubject()
        val runner = ArtifactAssertionRunner(result)
        val firstAssertionResult = runner.runAssertion(assertionSuccess)
        val lastAssertionResult = runner.runAssertion(assertionSuccess)

        Truth.assertWithMessage("Executed").that(executionCount).isEqualTo(2)
        Truth.assertWithMessage("Run status")
            .that(result.runStatus)
            .isEqualTo(RunStatus.ASSERTION_SUCCESS)
        verifyExceptionMessage(firstAssertionResult, expectSuccess = true)
        verifyExceptionMessage(lastAssertionResult, expectSuccess = true)
    }

    @Test
    fun executesFailure() {
        val result = newResultReaderWithEmptySubject()
        val runner = ArtifactAssertionRunner(result)
        val firstAssertionResult = runner.runAssertion(assertionFailure)
        val lastAssertionResult = runner.runAssertion(assertionFailure)

        Truth.assertWithMessage("Executed").that(executionCount).isEqualTo(2)
        Truth.assertWithMessage("Run status")
            .that(result.runStatus)
            .isEqualTo(RunStatus.ASSERTION_FAILED)

        verifyExceptionMessage(firstAssertionResult, expectSuccess = false)
        verifyExceptionMessage(lastAssertionResult, expectSuccess = false)
        Truth.assertWithMessage("Same exception")
            .that(firstAssertionResult)
            .hasMessageThat()
            .isEqualTo(lastAssertionResult?.message)
    }

    @Test
    fun updatesRunStatusFailureFirst() {
        val result = newResultReaderWithEmptySubject()
        val runner = ArtifactAssertionRunner(result)
        val firstAssertionResult = runner.runAssertion(assertionFailure)
        val lastAssertionResult = runner.runAssertion(assertionSuccess)
        Truth.assertWithMessage("Executed").that(executionCount).isEqualTo(2)
        verifyExceptionMessage(firstAssertionResult, expectSuccess = false)
        verifyExceptionMessage(lastAssertionResult, expectSuccess = true)
        Truth.assertWithMessage("Run status")
            .that(result.runStatus)
            .isEqualTo(RunStatus.ASSERTION_FAILED)
    }

    @Test
    fun updatesRunStatusFailureLast() {
        val result = newResultReaderWithEmptySubject()
        val runner = ArtifactAssertionRunner(result)
        val firstAssertionResult = runner.runAssertion(assertionSuccess)
        val lastAssertionResult = runner.runAssertion(assertionFailure)
        Truth.assertWithMessage("Executed").that(executionCount).isEqualTo(2)
        verifyExceptionMessage(firstAssertionResult, expectSuccess = true)
        verifyExceptionMessage(lastAssertionResult, expectSuccess = false)
        Truth.assertWithMessage("Run status")
            .that(result.runStatus)
            .isEqualTo(RunStatus.ASSERTION_FAILED)
    }

    private fun verifyExceptionMessage(actual: Throwable?, expectSuccess: Boolean) {
        if (expectSuccess) {
            Truth.assertWithMessage("Expected exception").that(actual).isNull()
        } else {
            assertExceptionMessage(actual, Consts.FAILURE)
        }
    }

    companion object {
        private fun newAssertionData(assertion: (FlickerSubject) -> Unit) =
            AssertionData.newTestInstance(AssertionTag.ALL, EventLogSubject::class, assertion)

        private fun newResultReaderWithEmptySubject(): ResultData {
            val writer = newTestResultWriter()
            val monitor = EventLogMonitor()
            monitor.start()
            monitor.stop()
            monitor.setResult(writer)
            return writer.write()
        }
    }
}
