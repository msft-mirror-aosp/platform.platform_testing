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

package com.android.server.wm.flicker.traces.region

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.assertions.Fact
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.region.Region
import com.android.server.wm.traces.common.region.RegionEntry
import kotlin.math.abs

/** Subject for [Rect] objects, used to make assertions over behaviors that occur on a rectangle. */
class RegionSubject(
    override val parent: FlickerSubject?,
    val regionEntry: RegionEntry,
    override val timestamp: Timestamp
) : FlickerSubject() {

    /** Custom constructor for existing android regions */
    constructor(
        region: Region?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(parent, RegionEntry(region ?: Region.EMPTY, timestamp), timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: Array<Rect>,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(Region(rect), parent, timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: Rect?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(Region.from(rect), parent, timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: RectF?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(rect?.toRect(), parent, timestamp)

    /** Custom constructor for existing rects */
    constructor(
        rect: Array<RectF>,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(mergeRegions(rect.map { Region.from(it.toRect()) }.toTypedArray()), parent, timestamp)

    /** Custom constructor for existing regions */
    constructor(
        regions: Array<Region>,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(mergeRegions(regions), parent, timestamp)

    /**
     * Custom constructor
     *
     * @param regionEntry to assert
     * @param parent containing the entry
     */
    constructor(
        regionEntry: RegionEntry?,
        parent: FlickerSubject? = null,
        timestamp: Timestamp
    ) : this(regionEntry?.region, parent, timestamp)

    val region = regionEntry.region

    private val android.graphics.Rect.area
        get() = this.width() * this.height()
    private val Rect.area
        get() = this.width * this.height

    override val selfFacts = listOf(Fact("Region - Covered", region.toString()))

    /** {@inheritDoc} */
    override fun fail(reason: List<Fact>): FlickerSubject {
        val newReason = reason.toMutableList()
        return super.fail(newReason)
    }

    /** Asserts that the current [Region] doesn't contain layers */
    fun isEmpty(): RegionSubject = apply { check(regionEntry.region.isEmpty) { "Region is empty" } }

    /** Asserts that the current [Region] doesn't contain layers */
    fun isNotEmpty(): RegionSubject = apply {
        check(regionEntry.region.isNotEmpty) { "Region is not empty" }
    }

    private fun assertLeftRightAndAreaEquals(other: Region) {
        check { MSG_ERROR_LEFT_POSITION }.that(region.bounds.left).isEqual(other.bounds.left)
        check { MSG_ERROR_RIGHT_POSITION }.that(region.bounds.right).isEqual(other.bounds.right)
        check { MSG_ERROR_AREA }.that(region.bounds.area).isEqual(other.bounds.area)
    }

    /** Subtracts [other] from this subject [region] */
    fun minus(other: Region): RegionSubject {
        val remainingRegion = Region.from(this.region)
        remainingRegion.op(other, Region.Op.XOR)
        return RegionSubject(remainingRegion, this, timestamp)
    }

    /** Adds [other] to this subject [region] */
    fun plus(other: Region): RegionSubject {
        val remainingRegion = Region.from(this.region)
        remainingRegion.op(other, Region.Op.UNION)
        return RegionSubject(remainingRegion, this, timestamp)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are smaller or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(subject: RegionSubject): RegionSubject = apply {
        isHigherOrEqual(subject.region)
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller or equal to those of
     * [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(other: Rect): RegionSubject = apply { isHigherOrEqual(Region.from(other)) }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller or equal to those of
     * [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigherOrEqual(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isLowerOrEqual(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }
            .that(region.bounds.bottom)
            .isLowerOrEqual(other.bounds.bottom)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are greater or equal to
     * those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLowerOrEqual(subject: RegionSubject): RegionSubject = apply {
        isLowerOrEqual(subject.region)
    }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater or equal to those of
     * [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLowerOrEqual(other: Rect): RegionSubject = apply { isLowerOrEqual(Region.from(other)) }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater or equal to those of
     * [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLowerOrEqual(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isGreaterOrEqual(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }
            .that(region.bounds.bottom)
            .isGreaterOrEqual(other.bounds.bottom)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are smaller than those
     * of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigher(subject: RegionSubject): RegionSubject = apply { isHigher(subject.region) }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigher(other: Rect): RegionSubject = apply { isHigher(Region.from(other)) }

    /**
     * Asserts that the top and bottom coordinates of [other] are smaller than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isHigher(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isLower(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }.that(region.bounds.bottom).isLower(other.bounds.bottom)
    }

    /**
     * Asserts that the top and bottom coordinates of [RegionSubject.region] are greater than those
     * of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLower(subject: RegionSubject): RegionSubject = apply { isLower(subject.region) }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLower(other: Rect): RegionSubject = apply { isLower(Region.from(other)) }

    /**
     * Asserts that the top and bottom coordinates of [other] are greater than those of [region].
     *
     * Also checks that the left and right positions, as well as area, don't change
     */
    fun isLower(other: Region): RegionSubject = apply {
        assertLeftRightAndAreaEquals(other)
        check { MSG_ERROR_TOP_POSITION }.that(region.bounds.top).isGreater(other.bounds.top)
        check { MSG_ERROR_BOTTOM_POSITION }
            .that(region.bounds.bottom)
            .isGreater(other.bounds.bottom)
    }

    /**
     * Asserts that [region] covers at most [testRegion], that is, its area doesn't cover any point
     * outside of [testRegion].
     *
     * @param testRegion Expected covered area
     */
    fun coversAtMost(testRegion: Region): RegionSubject = apply {
        if (!region.coversAtMost(testRegion)) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Out-of-bounds region", region.outOfBoundsRegion(testRegion))
            )
        }
    }

    /**
     * Asserts that [region] covers at most [testRect], that is, its area doesn't cover any point
     * outside of [testRect].
     *
     * @param testRect Expected covered area
     */
    fun coversAtMost(testRect: Rect): RegionSubject = apply { coversAtMost(Region.from(testRect)) }

    /**
     * Asserts that [region] is not bigger than [testRegion], even if the regions don't overlap.
     *
     * @param testRegion Area to compare to
     */
    fun notBiggerThan(testRegion: Region): RegionSubject = apply {
        val testArea = testRegion.bounds.area
        val area = region.bounds.area

        if (area > testArea) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Area of test region", testArea),
                Fact("Covered region", region),
                Fact("Area of region", area)
            )
        }
    }

    /**
     * Asserts that [region] is positioned to the right and bottom from [testRegion], but the
     * regions can overlap and [region] can be smaller than [testRegion]
     *
     * @param testRegion Area to compare to
     * @param threshold Offset threshold by which the position might be off
     */
    fun isToTheRightBottom(testRegion: Region, threshold: Int): RegionSubject = apply {
        val horizontallyPositionedToTheRight =
            testRegion.bounds.left - threshold <= region.bounds.left
        val verticallyPositionedToTheBottom = testRegion.bounds.top - threshold <= region.bounds.top

        if (!horizontallyPositionedToTheRight || !verticallyPositionedToTheBottom) {
            fail(Fact("Region to test", testRegion), Fact("Actual region", region))
        }
    }

    /**
     * Asserts that [region] covers at least [testRegion], that is, its area covers each point in
     * the region
     *
     * @param testRegion Expected covered area
     */
    fun coversAtLeast(testRegion: Region): RegionSubject = apply {
        if (!region.coversAtLeast(testRegion)) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Uncovered region", region.uncoveredRegion(testRegion))
            )
        }
    }

    /**
     * Asserts that [region] covers at least [testRect], that is, its area covers each point in the
     * region
     *
     * @param testRect Expected covered area
     */
    fun coversAtLeast(testRect: Rect): RegionSubject = apply {
        coversAtLeast(Region.from(testRect))
    }

    /**
     * Asserts that [region] covers at exactly [testRegion]
     *
     * @param testRegion Expected covered area
     */
    fun coversExactly(testRegion: Region): RegionSubject = apply {
        val intersection = Region.from(region)
        val isNotEmpty = intersection.op(testRegion, Region.Op.XOR)

        if (isNotEmpty) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Uncovered region", intersection)
            )
        }
    }

    /**
     * Asserts that [region] covers at exactly [testRect]
     *
     * @param testRect Expected covered area
     */
    fun coversExactly(testRect: Rect): RegionSubject = apply {
        coversExactly(Region.from(testRect))
    }

    /**
     * Asserts that [region] and [testRegion] overlap
     *
     * @param testRegion Other area
     */
    fun overlaps(testRegion: Region): RegionSubject = apply {
        val intersection = Region.from(region)
        val isEmpty = !intersection.op(testRegion, Region.Op.INTERSECT)

        if (isEmpty) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Overlap region", intersection)
            )
        }
    }

    /**
     * Asserts that [region] and [testRect] overlap
     *
     * @param testRect Other area
     */
    fun overlaps(testRect: Rect): RegionSubject = apply { overlaps(Region.from(testRect)) }

    /**
     * Asserts that [region] and [testRegion] don't overlap
     *
     * @param testRegion Other area
     */
    fun notOverlaps(testRegion: Region): RegionSubject = apply {
        val intersection = Region.from(region)
        val isEmpty = !intersection.op(testRegion, Region.Op.INTERSECT)

        if (!isEmpty) {
            fail(
                Fact("Region to test", testRegion),
                Fact("Covered region", region),
                Fact("Overlap region", intersection)
            )
        }
    }

    /**
     * Asserts that [region] and [testRect] don't overlap
     *
     * @param testRect Other area
     */
    fun notOverlaps(testRect: Rect): RegionSubject = apply { notOverlaps(Region.from(testRect)) }

    /**
     * Asserts that [region] and [previous] have same aspect ratio, margin of error up to 0.1.
     *
     * @param other Other region
     */
    fun isSameAspectRatio(other: RegionSubject): RegionSubject = apply {
        val aspectRatio = this.region.width.toFloat() / this.region.height
        val otherAspectRatio = other.region.width.toFloat() / other.region.height
        check { "Aspect Ratio Difference" }
            .that(abs(aspectRatio - otherAspectRatio))
            .isLowerOrEqual(0.1f)
    }

    companion object {
        @VisibleForTesting const val MSG_ERROR_TOP_POSITION = "Top position"

        @VisibleForTesting const val MSG_ERROR_BOTTOM_POSITION = "Bottom position"

        @VisibleForTesting const val MSG_ERROR_LEFT_POSITION = "Left position"

        @VisibleForTesting const val MSG_ERROR_RIGHT_POSITION = "Right position"

        @VisibleForTesting const val MSG_ERROR_AREA = "Rect area"

        private fun mergeRegions(regions: Array<Region>): Region {
            val result = Region.EMPTY
            regions.forEach { region ->
                region.rects.forEach { rect -> result.op(rect, Region.Op.UNION) }
            }
            return result
        }
    }
}
