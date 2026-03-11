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

import android.graphics.Bitmap
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.runner.Description

/** Writes accessibility check results and artifacts to disk. */
class ResultsWriter {
    private val timeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss.SSSX").withZone(ZoneOffset.UTC)

    private var testName: String = "SUITE.EntireClassExecution"
    private val annotator =
        ScreenshotAnnotator(InstrumentationRegistry.getInstrumentation().targetContext)

    /** Sets the test description to be used for naming output files. */
    fun setTestDescription(description: Description) {
        testName = getClassAndMethodName(description)
    }

    /**
     * Saves a report with the screenshot and accessibility hierarchy to disk.
     *
     * @param element The root element of the hierarchy.
     * @param results The list of check results
     * @param screenshot The screenshot to save, or null if none.
     */
    fun saveResults(
        element: AccessibilityNodeInfo,
        results: List<AccessibilityHierarchyCheckResult>,
        screenshot: Bitmap?,
    ) {
        val severeResults =
            results.filter {
                it.type == AccessibilityCheckResult.AccessibilityCheckResultType.ERROR
            }
        if (severeResults.isEmpty()) return

        val timestamp = timeFormatter.format(Instant.now())

        // Save screenshot with all results overlaid on top
        if (screenshot != null) {
            val screenshotFile = artifactFile(timestamp, "screenshot", "png")
            try {
                val annotatedScreenshot = annotator.annotateScreenshot(screenshot, severeResults)
                FileOutputStream(screenshotFile).use { out ->
                    if (!annotatedScreenshot.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw IOException("Failed to compress bitmap")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save screenshot", e)
            }
        }

        // Dump accessibility hierarchy
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.dumpWindowHierarchy(artifactFile(timestamp, "hierarchy", "uix"))
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save accessibility hierarchy", e)
        }
    }

    private fun artifactFile(timestamp: String, suffix: String, ext: String): File {
        val fileName = "accessibilityCheck-$testName-$timestamp-$suffix.$ext"
        return File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, fileName)
    }

    companion object {
        private const val TAG = "ResultsWriter"

        private fun getClassAndMethodName(description: Description): String {
            val methodName = description.methodName ?: "EntireClassExecution"
            val testClass = description.testClass
            val className = testClass?.simpleName ?: "SUITE"
            return "$className.$methodName"
        }
    }
}
