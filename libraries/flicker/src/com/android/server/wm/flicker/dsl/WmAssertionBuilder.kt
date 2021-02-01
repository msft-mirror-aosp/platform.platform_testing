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

package com.android.server.wm.flicker.dsl

import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import kotlin.reflect.KClass

class WmAssertionBuilder @JvmOverloads constructor(
    assertions: MutableList<AssertionData> = mutableListOf()
) : AssertionTypeBuilder<WindowManagerTraceSubject, WindowManagerStateSubject>(assertions) {
    override val entrySubjectClass: KClass<out FlickerSubject>
        get() = WindowManagerStateSubject::class

    override val traceSubjectClass: KClass<out FlickerSubject>
        get() = WindowManagerTraceSubject::class

    override fun copy(): AssertionTypeBuilder<WindowManagerTraceSubject,
        WindowManagerStateSubject> = WmAssertionBuilder(assertions.toMutableList())
}