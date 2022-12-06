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

package com.android.server.wm.flicker.junit

import com.android.server.wm.flicker.Scenario
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass

class LegacyFlickerDecorator(
    testClass: TestClass,
    scenario: Scenario?,
    inner: IFlickerJUnitDecorator?
) : AbstractFlickerRunnerDecorator(testClass, scenario, inner) {
    override fun getChildDescription(method: FrameworkMethod?): Description? {
        return inner?.getChildDescription(method)
    }

    override fun getTestMethods(test: Any): List<FrameworkMethod> {
        return inner?.getTestMethods(test) ?: emptyList()
    }

    override fun getMethodInvoker(method: FrameworkMethod, test: Any): Statement {
        return object : Statement() {
            override fun evaluate() {
                val description = getChildDescription(method)
                doRunTransition(test, description)
                inner?.getMethodInvoker(method, test)?.evaluate()
            }
        }
    }
}
