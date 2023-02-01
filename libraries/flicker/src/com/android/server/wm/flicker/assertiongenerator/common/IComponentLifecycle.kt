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

package com.android.server.wm.flicker.assertiongenerator.common

interface IComponentLifecycle {
    val size: Int

    val elementIds: Set<Any> // needs to be int for sf, string for wm

    operator fun get(elementId: Any): IElementLifecycle?

    operator fun set(elementId: Any, elementLifecycles: IElementLifecycle)

    fun getOrPut(elementId: Any, elementLifecycles: IElementLifecycle): IElementLifecycle

    /** Get "first" entry in the map */
    fun getOneEntry(): IElementLifecycle
}
