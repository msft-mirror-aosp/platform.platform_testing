/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker.common.traces.layers

import com.android.server.wm.flicker.common.RectF
import com.android.server.wm.flicker.common.Region

interface ILayer<LayerT : ILayer<LayerT>> {
    val name: String
    val id: Int
    val parentId: Int
    val visibleRegion: Region
    val z: Int
    val sourceBounds: RectF?
    val transform: Transform

    var parent: LayerT
    val children: MutableCollection<LayerT>
    val occludedBy: MutableCollection<LayerT>
    val partiallyOccludedBy: MutableCollection<LayerT>
    val coveredBy: MutableCollection<LayerT>
    val isVisible: Boolean
    val isHiddenByParent: Boolean
    val isOpaque: Boolean
    val hiddenByParentReason: String
    val isInvisible: Boolean
    val visibilityReason: String
    val isHiddenByPolicy: Boolean
    val screenBounds: RectF

    fun addChild(childLayer: LayerT)
    fun contains(innerLayer: LayerT): Boolean
    fun overlaps(other: LayerT): Boolean
}