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

package com.android.server.wm.flicker.service.assertors.factories

import com.android.server.wm.flicker.service.IScenarioInstance
import com.android.server.wm.flicker.service.assertors.IFaasAssertion
import com.android.server.wm.flicker.service.config.FlickerServiceConfig

open class AssertionFactory : IAssertionFactory {
    override fun generateAssertionsFor(
        scenarioInstance: IScenarioInstance
    ): Collection<IFaasAssertion> {
        val assertionTemplates =
            FlickerServiceConfig.getScenarioConfigFor(scenarioInstance.type).assertionTemplates
        return assertionTemplates.map { it.createAssertion(scenarioInstance) }
    }
}
