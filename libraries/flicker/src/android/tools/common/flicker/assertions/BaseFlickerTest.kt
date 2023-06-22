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

package android.tools.common.flicker.assertions

import android.tools.common.Logger
import android.tools.common.flicker.subject.events.EventLogSubject
import android.tools.common.flicker.subject.layers.LayerTraceEntrySubject
import android.tools.common.flicker.subject.layers.LayersTraceSubject
import android.tools.common.flicker.subject.region.RegionTraceSubject
import android.tools.common.flicker.subject.wm.WindowManagerStateSubject
import android.tools.common.flicker.subject.wm.WindowManagerTraceSubject
import android.tools.common.traces.component.IComponentMatcher

abstract class BaseFlickerTest(
    private val defaultAssertionName: String,
    private val assertionFactory: AssertionFactory = AssertionFactory()
) : FlickerTest {
    protected abstract fun doProcess(assertion: AssertionData)

    override fun assertWmStart(name: String, assertion: WindowManagerStateSubject.() -> Unit) {
        Logger.withTracing("assertWmStart") {
            val assertionData = assertionFactory.createWmStartAssertion(name, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWmEnd(name: String, assertion: WindowManagerStateSubject.() -> Unit) {
        Logger.withTracing("assertWmEnd") {
            val assertionData = assertionFactory.createWmEndAssertion(name, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWm(name: String, assertion: WindowManagerTraceSubject.() -> Unit) {
        Logger.withTracing("assertWm") {
            val assertionData = assertionFactory.createWmAssertion(name, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWmTag(
        name: String,
        tag: String,
        assertion: WindowManagerStateSubject.() -> Unit
    ) {
        Logger.withTracing("assertWmTag") {
            val assertionData = assertionFactory.createWmTagAssertion(tag, name, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWmVisibleRegion(
        name: String,
        componentMatcher: IComponentMatcher,
        assertion: RegionTraceSubject.() -> Unit
    ) {
        Logger.withTracing("assertWmVisibleRegion") {
            val assertionData =
                assertionFactory.createWmVisibleRegionAssertion(componentMatcher, name, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayersStart(name: String, assertion: LayerTraceEntrySubject.() -> Unit) {
        Logger.withTracing("assertLayersStart") {
            val assertionData = assertionFactory.createLayersStartAssertion(name, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayersEnd(name: String, assertion: LayerTraceEntrySubject.() -> Unit) {
        Logger.withTracing("assertLayersEnd") {
            val assertionData = assertionFactory.createLayersEndAssertion(name, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayers(name: String, assertion: LayersTraceSubject.() -> Unit) {
        Logger.withTracing("assertLayers") {
            val assertionData =
                assertionFactory.createLayersAssertion(name = defaultAssertionName, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertLayersTag(
        name: String,
        tag: String,
        assertion: LayerTraceEntrySubject.() -> Unit
    ) {
        Logger.withTracing("assertLayersTag") {
            val assertionData =
                assertionFactory.createLayersTagAssertion(
                    tag,
                    name = defaultAssertionName,
                    assertion
                )
            doProcess(assertionData)
        }
    }

    override fun assertLayersVisibleRegion(
        name: String,
        componentMatcher: IComponentMatcher,
        useCompositionEngineRegionOnly: Boolean,
        assertion: RegionTraceSubject.() -> Unit
    ) {
        Logger.withTracing("assertLayersVisibleRegion") {
            val assertionData =
                assertionFactory.createLayersVisibleRegionAssertion(
                    componentMatcher,
                    name = defaultAssertionName,
                    useCompositionEngineRegionOnly,
                    assertion
                )
            doProcess(assertionData)
        }
    }

    override fun assertEventLog(name: String, assertion: EventLogSubject.() -> Unit) {
        Logger.withTracing("assertEventLog") {
            val assertionData =
                assertionFactory.createEventLogAssertion(name = defaultAssertionName, assertion)
            doProcess(assertionData)
        }
    }

    override fun assertWmStart(assertion: WindowManagerStateSubject.() -> Unit) =
        assertWmStart(name = defaultAssertionName, assertion)

    override fun assertWmEnd(assertion: WindowManagerStateSubject.() -> Unit) =
        assertWmEnd(name = defaultAssertionName, assertion)

    override fun assertWm(assertion: WindowManagerTraceSubject.() -> Unit) =
        assertWm(name = defaultAssertionName, assertion)

    override fun assertWmTag(tag: String, assertion: WindowManagerStateSubject.() -> Unit) =
        assertWmTag(name = defaultAssertionName, tag, assertion)

    override fun assertWmVisibleRegion(
        componentMatcher: IComponentMatcher,
        assertion: RegionTraceSubject.() -> Unit
    ) = assertWmVisibleRegion(name = defaultAssertionName, componentMatcher, assertion)

    override fun assertLayersStart(assertion: LayerTraceEntrySubject.() -> Unit) =
        assertLayersStart(name = defaultAssertionName, assertion)

    override fun assertLayersEnd(assertion: LayerTraceEntrySubject.() -> Unit) =
        assertLayersEnd(name = defaultAssertionName, assertion)

    override fun assertLayers(assertion: LayersTraceSubject.() -> Unit) =
        assertLayers(name = defaultAssertionName, assertion)

    override fun assertLayersTag(tag: String, assertion: LayerTraceEntrySubject.() -> Unit) =
        assertLayersTag(name = defaultAssertionName, tag, assertion)

    override fun assertLayersVisibleRegion(
        componentMatcher: IComponentMatcher,
        useCompositionEngineRegionOnly: Boolean,
        assertion: RegionTraceSubject.() -> Unit
    ) =
        assertLayersVisibleRegion(
            name = defaultAssertionName,
            componentMatcher,
            useCompositionEngineRegionOnly,
            assertion
        )

    override fun assertEventLog(assertion: EventLogSubject.() -> Unit) =
        assertEventLog(name = defaultAssertionName, assertion)
}
