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

package com.android.server.wm.traces.common

import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

class ComponentSplashScreenMatcher(val componentNameMatcher: ComponentNameMatcher) :
    IComponentMatcher {
    override fun windowMatchesAnyOf(windows: Array<WindowState>): Boolean {
        error("Unimplemented - There are no splashscreen windows")
    }

    override fun activityMatchesAnyOf(activities: Array<Activity>): Boolean {
        error("Unimplemented - There are no splashscreen windows")
    }

    override fun layerMatchesAnyOf(layers: Array<Layer>): Boolean {
        return layers.any {
            if (!it.name.contains("Splash Screen")) {
                return@any false
            }
            if (it.children.isNotEmpty()) {
                // Not leaf splash screen layer but container of the splash screen layer
                return@any false
            }
            val grandParent = it.parent?.parent
            requireNotNull(grandParent) { "Splash screen layer's grandparent shouldn't be null" }
            return@any componentNameMatcher.layerMatchesAnyOf(grandParent)
        }
    }

    override fun toActivityIdentifier(): String {
        error("Unimplemented - There are no splashscreen windows")
    }

    override fun toWindowIdentifier(): String {
        error("Unimplemented - There are no splashscreen windows")
    }

    override fun toLayerIdentifier(): String {
        return "Splash Screen ${componentNameMatcher.className}"
    }

    override fun check(
        layers: Collection<Layer>,
        condition: (Collection<Layer>) -> Boolean
    ): Boolean {
        val splashScreenLayer = layers.filter { layerMatchesAnyOf(it) }
        require(splashScreenLayer.size < 1) {
            "More than on SplashScreen layer found. Only up to 1 match was expected."
        }
        return condition(splashScreenLayer)
    }
}
