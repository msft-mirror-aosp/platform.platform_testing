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
 * A collection of gestures for UI objects that implements flake-proof patterns and adds
 * diagnostics. Don't use these gestures directly from the test, this class should be used only by
 * TAPL.
 */
object Gestures {
    private val WAIT_TIME = Duration.ofSeconds(10)

    private fun waitForObjectCondition(
        uiObject: UiObject2,
        objectName: String,
        condition: UiObject2Condition<Boolean>,
        conditionName: String
    ) {
        assertTrue(
            "UI object '$objectName' is not $conditionName.",
            uiObject.wait(condition, WAIT_TIME.toMillis())
        )
    }

    private fun waitForObjectEnabled(uiObject: UiObject2, objectName: String) {
        waitForObjectCondition(uiObject, objectName, Until.enabled(true), "enabled")
    }

    private fun waitForObjectClickable(uiObject: UiObject2, waitReason: String) {
        waitForObjectCondition(uiObject, waitReason, Until.clickable(true), "clickable")
    }

    /**
     * Wait for the object to become clickable and enabled, then clicks the object.
     *
     * @param [uiObject] The object to click
     * @param [objectName] Name of the object for diags
     */
    @JvmStatic
    fun click(uiObject: UiObject2, objectName: String) {
        try {
            waitForObjectEnabled(uiObject, objectName)
            waitForObjectClickable(uiObject, objectName)
            uiObject.click()
        } catch (e: StaleObjectException) {
            throw AssertionError(
                "UI object '$objectName' has disappeared from the screen during the click gesture.",
                e
            )
        }
    }
}
