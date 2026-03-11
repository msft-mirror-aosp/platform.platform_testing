/*
 * Copyright (C) 2026 The Android Open Source Project
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
package android.platform.uiautomatoraccessibility

import android.platform.test.microbenchmark.Microbenchmark
import android.platform.uiautomatoraccessibility.reporting.ResultsWriter
import com.google.android.apps.common.testing.accessibility.framework.integrations.uiautomator.UiAutomatorAccessibilityTestRuleBase
import org.junit.runner.Description
import org.junit.runner.RunWith

/**
 * Enables Android platform-specific accessibility checks for UiAutomator tests using Accessibility
 * Test Framework. The checks will run automatically on every test action like click(), swipe() etc.
 * You can also run them manually at a time of your choosing with [runChecks()].
 *
 * Example usage:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class ExampleTest {
 *   @Rule val a11yRule = PlatformUiAutomatorAccessibilityTestRule()
 * }
 * ```
 *
 * Sometimes, you might need to suppress certain errors because they are false positives or they are
 * real issues to be addressed in the future. Suppress failures by calling [configureSuppressions]:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class ExampleTest {
 *   @Rule val a11yRule = PlatformUiAutomatorAccessibilityTestRule().configureSuppressions {
 *      // TODO: fix touch target sizes, then remove this suppression
 *      it.addSuppressingResultMatcher(
 *          allOf(
 *              AccessibilityCheckResultUtils.matchesCheck(TouchTargetSizeCheck:class.java),
 *              AccessibilityCheckResultUtils.matchesElements(
 *                  ElementMatchers.withResourceName(endsWith("some_view_id"))
 *              )
 *          );
 *   }
 * }
 * ```
 *
 * @property checkOnPaused If true, accessibility checks may be evaluated from the top active window
 *   when an Activity transitions to PAUSED. In a test using ActivityScenario or something similar,
 *   this may evaluate the UI at the end of the test before tear down occurs.
 * @property runA11yCheckAfterTest If true, accessibility checks will be evaluated from the top
 *   active window after the test method completes.
 * @property deferCheckExceptions If true, accessibility check exceptions will be collected and
 *   thrown after the test completes. Otherwise, exceptions will be thrown immediately when they
 *   occur.
 * @see UiAutomatorAccessibilityTestRuleBase
 */
class PlatformUiAutomatorAccessibilityTestRule
@JvmOverloads
constructor(
    checkOnPaused: Boolean = true,
    runA11yCheckAfterTest: Boolean = true,
    deferCheckExceptions: Boolean = false,
) :
    UiAutomatorAccessibilityTestRuleBase<PlatformUiAutomatorAccessibilityTestRule>(
        checkOnPaused,
        runA11yCheckAfterTest,
        deferCheckExceptions,
    ) {

    private val resultsWriter =
        ResultsWriter().also {
            addOnBeforeListener(it::setTestDescription)
            validator.addCheckResultsListener(it::saveResults)
        }

    init {
        validator.setRunChecksFromRootView(true)

        GlobalSuppressions.addAll(suppressor)
        disableChecksIf { it.isPerformanceTest() }
    }

    override fun getThis() = this
}

/** Returns true if the test class is a microbenchmark performance test, false otherwise. */
private fun Class<*>.isPerformanceTest(): Boolean {
    val runWith = getAnnotation(RunWith::class.java)
    return runWith?.value != null && Microbenchmark::class.java.isAssignableFrom(runWith.value.java)
}

/** Returns true if the test is a microbenchmark performance test, false otherwise. */
private fun Description.isPerformanceTest() = testClass.isPerformanceTest()
