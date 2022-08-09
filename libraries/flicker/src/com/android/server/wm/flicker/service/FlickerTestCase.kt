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

package com.android.server.wm.flicker.service

import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.flicker.service.config.common.AssertionInvocationGroup
import junit.framework.Assert

class FlickerTestCase(val results: List<AssertionResult>, isBlockingTest: Boolean) {

    private val resultsToReport = if (isBlockingTest) {
        results.filter { it.invocationGroup == AssertionInvocationGroup.BLOCKING }
    } else {
        results
    }

    val shouldSkip = resultsToReport.isEmpty()

    // Used by the FlickerBlockJUnit4ClassRunner to identify the test method within this class
    annotation class InjectedTest

    @InjectedTest
    fun injectedTest(param: Any) {
        if (containsFailuresToReport) {
            Assert.fail(assertionMessage)
        }
    }

    private val containsFailuresToReport: Boolean get() = resultsToReport.any { it.failed }

    private val assertionMessage: String get() {
        if (!containsFailuresToReport) {
            return "${resultsToReport.size}/${resultsToReport.size} PASSED"
        }

        if (resultsToReport.size == 1) {
            return resultsToReport[0].assertionError!!.message
        }

        return buildString {
            append("$failedCount/${resultsToReport.size} FAILED\n")
            for (result in resultsToReport) {
                if (result.failed) {
                    append("\n${result.assertionError!!.message.prependIndent("  ")}")
                }
            }
        }
    }

    private val failedCount: Int get() = results.filter { it.failed }.size
}
