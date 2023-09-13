/*
 * Copyright 2023 The Android Open Source Project
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
package platform.test.screenshot

import android.graphics.Bitmap

private val LIGHT_COLOR_MAPPING =
    arrayOf(
        -11640468 to -15111342,
        -986893 to -1641480,
    )

private val DARK_COLOR_MAPPING =
    arrayOf(-11640468 to -8128307, -986893 to -15590111, -65536 to -8128307)

/**
 * Perform a Material You Color simulation for [originalBitmap] and return a bitmap after Material
 * You simulation.
 */
fun bitmapWithMaterialYouColorsSimulation(originalBitmap: Bitmap, isDarkTheme: Boolean): Bitmap {
    val bitmapArray = originalBitmap.toIntArray()
    val mappingToUse =
        if (isDarkTheme) {
            DARK_COLOR_MAPPING
        } else {
            LIGHT_COLOR_MAPPING
        }
    for (i in 0..originalBitmap.height - 1) {
        for (j in 0..originalBitmap.width - 1) {
            val pixelValue = bitmapArray[j + i * originalBitmap.width]
            for (k in mappingToUse) {
                if (pixelValue == k.first) {
                    bitmapArray[j + i * originalBitmap.width] = k.second
                    break
                }
            }
        }
    }

    return Bitmap.createBitmap(
        bitmapArray,
        originalBitmap.width,
        originalBitmap.height,
        originalBitmap.config
    )
}
