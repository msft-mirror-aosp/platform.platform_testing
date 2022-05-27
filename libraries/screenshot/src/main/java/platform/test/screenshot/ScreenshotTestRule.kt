/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.screenshot.matchers.BitmapMatcher
import platform.test.screenshot.matchers.MSSIMMatcher
import platform.test.screenshot.matchers.PixelPerfectMatcher
import platform.test.screenshot.proto.ScreenshotResultProto
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Rule to be added to a test to facilitate screenshot testing.
 *
 * This rule records current test name and when instructed it will perform the given bitmap
 * comparison against the given golden. All the results (including result proto file) are stored
 * into the device to be retrieved later.
 *
 * @param config To configure where this rule should look for goldens.
 * @param outputRootDir The root directory for output files.
 *
 * @see Bitmap.assertAgainstGolden
 */
@SuppressLint("SyntheticAccessor")
open class ScreenshotTestRule(
    val goldenImagePathManager: GoldenImagePathManager
) : TestRule {

    private val imageExtension = ".png"
    private val resultBinaryProtoFileSuffix = "goldResult.pb"
    // This is used in CI to identify the files.
    private val resultProtoFileSuffix = "goldResult.textproto"

    // Magic number for an in-progress status report
    private val bundleStatusInProgress = 2
    private val bundleKeyPrefix = "platform_screenshots_"

    private lateinit var testIdentifier: String
    private lateinit var deviceId: String

    private val testWatcher = object : TestWatcher() {
        override fun starting(description: Description?) {
            testIdentifier = "${description!!.className}_${description.methodName}"
        }
    }

    override fun apply(base: Statement, description: Description?): Statement {
        return ScreenshotTestStatement(base)
            .run { testWatcher.apply(this, description) }
    }

    class ScreenshotTestStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            base.evaluate()
        }
    }

    private fun fetchExpectedImage(goldenIdentifier: String): Bitmap? {
        val instrument = InstrumentationRegistry.getInstrumentation()
        return listOf(
                instrument.targetContext.applicationContext,
                instrument.context
        ).map {
            try {
                it.assets.open(
                    goldenImagePathManager.goldenIdentifierResolver(goldenIdentifier)
                ).use {
                    return@use BitmapFactory.decodeStream(it)
                }
            } catch (e: FileNotFoundException) {
                return@map null
            }
        }.filterNotNull().firstOrNull()
    }

    /**
     * Asserts the given bitmap against the golden identified by the given name.
     *
     * Note: The golden identifier should be unique per your test module (unless you want multiple
     * tests to match the same golden). The name must not contain extension. You should also avoid
     * adding strings like "golden", "image" and instead describe what is the golder referring to.
     *
     * @param actual The bitmap captured during the test.
     * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
     * @param matcher The algorithm to be used to perform the matching.
     *
     * @see MSSIMMatcher
     * @see PixelPerfectMatcher
     * @see Bitmap.assertAgainstGolden
     *
     * @throws IllegalArgumentException If the golden identifier contains forbidden characters or
     * is empty.
     */
    fun assertBitmapAgainstGolden(
        actual: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher
    ) {
        if (!goldenIdentifier.matches("^[A-Za-z0-9_-]+$".toRegex())) {
            throw IllegalArgumentException(
                "The given golden identifier '$goldenIdentifier' does not satisfy the naming " +
                    "requirement. Allowed characters are: '[A-Za-z0-9_-]'"
            )
        }

        val expected = fetchExpectedImage(goldenIdentifier)
        if (expected == null) {
            reportResult(
                status = ScreenshotResultProto.DiffResult.Status.MISSING_REFERENCE,
                goldenIdentifier = goldenIdentifier,
                actual = actual
            )
            throw AssertionError(
                "Missing golden image " +
                    "'${goldenImagePathManager.goldenIdentifierResolver(goldenIdentifier)}'. " +
                    "Did you mean to check in a new image?"
            )
        }

        if (actual.width != expected.width || actual.height != expected.height) {
            reportResult(
                status = ScreenshotResultProto.DiffResult.Status.FAILED,
                goldenIdentifier = goldenIdentifier,
                actual = actual,
                expected = expected
            )
            throw AssertionError(
                "Sizes are different! Expected: [${expected.width}, ${expected
                    .height}], Actual: [${actual.width}, ${actual.height}]"
            )
        }

        val comparisonResult = matcher.compareBitmaps(
            expected = expected.toIntArray(),
            given = actual.toIntArray(),
            width = actual.width,
            height = actual.height
        )

        val status = if (comparisonResult.matches) {
            ScreenshotResultProto.DiffResult.Status.PASSED
        } else {
            ScreenshotResultProto.DiffResult.Status.FAILED
        }

        reportResult(
            status = status,
            goldenIdentifier = goldenIdentifier,
            actual = actual,
            comparisonStatistics = comparisonResult.comparisonStatistics,
            expected = expected,
            diff = comparisonResult.diff
        )

        if (!comparisonResult.matches) {
            throw AssertionError(
                "Image mismatch! Comparison stats: '${comparisonResult
                    .comparisonStatistics}'"
            )
        }
    }

    private fun reportResult(
        status: ScreenshotResultProto.DiffResult.Status,
        goldenIdentifier: String,
        actual: Bitmap,
        comparisonStatistics: ScreenshotResultProto.DiffResult.ComparisonStatistics? = null,
        expected: Bitmap? = null,
        diff: Bitmap? = null
    ) {
        val resultProto = ScreenshotResultProto.DiffResult
            .newBuilder()
            .setResultType(status)
            .addMetadata(
                ScreenshotResultProto.Metadata.newBuilder()
                    .setKey("repoRootPath")
                    .setValue(goldenImagePathManager.deviceLocalPath))

        if (comparisonStatistics != null) {
            resultProto.comparisonStatistics = comparisonStatistics
        }
        resultProto.imageLocationGolden =
            goldenImagePathManager.goldenIdentifierResolver(goldenIdentifier)

        val report = Bundle()

        actual.writeToDevice(OutputFileType.IMAGE_ACTUAL).also {
            resultProto.imageLocationTest = it.name
            report.putString(bundleKeyPrefix + OutputFileType.IMAGE_ACTUAL, it.absolutePath)
        }
        diff?.run {
            writeToDevice(OutputFileType.IMAGE_DIFF).also {
                resultProto.imageLocationDiff = it.name
                report.putString(bundleKeyPrefix + OutputFileType.IMAGE_DIFF, it.absolutePath)
            }
        }
        expected?.run {
            writeToDevice(OutputFileType.IMAGE_EXPECTED).also {
                resultProto.imageLocationReference = it.name
                report.putString(
                    bundleKeyPrefix + OutputFileType.IMAGE_EXPECTED,
                    it.absolutePath
                )
            }
        }

        writeToDevice(OutputFileType.RESULT_PROTO) {
            it.write(resultProto.build().toString().toByteArray())
        }.also {
            report.putString(bundleKeyPrefix + OutputFileType.RESULT_PROTO, it.absolutePath)
        }

        writeToDevice(OutputFileType.RESULT_BIN_PROTO) {
            it.write(resultProto.build().toByteArray())
        }.also {
            report.putString(bundleKeyPrefix + OutputFileType.RESULT_BIN_PROTO, it.absolutePath)
        }

        InstrumentationRegistry.getInstrumentation().sendStatus(bundleStatusInProgress, report)
    }

    internal fun getPathOnDeviceFor(fileType: OutputFileType): File {
        val fileName = when (fileType) {
            OutputFileType.IMAGE_ACTUAL ->
                "${testIdentifier}_actual_$goldenImagePathManager.$imageExtension"
            OutputFileType.IMAGE_EXPECTED ->
                "${testIdentifier}_expected_$goldenImagePathManager.$imageExtension"
            OutputFileType.IMAGE_DIFF ->
                "${testIdentifier}_diff_$goldenImagePathManager.$imageExtension"
            OutputFileType.RESULT_PROTO -> "${testIdentifier}_$resultProtoFileSuffix"
            OutputFileType.RESULT_BIN_PROTO -> "${testIdentifier}_$resultBinaryProtoFileSuffix"
        }
        return File(goldenImagePathManager.deviceLocalPath, fileName)
    }

    private fun Bitmap.writeToDevice(fileType: OutputFileType): File {
        return writeToDevice(fileType) {
            compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, it)
        }
    }

    private fun writeToDevice(
        fileType: OutputFileType,
        writeAction: (FileOutputStream) -> Unit
    ): File {
        val fileGolden = File(goldenImagePathManager.deviceLocalPath)
        if (!fileGolden.exists() && !fileGolden.mkdir()) {
            throw IOException("Could not create folder $fileGolden.")
        }

        var file = getPathOnDeviceFor(fileType)
        try {
            FileOutputStream(file).use {
                writeAction(it)
            }
        } catch (e: Exception) {
            throw IOException(
                "Could not write file to storage (path: ${file.absolutePath}). " +
                    " Stacktrace: " + e.stackTrace
            )
        }
        return file
    }
}

internal fun Bitmap.toIntArray(): IntArray {
    val bitmapArray = IntArray(width * height)
    getPixels(bitmapArray, 0, width, 0, 0, width, height)
    return bitmapArray
}

/**
 * Asserts this bitmap against the golden identified by the given name.
 *
 * Note: The golden identifier should be unique per your test module (unless you want multiple tests
 * to match the same golden). The name must not contain extension. You should also avoid adding
 * strings like "golden", "image" and instead describe what is the golder referring to.
 *
 * @param rule The screenshot test rule that provides the comparison and reporting.
 * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
 * @param matcher The algorithm to be used to perform the matching. By default [MSSIMMatcher]
 * is used.
 *
 * @see MSSIMMatcher
 * @see PixelPerfectMatcher
 */
fun Bitmap.assertAgainstGolden(
    rule: ScreenshotTestRule,
    goldenIdentifier: String,
    matcher: BitmapMatcher = MSSIMMatcher()
) {
    rule.assertBitmapAgainstGolden(this, goldenIdentifier, matcher = matcher)
}

/**
 * Type of file that can be produced by the [ScreenshotTestRule].
 */
internal enum class OutputFileType {
    IMAGE_ACTUAL,
    IMAGE_EXPECTED,
    IMAGE_DIFF,
    RESULT_PROTO,
    RESULT_BIN_PROTO
}
