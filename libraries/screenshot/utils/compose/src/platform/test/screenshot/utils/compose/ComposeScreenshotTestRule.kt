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

package platform.test.screenshot.utils.compose

import android.app.Activity
import android.app.ActivityOptions
import android.app.Dialog
import android.app.WindowConfiguration
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.text.TextUtilsCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.compose.theme.PlatformTheme
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.screenshot.BitmapDiffer
import platform.test.screenshot.DeviceEmulationRule
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.FontsRule
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.HardwareRenderingRule
import platform.test.screenshot.MaterialYouColorsRule
import platform.test.screenshot.PerfectMatcher
import platform.test.screenshot.ScreenshotActivity
import platform.test.screenshot.ScreenshotAsserterFactory
import platform.test.screenshot.ScreenshotTestRule
import platform.test.screenshot.TimeZoneRule
import platform.test.screenshot.UnitTestBitmapMatcher
import platform.test.screenshot.captureToBitmapAsync
import platform.test.screenshot.dialogScreenshotTest
import platform.test.screenshot.matchers.BitmapMatcher

/**
 * Creates a `ComposeScreenshotTestRule`
 *
 * A `v2.AndroidComposeTestRule` is instantiated, which ensures a more deterministic scheduling of
 * coroutines by using the `StandardTestDispatcher`.
 */
fun createComposeScreenshotTestRule(
    emulationSpec: DeviceEmulationSpec,
    pathManager: GoldenPathManager,
    enforcePerfectPixelMatch: Boolean = false,
    effectContext: CoroutineContext = EmptyCoroutineContext,
): ComposeScreenshotTestRule {

    return ComposeScreenshotTestRule(
        emulationSpec = emulationSpec,
        screenshotRule = ScreenshotTestRule(pathManager),
        useV2ComposeTestRule = true,
        effectContext = effectContext,
        customMatcher = if (enforcePerfectPixelMatch) PerfectMatcher else null,
    )
}

class ComposeScreenshotTestRule(
    private val emulationSpec: DeviceEmulationSpec,
    private val screenshotRule: ScreenshotTestRule,
    useV2ComposeTestRule: Boolean,
    effectContext: CoroutineContext = EmptyCoroutineContext,
    customMatcher: BitmapMatcher? = null,
) : TestRule, BitmapDiffer by screenshotRule, ScreenshotAsserterFactory by screenshotRule {
    /**
     * A rule for Compose screenshot diff tests.
     *
     * @param useV2ComposeTestRule Whether to create a `v2.AndroidComposeTestRule`, which ensures
     *   more deterministic scheduling of coroutines using the `StandardTestDispatcher`.
     */
    @Deprecated("pass in `useV2ComposeTestRule = true` to ensure a v2.ComposeTestRule is created.")
    constructor(
        emulationSpec: DeviceEmulationSpec,
        screenshotRule: ScreenshotTestRule,
        effectContext: CoroutineContext = EmptyCoroutineContext,
        customMatcher: BitmapMatcher? = null,
    ) : this(
        emulationSpec = emulationSpec,
        screenshotRule = screenshotRule,
        useV2ComposeTestRule = false,
        effectContext = effectContext,
        customMatcher = customMatcher,
    )

    @Deprecated(
        "Use createComposeScreenshotTestRule to ensure a v2.ComposeTestRule is created.",
        replaceWith = ReplaceWith("createComposeScreenshotTestRule"),
    )
    constructor(
        emulationSpec: DeviceEmulationSpec,
        pathManager: GoldenPathManager,
        enforcePerfectPixelMatch: Boolean = false,
        effectContext: CoroutineContext = EmptyCoroutineContext,
    ) : this(
        emulationSpec = emulationSpec,
        screenshotRule = ScreenshotTestRule(pathManager),
        useV2ComposeTestRule = false,
        effectContext = effectContext,
        customMatcher = if (enforcePerfectPixelMatch) PerfectMatcher else null,
    )

    private val timeZoneRule = TimeZoneRule()
    private val colorsRule = MaterialYouColorsRule()
    private val fontsRule = FontsRule()
    private val hardwareRenderingRule = HardwareRenderingRule()
    private val deviceEmulationRule = DeviceEmulationRule(emulationSpec)
    private val activityRule =
        ActivityScenarioRule(
            ScreenshotActivity::class.java,
            ActivityOptions.makeBasic()
                .apply { launchWindowingMode = WindowConfiguration.WINDOWING_MODE_FULLSCREEN }
                .toBundle(),
        )

    @OptIn(ExperimentalTestApi::class)
    val composeRule =
        if (useV2ComposeTestRule) {
            AndroidComposeTestRuleV2(
                activityRule = activityRule,
                effectContext = effectContext,
                activityProvider = {
                    var activity: ScreenshotActivity? = null
                    activityRule.scenario.onActivity { activity = it }
                    if (activity == null) {
                        throw IllegalStateException(
                            "Activity was not set in the ActivityScenarioRule!"
                        )
                    }
                    return@AndroidComposeTestRuleV2 activity
                },
            )
        } else {
            AndroidComposeTestRule(
                activityRule = activityRule,
                effectContext = effectContext,
                activityProvider = {
                    var activity: ScreenshotActivity? = null
                    activityRule.scenario.onActivity { activity = it }
                    if (activity == null) {
                        throw IllegalStateException(
                            "Activity was not set in the ActivityScenarioRule!"
                        )
                    }
                    return@AndroidComposeTestRule activity
                },
            )
        }

    private val commonRule =
        RuleChain.outerRule(deviceEmulationRule).around(screenshotRule).around(composeRule)

    // As denoted in `MaterialYouColorsRule` and `FontsRule`, these two rules need to come first,
    // though their relative orders are not critical.
    private val deviceRule = RuleChain.outerRule(colorsRule).around(timeZoneRule).around(commonRule)
    private val roboRule =
        RuleChain.outerRule(fontsRule)
            .around(colorsRule)
            .around(hardwareRenderingRule)
            .around(commonRule)
    private val matcher = customMatcher ?: UnitTestBitmapMatcher

    private val isRobolectric = Build.FINGERPRINT.contains("robolectric")

    override fun apply(base: Statement, description: Description): Statement {
        val ruleToApply = if (isRobolectric) roboRule else deviceRule
        return ruleToApply.apply(base, description)
    }

    /**
     * Compare [content] with the golden image identified by [goldenIdentifier] in the context of
     * [testSpec]. If [content] is `null`, we will take a screenshot of the current [composeRule]
     * content. [setupActivity] runs before setting [content], [beforeActivity] runs between setting
     * [content] and taking the screenshot.
     */
    fun screenshotTest(
        goldenIdentifier: String,
        clearFocus: Boolean = false,
        beforeScreenshot: () -> Unit = {},
        setupActivity: () -> Unit = {},
        viewFinder: () -> SemanticsNodeInteraction = { composeRule.onRoot() },
        content: (@Composable () -> Unit)? = null,
    ) {
        // Make sure that the activity draws full screen and fits the whole display instead of the
        // system bars.
        val activity = composeRule.activity
        activity.mainExecutor.execute { activity.window.setDecorFitsSystemWindows(false) }

        setupActivity()

        emulationSpec.locale?.let { Locale.setDefault(it) }

        // Set the content using the AndroidComposeRule to make sure that the Activity is set up
        // correctly.
        if (content != null) {
            var focusManager: FocusManager? = null

            composeRule.setContent {
                val focusManager = LocalFocusManager.current.also { focusManager = it }
                CustomLocale(locale = emulationSpec.locale) {
                    PlatformTheme {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            // Turn off text input caret blinking - caret will always be visible.
                            // Blinking would lead to flakiness if a text input field has focus.
                            CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                                content()

                                // Clear the focus early. This disposable effect will run after any
                                // DisposableEffect in content() but will run before layout/drawing,
                                // so clearing focus early here will make sure we never draw a
                                // focused effect.
                                if (clearFocus) {
                                    DisposableEffect(Unit) {
                                        focusManager.clearFocus()
                                        onDispose {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
            beforeScreenshot()

            // Make sure focus is still cleared after everything settles.
            if (clearFocus) {
                focusManager!!.clearFocus()
            }
        }
        composeRule.waitForIdle()

        val view = (viewFinder().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = view.captureToBitmapAsync().get(10, TimeUnit.SECONDS)
        screenshotRule.assertBitmapAgainstGolden(bitmap, goldenIdentifier, matcher)
    }

    fun dialogScreenshotTest(
        goldenIdentifier: String,
        shouldWaitForTheDialog: (Dialog) -> Boolean = { false },
        frameLimit: Int = 10,
        waitForIdle: () -> Unit = {},
        dialogProvider: (Activity) -> Dialog,
    ) {
        dialogScreenshotTest(
            activityRule = composeRule.activityRule,
            bitmapDiffer = screenshotRule,
            matcher = matcher,
            goldenIdentifier = goldenIdentifier,
            frameLimit = frameLimit,
            checkDialog = shouldWaitForTheDialog,
            waitForIdle = {
                composeRule.waitForIdle()
                waitForIdle()
            },
            dialogProvider = dialogProvider,
        )
    }
}

@Composable
fun CustomLocale(locale: Locale?, content: @Composable () -> Unit) {
    if (locale == null) {
        return content()
    }
    // Override the local context with the locale so that string resources get translated to the
    // right language.
    val previousConfiguration = LocalConfiguration.current
    val configuration =
        remember(previousConfiguration, locale) {
            Configuration().apply {
                updateFrom(previousConfiguration)
                setLocales(LocaleList(locale))
            }
        }
    val previousContext = LocalContext.current
    val context =
        remember(previousContext, configuration) {
            ContextThemeWrapper(previousContext, 0).apply {
                applyOverrideConfiguration(configuration)
            }
        }

    val direction =
        remember(locale) {
            if (TextUtilsCompat.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
        }

    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides configuration,
        LocalLayoutDirection provides direction,
        content = content,
    )
}
