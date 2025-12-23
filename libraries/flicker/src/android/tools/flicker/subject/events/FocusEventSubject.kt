/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.tools.flicker.subject.events

import android.tools.flicker.assertions.Fact
import android.tools.flicker.subject.exceptions.ExceptionMessageBuilder
import android.tools.flicker.subject.exceptions.IncorrectFocusException
import android.tools.traces.events.FocusEvent

interface FocusEventSubject {
    val focusChanges: List<FocusEvent>
    val exceptionMessageBuilder: ExceptionMessageBuilder

    fun focusChanges(vararg windows: String) = apply {
        val builder =
            exceptionMessageBuilder
                .setExpected(windows.joinToString(" -> "))
                .setActual(focusChanges.map { Fact("Focus change", it) })

        if (windows.isEmpty()) {
            val errorMsgBuilder =
                builder.setMessage("No windows specified for focus change assertion")
            throw IncorrectFocusException(errorMsgBuilder)
        }

        if (focusChanges.isEmpty()) {
            val errorMsgBuilder = builder.setMessage("Focus did not change")
            throw IncorrectFocusException(errorMsgBuilder)
        }

        if (windows.size > focusChanges.size) {
            val errorMsgBuilder = builder.setMessage("More windows specified than focus changes")
            throw IncorrectFocusException(errorMsgBuilder)
        }

        for ((index, focusChange) in focusChanges.withIndex()) {
            if (index + windows.size > focusChanges.size) {
                break
            }

            if (!focusChange.window.contains(windows.first())) {
                continue
            }

            val subsequence = focusChanges.subList(index, index + windows.size)
            if (subsequence.zip(windows).all { (focus, window) -> focus.window.contains(window) }) {
                return@apply
            }
        }

        val errorMsgBuilder = builder.setMessage("Incorrect focus change")
        throw IncorrectFocusException(errorMsgBuilder)
    }

    fun focusDoesNotChange() = apply {
        if (focusChanges.isNotEmpty()) {
            val actual = focusChanges.map { Fact("Focus change", it) }
            val errorMsgBuilder =
                exceptionMessageBuilder
                    .setMessage("Focus changes")
                    .setExpected("")
                    .setActual(actual)
            throw IncorrectFocusException(errorMsgBuilder)
        }
    }
}
