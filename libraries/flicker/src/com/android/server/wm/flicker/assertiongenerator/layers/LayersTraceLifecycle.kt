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

import com.android.server.wm.flicker.assertiongenerator.common.IElementLifecycle
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle

class LayersTraceLifecycle(
    val lifecycleMap: MutableMap<Int, LayersElementLifecycle> = mutableMapOf()
) : ITraceLifecycle {
    override val size: Int
        get() = lifecycleMap.size

    override val elementIds: Set<Int>
        get() = lifecycleMap.keys

    override operator fun get(elementId: Any): LayersElementLifecycle? {
        return lifecycleMap[elementId as Int]
    }

    // can't have LayersElementLifecycle because
    // compiler won't allow different types in the function signature
    override operator fun set(elementId: Any, elementLifecycle: IElementLifecycle) {
        lifecycleMap[elementId as Int] = elementLifecycle as LayersElementLifecycle
    }

    override fun getOrPut(elementId: Any, elementLifecycle: IElementLifecycle):
        LayersElementLifecycle {
        return lifecycleMap.getOrPut(elementId as Int){elementLifecycle as LayersElementLifecycle}
    }
}
