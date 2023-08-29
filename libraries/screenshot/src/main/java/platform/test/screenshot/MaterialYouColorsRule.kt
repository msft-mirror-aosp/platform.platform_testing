/*
 * Copyright 2022 The Android Open Source Project
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

package platform.test.screenshot

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A rule to change the system colors using the given [seedColorHex].
 *
 * This is especially useful to change the colors before starting an activity using an
 * [ActivityScenarioRule] or any other rule.
 */
class MaterialYouColorsRule(
    private val colorsConfiguration: String = DEFAULT_SCREENSHOT_CONFIGURATION
) : TestRule {
    constructor(
        seedColorHex: String,
        accentColorHex: String
    ) : this(
        """
        {
            "android.theme.customization.system_palette": "$seedColorHex",
            "android.theme.customization.accent_color": "$accentColorHex"
        }
    """
            .trimIndent()
    )

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val previousSettingValue =
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES
                    )

                if (previousSettingValue == colorsConfiguration) {
                    // The system colors should already be what we expect, so no need to set them.
                    base.evaluate()
                    return
                }

                try {
                    changeColorSetting(context, colorsConfiguration, previousSettingValue)
                    base.evaluate()
                } finally {
                    // Restore the previous colors.
                    changeColorSetting(context, previousSettingValue, colorsConfiguration)
                }
            }
        }
    }

    private fun changeColorSetting(
        context: Context,
        settingValue: String?,
        previousSettingValue: String?
    ) {
        fun primaryDark() = context.getColor(android.R.color.system_primary_dark)

        // Save the current value of the system_primary_dark color.
        val primaryDarkBefore = primaryDark()

        // Change the Material You colors configuration.
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            settingValue
        )

        // Wait for the setting propagation by waiting for the system_primary_dark color to change.
        waitFor(
            timeoutMs = 5_000,
            errorMessage =
                """
                system_primary_dark color did not change within 5s
                primaryDarkBefore=${primaryDark()}
                settingValue=$settingValue
                previousSettingValue=$previousSettingValue
            """
                    .trimIndent()
        ) {
            primaryDarkBefore != primaryDark()
        }
    }

    private fun waitFor(
        timeoutMs: Long,
        errorMessage: String,
        stepMs: Long = 50,
        condition: () -> Boolean,
    ) {
        val start = SystemClock.uptimeMillis()
        while (!condition()) {
            if (SystemClock.uptimeMillis() - start > timeoutMs) {
                Assert.fail(errorMessage)
            }
            Thread.sleep(stepMs)
        }
    }

    companion object {
        /**
         * A nice green/blue configuration that is one of the suggest presets in the wallpaper
         * picker.
         */
        val DEFAULT_SCREENSHOT_CONFIGURATION =
            """
                {
                    "android.theme.customization.system_palette": "B1EBFF",
                    "android.theme.customization.accent_color": "B1EBFF",
                    "android.theme.customization.theme_style":"FRUIT_SALAD"
                }
            """
                .trimIndent()
    }
}
