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

import android.view.MotionEvent

/**
 * Represents a recorded MotionEvent. The test is not using MotionEvent because of its special
 * lifetime / recycle semantics.
 */
data class RecordedMotionEvent(
    val downTime: Long,
    val eventTime: Long,
    val action: Int,
    val pointerCount: Int,
    val pointerProperties: Array<MotionEvent.PointerProperties>,
    val pointerCoords: Array<MotionEvent.PointerCoords>,
    val metaState: Int,
    val buttonState: Int,
    val xPrecision: Float,
    val yPrecision: Float,
    val deviceId: Int,
    val edgeFlags: Int,
    val source: Int,
    val displayId: Int,
    val flags: Int,
    val actionButton: Int,
) {
    companion object {
        fun from(motionEvent: MotionEvent): RecordedMotionEvent {
            return RecordedMotionEvent(
                downTime = motionEvent.downTime,
                eventTime = motionEvent.eventTime,
                action = motionEvent.action,
                pointerCount = motionEvent.pointerCount,
                pointerProperties = copyPointerProperties(motionEvent),
                pointerCoords = copyPointerCoords(motionEvent),
                metaState = motionEvent.metaState,
                buttonState = motionEvent.buttonState,
                xPrecision = motionEvent.xPrecision,
                yPrecision = motionEvent.yPrecision,
                deviceId = motionEvent.deviceId,
                edgeFlags = motionEvent.edgeFlags,
                source = motionEvent.source,
                displayId = motionEvent.displayId,
                flags = motionEvent.flags,
                actionButton = motionEvent.actionButton,
            )
        }
    }
}

private fun copyPointerProperties(motionEvent: MotionEvent): Array<MotionEvent.PointerProperties> {
    val result =
        Array<MotionEvent.PointerProperties>(motionEvent.getPointerCount()) {
            MotionEvent.PointerProperties()
        }
    result.forEachIndexed { index, element -> motionEvent.getPointerProperties(index, element) }
    return result
}

private fun copyPointerCoords(motionEvent: MotionEvent): Array<MotionEvent.PointerCoords> {
    val result =
        Array<MotionEvent.PointerCoords>(motionEvent.getPointerCount()) {
            MotionEvent.PointerCoords()
        }

    result.forEachIndexed { index, element -> motionEvent.getPointerCoords(index, element) }
    return result
}
