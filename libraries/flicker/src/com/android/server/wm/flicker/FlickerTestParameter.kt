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

package com.android.server.wm.flicker

import android.view.Surface
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.dsl.AssertionTag
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject

/**
 * Specification of a flicker test for JUnit ParameterizedRunner class
 */
data class FlickerTestParameter(
    @JvmField val config: MutableMap<String, Any?>,
    @JvmField val name: String = defaultName(config)
) {
    internal var internalFlicker: Flicker? = null
    internal val flicker: Flicker get() = internalFlicker ?: error("Flicker not initialized")
    val isRotated: Boolean
        get() = config.startRotation == Surface.ROTATION_90 ||
            config.startRotation == Surface.ROTATION_270

    fun clear() {
        internalFlicker?.clear()
    }

    fun assertWmStart(assertion: WindowManagerStateSubject.() -> Any) {
        val assertionData = buildWmStartAssertion(assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertWmEnd(assertion: WindowManagerStateSubject.() -> Any) {
        val assertionData = buildWmEndAssertion(assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertWm(assertion: WindowManagerTraceSubject.() -> Any) {
        val assertionData = buildWMAssertion(assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertWmTag(tag: String, assertion: WindowManagerStateSubject.() -> Any) {
        val assertionData = buildWMTagAssertion(tag, assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertLayersStart(assertion: LayerTraceEntrySubject.() -> Any) {
        val assertionData = buildLayersStartAssertion(assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertLayersEnd(assertion: LayerTraceEntrySubject.() -> Any) {
        val assertionData = buildLayersEndAssertion(assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertLayers(assertion: LayersTraceSubject.() -> Any) {
        val assertionData = buildLayersAssertion(assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertLayersTag(tag: String, assertion: LayerTraceEntrySubject.() -> Any) {
        val assertionData = buildLayersTagAssertion(tag, assertion)
        this.flicker.checkAssertion(assertionData)
    }

    fun assertEventLog(assertion: EventLogSubject.() -> Any) {
        val assertionData = buildEventLogAssertion(assertion)
        this.flicker.checkAssertion(assertionData)
    }

    override fun toString(): String = name

    companion object {
        fun defaultName(config: Map<String, Any?>) = buildString {
            append(config.startRotationName)
            if (config.endRotation != config.startRotation) {
                append("_${config.endRotationName}")
            }
        }

        @JvmStatic
        fun buildWmStartAssertion(assertion: WindowManagerStateSubject.() -> Any): AssertionData =
            AssertionData(tag = AssertionTag.START,
                expectedSubjectClass = WindowManagerStateSubject::class,
                assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildWmEndAssertion(assertion: WindowManagerStateSubject.() -> Any): AssertionData =
            AssertionData(tag = AssertionTag.END,
                expectedSubjectClass = WindowManagerStateSubject::class,
                assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildWMAssertion(assertion: WindowManagerTraceSubject.() -> Any): AssertionData =
            AssertionData(tag = AssertionTag.ALL,
                expectedSubjectClass = WindowManagerTraceSubject::class,
                assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildWMTagAssertion(
            tag: String,
            assertion: WindowManagerStateSubject.() -> Any
        ): AssertionData = AssertionData(tag = tag,
            expectedSubjectClass = WindowManagerStateSubject::class,
            assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildLayersStartAssertion(assertion: LayerTraceEntrySubject.() -> Any): AssertionData =
            AssertionData(tag = AssertionTag.START,
                expectedSubjectClass = LayerTraceEntrySubject::class,
                assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildLayersEndAssertion(assertion: LayerTraceEntrySubject.() -> Any): AssertionData =
            AssertionData(tag = AssertionTag.END,
                expectedSubjectClass = LayerTraceEntrySubject::class,
                assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildLayersAssertion(assertion: LayersTraceSubject.() -> Any): AssertionData =
            AssertionData(tag = AssertionTag.ALL,
                expectedSubjectClass = LayersTraceSubject::class,
                assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildLayersTagAssertion(
            tag: String,
            assertion: LayerTraceEntrySubject.() -> Any
        ): AssertionData = AssertionData(tag = tag,
            expectedSubjectClass = LayerTraceEntrySubject::class,
            assertion = assertion as FlickerSubject.() -> Unit)

        @JvmStatic
        fun buildEventLogAssertion(assertion: EventLogSubject.() -> Any): AssertionData =
            AssertionData(tag = AssertionTag.ALL,
                expectedSubjectClass = EventLogSubject::class,
                assertion = assertion as FlickerSubject.() -> Unit)
    }
}

internal const val REPETITIONS = "repetitions"
internal const val START_ROTATION = "startRotation"
internal const val END_ROTATION = "endRotation"

val Map<String, Any?>.repetitions: Int
    get() = this.getOrDefault(REPETITIONS, 1) as Int

val Map<String, Any?>.startRotation: Int
    get() = this.getOrDefault(START_ROTATION, Surface.ROTATION_0) as Int

val Map<String, Any?>.endRotation: Int
    get() = this.getOrDefault(END_ROTATION, startRotation) as Int

val Map<String, Any?>.startRotationName: String
    get() = Surface.rotationToString(startRotation)

val Map<String, Any?>.endRotationName: String
    get() = Surface.rotationToString(endRotation)