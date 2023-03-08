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

package android.tools.common.flicker.subject.region

import android.tools.common.datatypes.Rect
import android.tools.common.datatypes.Region
import android.tools.common.flicker.subject.FlickerSubject
import android.tools.common.flicker.subject.FlickerTraceSubject
import android.tools.common.traces.region.RegionTrace

class RegionTraceSubject(val trace: RegionTrace, override val parent: FlickerSubject?) :
    FlickerTraceSubject<RegionSubject>() {

    override val subjects by lazy { trace.entries.map { RegionSubject(it, this, it.timestamp) } }

    private val componentsAsString =
        if (trace.components == null) {
            "<any>"
        } else {
            "[${trace.components}]"
        }

    /**
     * Asserts that the visible area covered by any element in the state covers at most [testRegion]
     * , that is, if the area of no elements cover any point outside of [testRegion].
     *
     * @param testRegion Expected covered area
     */
    fun coversAtMost(testRegion: Region): RegionTraceSubject = apply {
        addAssertion("coversAtMost($testRegion, $componentsAsString") {
            it.coversAtMost(testRegion)
        }
    }

    /**
     * Asserts that the visible area covered by any element in the state covers at most [testRegion]
     * , that is, if the area of no elements cover any point outside of [testRegion].
     *
     * @param testRegion Expected covered area
     */
    fun coversAtMost(testRegion: Rect): RegionTraceSubject = this.coversAtMost(testRegion)

    /**
     * Asserts that the visible area covered by any element in the state covers at least
     * [testRegion], that is, if the area of its elements visible region covers each point in the
     * region.
     *
     * @param testRegion Expected covered area
     */
    fun coversAtLeast(testRegion: Region): RegionTraceSubject = apply {
        addAssertion("coversAtLeast($testRegion, $componentsAsString)") {
            it.coversAtLeast(testRegion)
        }
    }

    /**
     * Asserts that the visible area covered by any element in the state covers at least
     * [testRegion], that is, if the area of its elements visible region covers each point in the
     * region.
     *
     * @param testRegion Expected covered area
     */
    fun coversAtLeast(testRegion: Rect): RegionTraceSubject =
        this.coversAtLeast(Region.from(testRegion))

    /**
     * Asserts that the visible region of the trace entries is exactly [expectedVisibleRegion].
     *
     * @param expectedVisibleRegion Expected visible region of the layer
     */
    fun coversExactly(expectedVisibleRegion: Region): RegionTraceSubject = apply {
        addAssertion("coversExactly($expectedVisibleRegion, $componentsAsString)") {
            it.coversExactly(expectedVisibleRegion)
        }
    }
}
