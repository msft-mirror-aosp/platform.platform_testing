/*
 * Copyright (C) 2022 The Android Open Source Project
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
package android.platform.test.rule

import android.platform.test.rule.OrientationRule.Landscape
import android.platform.test.rule.OrientationRule.Portrait
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Locks orientation before running a test class and unlock after.
 *
 * The orientation is natural by default, and landscape or portrait if the test or one of its
 * superclasses is marked with the [Landscape] or [Portrait] annotation.
 *
 * Important: if screen dimensions change in between the test, it is not guaranteed the orientation
 * will match the one set. For example, if a two screens foldable device uses the [Portrait]
 * annotation while folded, and then the screen is changed to a bigger one, it might result in the
 * new orientation to be landscape instead (as the portrait orientation was leaving the device with
 * the natural orientation, but with the big screen natural orientation is landscape).
 */
class OrientationRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        val testClass = description.testClass

        val hasLandscapeAnnotation = testClass.hasAnnotation(Landscape::class.java)
        val hasPortraitAnnotation = testClass.hasAnnotation(Portrait::class.java)

        val orientationRule =
            when {
                hasLandscapeAnnotation && hasPortraitAnnotation ->
                    error("Both @Portrait and @Landscape annotations are not yet supported.")
                hasLandscapeAnnotation -> LandscapeOrientationRule()
                hasPortraitAnnotation -> PortraitOrientationRule()
                else -> NaturalOrientationRule()
            }

        // If the filter is not provided, we apply the orientation rule to everything.
        val deviceTypeFilter = testClass.getDeviceTypeFilter()

        return if (
            deviceTypeFilter.isNullOrEmpty() || deviceTypeFilter.any { matcher -> matcher.match() }
        ) {
            orientationRule.apply(base, description)
        } else {
            // By default, natural orientation.
            NaturalOrientationRule().apply(base, description)
        }
    }

    private fun <T> Class<T>?.getDeviceTypeFilter(): Array<DeviceTypeFilter>? =
        this?.getAnnotation(Landscape::class.java)?.deviceType
            ?: this?.getAnnotation(Portrait::class.java)?.deviceType

    /**
     * The orientation is applied only if the device type is within one of those in [deviceType].
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    annotation class Landscape(val deviceType: Array<DeviceTypeFilter> = [])

    /**
     * The orientation is applied only if the device type is within one of those in [deviceType].
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    annotation class Portrait(val deviceType: Array<DeviceTypeFilter> = [])
}

enum class DeviceTypeFilter(val match: () -> Boolean) {
    TABLET({ isTablet() }),
    FOLDABLE({ isFoldable() }),
    LARGE_SCREEN({ isLargeScreen() }),
    SMALL_SCREEN({ !isLargeScreen() })
}
