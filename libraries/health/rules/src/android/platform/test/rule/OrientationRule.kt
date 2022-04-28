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
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule will lock orientation before running a test class and unlock after. The orientation is
 * natural by default, and landscape if the test or one of its superclasses is marked with the
 * [Landscape] annotation.
 */
class OrientationRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        val landscape = hasLandscapeAnnotation(description.testClass)
        val orientationRule =
            if (landscape) LandscapeOrientationRule() else NaturalOrientationRule()
        return orientationRule.apply(base, description)
    }

    private fun hasLandscapeAnnotation(testClass: Class<*>?): Boolean =
        if (testClass == null) false
        else if (testClass.isAnnotationPresent(Landscape::class.java)) true
        else hasLandscapeAnnotation(testClass.superclass)

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    annotation class Landscape
}
