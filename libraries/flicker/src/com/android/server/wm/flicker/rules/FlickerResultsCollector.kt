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

package com.android.server.wm.flicker.rules

import android.device.collectors.BaseCollectionListener
import android.util.Log
import com.android.helpers.ICollectorHelper

class FlickerResultsCollector : BaseCollectionListener<Boolean>() {
    private val collectionHelper = CollectionHelper()
    private var criticalUserJourneyName: String = UNDEFINED_CUJ

    init {
        createHelperInstance(collectionHelper)
    }

    fun setCriticalUserJourneyName(className: String?) {
        this.criticalUserJourneyName = className ?: UNDEFINED_CUJ
    }

    fun postRunResults(results: Map<String, Int>) {
        collectionHelper.postMetrics(assertionsToMetrics(results))
    }

    fun getMetrics(): Map<String, Int> {
        return collectionHelper.metrics
    }

    /**
     * Convert the assertions generated by the Flicker Service to specific metric key pairs that
     * contain enough information to later further and analyze in dashboards.
     */
    private fun assertionsToMetrics(assertions: Map<String, Int>): Map<String, Int> {
        val processedAssertions: MutableMap<String, Int> = mutableMapOf()

        for ((assertionName, result) in assertions) {
            // Add information about the CUJ we are running the assertions on
            processedAssertions["$FASS_METRICS_PREFIX::$criticalUserJourneyName::$assertionName"] =
                    result
        }

        return processedAssertions
    }

    class CollectionHelper : ICollectorHelper<Int> {
        private var metrics: MutableMap<String, Int> = mutableMapOf()

        fun postMetrics(results: Map<String, Int>) {
            for ((key, res) in results) {
                require(res == 1 || res == 0)
                // If a failure is posted for key then we fail
                metrics[key] = (metrics[key] ?: 1) and res
            }
        }

        /** Do nothing. */
        override fun startCollecting(): Boolean {
            Log.i(LOG_TAG, "startCollecting")
            return true
        }

        /** Do nothing. */
        override fun stopCollecting(): Boolean {
            Log.i(LOG_TAG, "stopCollecting")
            return true
        }

        /** Collect the assertions metrics for Flicker as a Service.  */
        override fun getMetrics(): Map<String, Int> {
            Log.i(LOG_TAG, "getMetrics")
            return metrics
        }

        companion object {
            private val LOG_TAG = this::class.java.simpleName
        }
    }

    companion object {
        // Unique prefix to add to all fass metrics to identify them
        private const val FASS_METRICS_PREFIX = "FASS"
        private const val UNDEFINED_CUJ = "UndefinedCUJ"
    }
}