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
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.google.common.truth.Truth
import kotlin.reflect.KClass
import org.junit.Test

/** Tests for [AssertionStateDataFactory] */
open class AssertionStateDataFactoryTest {
    protected open val wmAssertionFactory
        get() = AssertionStateDataFactory(WindowManagerStateSubject::class)
    protected open val layersAssertionFactory
        get() = AssertionStateDataFactory(LayerTraceEntrySubject::class)
    protected open val eventLogAssertionFactory
        get() = AssertionStateDataFactory(EventLogSubject::class)

    @Test
    open fun checkBuildsStartAssertion() {
        validate(
            wmAssertionFactory.createStartStateAssertion {},
            WindowManagerStateSubject::class,
            AssertionTag.START
        )
        validate(
            layersAssertionFactory.createStartStateAssertion {},
            LayerTraceEntrySubject::class,
            AssertionTag.START
        )
        validate(
            eventLogAssertionFactory.createStartStateAssertion {},
            EventLogSubject::class,
            AssertionTag.START
        )
    }

    @Test
    open fun checkBuildsEndAssertion() {
        validate(
            wmAssertionFactory.createEndStateAssertion {},
            WindowManagerStateSubject::class,
            AssertionTag.END
        )
        validate(
            layersAssertionFactory.createEndStateAssertion {},
            LayerTraceEntrySubject::class,
            AssertionTag.END
        )
        validate(
            eventLogAssertionFactory.createEndStateAssertion {},
            EventLogSubject::class,
            AssertionTag.END
        )
    }

    @Test
    open fun checkBuildsTagAssertion() {
        validate(
            wmAssertionFactory.createTagAssertion(TAG) {},
            WindowManagerStateSubject::class,
            TAG
        )
        validate(
            layersAssertionFactory.createTagAssertion(TAG) {},
            LayerTraceEntrySubject::class,
            TAG
        )
        validate(eventLogAssertionFactory.createTagAssertion(TAG) {}, EventLogSubject::class, TAG)
    }

    protected fun validate(
        assertionData: AssertionData,
        expectedSubject: KClass<*>,
        expectedTag: String
    ) {
        Truth.assertWithMessage("Subject")
            .that(assertionData.expectedSubjectClass)
            .isEqualTo(expectedSubject)
        Truth.assertWithMessage("Tag").that(assertionData.tag).isEqualTo(expectedTag)
    }

    companion object {
        internal const val TAG = "tag"
    }
}
