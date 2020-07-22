/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.graphics.Rect
import android.graphics.Region
import android.util.Log
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

/** Truth subject for [LayersTrace] objects.  */
class LayersTraceSubject private constructor(
    fm: FailureMetadata,
    subject: LayersTrace
) : Subject<LayersTraceSubject, LayersTrace>(fm, subject) {

    private val assertionsChecker = AssertionsChecker<LayerTraceEntry>()
    private var newAssertion = true

    private fun addAssertion(name: String, assertion: TraceAssertion<LayerTraceEntry>) {
        if (newAssertion) {
            assertionsChecker.add(assertion, name)
        } else {
            assertionsChecker.append(assertion, name)
        }
    }

    /**
     * Run the assertions for all trace entries
     */
    fun forAllEntries() {
        test()
    }

    /**
     * Run the assertions for all trace entries within the specified time range
     */
    fun forRange(startTime: Long, endTime: Long) {
        assertionsChecker.filterByRange(startTime, endTime)
        test()
    }

    /**
     * Signal that the last assertion set is complete. The next assertion added will start a new
     * set of assertions.
     *
     * E.g.: checkA().then().checkB()
     *
     * Will produce two sets of assertions (checkA) and (checkB) and checkB will only be checked
     * after checkA passes.
     */
    fun then(): LayersTraceSubject {
        newAssertion = true
        assertionsChecker.checkChangingAssertions()
        return this
    }

    /**
     * Signal that the last assertion set is not complete. The next assertion added will be
     * appended to the current set of assertions.
     *
     * E.g.: checkA().and().checkB()
     *
     * Will produce one sets of assertions (checkA, checkB) and the assertion will only pass is both
     * checkA and checkB pass.
     */
    fun and(): LayersTraceSubject {
        newAssertion = false
        return this
    }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    fun skipUntilFirstAssertion(): LayersTraceSubject {
        assertionsChecker.skipUntilFirstAssertion()
        return this
    }

    /**
     * Run the assertions only in the first trace entry
     */
    fun inTheBeginning() {
        if (actual()!!.entries.isEmpty()) {
            fail("No entries found.")
        }
        assertionsChecker.checkFirstEntry()
        test()
    }

    /**
     * Run the assertions only in the last  trace entry
     */
    fun atTheEnd() {
        if (actual()!!.entries.isEmpty()) {
            fail("No entries found.")
        }
        assertionsChecker.checkLastEntry()
        test()
    }

    private fun test() {
        val failures = assertionsChecker.test(actual()!!.entries)
        if (failures.isNotEmpty()) {
            val failureLogs = failures.joinToString("\n")
            var tracePath = ""
            if (actual().hasSource()) {
                tracePath = "Layers Trace can be found in: ${actual().source?.toAbsolutePath()}" +
                        "\nChecksum: " + actual().sourceChecksum + "\n"
            }
            fail(tracePath + failureLogs)
        }
    }

    fun coversRegion(rect: Rect): LayersTraceSubject {
        return this.coversRegion(Region(rect))
    }

    fun coversRegion(region: Region): LayersTraceSubject {
        addAssertion("coversRegion($region)") { it.coversRegion(region) }
        return this
    }

    fun hasVisibleRegion(layerName: String, size: Region): LayersTraceSubject {
        addAssertion("hasVisibleRegion($layerName$size)") {
            it.hasVisibleRegion(layerName, size)
        }
        return this
    }

    fun hasNotLayer(layerName: String): LayersTraceSubject {
        addAssertion("hasNotLayer($layerName)") { it.exists(layerName).negate() }
        return this
    }

    fun hasLayer(layerName: String): LayersTraceSubject {
        addAssertion("hasLayer($layerName)") { it.exists(layerName) }
        return this
    }

    fun showsLayer(layerName: String): LayersTraceSubject {
        addAssertion("showsLayer($layerName)") { it.isVisible(layerName) }
        return this
    }

    fun replaceVisibleLayer(
        previousLayerName: String,
        currentLayerName: String
    ): LayersTraceSubject {
        return hidesLayer(previousLayerName).and().showsLayer(currentLayerName)
    }

    fun hidesLayer(layerName: String): LayersTraceSubject {
        addAssertion("hidesLayer($layerName)") { it.isVisible(layerName).negate() }
        return this
    }

    companion object {
        // Boiler-plate Subject.Factory for LayersTraceSubject
        private val FACTORY = Factory { fm: FailureMetadata, subject: LayersTrace ->
            LayersTraceSubject(fm, subject)
        }

        // User-defined entry point
        @JvmStatic
        fun assertThat(entry: LayersTrace): LayersTraceSubject {
            return Truth.assertAbout(FACTORY).that(entry)
        }

        // User-defined entry point. Ignores orphaned layers because of b/141326137
        @JvmStatic
        @JvmOverloads
        fun assertThat(result: TransitionResult,
                       orphanLayerCallback: (Layer) -> Boolean = {
                           Log.w(FLICKER_TAG, "Ignoring orphaned layer $it")
                           true
                       }
        ): LayersTraceSubject {
            val entries = LayersTrace.parseFrom(
                    result.layersTrace,
                    result.layersTracePath,
                    result.layersTraceChecksum,
                    orphanLayerCallback)
            return Truth.assertWithMessage(result.toString()).about(FACTORY).that(entries)
        }

        // Static method for getting the subject factory (for use with assertAbout())
        @JvmStatic
        fun entries(): Factory<LayersTraceSubject, LayersTrace> {
            return FACTORY
        }
    }
}