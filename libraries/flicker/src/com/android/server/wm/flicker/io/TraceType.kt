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

package com.android.server.wm.flicker.io

/** Types of traces/dumps that cna be in a flicker result */
enum class TraceType(val fileName: String) {
    SF("layers_trace$WINSCOPE_EXT"),
    WM("wm_trace$WINSCOPE_EXT"),
    TRANSACTION("transactions_trace$WINSCOPE_EXT"),
    TRANSITION("transition_trace$WINSCOPE_EXT"),
    SCREEN_RECORDING("transition.mp4"),
    SF_DUMP("sf_dump$WINSCOPE_EXT"),
    WM_DUMP("wm_dump$WINSCOPE_EXT");

    companion object {
        fun fromFileName(fileName: String): TraceType {
            return when {
                fileName == SF.fileName -> SF
                fileName == WM.fileName -> WM
                fileName == TRANSACTION.fileName -> TRANSACTION
                fileName == TRANSITION.fileName -> TRANSITION
                fileName == SCREEN_RECORDING.fileName -> SCREEN_RECORDING
                fileName.endsWith(SF_DUMP.fileName) -> SF_DUMP
                fileName.endsWith(WM_DUMP.fileName) -> WM_DUMP
                else -> error("Unknown trace type for fileName=$fileName")
            }
        }
    }
}
