package android.platform.uiautomator_helpers

import android.animation.TimeInterpolator
import android.app.Instrumentation
import android.content.Context
import android.graphics.PointF
import android.os.Bundle
import android.platform.test.util.HealthTestingUtils.waitForValueToSettle
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import java.io.IOException
import java.time.Duration

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

private const val TAG = "DeviceHelpers"

object DeviceHelpers {
    private val SHORT_WAIT = Duration.ofMillis(1500)
    private val LONG_WAIT = Duration.ofSeconds(10)
    private val DOUBLE_TAP_INTERVAL = Duration.ofMillis(100)

    private val instrumentationRegistry = InstrumentationRegistry.getInstrumentation()

    @JvmStatic
    val uiDevice: UiDevice
        get() = UiDevice.getInstance(instrumentationRegistry)

    @JvmStatic
    val context: Context
        get() = instrumentationRegistry.getTargetContext()

    /**
     * Waits for an object to be visible and returns it.
     *
     * Throws an error with message provided by [errorProvider] if the object is not found.
     */
    fun UiDevice.waitForObj(
            selector: BySelector,
            timeout: Duration = SHORT_WAIT,
            errorProvider: () -> String = { "Object $selector not found" },
    ): UiObject2 = waitForNullableObj(selector, timeout) ?: error(errorProvider())

    /**
     * Waits for an object to be visible and returns it.
     *
     * Throws an error with message provided by [errorProvider] if the object is not found.
     */
    fun UiObject2.waitForObj(
            selector: BySelector,
            timeout: Duration = LONG_WAIT,
            errorProvider: () -> String = { "Object $selector not found" },
    ): UiObject2 = wait(Until.findObject(selector), timeout.toMillis()) ?: error(errorProvider())

    /**
     * Waits for an object to be visible and returns it. Returns `null` if the object is not found.
     */
    fun UiDevice.waitForNullableObj(
            selector: BySelector,
            timeout: Duration = SHORT_WAIT,
    ): UiObject2? = wait(Until.findObject(selector), timeout.toMillis())

    /**
     * Waits for objects matched by [selector] to be visible and returns them. Returns `null` if no
     * objects are found
     */
    fun UiDevice.waitForNullableObjects(
            selector: BySelector,
            timeout: Duration = SHORT_WAIT,
    ): List<UiObject2>? = wait(Until.findObjects(selector), timeout.toMillis())

    /**
     * Asserts visibility of a [selector], waiting for [timeout] until visibility matches the
     * expected.
     *
     * If [container] is provided, the object is searched only inside of it.
     */
    @JvmOverloads
    @JvmStatic
    fun UiDevice.assertVisibility(
            selector: BySelector,
            visible: Boolean = true,
            timeout: Duration = LONG_WAIT,
            container: UiObject2? = null,
            customMessageProvider: (() -> String)? = null,
    ) {
        val expectedVisibilityCondition =
                if (visible) {
                    Until.hasObject(selector)
                } else {
                    Until.gone(selector)
                }
        val assertWithMessage =
                if (customMessageProvider != null) {
                    assertWithMessage(customMessageProvider())
                } else {
                    assertWithMessage(
                            "Visibility of %s didn't become %s within %s",
                            selector,
                            visible,
                            timeout.toMillis()
                    )
                }
        if (container != null) {
            assertWithMessage
                    .that(container.wait(expectedVisibilityCondition, timeout.toMillis()))
                    .isTrue()
        } else {
            assertWithMessage.that(wait(expectedVisibilityCondition, timeout.toMillis())).isTrue()
        }
    }

    /**
     * Asserts visibility of a [selector] inside this [UiObject2], waiting for [timeout] until
     * visibility matches the expected.
     */
    fun UiObject2.assertVisibility(
            selector: BySelector,
            visible: Boolean,
            timeout: Duration = LONG_WAIT,
            customMessageProvider: (() -> String)? = null,
    ) =
            uiDevice.assertVisibility(
                    selector,
                    visible,
                    timeout,
                    container = this,
                    customMessageProvider
            )

    /** Asserts that a this selector is visible. Throws otherwise. */
    fun BySelector.assertVisible(timeout: Duration = LONG_WAIT): Unit =
            uiDevice.assertVisibility(selector = this, visible = true, timeout = timeout)

    /** Asserts that a this selector is invisible. Throws otherwise. */
    fun BySelector.assertInvisible(timeout: Duration = LONG_WAIT): Unit =
            uiDevice.assertVisibility(selector = this, visible = false, timeout = timeout)

    /**
     * Executes a shell command on the device.
     *
     * Adds some logging. Throws [RuntimeException] In case of failures.
     */
    @JvmStatic
    fun UiDevice.shell(command: String): String {
        Log.d(TAG, "Executing Shell Command: ${command}")
        return try {
            executeShellCommand(command)
        } catch (e: IOException) {
            Log.e(TAG, "IOException Occurred.", e)
            throw RuntimeException(e)
        }
    }

    /** Perform double tap at specified x and y position */
    @JvmStatic
    fun UiDevice.doubleTapAt(x: Int, y: Int) {
        click(x, y)
        Thread.sleep(DOUBLE_TAP_INTERVAL.toMillis())
        click(x, y)
    }

    /**
     * Aims at replacing [UiDevice.swipe].
     *
     * This should be used instead of [UiDevice.swipe] as it causes less flakiness. See
     * [BetterSwipe].
     */
    @JvmStatic
    fun UiDevice.betterSwipe(
            startX: Int,
            startY: Int,
            endX: Int,
            endY: Int,
            interpolator: TimeInterpolator = FLING_GESTURE_INTERPOLATOR
    ): Unit =
            BetterSwipe.from(PointF(startX.toFloat(), startY.toFloat()))
                    .to(PointF(endX.toFloat(), endY.toFloat()), interpolator = interpolator)
                    .release()

    /** [message] will be visible to the terminal when using `am instrument`. */
    fun printInstrumentationStatus(tag: String, message: String) {
        val result =
                Bundle().apply {
                    putString(Instrumentation.REPORT_KEY_STREAMRESULT, "[$tag]: $message")
                }
        instrumentationRegistry.sendStatus(/* resultCode= */ 0, result)
    }

    /**
     * Returns whether the screen is on.
     *
     * As this uses [waitForValueToSettle], it is resilient to fast screen on/off happening.
     */
    val UiDevice.isScreenOnSettled: Boolean
        get() = waitForValueToSettle({ "Screen on didn't settle" }, { isScreenOn })
}
