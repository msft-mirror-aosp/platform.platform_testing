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

package com.android.server.wm.flicker.assertiongenerator.windowmanager

import com.android.server.wm.flicker.assertiongenerator.common.IComponentLifecycle
import com.android.server.wm.flicker.assertiongenerator.common.IElementLifecycle
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle
import com.android.server.wm.traces.common.ComponentNameMatcher

class WmTraceLifecycle(
    val lifecycleMap: MutableMap<ComponentNameMatcher, WmComponentLifecycle> = mutableMapOf()
) : ITraceLifecycle {
    override val size: Int
        get() = lifecycleMap.size

    override val elementIds: MutableSet<ComponentNameMatcher>
        get() = lifecycleMap.keys

    override fun get(elementId: Any): WmComponentLifecycle? {
        return lifecycleMap[elementId as ComponentNameMatcher]
    }

    override fun set(elementId: Any, elementLifecycles: IComponentLifecycle) {
        lifecycleMap[elementId as ComponentNameMatcher] = elementLifecycles as WmComponentLifecycle
    }

    override fun getOrPut(
        elementId: Any,
        elementLifecycles: IComponentLifecycle
    ): IComponentLifecycle {
        return lifecycleMap.getOrPut(elementId as ComponentNameMatcher) {
            elementLifecycles as WmComponentLifecycle
        }
    }

    fun add(
        elementComponentNameMatcher: ComponentNameMatcher,
        elementId: String,
        elementLifecycle: IElementLifecycle
    ) {
        lifecycleMap[elementComponentNameMatcher]
            ?: run { lifecycleMap[elementComponentNameMatcher] = WmComponentLifecycle() }
        lifecycleMap[elementComponentNameMatcher]!![elementId] =
            elementLifecycle as WmElementLifecycle
    }

    override fun equals(other: Any?): Boolean {
        return other is WmTraceLifecycle && this.lifecycleMap == other.lifecycleMap
    }

    override fun hashCode(): Int {
        var result = lifecycleMap.hashCode()
        result = 31 * result + size
        result = 31 * result + elementIds.hashCode()
        return result
    }

    override fun toString(): String {
        return "WmTraceLifecycle:\n$lifecycleMap\n\n"
    }
}
