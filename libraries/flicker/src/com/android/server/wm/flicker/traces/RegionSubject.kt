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

package com.android.server.wm.flicker.traces

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.parser.toAndroidRect
import com.android.server.wm.traces.parser.toAndroidRegion
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder

/**
 * Truth subject for [Rect] objects, used to make assertions over behaviors that occur on a
 * rectangle.
 */
class RegionSubject(
    fm: FailureMetadata,
    private val subject: FlickerSubject?,
    val region: android.graphics.Region,
    private val failureFacts: List<Fact>
) : FlickerSubject(fm, region) {
    private val topPositionSubject
        get() = check(MSG_ERROR_TOP_POSITION).that(region.bounds.top)
    private val bottomPositionSubject
        get() = check(MSG_ERROR_BOTTOM_POSITION).that(region.bounds.bottom)
    private val leftPositionSubject
        get() = check(MSG_ERROR_LEFT_POSITION).that(region.bounds.left)
    private val rightPositionSubject
        get() = check(MSG_ERROR_RIGHT_POSITION).that(region.bounds.right)
    private val areaSubject
        get() = check(MSG_ERROR_AREA).that(region.bounds.area)

    private val android.graphics.Rect.area get() = this.width() * this.height()
    private val Rect.area get() = this.width * this.height

    override val defaultFacts: String = buildString {
        if (subject?.defaultFacts != null) {
            append(subject.defaultFacts)
            append("\n")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun clone(): FlickerSubject {
        return RegionSubject(fm, subject, region, failureFacts)
    }

    /**
     * {@inheritDoc}
     */
    override fun fail(reason: List<Fact>): FlickerSubject {
        val newReason = reason.toMutableList()
        newReason.addAll(failureFacts)
        return super.fail(newReason)
    }

    private fun assertLeftRightAndAreaEquals(other: android.graphics.Region) {
        leftPositionSubject.isEqualTo(other.bounds.left)
        rightPositionSubject.isEqualTo(other.bounds.right)
        areaSubject.isEqualTo(other.bounds.area)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are smaller
     * or equal to those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(subject: RegionSubject): RegionSubject = apply {
        isHigherOrEqual(subject.region)
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(other: Rect): RegionSubject = apply {
        isHigherOrEqual(other.toAndroidRect())
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(other: Region): RegionSubject = apply {
        isHigherOrEqual(other.toAndroidRegion())
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(other: android.graphics.Rect): RegionSubject = apply {
        isHigherOrEqual(android.graphics.Region(other))
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(other: android.graphics.Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        topPositionSubject.isAtMost(other.bounds.top)
        bottomPositionSubject.isAtMost(other.bounds.bottom)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are greater
     * or equal to those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLowerOrEqual(subject: RegionSubject): RegionSubject = apply {
        isLowerOrEqual(subject.region)
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLowerOrEqual(other: Rect): RegionSubject = apply {
        isLowerOrEqual(other.toAndroidRect())
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLowerOrEqual(other: android.graphics.Rect): RegionSubject = apply {
        isLowerOrEqual(android.graphics.Region(other))
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLowerOrEqual(other: android.graphics.Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        topPositionSubject.isAtLeast(other.bounds.top)
        bottomPositionSubject.isAtLeast(other.bounds.bottom)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are smaller than
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigher(subject: RegionSubject): RegionSubject = apply {
        isHigher(subject.region)
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigher(other: Rect): RegionSubject = apply {
        isHigher(other.toAndroidRect())
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigher(other: android.graphics.Rect): RegionSubject = apply {
        isHigher(android.graphics.Region(other))
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigher(other: android.graphics.Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        topPositionSubject.isLessThan(other.bounds.top)
        bottomPositionSubject.isLessThan(other.bounds.bottom)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are greater than
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLower(subject: RegionSubject): RegionSubject = apply {
        isLower(subject.region)
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLower(other: Rect): RegionSubject = apply {
        isLower(other.toAndroidRect())
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLower(other: android.graphics.Rect): RegionSubject = apply {
        isLower(android.graphics.Region(other))
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLower(other: android.graphics.Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        topPositionSubject.isGreaterThan(other.bounds.top)
        bottomPositionSubject.isGreaterThan(other.bounds.bottom)
    }

    /**
     * Asserts that [region] covers at most [testRegion], that is, its area doesn't cover any
     * point outside of [testRegion].
     *
     * @param testRegion Expected covered area
     */
    fun coversAtMost(testRegion: android.graphics.Region): RegionSubject = apply {
        val testRect = testRegion.bounds
        val intersection = android.graphics.Region(region)
        val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
            !intersection.op(region, android.graphics.Region.Op.XOR)

        if (!covers) {
            fail(Fact.fact("Region to test", testRegion),
                Fact.fact("Covered region", region),
                Fact.fact("Out-of-bounds region", intersection))
        }
    }

    /**
     * Asserts that [region] covers at most [testRegion], that is, its area doesn't cover any
     * point outside of [testRegion].
     *
     * @param testRegion Expected covered area
     */
    fun coversAtMost(testRegion: Region): RegionSubject = apply {
        coversAtMost(testRegion.toAndroidRegion())
    }

    /**
     * Asserts that [region] covers at most [testRect], that is, its area doesn't cover any
     * point outside of [testRect].
     *
     * @param testRect Expected covered area
     */
    fun coversAtMost(testRect: Rect): RegionSubject = apply {
        coversAtMost(Region(testRect))
    }

    /**
     * Asserts that [region] covers at most [testRect], that is, its area doesn't cover any
     * point outside of [testRect].
     *
     * @param testRect Expected covered area
     */
    fun coversAtMost(testRect: android.graphics.Rect): RegionSubject = apply {
        coversAtMost(android.graphics.Region(testRect))
    }

    /**
     * Asserts that [region] covers at least [testRegion], that is, its area covers each point
     * in the region
     *
     * @param testRegion Expected covered area
     */
    fun coversAtLeast(testRegion: android.graphics.Region): RegionSubject = apply {
        val intersection = android.graphics.Region(region)
        val covers = intersection.op(testRegion, android.graphics.Region.Op.INTERSECT) &&
            !intersection.op(testRegion, android.graphics.Region.Op.XOR)

        if (!covers) {
            fail(Fact.fact("Region to test", testRegion),
                Fact.fact("Covered region", region),
                Fact.fact("Uncovered region", intersection))
        }
    }

    /**
     * Asserts that [region] covers at least [testRegion], that is, its area covers each point
     * in the region
     *
     * @param testRegion Expected covered area
     */
    fun coversAtLeast(testRegion: Region): RegionSubject = apply {
        coversAtLeast(testRegion.toAndroidRegion())
    }

    /**
     * Asserts that [region] covers at least [testRect], that is, its area covers each point
     * in the region
     *
     * @param testRect Expected covered area
     */
    fun coversAtLeast(testRect: Rect): RegionSubject = apply {
        coversAtLeast(Region(testRect))
    }

    /**
     * Asserts that [region] covers at least [testRect], that is, its area covers each point
     * in the region
     *
     * @param testRect Expected covered area
     */
    fun coversAtLeast(testRect: android.graphics.Rect): RegionSubject = apply {
        coversAtLeast(android.graphics.Region(testRect))
    }

    /**
     * Asserts that [region] covers at exactly [testRegion]
     *
     * @param testRegion Expected covered area
     */
    fun coversExactly(testRegion: android.graphics.Region): RegionSubject = apply {
        val intersection = android.graphics.Region(region)
        val isNotEmpty = intersection.op(testRegion, android.graphics.Region.Op.XOR)

        if (isNotEmpty) {
            intersection.op(testRegion, android.graphics.Region.Op.INTERSECT)
            fail(Fact.fact("Region to test", testRegion),
                Fact.fact("Covered region", region),
                Fact.fact("Uncovered region", intersection))
        }
    }

    /**
     * Asserts that [region] covers at exactly [testRegion]
     *
     * @param testRegion Expected covered area
     */
    fun coversExactly(testRegion: Region): RegionSubject = apply {
        coversExactly(testRegion.toAndroidRegion())
    }

    /**
     * Asserts that [region] covers at exactly [testRect]
     *
     * @param testRect Expected covered area
     */
    fun coversExactly(testRect: Rect): RegionSubject = apply {
        coversExactly(testRect.toAndroidRect())
    }

    /**
     * Asserts that [region] covers at exactly [testRect]
     *
     * @param testRect Expected covered area
     */
    fun coversExactly(testRect: android.graphics.Rect): RegionSubject = apply {
        coversExactly(android.graphics.Region(testRect))
    }

    /**
     * Asserts that [region] and [testRegion] overlap
     *
     * @param testRegion Other area
     */
    fun overlaps(testRegion: android.graphics.Region): RegionSubject = apply {
        val intersection = android.graphics.Region(region)
        val isEmpty = !intersection.op(testRegion, android.graphics.Region.Op.INTERSECT)

        if (isEmpty) {
            fail(Fact.fact("Region to test", testRegion),
                Fact.fact("Covered region", region),
                Fact.fact("Overlap region", intersection))
        }
    }

    /**
     * Asserts that [region] and [testRegion] overlap
     *
     * @param testRegion Other area
     */
    fun overlaps(testRegion: Region): RegionSubject = apply {
        overlaps(testRegion.toAndroidRegion())
    }

    /**
     * Asserts that [region] and [testRect] overlap
     *
     * @param testRect Other area
     */
    fun overlaps(testRect: android.graphics.Rect): RegionSubject = apply {
        overlaps(android.graphics.Region(testRect))
    }

    /**
     * Asserts that [region] and [testRect] overlap
     *
     * @param testRect Other area
     */
    fun overlaps(testRect: Rect): RegionSubject = apply {
        overlaps(testRect.toAndroidRect())
    }

    /**
     * Asserts that [region] and [testRegion] don't overlap
     *
     * @param testRegion Other area
     */
    fun notOverlaps(testRegion: android.graphics.Region): RegionSubject = apply {
        val intersection = android.graphics.Region(region)
        val isEmpty = !intersection.op(testRegion, android.graphics.Region.Op.INTERSECT)

        if (!isEmpty) {
            fail(Fact.fact("Region to test", testRegion),
                Fact.fact("Covered region", region),
                Fact.fact("Overlap region", intersection))
        }
    }

    /**
     * Asserts that [region] and [testRegion] don't overlap
     *
     * @param testRegion Other area
     */
    fun notOverlaps(testRegion: Region): RegionSubject = apply {
        notOverlaps(testRegion.toAndroidRegion())
    }

    /**
     * Asserts that [region] and [testRect] don't overlap
     *
     * @param testRect Other area
     */
    fun notOverlaps(testRect: android.graphics.Rect): RegionSubject = apply {
        notOverlaps(android.graphics.Region(testRect))
    }

    /**
     * Asserts that [region] and [testRect] don't overlap
     *
     * @param testRect Other area
     */
    fun notOverlaps(testRect: Rect): RegionSubject = apply {
        notOverlaps(testRect.toAndroidRect())
    }

    companion object {
        @VisibleForTesting
        const val MSG_ERROR_TOP_POSITION = "Incorrect top position"

        @VisibleForTesting
        const val MSG_ERROR_BOTTOM_POSITION = "Incorrect top position"

        @VisibleForTesting
        const val MSG_ERROR_LEFT_POSITION = "Incorrect left position"

        @VisibleForTesting
        const val MSG_ERROR_RIGHT_POSITION = "Incorrect right position"

        @VisibleForTesting
        const val MSG_ERROR_AREA = "Incorrect rect area"

        private fun mergeRegions(regions: List<Region>): android.graphics.Region {
            val region = android.graphics.Region()
            regions.forEach { region.op(it.rect.toAndroidRect(), android.graphics.Region.Op.UNION) }
            return region
        }

        /**
         * Boiler-plate Subject.Factory for RectSubject
         */
        @JvmStatic
        @JvmOverloads
        fun getFactory(
            flickerSubject: FlickerSubject? = null,
            layerVisibilityFacts: List<Fact> = emptyList()
        ) = Factory { fm: FailureMetadata, region: android.graphics.Region? ->
                val subjectRegion = region ?: android.graphics.Region()
                RegionSubject(fm, flickerSubject, subjectRegion, layerVisibilityFacts)
            }

        /**
         * User-defined entry point for existing rects
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            rect: Rect?,
            flickerSubject: FlickerSubject? = null,
            layerVisibilityFacts: List<Fact> = emptyList()
        ): RegionSubject {
            val region = android.graphics.Region(rect?.toAndroidRect() ?: android.graphics.Rect())
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(flickerSubject, layerVisibilityFacts))
                .that(region) as RegionSubject
            strategy.init(subject)
            return subject
        }

        /**
         * User-defined entry point for existing regions
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            regions: List<Region>,
            flickerSubject: FlickerSubject? = null,
            layerVisibilityFacts: List<Fact> = emptyList()
        ): RegionSubject {
            val mergedRegion = mergeRegions(regions)
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(flickerSubject, layerVisibilityFacts))
                .that(mergedRegion) as RegionSubject
            strategy.init(subject)
            return subject
        }

        /**
         * User-defined entry point for existing regions
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            region: Region?,
            flickerSubject: FlickerSubject? = null
        ): RegionSubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(flickerSubject))
                .that(region?.toAndroidRegion()) as RegionSubject
            strategy.init(subject)
            return subject
        }
    }
}