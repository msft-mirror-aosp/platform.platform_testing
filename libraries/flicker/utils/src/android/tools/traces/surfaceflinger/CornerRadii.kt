/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.tools.traces.surfaceflinger

import android.tools.withCache

/**
 * Wrapper for CornerRadiiProto
 * (external/perfetto/protos/perfetto/trace/android/surfaceflinger_layers.proto)
 *
 * This class is used by Flicker
 */
data class CornerRadii constructor(val tl: Float, val tr: Float, val bl: Float, val br: Float) {
    fun isEmpty(): Boolean {
        return tl == 0f && tr == 0f && bl == 0f && br == 0f
    }

    companion object {
        val EMPTY: CornerRadii
            get() = withCache { CornerRadii(0f, 0f, 0f, 0f) }

        @JvmStatic
        fun from(radius: Float): CornerRadii = withCache {
            CornerRadii(radius, radius, radius, radius)
        }
    }
}
