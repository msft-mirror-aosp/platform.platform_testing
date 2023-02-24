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

import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.io.ResultData
import com.android.server.wm.flicker.io.TransitionTimeRange

object Consts {
    internal const val FAILURE = "Expected failure"

    internal val TEST_RESULT =
        ResultData(
            _artifactPath = getDefaultFlickerOutputDir(),
            _transitionTimeRange = TransitionTimeRange.EMPTY,
            _executionError = null,
            _runStatus = RunStatus.RUN_EXECUTED
        )

    internal val RESULT_FAILURE =
        ResultData(
            _artifactPath = getDefaultFlickerOutputDir(),
            _transitionTimeRange = TransitionTimeRange.EMPTY,
            _executionError = null,
            _runStatus = RunStatus.RUN_EXECUTED
        )
}
