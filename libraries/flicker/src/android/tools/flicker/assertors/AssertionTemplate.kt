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

package android.tools.flicker.assertors

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.AssertionData
import android.tools.flicker.assertions.FlickerChecker
import android.tools.flicker.assertions.ServiceFlickerChecker
import android.tools.flicker.assertions.SubjectsParser
import android.tools.flicker.subject.exceptions.FlickerAssertionError

/** Base class for a FaaS assertion */
abstract class AssertionTemplate @JvmOverloads constructor(name: String? = null) {
    protected open val name =
        this::class.simpleName
            ?: name
            ?: error("Must provide a name to assertions when using anonymous classes.")
    val id
        get() = AssertionId(name)

    open val assertionHint: String? = null

    fun qualifiedAssertionName(scenarioInstance: ScenarioInstance): String =
        "${scenarioInstance.type}::$name"

    /** Evaluates assertions */
    abstract fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerChecker)

    fun createAssertions(scenarioInstance: ScenarioInstance): Collection<AssertionData> {
        val flicker = ServiceFlickerChecker()

        val mainBlockAssertions = mutableListOf<AssertionData>()
        try {
            doEvaluate(scenarioInstance, flicker)
        } catch (e: FlickerAssertionError) {
            assertionHint?.let { e.messageBuilder.addHint(it) }
            mainBlockAssertions.add(
                object : AssertionData {
                    override fun checkAssertion(run: SubjectsParser) {
                        throw e
                    }
                }
            )
        } catch (e: Throwable) {
            // Any failure that occurred outside the Flicker assertion blocks
            mainBlockAssertions.add(
                object : AssertionData {
                    override fun checkAssertion(run: SubjectsParser) {
                        throw e
                    }
                }
            )
        }

        return flicker.assertions.map { wrapAssertionWithHint(it) } + mainBlockAssertions
    }

    private fun wrapAssertionWithHint(assertionData: AssertionData): AssertionData {
        return object : AssertionData {
            override fun checkAssertion(run: SubjectsParser) {
                try {
                    assertionData.checkAssertion(run)
                } catch (e: FlickerAssertionError) {
                    assertionHint?.let { e.messageBuilder.addHint(it) }
                    throw e
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        // Ensure both assertions are instances of the same class.
        return this::class == other::class
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class Assertion(
    val flickerAssertions: List<AssertionData>,
    val genericAssertions: List<Throwable>,
)
