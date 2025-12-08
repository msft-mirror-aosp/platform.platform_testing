/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiAccessibilityValidator
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.integrations.common.AccessibilityNodeInfoValidator
import java.util.function.Consumer
import org.junit.rules.ExternalResource

/**
 * Enables accessibility checks for UiAutomator tests using Accessibility Test Framework. The checks
 * will run automatically on every test action like click(), swipe() etc. You can also run them
 * manually at a time of your choosing with [runChecks()].
 *
 * Example usage:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class ExampleTest {
 *   @Rule val a11yRule = UiAutomatorAccessibilityTestRule()
 * }
 * ```
 *
 * Sometimes, you might need to suppress certain errors because they are false positives or they are
 * real issues to be addressed in the future. Suppress failures by modifying the rule's default
 * validator, or by passing in a custom validator:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class ExampleTest {
 *   @Rule val a11yRule = UiAutomatorAccessibilityTestRule().configureValidator {
 *      // TODO: fix touch target sizes, then remove this suppression
 *      it.suppressingResultMatcher =
 *          AccessibilityCheckResultUtils.matchesCheck(TouchTargetSizeCheck.class)
 *   }
 * }
 * ```
 *
 * @see AccessibilityNodeInfoValidator
 */
class UiAutomatorAccessibilityTestRule
@JvmOverloads
constructor(
    /**
     * If true, accessibility checks may be evaluated from the top active window when an Activity
     * transitions to PAUSED. In a test using ActivityScenario or something similar, this may
     * evaluate the UI at the end of the test before tear down occurs.
     */
    val checkOnPaused: Boolean = true,

    /**
     * If true, accessibility checks will be evaluated from the top active window after the test
     * method completes.
     */
    val runA11yCheckAfterTest: Boolean = true,

    /**
     * If you want to customize Parameters, suppressions, etc, either modify this one, or pass in
     * your own validator with the various options set.
     */
    val validator: AccessibilityNodeInfoValidator =
        AccessibilityNodeInfoValidator(InstrumentationRegistry.getInstrumentation().targetContext),
) : ExternalResource() {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val validatorCheck: UiAccessibilityValidator = UiAccessibilityValidator { node ->
        validator.check(node)
    }

    @SuppressWarnings(
        "method.invocation.invalid"
    ) // Initialization will finish before runActivityChecks is called.
    private val activityLifecycleCallback: ActivityLifecycleCallback =
        ActivityLifecycleCallback { activity, stage ->
            if (stage == Stage.PAUSED) {
                runChecks()
            }
        }

    override fun before() {
        super.before()

        Configurator.getInstance().addUiAccessibilityValidator(validatorCheck)

        if (checkOnPaused) {
            ActivityLifecycleMonitorRegistry.getInstance()
                .addLifecycleCallback(activityLifecycleCallback)
        }
    }

    override fun after() {
        try {
            if (runA11yCheckAfterTest) {
                runChecks()
            }
        } finally {
            Configurator.getInstance().removeUiAccessibilityValidator(validatorCheck)

            if (checkOnPaused) {
                ActivityLifecycleMonitorRegistry.getInstance()
                    .removeLifecycleCallback(activityLifecycleCallback)
            }
            super.after()
        }
    }

    /**
     * Runs the a11y checks on the current window's a11y tree, for explicit verification at specific
     * points of a test.
     *
     * @throws IllegalStateException if there is no root window.
     */
    fun runChecks() {
        return runChecks(
            uiDevice.windowRoots.firstOrNull()
                ?: throw IllegalStateException("No root window found")
        )
    }

    /**
     * Runs the a11y checks on the given a11y tree, for explicit verification at specific points of
     * a test.
     *
     * @param node The point at which to start checks.
     */
    fun runChecks(node: AccessibilityNodeInfo) {
        validator.check(node)
    }

    /**
     * Fluent method for accessing the [validator] in a one-liner during initialization.
     *
     * This avoids any race conditions that might arise by trying to alter the validator in a
     * `@Before` setup method.
     *
     * @param onConfigure called with [validator] for easy altering of its options
     */
    fun configureValidator(
        onConfigure: Consumer<AccessibilityNodeInfoValidator>
    ): UiAutomatorAccessibilityTestRule {
        onConfigure.accept(validator)
        return this
    }
}
