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

package com.android.server.wm.flicker.service.assertors

import android.util.Log
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.traces.common.tags.Transition
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object AssertionConfigParser {
    private const val ASSERTORS_KEY = "assertors"
    private const val NAME_KEY = "name"
    private const val TRANSITION_KEY = "transition"
    private const val ASSERTIONS_KEY = "assertions"

    internal const val PRESUBMIT_KEY = "presubmit"
    internal const val POSTSUBMIT_KEY = "postsubmit"
    internal const val FLAKY_KEY = "flaky"
    internal const val WM_TRACE_KEY = "wmTrace"
    internal const val LAYERS_TRACE_KEY = "layersTrace"

    /**
     * Parses assertor config JSON file. The format expected is:
     * <pre>
     * {
     *  "assertors": [
     *   {
     *      "name": "com.android.server.wm.flicker.service.assertors.RotationAssertor",
     *      "transition": "ROTATION",
     *      "assertions": {
     *         "presubmit": {
     *              "wmTrace": [
     *                  "navBarWindowIsVisible"
     *              ],
     *              "layersTrace" : [
     *                  "navBarLayerIsVisible",
     *                  "navBarLayerRotatesAndScales"
     *              ]
     *          },
     *          "postsubmit": {
     *              "wmTrace": [ ],
     *              "layersTrace" : [ ]
     *          },
     *          "flaky": {
     *              "wmTrace": [ ],
     *              "layersTrace" : [
     *                  "entireScreenCovered"
     *              ]
     *          }
     *      }
     *   }
     *  ]
     * }
     * </pre>
     *
     * @param config string containing a json file
     * @return an array of [AssertorConfigModel] configurations
     */
    @JvmStatic
    fun parseConfigFile(config: String): Array<AssertorConfigModel> {
        val assertorsConfig = mutableListOf<AssertorConfigModel>()
        try {
            val jsonArray = JSONObject(config).getJSONArray(ASSERTORS_KEY)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val jsonAssertions = jsonObject.getJSONObject(ASSERTIONS_KEY)

                val assertionsList =
                    parseAssertionObject(jsonAssertions
                        .getJSONObject(PRESUBMIT_KEY), PRESUBMIT_KEY) +
                    parseAssertionObject(jsonAssertions
                        .getJSONObject(POSTSUBMIT_KEY), POSTSUBMIT_KEY) +
                    parseAssertionObject(jsonAssertions.getJSONObject(FLAKY_KEY), FLAKY_KEY)

                assertorsConfig.add(
                    AssertorConfigModel(
                        jsonObject.getString(NAME_KEY),
                        Transition.valueOf(jsonObject.getString(TRANSITION_KEY)),
                        assertionsList
                    )
                )
            }
        } catch (e: JSONException) {
            Log.e(FLICKER_TAG, "Unable to parse the json object", e)
        }

        return assertorsConfig.toTypedArray()
    }

    /**
     * Parses an assertions JSONObject configuration.
     *
     * @param assertionCategoryObject a [JSONObject] with assertion names and traces
     * @param category the category of the assertion (presubmit/postsubmit/flaky)
     * @return an array of assertion details
     */
    @JvmStatic
    private fun parseAssertionObject(
        assertionCategoryObject: JSONObject,
        category: String
    ): Array<AssertionData> {
        val assertions = mutableListOf<AssertionData>()
        try {
            assertions.addAll(
                parseAssertionArray(
                    assertionCategoryObject.getJSONArray(WM_TRACE_KEY),
                    WM_TRACE_KEY,
                    category
                )
            )
            assertions.addAll(
                parseAssertionArray(
                    assertionCategoryObject.getJSONArray(LAYERS_TRACE_KEY),
                    LAYERS_TRACE_KEY,
                    category
                )
            )
        } catch (e: JSONException) {
            Log.e(FLICKER_TAG, "Unable to parse the json object", e)
        }

        return assertions.toTypedArray()
    }

    /**
     * Splits an assertions JSONArray into an array of [AssertionData].
     *
     * @param assertionsArray a [JSONArray] with assertion names
     * @param trace trace on which the assertion should be run
     * @param category the category of the assertion (presubmit/postsubmit/flaky)
     * @return an array of assertion details
     */
    @JvmStatic
    private fun parseAssertionArray(
        assertionsArray: JSONArray,
        trace: String,
        category: String
    ): Array<AssertionData> {
        val assertions = mutableListOf<AssertionData>()
        try {
            for (i in 0 until assertionsArray.length()) {
                assertions.add(AssertionData(assertionsArray.getString(i), trace, category))
            }
        } catch (e: JSONException) {
            Log.e(FLICKER_TAG, "Unable to parse the json object", e)
        }

        return assertions.toTypedArray()
    }
}