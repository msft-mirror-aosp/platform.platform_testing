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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object AssertionConfigParser {
    private const val NAME_KEY = "name"
    private const val ASSERTIONS_KEY = "assertions"
    private const val WM_TRACE_KEY = "wmTrace"
    private const val LAYERS_TRACE_KEY = "layersTrace"
    private const val PRESUBMIT_KEY = "presubmit"
    private const val POSTSUBMIT_KEY = "postsubmit"
    private const val FLAKY_KEY = "flaky"

    /**
     * Parses assertor config JSON file. The format expected is:
     * <pre>
     * [
     *   {
     *      "name": "RotationAssertor",
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
     * ]
     * </pre>
     *
     * @param config string containing a json file
     * @return an array of [AssertorConfigModel] configurations
     */
    @JvmStatic
    fun parseConfigFile(config: String): Array<AssertorConfigModel> {
        val assertionsList = mutableListOf<AssertorConfigModel>()
        try {
            val jsonArray = JSONArray(config)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val jsonAssertions = jsonObject.getJSONObject(ASSERTIONS_KEY)
                assertionsList.add(
                    AssertorConfigModel(
                        jsonObject.getString(NAME_KEY),
                        parseAssertionObject(jsonAssertions.getJSONObject(PRESUBMIT_KEY)),
                        parseAssertionObject(jsonAssertions.getJSONObject(POSTSUBMIT_KEY)),
                        parseAssertionObject(jsonAssertions.getJSONObject(FLAKY_KEY))
                    )
                )
            }
        } catch (e: JSONException) {
            Log.e(FLICKER_TAG, "Unable to parse the json object", e)
        }

        return assertionsList.toTypedArray()
    }

    /**
     * Parses an assertions JSONObject configuration.
     *
     * @param assertionCategoryObject a [JSONObject] with assertion names and categories
     * @return an array of assertion details
     */
    @JvmStatic
    private fun parseAssertionObject(assertionCategoryObject: JSONObject): Array<AssertionData> {
        val assertions = mutableListOf<AssertionData>()
        try {
            assertions.addAll(
                parseAssertionArray(
                    assertionCategoryObject.getJSONArray(WM_TRACE_KEY),
                    WM_TRACE_KEY
                )
            )
            assertions.addAll(
                parseAssertionArray(
                    assertionCategoryObject.getJSONArray(LAYERS_TRACE_KEY),
                    LAYERS_TRACE_KEY
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
     * @param category trace on which the assertion should be run
     * @return an array of assertion details
     */
    @JvmStatic
    private fun parseAssertionArray(
        assertionsArray: JSONArray,
        category: String
    ): Array<AssertionData> {
        val assertions = mutableListOf<AssertionData>()
        try {
            for (i in 0 until assertionsArray.length()) {
                assertions.add(AssertionData(assertionsArray.getString(i), category))
            }
        } catch (e: JSONException) {
            Log.e(FLICKER_TAG, "Unable to parse the json object", e)
        }

        return assertions.toTypedArray()
    }
}