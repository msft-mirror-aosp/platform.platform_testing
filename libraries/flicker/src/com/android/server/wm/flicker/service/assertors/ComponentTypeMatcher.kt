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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.service.ScenarioInstance
import com.android.server.wm.traces.common.component.ComponentName
import com.android.server.wm.traces.common.component.matchers.ComponentNameMatcher

/**
 * ComponentMatcher based on type (e.g. open/close app) It is initialized late based on the
 * transition passed through the execute function of the assertion
 */
class ComponentTypeMatcher(val name: String) : ComponentNameMatcher(ComponentName("", "")) {
    constructor(name: String, componentBuilder: ComponentTemplate) : this(name) {
        this.componentBuilder = componentBuilder
    }

    var initialized = false
    override var component: ComponentName = super.component
        get() {
            if (!initialized) {
                throw RuntimeException("Component was not initialized yet")
            }
            return field
        }

    var componentBuilder: ComponentTemplate = Components.EMPTY

    fun initialize(scenarioInstance: ScenarioInstance) {
        Utils.componentNameMatcherHardcoded(name)?.run {
            this@ComponentTypeMatcher.component = this.component
        }
            ?: run {
                // safe cast because both openingAppFrom and closingAppFrom
                // in Components return ComponentNameMatcher
                val componentMatcher =
                    componentBuilder.build(scenarioInstance) as ComponentNameMatcher
                this.component = componentMatcher.component
            }
        this.initialized = true
    }

    override fun toString(): String {
        return "Components.$componentBuilder"
    }

    override fun equals(other: Any?): Boolean {
        return other is ComponentTypeMatcher &&
            name == other.name &&
            componentBuilder == other.componentBuilder
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + componentBuilder.hashCode()
        return result
    }
}

class ConfigException(message: String) : Exception(message)
