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

class WmComponentLifecycle(
    val lifecyclesMap: MutableMap<String, WmElementLifecycle> = mutableMapOf()
) : IComponentLifecycle {
    override val size: Int
        get() = lifecyclesMap.size

    override val elementIds: MutableSet<String>
        get() = lifecyclesMap.keys

    val traceLength: Int
        get() = getOneEntry().states.size

    override fun get(elementId: Any): WmElementLifecycle? {
        return lifecyclesMap[elementId as String]
    }

    override fun set(elementId: Any, elementLifecycles: IElementLifecycle) {
        lifecyclesMap[elementId as String] = elementLifecycles as WmElementLifecycle
    }

    override fun getOrPut(elementId: Any, elementLifecycles: IElementLifecycle): IElementLifecycle {
        return lifecyclesMap.getOrPut(elementId as String) {
            elementLifecycles as WmElementLifecycle
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

    /** @inheritDoc */
    override fun getOneEntry(): WmElementLifecycle {
        return lifecyclesMap.values.first()
    }

    override fun equals(other: Any?): Boolean {
        return other is WmComponentLifecycle && this.lifecyclesMap == other.lifecyclesMap
    }

    override fun hashCode(): Int {
        return lifecyclesMap.hashCode()
    }

    override fun toString(): String {
        return "WmComponentMatcherLifecycle:\n$lifecyclesMap\n\n"
    }
}
