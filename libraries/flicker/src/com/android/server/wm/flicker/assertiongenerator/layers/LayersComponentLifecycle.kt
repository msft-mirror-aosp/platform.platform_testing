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

class LayersComponentLifecycle(
    val lifecyclesMap: MutableMap<Int, LayersElementLifecycle> = mutableMapOf()
) : IComponentLifecycle {
    override val size: Int
        get() = lifecyclesMap.size

    override val elementIds: MutableSet<Int>
        get() = lifecyclesMap.keys

    val traceLength: Int
        get() = getOneEntry().states.size

    override fun get(elementId: Any): LayersElementLifecycle? {
        return lifecyclesMap[elementId as Int]
    }

    override fun set(elementId: Any, elementLifecycles: IElementLifecycle) {
        lifecyclesMap[elementId as Int] = elementLifecycles as LayersElementLifecycle
    }

    override fun getOrPut(elementId: Any, elementLifecycles: IElementLifecycle): IElementLifecycle {
        return lifecyclesMap.getOrPut(elementId as Int) {
            elementLifecycles as LayersElementLifecycle
        }
    }

    fun getName(): String {
        val elementLifecycle = getOneEntry()
        for (state in elementLifecycle.states) {
            if (state != null) {
                return state.name
            }
        }
        return ""
    }

    /** Get "first" entry in the map */
    fun getOneEntry(): LayersElementLifecycle {
        return lifecyclesMap.values.first()
    }

    override fun equals(other: Any?): Boolean {
        return other is LayersComponentLifecycle && this.lifecyclesMap == other.lifecyclesMap
    }

    override fun hashCode(): Int {
        return lifecyclesMap.hashCode()
    }

    override fun toString(): String {
        return "ComponentMatcherLifecycle:\n$lifecyclesMap\n\n"
    }
}
