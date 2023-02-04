/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.traces.eventlog

import com.android.server.wm.flicker.assertions.Fact
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.events.FocusEvent

class FocusEventSubject(val event: FocusEvent, override val parent: EventLogSubject?) :
    FlickerSubject() {
    override val timestamp: Timestamp
        get() = event.timestamp
    override val selfFacts by lazy { listOf(Fact(event.toString())) }

    fun hasFocus() {
        check { "Has focus" }.that(event.hasFocus()).isEqual(true)
    }

    fun hasNotFocus() {
        check { "Has not focus" }.that(event.hasFocus()).isEqual(false)
    }
}
