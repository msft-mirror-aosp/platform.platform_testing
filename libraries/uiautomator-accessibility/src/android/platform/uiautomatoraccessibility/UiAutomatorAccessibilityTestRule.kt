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
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.common.AccessibilityNodeInfoValidator
import com.google.android.apps.common.testing.accessibility.framework.integrations.common.Suppressor
import java.util.function.Consumer
import java.util.function.Predicate
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement

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
 * real issues to be addressed in the future. Suppress failures by calling [configureSuppressions]:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class ExampleTest {
 *   @Rule val a11yRule = UiAutomatorAccessibilityTestRule().configureSuppressions {
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
) : ExternalResource() {
    /**
     * If you want to suppress certain results, either modify this one or call
     * [configureSuppressions].
     */
    val suppressor = Suppressor<AccessibilityHierarchyCheckResult>()

    /**
     * If you want to customize Parameters, suppressions, etc, either modify this one or call
     * [configureValidator].
     */
    val validator: AccessibilityNodeInfoValidator =
        AccessibilityNodeInfoValidator(InstrumentationRegistry.getInstrumentation().targetContext)
            .setSuppressingResultMatcherSupplier(suppressor::getMatcher)
            .setScreenshotCapturer {
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot()
            }
            .setRunChecksFromRootView(true)

    private var _isCheckingEnabled: Boolean = true
    private val beforeListeners: MutableList<Consumer<Description>> = mutableListOf()

    /**
     * If true, all accessibility checks will be disabled, even ones run manually via [runChecks].
     * Use this to disable checks for certain tests where accessibility checking is problematic such
     * as during benchmark tests.
     *
     * Do not use this method to disable tests that have real failing checks. Use
     * [AccessibilityNodeInfoValidator#suppressingResultMatcher] instead.
     *
     * This can be set at any time during a test and is thread-safe.
     */
    var isCheckingEnabled: Boolean
        get() = synchronized(this) { _isCheckingEnabled }
        set(value) = synchronized(this) { _isCheckingEnabled = value }

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val validatorCheck: UiAccessibilityValidator = UiAccessibilityValidator { node ->
        if (!isCheckingEnabled) return@UiAccessibilityValidator

        validator.check(node)
    }

    @SuppressWarnings(
        "method.invocation.invalid"
    ) // Initialization will finish before runActivityChecks is called.
    private val activityLifecycleCallback: ActivityLifecycleCallback =
        ActivityLifecycleCallback { activity, stage ->
            if (!isCheckingEnabled) return@ActivityLifecycleCallback

            if (stage == Stage.PAUSED) {
                runChecks()
            }
        }

    override fun apply(base: Statement, description: Description): Statement {
        suppressor.clearTestSpecificSuppressingResultMatcher()

        for (listener in beforeListeners) {
            listener.accept(description)
        }
        return super.apply(base, description)
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
            if (runA11yCheckAfterTest && isCheckingEnabled) {
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
        if (!isCheckingEnabled) return

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
        if (!isCheckingEnabled) return

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

    /**
     * Fluent method for adding suppressions in a one-liner during initialization.
     *
     * This avoids any race conditions that might arise by trying to alter the suppressor in a
     * `@Before` setup method.
     *
     * @param onConfigure called with [suppressor] for easy altering of its options
     */
    fun configureSuppressions(
        onConfigure: Consumer<Suppressor<AccessibilityHierarchyCheckResult>>
    ): UiAutomatorAccessibilityTestRule {
        onConfigure.accept(suppressor)
        return this
    }

    /**
     * Disables all checks based on some predicate on the test description, like the test class name
     * or test method name.
     *
     * This is a way to configure the rule per-test or per-test-class if needed, for instance to
     * disable checks for a performance test.
     *
     * For example:
     * ```kotlin
     * @RunWith(AndroidJUnit4::class)
     * class ExampleTest {
     *   @Rule
     *   val a11yRule = UiAutomatorAccessibilityTestRule().disableChecksIf {
     *     it.methodName?.contains("performance")
     *   }
     *   ...
     * }
     * ```
     *
     * Note: if disableChecksIf() is called multiple times (e.g. by chaining calls or by
     * subclasses), the checks will be disabled if *any* of the predicates return true.
     *
     * @param predicate The predicate to test against the test [Description]. If true, all checks
     *   will be disabled. This might be run at arbitrary times, so it must be idempotent.
     * @see isCheckingEnabled
     */
    fun disableChecksIf(predicate: Predicate<Description>): UiAutomatorAccessibilityTestRule {
        return addOnBeforeListener { description ->
            // Chaining calls gives precedent to any listeners that have already disabled it.
            if (!isCheckingEnabled) return@addOnBeforeListener
            isCheckingEnabled = !predicate.test(description)
        }
    }

    /**
     * Adds a listener to be called before each test begins.
     *
     * This is a way to configure the rule per-test or per-test class if needed, for instance to
     * disable checks for a specific test, like a performance test.
     *
     * For example:
     * ```kotlin
     * @RunWith(AndroidJUnit4::class)
     * class ExampleTest {
     *   @Rule
     *   val a11yRule = UiAutomatorAccessibilityTestRule().addOnBeforeListener { description ->
     *     // Disable screenshots on performance tests
     *     if (description.methodName?.contains("performance")) {
     *       a11yRule.validator.screenshotCapturer = null
     *     }
     *   }
     *   ...
     * }
     * ```
     *
     * @param listener The listener to be called during [before()].
     */
    private fun addOnBeforeListener(
        listener: Consumer<Description>
    ): UiAutomatorAccessibilityTestRule {
        beforeListeners.add(listener)
        return this
    }

    /** Removes a listener added with [addOnBeforeListener] */
    private fun removeOnBeforeListener(
        listener: Consumer<Description>
    ): UiAutomatorAccessibilityTestRule {
        beforeListeners.remove(listener)
        return this
    }
}
