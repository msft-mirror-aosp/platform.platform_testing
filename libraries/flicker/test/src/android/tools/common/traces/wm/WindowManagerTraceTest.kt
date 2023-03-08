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

package android.tools.common.traces.wm

import android.tools.InitRule
import android.tools.common.Cache
import android.tools.common.CrossPlatform
import android.tools.readWmTraceFromFile
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Modifier
import org.junit.Before
import org.junit.ClassRule
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
        get() = readWmTraceFromFile("wm_trace_openchrome.pb", legacyTrace = true)

    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canDetectAppWindow() {
        val appWindows =
            trace
                .getEntryExactlyAt(CrossPlatform.timestamp.from(elapsedNanos = 9213763541297L))
                .appWindows
        assertWithMessage("Unable to detect app windows").that(appWindows.size).isEqualTo(2)
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
            val trace = readWmTraceFromFile(traceName, legacyTrace = true)
            assertWithMessage("Unable to parse dump").that(trace.entries.size).isGreaterThan(1)

            trace.entries.forEach { entry: WindowManagerState ->
                entry::class.java.accessProperties(entry)
                entry.displays.forEach { it::class.java.accessProperties(it) }
            }
        }
    }

    @Test
    fun canDetectValidState() {
        val entry =
            trace.getEntryExactlyAt(CrossPlatform.timestamp.from(elapsedNanos = 9213763541297))
        assertWithMessage("${entry.timestamp}: ${entry.getIsIncompleteReason()}")
            .that(entry.isIncomplete())
            .isFalse()
    }

    @Test
    fun canDetectInvalidState() {
        val entry =
            trace.getEntryExactlyAt(CrossPlatform.timestamp.from(elapsedNanos = 9215511235586))
        assertWithMessage("${entry.timestamp}: ${entry.getIsIncompleteReason()}")
            .that(entry.isIncomplete())
            .isTrue()

        assertThat(entry.getIsIncompleteReason()).contains("No resumed activities found")
    }

    @Test
    fun canSlice() {
        val trace =
            readWmTraceFromFile(
                "wm_trace_openchrome2.pb",
                from = 174686204723645,
                to = 174686640998584,
                legacyTrace = true
            )

        assertThat(trace.entries).asList().isNotEmpty()
        assertThat(trace.entries.first().timestamp.elapsedNanos).isEqualTo(174686204723645)
        assertThat(trace.entries.last().timestamp.elapsedNanos).isEqualTo(174686640998584)
    }

    @Test
    fun canSliceWithWrongTimestamps() {
        val trace =
            readWmTraceFromFile(
                "wm_trace_openchrome2.pb",
                from = 9213763541297,
                to = 9215895891561,
                legacyTrace = true
            )
        assertThat(trace.entries).asList().isEmpty()
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
