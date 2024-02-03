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

package android.tools.device.flicker.assertions

import android.tools.common.flicker.assertions.AssertionData
import android.tools.common.flicker.assertions.AssertionRunner
import android.tools.common.flicker.assertions.ReaderAssertionRunner
import android.tools.common.io.Reader
import android.tools.common.io.RunStatus

/**
 * Helper class to run an assertions
 *
 * @param resultReader helper class to read the flicker artifact
 * @param innerRunner helper class to run the assertion
 */
abstract class BaseAssertionRunner(
    private val resultReader: Reader,
    private val innerRunner: AssertionRunner = ReaderAssertionRunner(resultReader)
) : AssertionRunner by innerRunner {
    protected abstract fun doUpdateStatus(newStatus: RunStatus)

    override fun runAssertion(assertion: AssertionData): Throwable? =
        innerRunner.runAssertion(assertion).also { updateResultStatus(it) }

    private fun updateResultStatus(error: Throwable?) {
        val newStatus =
            if (error == null) RunStatus.ASSERTION_SUCCESS else RunStatus.ASSERTION_FAILED

        if (resultReader.isFailure || resultReader.runStatus == newStatus) return

        doUpdateStatus(newStatus)
    }
}
