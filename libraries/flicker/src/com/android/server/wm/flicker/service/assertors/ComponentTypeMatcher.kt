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
import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.traces.common.ComponentName
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.transition.Transition

/**
 * ComponentMatcher based on type (e.g. open/close app) It is initialized late based on the
 * transition passed through the execute function of the assertion
 */
class ComponentTypeMatcher(val name: String) : ComponentNameMatcher(ComponentName("", "")) {
    var initialized = false
    override var component: ComponentName = super.component
        get() {
            if (!initialized) {
                throw RuntimeException("Component was not initialized yet")
            }
            return field
        }

    var componentBuilder: ComponentBuilder = Components.EMPTY

    fun initialize(transition: Transition) {
        Utils.componentNameMatcherHardcoded(name)?.run {
            this@ComponentTypeMatcher.component = this.component
        }
            ?: run {
                // safe cast because both openingAppFrom and closingAppFrom
                // in Components return ComponentNameMatcher
                val componentMatcher = componentBuilder.build(transition) as ComponentNameMatcher
                this.component = componentMatcher.component
            }
        this.initialized = true
    }

    override fun toString(): String {
        return "Components.$componentBuilder"
    }

    companion object {
        fun componentMatcherFromName(
            name: String,
            traceConfiguration: DeviceTraceConfiguration
        ): ComponentNameMatcher? {
            Utils.componentNameMatcherHardcoded(name)?.run {
                return ComponentNameMatcher(this.component)
            }
                ?: run {
                    val componentMatcher = ComponentTypeMatcher(name)
                    traceConfiguration.run {
                        if (this.componentToTypeMap[name] != null) {
                            componentMatcher.componentBuilder = this.componentToTypeMap[name]!!
                            return componentMatcher
                        } else {
                            return null
                        }
                    }
                        ?: run {
                            throw ConfigException("Missing trace configuration - component $name")
                        }
                }
        }
    }
}

class ConfigException(message: String) : Exception(message)
