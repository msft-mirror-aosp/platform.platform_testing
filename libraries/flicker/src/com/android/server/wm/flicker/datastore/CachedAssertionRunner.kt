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

package com.android.server.wm.flicker.datastore

import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.assertions.BaseAssertionRunner
import com.android.server.wm.flicker.assertions.SubjectsParser
import com.android.server.wm.traces.common.IScenario

/**
 * Helper class to run an assertion on a flicker artifact from a [DataStore]
 *
 * @param scenario flicker scenario existing in the [DataStore]
 * @param resultReader helper class to read the flicker artifact
 * @param subjectsParser helper class to convert a result into flicker subjects
 */
class CachedAssertionRunner(
    private val scenario: IScenario,
    resultReader: CachedResultReader = CachedResultReader(scenario, DEFAULT_TRACE_CONFIG),
    subjectsParser: SubjectsParser = SubjectsParser(resultReader)
) : BaseAssertionRunner(resultReader, subjectsParser) {
    override fun doUpdateStatus(newStatus: RunStatus) {
        val result = DataStore.getResult(scenario)
        result.updateStatus(newStatus)
        DataStore.replaceResult(scenario, result)
    }
}
