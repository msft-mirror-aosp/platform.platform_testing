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

package com.android.server.wm.flicker.assertions

import com.android.server.wm.flicker.AssertionTag
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import org.junit.Test

/** Tests for [AssertionDataFactoryTest] */
class AssertionDataFactoryTest : AssertionStateDataFactoryTest() {
    override val wmAssertionFactory: AssertionStateDataFactory
        get() =
            AssertionDataFactory(WindowManagerStateSubject::class, WindowManagerTraceSubject::class)
    override val layersAssertionFactory: AssertionStateDataFactory
        get() = AssertionDataFactory(LayerTraceEntrySubject::class, LayersTraceSubject::class)

    @Test
    fun checkBuildsTraceAssertion() {
        validate(
            (wmAssertionFactory as AssertionDataFactory).createTraceAssertion {},
            WindowManagerTraceSubject::class,
            AssertionTag.ALL
        )
        validate(
            (layersAssertionFactory as AssertionDataFactory).createTraceAssertion {},
            LayersTraceSubject::class,
            AssertionTag.ALL
        )
    }
}
