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
package android.platform.test.scenario.tapl_common

import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiObject2Condition
import androidx.test.uiautomator.Until
import java.time.Duration
import org.junit.Assert.assertTrue

/**
 * Ui object with diagnostic metadata and flake-free gestures.
 * @param [uiObject] UI Automator object
 * @param [name] Name of the object for diags
 */
class TaplUiObject internal constructor(private val uiObject: UiObject2, private val name: String) {
    private fun waitForObjectCondition(
        condition: UiObject2Condition<Boolean>,
        conditionName: String
    ) {
        assertTrue(
            "UI object '$name' is not $conditionName.",
            uiObject.wait(condition, WAIT_TIME.toMillis())
        )
    }

    private fun waitForObjectEnabled() {
        waitForObjectCondition(Until.enabled(true), "enabled")
    }

    private fun waitForObjectClickable() {
        waitForObjectCondition(Until.clickable(true), "clickable")
    }

    /** Wait for the object to become clickable and enabled, then clicks the object. */
    fun click() {
        try {
            waitForObjectEnabled()
            waitForObjectClickable()
            uiObject.click()
        } catch (e: StaleObjectException) {
            throw AssertionError(
                "UI object '$name' has disappeared from the screen during the click gesture.",
                e
            )
        }
    }

    companion object {
        private val WAIT_TIME = Duration.ofSeconds(10)
    }
}
