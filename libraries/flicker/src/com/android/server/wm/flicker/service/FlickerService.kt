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

package com.android.server.wm.flicker.service

import android.util.Log
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.traces.common.errors.ErrorTrace

/** Contains the logic for Flicker as a Service. */
class FlickerService : IFlickerService {
    /**
     * The entry point for WM Flicker Service.
     *
     * Calls the Tagging Engine and the Assertion Engine.
     *
     * @param reader A flicker trace reader
     * @return A pair with an [ErrorTrace] and a map that associates assertion names with 0 if it
     * fails and 1 if it passes
     */
    override fun process(reader: IReader): List<AssertionResult> {
        try {
            val assertionEngine =
                AssertionEngine(AssertionGeneratorConfigProducer()) {
                    Log.v("$FLICKER_TAG-ASSERT", it)
                }
            return assertionEngine.analyze(reader, AssertionEngine.AssertionsToUse.ALL)
        } catch (exception: Throwable) {
            Log.e("$FLICKER_TAG-ASSERT", "FAILED PROCESSING", exception)
            throw exception
        }
    }
}
