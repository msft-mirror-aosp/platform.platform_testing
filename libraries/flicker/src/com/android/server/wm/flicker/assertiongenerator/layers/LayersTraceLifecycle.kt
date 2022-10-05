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

package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.IComponentLifecycle
import com.android.server.wm.flicker.assertiongenerator.common.IElementLifecycle
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle
import com.android.server.wm.traces.common.ComponentNameMatcher

class LayersTraceLifecycle(
    val lifecycleMap: MutableMap<ComponentNameMatcher, LayersComponentLifecycle> = mutableMapOf()
) : ITraceLifecycle {
    override val size: Int
        get() = lifecycleMap.size

    override val elementIds: MutableSet<ComponentNameMatcher>
        get() = lifecycleMap.keys

    override operator fun get(elementId: Any): LayersComponentLifecycle? {
        return lifecycleMap[elementId as ComponentNameMatcher]
    }

    fun add(
        elementComponentNameMatcher: ComponentNameMatcher,
        elementId: Int,
        elementLifecycle: IElementLifecycle
    ) {
        lifecycleMap[elementComponentNameMatcher]
            ?: run { lifecycleMap[elementComponentNameMatcher] = LayersComponentLifecycle() }
        lifecycleMap[elementComponentNameMatcher]!![elementId] =
            elementLifecycle as LayersElementLifecycle
    }

    // can't have LayersElementLifecycle because
    // compiler won't allow different types in the function signature
    override operator fun set(elementId: Any, elementLifecycles: IComponentLifecycle) {
        lifecycleMap[elementId as ComponentNameMatcher] =
            elementLifecycles as LayersComponentLifecycle
    }

    override fun getOrPut(
        elementId: Any,
        elementLifecycles: IComponentLifecycle
    ): LayersComponentLifecycle {
        return lifecycleMap.getOrPut(elementId as ComponentNameMatcher) {
            elementLifecycles as LayersComponentLifecycle
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is LayersTraceLifecycle && this.lifecycleMap == other.lifecycleMap
    }

    override fun hashCode(): Int {
        var result = lifecycleMap.hashCode()
        result = 31 * result + size
        result = 31 * result + elementIds.hashCode()
        return result
    }

    override fun toString(): String {
        return "LayersTraceLifecycle:\n$lifecycleMap\n\n"
    }
}
