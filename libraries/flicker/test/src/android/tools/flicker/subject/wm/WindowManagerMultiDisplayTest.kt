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

package android.tools.flicker.subject.wm

import android.tools.Cache
import android.tools.CleanFlickerEnvironmentRuleWithDataStore
import android.tools.testutils.assertThrows
import android.tools.testutils.getWmTraceReaderFromAsset
import android.tools.traces.component.ComponentNameMatcher
import org.junit.Before
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [WindowManagerTraceSubject] tests for multi-display. To run this test: `atest
 * FlickerLibTest:android.tools.flicker.subject.wm.WindowManagerMultiDisplayTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WindowManagerMultiDisplayTest {
    private val chromeTraceReader = getWmTraceReaderFromAsset("wm_trace_openchrome")
    private val chromeTrace
        get() = chromeTraceReader.readWmTrace() ?: error("Unable to read WM trace")

    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun testCanScopeAssertionsToDisplay() {
        val subject = WindowManagerTraceSubject(chromeTrace, chromeTraceReader)

        subject.onDisplay(0).isNonAppWindowVisible(ComponentNameMatcher.STATUS_BAR).forAllEntries()
    }

    @Test
    fun testCanScopeAssertionsToNonExistingDisplay() {
        val subject = WindowManagerTraceSubject(chromeTrace, chromeTraceReader)

        assertThrows<AssertionError> {
            subject
                .onDisplay(999)
                .isNonAppWindowVisible(ComponentNameMatcher.STATUS_BAR)
                .forAllEntries()
        }
    }

    @Test
    fun testCanScopeFirstAndLastToDisplay() {
        val subject = WindowManagerTraceSubject(chromeTrace, chromeTraceReader)
        subject.onDisplay(0).first().isNonAppWindowVisible(ComponentNameMatcher.STATUS_BAR)

        assertThrows<AssertionError> {
            subject.onDisplay(999).last().isNonAppWindowVisible(ComponentNameMatcher.STATUS_BAR)
        }
    }

    @Test
    fun testChainedScoping() {
        val subject = WindowManagerTraceSubject(chromeTrace, chromeTraceReader)

        // This should pass because Display 0 has StatusBar
        subject.onDisplay(0).isNonAppWindowVisible(ComponentNameMatcher.STATUS_BAR).forAllEntries()

        // New subject to have a fresh assertionsChecker
        val subject2 = WindowManagerTraceSubject(chromeTrace, chromeTraceReader)
        assertThrows<AssertionError> {
            subject2
                .onDisplay(0)
                .isNonAppWindowVisible(ComponentNameMatcher.STATUS_BAR)
                .onDisplay(999)
                .isNonAppWindowVisible(ComponentNameMatcher.STATUS_BAR)
                .forAllEntries()
        }
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRuleWithDataStore()
    }
}
