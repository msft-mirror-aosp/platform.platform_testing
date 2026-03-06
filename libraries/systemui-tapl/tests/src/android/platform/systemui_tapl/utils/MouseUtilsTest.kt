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

import android.graphics.Point
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.uiautomator.UiObject2
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MouseUtilsTest {

    @get:Rule(order = 0) val mocks = MockitoJUnit.rule()
    @get:Rule(order = 1) val expect = Expect.create()

    @Mock private lateinit var mockUiObject: UiObject2

    @Before
    fun setUp() {
        `when`(mockUiObject.visibleCenter).thenReturn(Point(100, 200))
    }

    @Test
    fun mouseClick_injectsCorrectSequenceOfEvents() {
        val events = mutableListOf<RecordedMotionEvent>()

        mockUiObject.mouseClick(
            injectInputEvent = { event ->
                events.add(RecordedMotionEvent.from(event as MotionEvent))
            }
        )

        assertThat(events).hasSize(4)

        // ACTION_DOWN
        with(events[0]) {
            expect.that(action).isEqualTo(MotionEvent.ACTION_DOWN)
            expect.that(buttonState).isEqualTo(MotionEvent.BUTTON_PRIMARY)
            expect.that(actionButton).isEqualTo(0)
            expect.that(source).isEqualTo(InputDevice.SOURCE_MOUSE)

            expect.that(pointerCount).isEqualTo(1)
            expect.that(pointerProperties).hasLength(1)
            expect.that(pointerProperties[0].toolType).isEqualTo(MotionEvent.TOOL_TYPE_MOUSE)

            expect.that(pointerCoords).hasLength(1)
            expect.that(pointerCoords[0].x).isEqualTo(100f)
            expect.that(pointerCoords[0].y).isEqualTo(200f)
        }

        // ACTION_BUTTON_PRESS
        with(events[1]) {
            expect.that(action).isEqualTo(MotionEvent.ACTION_BUTTON_PRESS)
            expect.that(buttonState).isEqualTo(MotionEvent.BUTTON_PRIMARY)
            expect.that(actionButton).isEqualTo(MotionEvent.BUTTON_PRIMARY)
            expect.that(source).isEqualTo(InputDevice.SOURCE_MOUSE)

            expect.that(pointerCount).isEqualTo(1)
            expect.that(pointerProperties).hasLength(1)
            expect.that(pointerProperties[0].toolType).isEqualTo(MotionEvent.TOOL_TYPE_MOUSE)

            expect.that(pointerCoords).hasLength(1)
            expect.that(pointerCoords[0].x).isEqualTo(100f)
            expect.that(pointerCoords[0].y).isEqualTo(200f)
        }

        // ACTION_BUTTON_RELEASE
        with(events[2]) {
            expect.that(action).isEqualTo(MotionEvent.ACTION_BUTTON_RELEASE)
            expect.that(buttonState).isEqualTo(0)
            expect.that(actionButton).isEqualTo(MotionEvent.BUTTON_PRIMARY)
            expect.that(source).isEqualTo(InputDevice.SOURCE_MOUSE)

            expect.that(pointerCount).isEqualTo(1)
            expect.that(pointerProperties).hasLength(1)
            expect.that(pointerProperties[0].toolType).isEqualTo(MotionEvent.TOOL_TYPE_MOUSE)

            expect.that(pointerCoords).hasLength(1)
            expect.that(pointerCoords[0].x).isEqualTo(100f)
            expect.that(pointerCoords[0].y).isEqualTo(200f)
        }

        // ACTION_UP
        with(events[3]) {
            expect.that(action).isEqualTo(MotionEvent.ACTION_UP)
            expect.that(buttonState).isEqualTo(0)
            expect.that(actionButton).isEqualTo(0)
            expect.that(source).isEqualTo(InputDevice.SOURCE_MOUSE)

            expect.that(pointerCount).isEqualTo(1)
            expect.that(pointerProperties).hasLength(1)
            expect.that(pointerProperties[0].toolType).isEqualTo(MotionEvent.TOOL_TYPE_MOUSE)

            expect.that(pointerCoords).hasLength(1)
            expect.that(pointerCoords[0].x).isEqualTo(100f)
            expect.that(pointerCoords[0].y).isEqualTo(200f)
        }
    }

    @Test
    fun mouseHover_injectsHoverMoveEvent() {
        val events = mutableListOf<RecordedMotionEvent>()

        mockUiObject.mouseHover(
            injectInputEvent = { event ->
                events.add(RecordedMotionEvent.from(event as MotionEvent))
            }
        )

        assertThat(events).hasSize(1)

        with(events[0]) {
            expect.that(action).isEqualTo(MotionEvent.ACTION_HOVER_MOVE)
            expect.that(buttonState).isEqualTo(0)
            expect.that(actionButton).isEqualTo(0)
            expect.that(source).isEqualTo(InputDevice.SOURCE_MOUSE)

            expect.that(pointerCount).isEqualTo(1)
            expect.that(pointerProperties).hasLength(1)
            expect.that(pointerProperties[0].toolType).isEqualTo(MotionEvent.TOOL_TYPE_MOUSE)

            expect.that(pointerCoords).hasLength(1)
            expect.that(pointerCoords[0].x).isEqualTo(100f)
            expect.that(pointerCoords[0].y).isEqualTo(200f)
            expect.that(pointerCoords[0].pressure).isEqualTo(0f)
            expect.that(pointerCoords[0].size).isEqualTo(1f)
        }
    }
}
