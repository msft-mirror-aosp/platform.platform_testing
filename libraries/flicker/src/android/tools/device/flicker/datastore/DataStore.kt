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

package android.tools.device.flicker.datastore

import android.tools.common.IScenario
import android.tools.common.flicker.assertors.IAssertionResult
import android.tools.device.traces.io.IResultData
import androidx.annotation.VisibleForTesting

/** In memory data store for flicker transitions, assertions and results */
object DataStore {
    private val cachedResults = mutableMapOf<IScenario, IResultData>()
    private val cachedFlickerServiceAssertions = mutableMapOf<IScenario, List<IAssertionResult>>()

    @VisibleForTesting
    fun clear() {
        cachedResults.clear()
        cachedFlickerServiceAssertions.clear()
    }

    /** @return if the store has results for [scenario] */
    fun containsResult(scenario: IScenario): Boolean = cachedResults.containsKey(scenario)

    /**
     * Adds [result] to the store with [scenario] as id
     *
     * @throws IllegalStateException is [scenario] already exists in the data store
     */
    @Throws(IllegalStateException::class)
    fun addResult(scenario: IScenario, result: IResultData) {
        if (containsResult(scenario)) {
            error("Result for $scenario already in data store")
        }
        cachedResults[scenario] = result
    }

    /**
     * Replaces the old value [scenario] result in the store by [newResult]
     *
     * @throws IllegalStateException is [scenario] doesn't exist in the data store
     */
    @Throws(IllegalStateException::class)
    fun replaceResult(scenario: IScenario, newResult: IResultData) {
        if (!containsResult(scenario)) {
            error("Result for $scenario not in data store")
        }
        cachedResults[scenario] = newResult
    }

    /**
     * @return the result for [scenario]
     * @throws IllegalStateException is [scenario] doesn't exist in the data store
     */
    @Throws(IllegalStateException::class)
    fun getResult(scenario: IScenario): IResultData =
        cachedResults[scenario] ?: error("No value for $scenario")

    /** @return if the store has results for [scenario] */
    fun containsFlickerServiceResult(scenario: IScenario): Boolean =
        cachedFlickerServiceAssertions.containsKey(scenario)

    fun addFlickerServiceResults(scenario: IScenario, results: List<IAssertionResult>) {
        if (containsFlickerServiceResult(scenario)) {
            error("Result for $scenario already in data store")
        }
        cachedFlickerServiceAssertions[scenario] = results
    }

    fun getFlickerServiceResults(scenario: IScenario): List<IAssertionResult> {
        return cachedFlickerServiceAssertions[scenario]
            ?: error("No flicker service results for $scenario")
    }

    fun getFlickerServiceResultsForAssertion(
        scenario: IScenario,
        assertionName: String
    ): List<IAssertionResult> {
        return cachedFlickerServiceAssertions[scenario]?.filter {
            it.assertion.name == assertionName
        }
            ?: error("Assertion with name $assertionName not found for scenario $scenario")
    }
}
