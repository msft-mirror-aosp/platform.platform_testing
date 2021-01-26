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
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import kotlin.reflect.KClass

class LayersAssertionBuilder @JvmOverloads constructor(
    assertions: MutableList<AssertionData> = mutableListOf()
) : AssertionTypeBuilder<LayersTraceSubject, LayerTraceEntrySubject>(assertions) {
    override val entrySubjectClass: KClass<out FlickerSubject>
        get() = LayerTraceEntrySubject::class

    override val traceSubjectClass: KClass<out FlickerSubject>
        get() = LayersTraceSubject::class

    override fun copy(): AssertionTypeBuilder<LayersTraceSubject, LayerTraceEntrySubject> =
        LayersAssertionBuilder(assertions.toMutableList())
}