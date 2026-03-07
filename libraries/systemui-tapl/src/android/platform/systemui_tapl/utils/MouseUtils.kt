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

package android.platform.systemui_tapl.utils

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiObject2

/** Moves the mouse pointer to the visible center of this object. */
fun UiObject2.mouseHover() {
    mouseHover(injectInputEvent = ::injectInputEventUiAutomation)
}

/**
 * Moves the mouse pointer to the visible center of this object.
 *
 * @param injectInputEvent a function that is used to inject input events (useful for unit testing
 *   this method)
 */
internal fun UiObject2.mouseHover(injectInputEvent: (InputEvent) -> Unit) {
    val coords =
        visibleCenter.let { center ->
            MotionEvent.PointerCoords().apply {
                x = center.x.toFloat()
                y = center.y.toFloat()
                pressure = 0f
                size = 1f
            }
        }

    injectMouseInputEvent(
        injectInputEvent = injectInputEvent,
        action = MotionEvent.ACTION_HOVER_MOVE,
        pointerCoords = coords,
        actionButton = 0,
        buttonState = 0,
    )
}

/** Clicks the mouse pointer on the visible center of this object. */
fun UiObject2.mouseClick() {
    mouseClick(injectInputEvent = ::injectInputEventUiAutomation)
}

/**
 * Clicks the mouse pointer on the visible center of this object.
 *
 * @param injectInputEvent a function that is used to inject input events (useful for unit testing
 *   this method)
 */
internal fun UiObject2.mouseClick(injectInputEvent: (InputEvent) -> Unit) {
    val coords =
        visibleCenter.let { center ->
            MotionEvent.PointerCoords().apply {
                x = center.x.toFloat()
                y = center.y.toFloat()
            }
        }

    // This sequence has been observed in InputFlinger traces when clicking a mouse button.

    // ACTION_DOWN and ACTION_UP events may not have an actionButton set - if they do, they're
    // rejected. They should still carry the buttonState.
    injectMouseInputEvent(
        injectInputEvent = injectInputEvent,
        action = MotionEvent.ACTION_DOWN,
        pointerCoords = coords,
        actionButton = 0,
        buttonState = MotionEvent.BUTTON_PRIMARY,
    )

    injectMouseInputEvent(
        injectInputEvent = injectInputEvent,
        action = MotionEvent.ACTION_BUTTON_PRESS,
        pointerCoords = coords,
        actionButton = MotionEvent.BUTTON_PRIMARY,
        buttonState = MotionEvent.BUTTON_PRIMARY,
    )

    injectMouseInputEvent(
        injectInputEvent = injectInputEvent,
        action = MotionEvent.ACTION_BUTTON_RELEASE,
        pointerCoords = coords,
        actionButton = MotionEvent.BUTTON_PRIMARY,
        buttonState = 0,
    )

    injectMouseInputEvent(
        injectInputEvent = injectInputEvent,
        action = MotionEvent.ACTION_UP,
        pointerCoords = coords,
        actionButton = 0,
        buttonState = 0,
    )
}

/** Injects [event] using `UiAutomation.injectInputEvent`. */
fun injectInputEventUiAutomation(event: InputEvent) {
    InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true)
}

/**
 * Performs [inject] on [event], then recycles [event] (no matter if [inject] threw an exception or
 * not).
 */
private fun injectMouseInputEvent(
    injectInputEvent: (InputEvent) -> Unit,
    action: Int,
    pointerCoords: MotionEvent.PointerCoords,
    actionButton: Int,
    buttonState: Int,
) {
    val properties =
        MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }

    val time = SystemClock.uptimeMillis()
    val event =
        MotionEvent.obtain(
            /* downTime= */ time,
            /* eventTime= */ time,
            /* action= */ action,
            /* pointerCount= */ 1,
            /* pointerProperties= */ arrayOf(properties),
            /* pointerCoords= */ arrayOf(pointerCoords),
            /* metaState= */ 0,
            /* buttonState= */ buttonState,
            /* xPrecision= */ 1.0f,
            /* yPrecision= */ 1.0f,
            /* deviceId= */ 0,
            /* edgeFlags= */ 0,
            /* source= */ InputDevice.SOURCE_MOUSE,
            /* flags= */ 0,
        )

    try {
        event.setActionButton(actionButton)

        injectInputEvent(event)
    } finally {
        event.recycle()
    }
}
