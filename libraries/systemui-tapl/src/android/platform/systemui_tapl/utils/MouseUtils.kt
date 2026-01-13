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
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiObject2

/** Moves the mouse pointer to the visible center of this object. */
fun UiObject2.mouseHover() {
    val coords =
        visibleCenter.let { center ->
            MotionEvent.PointerCoords().apply {
                x = center.x.toFloat()
                y = center.y.toFloat()
                pressure = 0f
                size = 1f
            }
        }

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
            /* action= */ MotionEvent.ACTION_HOVER_MOVE,
            /* pointerCount= */ 1,
            /* pointerProperties= */ arrayOf(properties),
            /* pointerCoords= */ arrayOf(coords),
            /* metaState= */ 0,
            /* buttonState= */ 0,
            /* xPrecision= */ 1.0f,
            /* yPrecision= */ 1.0f,
            /* deviceId= */ 0,
            /* edgeFlags= */ 0,
            /* source= */ InputDevice.SOURCE_MOUSE,
            /* flags= */ 0,
        )

    try {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .injectInputEvent(event, /* sync= */ true)
    } finally {
        event.recycle()
    }
}
