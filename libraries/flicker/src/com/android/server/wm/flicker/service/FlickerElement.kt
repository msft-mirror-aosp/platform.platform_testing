/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.service

/**
 * Base class for common flicker components (WM and SF state) and saves the
 * list of flicker tags.
 */
abstract class FlickerElement {
    private val tags = mutableSetOf<TransitionTag>()

    fun getTags(): Set<TransitionTag> {
        return tags
    }

    fun addTag(tag: TransitionTag) {
        tags.add(tag)
    }
}
