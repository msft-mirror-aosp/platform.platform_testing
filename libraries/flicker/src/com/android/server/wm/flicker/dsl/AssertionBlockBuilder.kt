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

import com.android.server.wm.flicker.FlickerDslMarker
import com.android.server.wm.flicker.assertions.AssertionBlock
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject

/**
 * Placeholder for blocks of Flicker DSL assertions
 *
 * Currently supports [WindowManagerTraceSubject], [LayersTraceSubject] and list of [FocusEvent]s
 */
@FlickerDslMarker
class AssertionBlockBuilder @JvmOverloads constructor(
    private val legacyBuilder: AssertionTargetBuilder =
        AssertionTargetBuilder(
            LegacyAssertionTypeBuilder.newWMAssertions(),
            LegacyAssertionTypeBuilder.newLayerAssertions(),
            LegacyAssertionTypeBuilder.newEventLogAssertions()
        ),
    private val preSubmitBuilder: AssertionTargetBuilder =
        AssertionTargetBuilder(AssertionBlock.PRESUBMIT),
    private val postsubmitBuilder: AssertionTargetBuilder =
        AssertionTargetBuilder(AssertionBlock.POSTSUBMIT),
    private val flakyBuilder: AssertionTargetBuilder =
        AssertionTargetBuilder(AssertionBlock.FLAKY)
) {
    /**
     * Copy constructor
     */
    constructor(other: AssertionBlockBuilder) : this(
        AssertionTargetBuilder(other.legacyBuilder),
        AssertionTargetBuilder(other.preSubmitBuilder),
        AssertionTargetBuilder(other.postsubmitBuilder),
        AssertionTargetBuilder(other.flakyBuilder)
    )

    /**
     * Presubmit or flaky Assertions to run
     *
     * @param assertion Block of assertions to run
     */
    fun legacy(assertion: AssertionTargetBuilder.() -> Unit) {
        legacyBuilder.apply { assertion() }
    }

    /**
     * Assertions to run in presubmit
     *
     * @param assertion Block of assertions to run
     */
    fun presubmit(assertion: AssertionTargetBuilder.() -> Unit) {
        preSubmitBuilder.apply { assertion() }
    }

    /**
     * Assertions to run outside postsubmit
     *
     * @param assertion Block of assertions to run
     */
    fun postsubmit(assertion: AssertionTargetBuilder.() -> Unit) {
        postsubmitBuilder.apply { assertion() }
    }

    /**
     * Flaky assertions
     *
     * @param assertion Block of assertions to run
     */
    fun flaky(assertion: AssertionTargetBuilder.() -> Unit) {
        flakyBuilder.apply { assertion() }
    }

    @Deprecated("Move the assertion into one of the specific " +
        "blocks (presubmit, postsubmit, flaky) instead",
        replaceWith = ReplaceWith("presubmit { windowManagerTrace(assertion) }"))
    fun windowManagerTrace(assertion: WmAssertionBuilderLegacy.() -> Unit) {
        legacyBuilder.windowManagerTrace {
            (this as WmAssertionBuilderLegacy).apply(assertion)
        }
    }

    @Deprecated("Move the assertion into one of the specific " +
        "blocks (presubmit, postsubmit, flaky) instead",
        replaceWith = ReplaceWith("presubmit { layersTrace { assertion } }"))
    fun layersTrace(assertion: LayersAssertionBuilderLegacy.() -> Unit) {
        legacyBuilder.layersTrace {
            (this as LayersAssertionBuilderLegacy).apply(assertion)
        }
    }

    @Deprecated("Move the assertion into one of the specific " +
        "blocks (presubmit, postsubmit, flaky) instead",
        replaceWith = ReplaceWith("presubmit { eventLog { assertion } }"))
    fun eventLog(assertion: EventLogAssertionBuilderLegacy.() -> Unit) {
        legacyBuilder.eventLog {
            (this as EventLogAssertionBuilderLegacy).apply(assertion)
        }
    }

    fun build(): List<AssertionData> {
        return listOf(
            legacyBuilder.build(),
            preSubmitBuilder.build(),
            postsubmitBuilder.build(),
            flakyBuilder.build()
        ).flatten()
    }
}