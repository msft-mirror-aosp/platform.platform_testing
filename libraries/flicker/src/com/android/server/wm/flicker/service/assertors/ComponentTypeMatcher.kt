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
import com.android.server.wm.traces.common.ComponentName
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.transition.Transition

class ComponentTypeMatcher(
    val name: String
) : ComponentNameMatcher(
    ComponentName("", "")
) {
    var initialized = false
    val componentBuilder: ComponentBuilder
        get() {
            Components.byName[name]?.run{
                return this
            } ?: run {
                throw RuntimeException("Component builder with type $name does not exist")
            }
        }

    fun earlyInitialize() {
        Utils.componentNameMatcherHardcoded(name)
            ?.run{
                this@ComponentTypeMatcher.component = this.component
                initialized = true
            }
            ?: throw RuntimeException("ComponentMatcher with name " +
                "$name cannot be initialized early")
    }

    fun initialize(transition: Transition) {
        this.initialized = true
        Utils.componentNameMatcherHardcoded(name)
            ?. run { this@ComponentTypeMatcher.component = this.component }
            ?: run {
                // safe cast because both openingAppFrom and closingAppFrom
                // in Components return ComponentNameMatcher
                val componentMatcher = componentBuilder.build(transition) as ComponentNameMatcher
                this.component = componentMatcher.component
            }
    }
}
