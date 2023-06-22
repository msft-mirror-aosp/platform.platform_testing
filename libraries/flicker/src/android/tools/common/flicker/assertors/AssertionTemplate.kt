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

package android.tools.common.flicker.assertors

import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.AssertionInvocationGroup.NON_BLOCKING
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertions.AssertionData
import android.tools.common.flicker.assertions.FlickerTest
import android.tools.common.flicker.assertions.ServiceFlickerTest

/** Base class for a FaaS assertion */
abstract class AssertionTemplate(nameOverride: String? = null) {
    private val name = nameOverride ?: this::class.simpleName

    open fun defaultAssertionName(scenarioInstance: ScenarioInstance): String =
        "${scenarioInstance.type}::$name"
    open val stabilityGroup: AssertionInvocationGroup = NON_BLOCKING

    /** Evaluates assertions */
    abstract fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest)

    fun createAssertions(scenarioInstance: ScenarioInstance): Collection<AssertionData> {
        val flicker = ServiceFlickerTest(defaultAssertionName(scenarioInstance))
        doEvaluate(scenarioInstance, flicker)

        return flicker.assertions + doCreateExtraAssertions(scenarioInstance)
    }

    /** Evaluates assertions */
    protected open fun doCreateExtraAssertions(
        scenarioInstance: ScenarioInstance
    ): List<AssertionData> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        // Ensure both assertions are instances of the same class.
        return this::class == other::class
    }

    override fun hashCode(): Int {
        var result = stabilityGroup.hashCode()
        result = 31 * result + this::class.simpleName.hashCode()
        return result
    }
}
