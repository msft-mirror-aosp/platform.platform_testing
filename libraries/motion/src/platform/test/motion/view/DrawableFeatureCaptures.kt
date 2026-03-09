/*
 * Copyright (C) 2024 The Android Open Source Project
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

package platform.test.motion.view

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import platform.test.motion.golden.FeatureCapture
import platform.test.motion.golden.dataPointType

/** Common, generic [FeatureCapture] implementations for [Drawable]s. */
object DrawableFeatureCaptures {
    val bounds = FeatureCapture<Drawable, Rect>("bounds", DataPointTypes.rect) { it.bounds }
    val alpha = FeatureCapture<Drawable, Int>("alpha", Int.dataPointType) { it.alpha }
    val cornerRadii =
        FeatureCapture<GradientDrawable, CornerRadii>("cornerRadii", DataPointTypes.cornerRadii) {
            it.cornerRadii?.let { rawValues -> CornerRadii(rawValues) }
        }
}

/**
 * Wrapper type for [GradientDrawable.getCornerRadii], to provide equality checks on [rawValues].
 *
 * TODO(b/322324387): This is temporary until figuring out how to best implement custom equality
 *   checks.
 */
class CornerRadii(rawValues: FloatArray) {
    val rawValues = rawValues.clone()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val otherRawValues = (other as? CornerRadii)?.rawValues ?: return false
        return rawValues.contentEquals(otherRawValues)
    }

    override fun hashCode(): Int {
        return rawValues.contentHashCode()
    }
}
