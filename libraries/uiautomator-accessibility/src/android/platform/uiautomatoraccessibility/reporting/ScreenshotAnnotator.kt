/*
 * Copyright (C) 2026 The Android Open Source Project
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
package android.platform.uiautomatoraccessibility.reporting

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult

/** Annotates screenshots with accessibility check results. */
class ScreenshotAnnotator(context: Context) {

    private val paint =
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

    private val textPaint =
        Paint().apply {
            color = Color.WHITE
            textSize =
                TypedValue.convertDimensionToPixels(
                    TypedValue.COMPLEX_UNIT_SP,
                    14f,
                    context.resources.displayMetrics,
                )
            isAntiAlias = true
        }

    private val bgPaint = Paint().apply { style = Paint.Style.FILL }

    /**
     * Draws rectangles around accessibility check results on the screenshot.
     *
     * @param screenshot to annotate
     * @param results the results to draw
     * @return a new bitmap containing the modifications
     */
    fun annotateScreenshot(
        screenshot: Bitmap,
        results: List<AccessibilityHierarchyCheckResult>,
    ): Bitmap {
        if (results.isEmpty()) return screenshot

        val annotatedBitmap = screenshot.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotatedBitmap)

        results.forEachIndexed { index, result ->
            val element = result.element
            if (element != null) {
                val color = colors[index % colors.size]
                paint.color = color
                bgPaint.color = color

                val bounds = element.boundsInScreen.run { Rect(left, top, right, bottom) }
                canvas.drawRect(bounds, paint)

                // Draw a text label with a color background to the right of the element
                val label = index.toString()
                val labelBg = Rect()
                textPaint.getTextBounds(label, 0, label.length, labelBg)
                labelBg.inset(-PADDING, -PADDING)
                labelBg.offset(
                    bounds.right + paint.strokeWidth.toInt(),
                    bounds.bottom - paint.strokeWidth.toInt(),
                )

                canvas.drawRect(labelBg, bgPaint)
                canvas.drawText(
                    label,
                    (labelBg.left + PADDING).toFloat(),
                    (labelBg.bottom - PADDING).toFloat(),
                    textPaint,
                )
            }
        }
        return annotatedBitmap
    }

    companion object {
        private const val PADDING = 5
        private val colors =
            listOf(
                Color.rgb(255, 165, 0), // Orange
                Color.MAGENTA,
                Color.GREEN,
            )
    }
}
