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

package com.android.server.wm.flicker.windowmanager

import com.android.server.wm.flicker.readTestFile
import com.android.server.wm.flicker.readWmTraceFromFile
import com.android.server.wm.flicker.utils.MockWindowManagerTraceBuilder
import com.android.server.wm.flicker.utils.MockWindowStateBuilder
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Modifier
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [WindowManagerTrace] tests. To run this test: `atest
 * FlickerLibTest:WindowManagerTraceTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WindowManagerTraceTest {
    private val trace
        get() = readWmTraceFromFile("wm_trace_openchrome.pb")

    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseAllEntries() {
        val firstEntry = trace.entries[0]
        assertThat(firstEntry.timestamp.elapsedNanos).isEqualTo(9213763541297L)
        assertThat(firstEntry.windowStates.size).isEqualTo(10)
        assertThat(firstEntry.visibleWindows.size).isEqualTo(5)
        assertThat(trace.entries[trace.entries.size - 1].timestamp.elapsedNanos)
            .isEqualTo(9216093628925L)
    }

    @Test
    fun canDetectAppWindow() {
        val appWindows = trace.getEntryByElapsedTimestamp(9213763541297L).appWindows
        assertWithMessage("Unable to detect app windows").that(appWindows.size).isEqualTo(2)
    }

    @Test
    fun canParseFromDump() {
        val trace =
            try {
                WindowManagerTraceParser.parseFromDump(readTestFile("wm_trace_dump.pb"))
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        assertWithMessage("Unable to parse dump").that(trace).hasSize(1)
    }

    /**
     * Access all public methods and invokes all public getters from the object to check that all
     * lazy properties contain valid values
     */
    private fun <T> Class<T>.accessProperties(obj: Any) {
        val propertyValues =
            this.declaredFields
                .filter { Modifier.isPublic(it.modifiers) }
                .map { kotlin.runCatching { Pair(it.name, it.get(obj)) } }
                .filter { it.isFailure }

        assertWithMessage(
                "The following properties could not be read: " + propertyValues.joinToString("\n")
            )
            .that(propertyValues)
            .isEmpty()

        val getterValues =
            this.declaredMethods
                .filter {
                    Modifier.isPublic(it.modifiers) &&
                        it.name.startsWith("get") &&
                        it.parameterCount == 0
                }
                .map { kotlin.runCatching { Pair(it.name, it.invoke(obj)) } }
                .filter { it.isFailure }

        assertWithMessage(
                "The following methods could not be invoked: " + getterValues.joinToString("\n")
            )
            .that(getterValues)
            .isEmpty()

        this.superclass?.accessProperties(obj)
        if (obj is WindowContainer) {
            obj.children.forEach { it::class.java.accessProperties(it) }
        }
    }

    /**
     * Tests if all properties of the flicker objects are accessible. This is necessary because most
     * values are lazy initialized and only trigger errors when being accessed for the first time.
     */
    @Test
    fun canAccessAllProperties() {
        arrayOf("wm_trace_activity_transition.pb", "wm_trace_openchrome2.pb").forEach { traceName ->
            val trace = readWmTraceFromFile(traceName)
            assertWithMessage("Unable to parse dump").that(trace.entries.size).isGreaterThan(1)

            trace.entries.forEach { entry: WindowManagerState ->
                entry::class.java.accessProperties(entry)
                entry.displays.forEach { it::class.java.accessProperties(it) }
            }
        }
    }

    @Test
    fun canDetectValidState() {
        val entry = trace.getEntryByElapsedTimestamp(9213763541297)
        assertWithMessage("${entry.timestamp}: ${entry.getIsIncompleteReason()}")
            .that(entry.isIncomplete())
            .isFalse()
    }

    @Test
    fun canDetectInvalidState() {
        val entry = trace.getEntryByElapsedTimestamp(9215511235586)
        assertWithMessage("${entry.timestamp}: ${entry.getIsIncompleteReason()}")
            .that(entry.isIncomplete())
            .isTrue()

        assertThat(entry.getIsIncompleteReason()).contains("No resumed activities found")
    }

    @Test
    fun canSlice() {
        val trace = readWmTraceFromFile("wm_trace_openchrome2.pb")
        val splitlayersTrace = trace.sliceUsingElapsedTimestamp(174686204723645, 174686640998584)

        Truth.assertThat(splitlayersTrace).isNotEmpty()

        Truth.assertThat(splitlayersTrace.entries.first().timestamp.elapsedNanos)
            .isEqualTo(174686204723645)
        Truth.assertThat(splitlayersTrace.entries.last().timestamp.elapsedNanos)
            .isEqualTo(174686640998584)
    }

    @Test
    fun canSlice_wrongTimestamps() {
        val trace = readWmTraceFromFile("wm_trace_openchrome2.pb")
        val splitLayersTrace = trace.sliceUsingElapsedTimestamp(9213763541297, 9215895891561)

        Truth.assertThat(splitLayersTrace).isEmpty()
    }

    @Test
    fun canSlice_allBefore() {
        testSliceUsingElapsedTimestamp(
            0L,
            mockTraceForSliceTests.first().timestamp.elapsedNanos - 1,
            listOf<Long>()
        )
    }

    @Test
    fun canSlice_allAfter() {
        val from = mockTraceForSliceTests.last().elapsedTimestamp + 5
        val to = mockTraceForSliceTests.last().elapsedTimestamp + 20
        val splitLayersTrace = mockTraceForSliceTests.sliceUsingElapsedTimestamp(from, to)
        Truth.assertThat(splitLayersTrace).isEmpty()

        val splitLayersTraceWithInitialEntry =
            mockTraceForSliceTests.sliceUsingElapsedTimestamp(from, to, addInitialEntry = true)
        Truth.assertThat(splitLayersTraceWithInitialEntry).hasSize(1)
        Truth.assertThat(splitLayersTraceWithInitialEntry.first().timestamp)
            .isEqualTo(mockTraceForSliceTests.last().timestamp)
    }

    @Test
    fun canSlice_inMiddle() {
        testSliceUsingElapsedTimestamp(15L, 25L, listOf(15L, 18L, 25L))
    }

    @Test
    fun canSlice_fromBeforeFirstEntryToMiddle() {
        testSliceUsingElapsedTimestamp(
            mockTraceForSliceTests.first().timestamp.elapsedNanos - 1,
            27L,
            listOf(5L, 8L, 15L, 18L, 25L, 27L)
        )
    }

    @Test
    fun canSlice_fromMiddleToAfterLastEntry() {
        testSliceUsingElapsedTimestamp(
            18L,
            mockTraceForSliceTests.last().timestamp.elapsedNanos + 5,
            listOf(18L, 25L, 27L, 30L)
        )
    }

    @Test
    fun canSlice_fromBeforeToAfterLastEntry() {
        testSliceUsingElapsedTimestamp(
            mockTraceForSliceTests.first().timestamp.elapsedNanos - 1,
            mockTraceForSliceTests.last().timestamp.elapsedNanos + 1,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_fromExactStartToAfterLastEntry() {
        testSliceUsingElapsedTimestamp(
            mockTraceForSliceTests.first().timestamp,
            mockTraceForSliceTests.last().timestamp.elapsedNanos + 1,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_fromExactStartToExactEnd() {
        testSliceUsingElapsedTimestamp(
            mockTraceForSliceTests.first().timestamp,
            mockTraceForSliceTests.last().timestamp,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_fromExactStartToMiddle() {
        testSliceUsingElapsedTimestamp(
            mockTraceForSliceTests.first().timestamp,
            18L,
            listOf(5L, 8L, 15L, 18L)
        )
    }

    @Test
    fun canSlice_fromMiddleToExactEnd() {
        testSliceUsingElapsedTimestamp(
            18L,
            mockTraceForSliceTests.last().timestamp,
            listOf(18L, 25L, 27L, 30L)
        )
    }

    @Test
    fun canSlice_fromBeforeToExactEnd() {
        testSliceUsingElapsedTimestamp(
            mockTraceForSliceTests.first().timestamp.elapsedNanos - 1,
            mockTraceForSliceTests.last().timestamp,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_sameStartAndEnd() {
        testSliceUsingElapsedTimestamp(15L, 15L, listOf(15L))
    }

    @JvmName("testSliceUsingElapsedTimestamp1")
    private fun testSliceUsingElapsedTimestamp(from: Long, to: Long, expected: List<Timestamp>) {
        return testSliceUsingElapsedTimestamp(from, to, expected.map { it.elapsedNanos })
    }

    @JvmName("testSliceUsingElapsedTimestamp1")
    private fun testSliceUsingElapsedTimestamp(
        from: Timestamp?,
        to: Long,
        expected: List<Timestamp>
    ) {
        return testSliceUsingElapsedTimestamp(from, to, expected.map { it.elapsedNanos })
    }

    private fun testSliceUsingElapsedTimestamp(
        from: Long,
        to: Timestamp?,
        expected: List<Timestamp>
    ) {
        return testSliceUsingElapsedTimestamp(from, to, expected.map { it.elapsedNanos })
    }

    @JvmName("testSliceUsingElapsedTimestamp1")
    private fun testSliceUsingElapsedTimestamp(from: Long, to: Timestamp?, expected: List<Long>) {
        return testSliceUsingElapsedTimestamp(
            from,
            to?.elapsedNanos ?: error("missing elapsed timestamp"),
            expected
        )
    }

    @JvmName("testSliceUsingElapsedTimestamp2")
    private fun testSliceUsingElapsedTimestamp(
        from: Timestamp?,
        to: Timestamp?,
        expected: List<Timestamp>
    ) {
        return testSliceUsingElapsedTimestamp(
            from?.elapsedNanos ?: error("missing elapsed timestamp"),
            to?.elapsedNanos ?: error("missing elapsed timestamp"),
            expected.map { it.elapsedNanos }
        )
    }

    private fun testSliceUsingElapsedTimestamp(from: Timestamp?, to: Long, expected: List<Long>) {
        return testSliceUsingElapsedTimestamp(
            from?.elapsedNanos ?: error("missing elapsed timestamp"),
            to,
            expected
        )
    }

    private fun testSliceUsingElapsedTimestamp(
        from: Timestamp?,
        to: Timestamp?,
        expected: List<Long>
    ) {
        return testSliceUsingElapsedTimestamp(
            from?.elapsedNanos ?: error("missing elapsed timestamp"),
            to?.elapsedNanos ?: error("missing elapsed timestamp"),
            expected
        )
    }

    private fun testSliceUsingElapsedTimestamp(from: Long, to: Long, expected: List<Long>) {
        require(from <= to) { "`from` not before `to`" }
        val fromBefore = from < mockTraceForSliceTests.first().timestamp.elapsedNanos
        val fromAfter = from < mockTraceForSliceTests.first().timestamp.elapsedNanos

        val toBefore = to < mockTraceForSliceTests.first().timestamp.elapsedNanos
        val toAfter = mockTraceForSliceTests.last().timestamp.elapsedNanos < to

        require(
            fromBefore ||
                fromAfter ||
                mockTraceForSliceTests.map { it.timestamp.elapsedNanos }.contains(from)
        ) { "`from` need to be in the trace or before or after all entries" }
        require(
            toBefore ||
                toAfter ||
                mockTraceForSliceTests.map { it.timestamp.elapsedNanos }.contains(to)
        ) { "`to` need to be in the trace or before or after all entries" }

        testSliceWithOutInitialEntry(from, to, expected)
        if (!fromAfter) {
            testSliceWithOutInitialEntry(from - 1, to, expected)
            testSliceWithOutInitialEntry(from - 1, to + 1, expected)
        }
        if (!toBefore) {
            testSliceWithOutInitialEntry(from, to + 1, expected)
        }

        testSliceWithInitialEntry(from, to, expected)
        if (!fromBefore) {
            if (from < to) {
                testSliceWithInitialEntry(from + 1, to, expected)
            }
            testSliceWithInitialEntry(from + 1, to + 1, expected)
        }
        if (!toBefore) {
            testSliceWithInitialEntry(from, to + 1, expected)
        }
    }

    private fun testSliceWithOutInitialEntry(from: Long, to: Long, expected: List<Long>) {
        val splitLayersTrace = mockTraceForSliceTests.sliceUsingElapsedTimestamp(from, to)
        assertThat(splitLayersTrace.map { it.timestamp.elapsedNanos }).isEqualTo(expected)
    }

    private fun testSliceWithInitialEntry(from: Long, to: Long, expected: List<Long>) {
        val splitLayersTraceWithStartEntry =
            mockTraceForSliceTests.sliceUsingElapsedTimestamp(from, to, addInitialEntry = true)
        assertThat(splitLayersTraceWithStartEntry.map { it.timestamp.elapsedNanos })
            .isEqualTo(expected)
    }

    companion object {
        val mockTraceForSliceTests =
            MockWindowManagerTraceBuilder(
                    entries =
                        mutableListOf(
                            MockWindowStateBuilder(timestamp = 5),
                            MockWindowStateBuilder(timestamp = 8),
                            MockWindowStateBuilder(timestamp = 15),
                            MockWindowStateBuilder(timestamp = 18),
                            MockWindowStateBuilder(timestamp = 25),
                            MockWindowStateBuilder(timestamp = 27),
                            MockWindowStateBuilder(timestamp = 30),
                        )
                )
                .build()
    }
}
