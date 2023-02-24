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

package com.android.server.wm.flicker.service

import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.config.FaasScenarioType
import com.android.server.wm.traces.common.IScenario
import com.android.server.wm.traces.common.transition.Transition

interface IScenarioInstance : IScenario {
    val type: FaasScenarioType
    // A reader to read the part of the trace associated with the scenario instance
    val reader: IReader

    val associatedTransition: Transition?
}
