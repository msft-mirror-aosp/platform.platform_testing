/*
 * Copyright (C) 2026 The Android Open Source Project
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

package android.tools.flicker.subject.layers

import android.tools.Cache
import android.tools.CleanFlickerEnvironmentRuleWithDataStore
import android.tools.testutils.assertThrows
import android.tools.testutils.getLayerTraceReaderFromAsset
import android.tools.traces.component.ComponentNameMatcher
import org.junit.Before
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayersTraceSubject] tests for multi-display. To run this test: `atest
 * FlickerLibTest:android.tools.flicker.subject.layers.LayersTraceSubjectMultiDisplayTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayersTraceSubjectMultiDisplayTest {
    private val reader = getLayerTraceReaderFromAsset("layers_trace_multi_display.perfetto-trace")
    private val trace
        get() = reader.readLayersTrace() ?: error("Unable to read layers trace")

    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun testCanScopeAssertionsToDefaultDisplay() {
        val subject = LayersTraceSubject(trace, reader)

        subject
            .onDisplay(DEFAULT_DISPLAY)
            .isVisible(SETTINGS)
            .notContains(CALCULATOR)
            .forAllEntries()
    }

    @Test
    fun testCanScopeAssertionsToVirtualDisplay() {
        val subject = LayersTraceSubject(trace, reader)

        subject
            .onDisplay(VIRTUAL_DISPLAY)
            .isVisible(CALCULATOR)
            .notContains(SETTINGS)
            .forAllEntries()
    }

    @Test
    fun testCanScopeAssertionsToNonExistingDisplay() {
        val subject = LayersTraceSubject(trace, reader)

        assertThrows<AssertionError> {
            subject
                .onDisplay(NON_EXISTENT_DISPLAY)
                .isVisible(CALCULATOR)
                .isVisible(SETTINGS)
                .forAllEntries()
        }
    }

    @Test
    fun testCanScopeFirstAndLastToDisplay() {
        val subject = LayersTraceSubject(trace, reader)
        subject.onDisplay(DEFAULT_DISPLAY).first().isVisible(SETTINGS)

        assertThrows<AssertionError> {
            subject.onDisplay(NON_EXISTENT_DISPLAY).last().isVisible(SETTINGS)
        }
    }

    @Test
    fun testChainedScoping() {
        val subject = LayersTraceSubject(trace, reader)

        subject.onDisplay(DEFAULT_DISPLAY).isVisible(SETTINGS).forAllEntries()

        // New subject to have a fresh assertionsChecker
        val subject2 = LayersTraceSubject(trace, reader)
        assertThrows<AssertionError> {
            subject2
                .onDisplay(DEFAULT_DISPLAY)
                .isVisible(SETTINGS)
                .onDisplay(NON_EXISTENT_DISPLAY)
                .isVisible(SETTINGS)
                .forAllEntries()
        }
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRuleWithDataStore()
        private val CALCULATOR = ComponentNameMatcher("", "Calculator")
        private val SETTINGS = ComponentNameMatcher("", "SubSettings")
        private const val DEFAULT_DISPLAY = 0
        private const val VIRTUAL_DISPLAY = 10
        private const val NON_EXISTENT_DISPLAY = 999
    }
}
