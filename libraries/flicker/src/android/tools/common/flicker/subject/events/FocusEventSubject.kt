/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common.flicker.subject.events

import android.tools.common.flicker.assertions.Fact
import android.tools.common.flicker.subject.FlickerSubject
import android.tools.common.traces.events.FocusEvent

class FocusEventSubject(val event: FocusEvent, override val parent: EventLogSubject?) :
    FlickerSubject() {
    override val timestamp = event.timestamp
    override val selfFacts by lazy { listOf(Fact(event.toString())) }

    fun hasFocus() {
        check { "Has focus" }.that(event.hasFocus()).isEqual(true)
    }

    fun hasNotFocus() {
        check { "Has not focus" }.that(event.hasFocus()).isEqual(false)
    }
}
