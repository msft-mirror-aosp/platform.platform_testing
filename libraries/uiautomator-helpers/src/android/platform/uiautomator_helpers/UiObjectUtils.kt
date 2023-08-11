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

package android.platform.uiautomator_helpers

import android.graphics.PointF
import android.graphics.Rect
import android.platform.uiautomator_helpers.DeviceHelpers.uiDevice
import android.platform.uiautomator_helpers.WaitUtils.waitForValueToSettle
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import com.google.common.truth.Truth.assertWithMessage

/** Checks the view is in the vertical (Y) centre of the screen (+- 1 px). */
fun UiObject2.assertInVerticalCentre() {
    assertWithMessage("${this.resourceName} should be vertically (Y) centred (+- 1px)")
        .that(this.stableBounds.centerY())
        .isIn(uiDevice.displayHeight / 2 - 1..uiDevice.displayHeight / 2 + 1)
}

/** Checks the view is in the horizontal (X) centre of the screen (+- 1 px). */
fun UiObject2.assertInHorizontalCentre() {
    assertWithMessage("${this.resourceName} should be horizontally (X) centred (+- 1px)")
        .that(this.stableBounds.centerX())
        .isIn(uiDevice.displayWidth / 2 - 1..uiDevice.displayWidth / 2 + 1)
}

/**
 * Checks if top of the view is on the bottom side by checking top bound is in the middle of the
 * screen or in the bottom half of the screen vertically.
 */
fun UiObject2.assertTopOnBottomSide() {
    assertWithMessage("${this.resourceName} should be on the bottom side")
        .that(this.stableBounds.top > uiDevice.displayHeight / 2)
        .isTrue()
}

/**
 * Checks if top of the view is on the top side by checking top bound is in the middle of the screen
 * or in the top half of the screen vertically.
 */
fun UiObject2.assertTopOnTopSide() {
    assertWithMessage("${this.resourceName} should be on the top side")
        .that(this.stableBounds.top < uiDevice.displayHeight / 2)
        .isTrue()
}

/** Checks if view horizontal (X) centre is on the right side */
fun UiObject2.assertCenterOnTheRightSide() {
    assertWithMessage("${this.resourceName} should be on the right side")
        .that(this.stableBounds.centerX() > uiDevice.displayWidth / 2)
        .isTrue()
}

/** Checks if view horizontal (X) centre is on the left side */
fun UiObject2.assertCenterOnTheLeftSide() {
    assertWithMessage("${this.resourceName} should be on the left side")
        .that(this.stableBounds.centerX() < uiDevice.displayWidth / 2)
        .isTrue()
}

/**
 * Checks if view is on the right side by checking left bound is in the middle of the screen or in
 * the right half of the screen horizontally.
 */
fun UiObject2.assertOnTheRightSide() {
    assertWithMessage("${this.resourceName} should be on the right side")
        .that(this.stableBounds.left >= uiDevice.displayWidth / 2)
        .isTrue()
}

/**
 * Checks if view is on the left side by checking right bound is in the middle of the screen or in
 * the left half of the screen horizontally.
 */
fun UiObject2.assertOnTheLeftSide() {
    assertWithMessage("${this.resourceName} should be on the left side")
        .that(this.stableBounds.right <= uiDevice.displayWidth / 2)
        .isTrue()
}

private val UiObject2.stableBounds: Rect
    get() = waitForValueToSettle("${this.resourceName} bounds") { visibleBounds }

private const val MAX_FIND_ELEMENT_ATTEMPT = 15

/**
 * Scrolls [this] in [direction] ([Direction.DOWN] by default) until finding [selector]. It returns
 * the first object that matches [selector] or `null` if it's not found after
 * [MAX_FIND_ELEMENT_ATTEMPT] scrolls.
 *
 * Uses [BetterSwipe] to perform the scroll.
 */
@JvmOverloads
fun UiObject2.scrollUntilFound(
    selector: BySelector,
    direction: Direction = Direction.DOWN
): UiObject2? {
    val (from, to) = getPointsToScroll(direction)
    (0 until MAX_FIND_ELEMENT_ATTEMPT).forEach { _ ->
        val f = findObject(selector)
        if (f != null) return f
        BetterSwipe.from(from).to(to, interpolator = FLING_GESTURE_INTERPOLATOR).release()
    }
    return null
}

private data class Vector2F(val from: PointF, val to: PointF)

private fun UiObject2.getPointsToScroll(direction: Direction): Vector2F {
    return when (direction) {
        Direction.DOWN -> {
            Vector2F(
                PointF(visibleBounds.exactCenterX(), visibleBounds.bottom.toFloat() - 1f),
                PointF(visibleBounds.exactCenterX(), visibleBounds.top.toFloat() + 1f)
            )
        }
        Direction.UP -> {
            Vector2F(
                PointF(visibleBounds.exactCenterX(), visibleBounds.top.toFloat() + 1f),
                PointF(visibleBounds.exactCenterX(), visibleBounds.bottom.toFloat() - 1f)
            )
        }
        Direction.LEFT -> {
            Vector2F(
                PointF(visibleBounds.left.toFloat() + 1f, visibleBounds.exactCenterY()),
                PointF(visibleBounds.right.toFloat() - 1f, visibleBounds.exactCenterY())
            )
        }
        Direction.RIGHT -> {
            Vector2F(
                PointF(visibleBounds.right.toFloat() - 1f, visibleBounds.exactCenterY()),
                PointF(visibleBounds.left.toFloat() + 1f, visibleBounds.exactCenterY())
            )
        }
    }
}
