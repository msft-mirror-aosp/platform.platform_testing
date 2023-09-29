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

import android.annotation.ColorInt
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.platform.uiautomator_helpers.DeviceHelpers.shell
import android.provider.Settings.System
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import androidx.test.runner.screenshot.Screenshot
import com.android.internal.app.SimpleIconFactory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.screenshot.matchers.BitmapMatcher
import platform.test.screenshot.matchers.MSSIMMatcher
import platform.test.screenshot.matchers.PixelPerfectMatcher
import platform.test.screenshot.proto.ScreenshotResultProto
import java.io.PrintStream

/**
 * Rule to be added to a test to facilitate screenshot testing.
 *
 * This rule records current test name and when instructed it will perform the given bitmap
 * comparison against the given golden. All the results (including result proto file) are stored
 * into the device to be retrieved later.
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

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    testIdentifier = getTestIdentifier(description)
                    SimpleIconFactory.setPoolEnabled(false)
                    base.evaluate()
                } finally {
                    SimpleIconFactory.setPoolEnabled(true)
                }
            }
        }

    open fun getTestIdentifier(description: Description): String =
            "${description.className}_${description.methodName}"

    private val isRobolectric = Build.FINGERPRINT.contains("robolectric")
    private fun isGradle(): Boolean =
            java.lang.System.getProperty("java.class.path").contains("gradle-worker.jar")

    private fun writeLocalTempFile(
        fileType: OutputFileType,
        goldenIdentifier: String,
        bitmap: Bitmap?
    ): String? {
        return if (bitmap != null && isGradle() && isRobolectric) {
            val simpleFileName = when (fileType) {
                OutputFileType.IMAGE_ACTUAL -> "local_actual_${goldenIdentifier}.png"
                OutputFileType.IMAGE_DIFF -> "local_diff_${goldenIdentifier}.png"
                OutputFileType.IMAGE_EXPECTED -> "local_golden_${goldenIdentifier}.png"
                OutputFileType.RESULT_BIN_PROTO -> "local_bin_${goldenIdentifier}.pb"
                OutputFileType.RESULT_PROTO -> "local_proto_${goldenIdentifier}.text"
            }
            val file = File("/tmp", simpleFileName)
            val absoluteFilePath = file.absolutePath
            val fOut = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.PNG, 0, fOut)
            fOut.flush()
            fOut.close()
            absoluteFilePath
        } else {
            null
        }
    }

    private fun fetchExpectedImage(goldenIdentifier: String): Bitmap? {
        val instrument = InstrumentationRegistry.getInstrumentation()
        return listOf(
            instrument.targetContext.applicationContext,
            instrument.context
        ).map { context ->
            try {
                context.assets.open(
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
    @Deprecated("use the ScreenshotTestRuleAsserter")
    fun assertBitmapAgainstGolden(
        actual: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher
    ) {
        assertBitmapAgainstGolden(
            actual = actual,
            goldenIdentifier = goldenIdentifier,
            matcher = matcher,
            regions = emptyList<Rect>()
        )
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
     * @param regions An optional array of interesting regions for partial screenshot diff.
     *
     * @see MSSIMMatcher
     * @see PixelPerfectMatcher
     * @see Bitmap.assertAgainstGolden
     *
     * @throws IllegalArgumentException If the golden identifier contains forbidden characters or
     * is empty.
     */
    @Deprecated("use the ScreenshotTestRuleAsserter")
    fun assertBitmapAgainstGolden(
        actual: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher,
        regions: List<Rect>
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
                assetsPathRelativeToRepo = goldenImagePathManager.assetsPathRelativeToBuildRoot,
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
                assetsPathRelativeToRepo = goldenImagePathManager.assetsPathRelativeToBuildRoot,
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
            height = actual.height,
            regions = regions
        )

        val status = if (comparisonResult.matches) {
            ScreenshotResultProto.DiffResult.Status.PASSED
        } else {
            ScreenshotResultProto.DiffResult.Status.FAILED
        }

        if (!comparisonResult.matches) {
            reportResult(
                status = status,
                assetsPathRelativeToRepo = goldenImagePathManager.assetsPathRelativeToBuildRoot,
                goldenIdentifier = goldenIdentifier,
                actual = actual,
                comparisonStatistics = comparisonResult.comparisonStatistics,
                expected = highlightedBitmap(expected, regions),
                diff = comparisonResult.diff
            )
            throw AssertionError(
                "Image mismatch! Comparison stats: '${comparisonResult
                    .comparisonStatistics}'"
            )
        }
    }

    private fun reportResult(
        status: ScreenshotResultProto.DiffResult.Status,
        assetsPathRelativeToRepo: String,
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
                    .setValue(goldenImagePathManager.deviceLocalPath)
            )

        if (comparisonStatistics != null) {
            resultProto.comparisonStatistics = comparisonStatistics
        }

        val pathRelativeToAssets =
            goldenImagePathManager.goldenIdentifierResolver(goldenIdentifier)
        resultProto.imageLocationGolden = "$assetsPathRelativeToRepo/$pathRelativeToAssets"

        val report = Bundle()

        actual.writeToDevice(OutputFileType.IMAGE_ACTUAL, goldenIdentifier).also {
            resultProto.imageLocationTest = it.name
            report.putString(bundleKeyPrefix + OutputFileType.IMAGE_ACTUAL, it.absolutePath)
        }
        diff?.run {
            writeToDevice(OutputFileType.IMAGE_DIFF, goldenIdentifier).also {
                resultProto.imageLocationDiff = it.name
                report.putString(bundleKeyPrefix + OutputFileType.IMAGE_DIFF, it.absolutePath)
            }
        }
        expected?.run {
            writeToDevice(OutputFileType.IMAGE_EXPECTED, goldenIdentifier).also {
                resultProto.imageLocationReference = it.name
                report.putString(
                    bundleKeyPrefix + OutputFileType.IMAGE_EXPECTED,
                    it.absolutePath
                )
            }
        }

        writeToDevice(OutputFileType.RESULT_PROTO, goldenIdentifier) {
            it.write(resultProto.build().toString().toByteArray())
        }.also {
            report.putString(bundleKeyPrefix + OutputFileType.RESULT_PROTO, it.absolutePath)
        }

        writeToDevice(OutputFileType.RESULT_BIN_PROTO, goldenIdentifier) {
            it.write(resultProto.build().toByteArray())
        }.also {
            report.putString(bundleKeyPrefix + OutputFileType.RESULT_BIN_PROTO, it.absolutePath)
        }

        InstrumentationRegistry.getInstrumentation().sendStatus(bundleStatusInProgress, report)

        if (isGradle() && isRobolectric) {
            val actualFilePath = writeLocalTempFile(
                    fileType = OutputFileType.IMAGE_ACTUAL,
                    goldenIdentifier = goldenIdentifier,
                    bitmap = actual
            )
            val expectedFilePath = writeLocalTempFile(
                    fileType = OutputFileType.IMAGE_EXPECTED,
                    goldenIdentifier = goldenIdentifier,
                    bitmap = expected
            )
            val diffFilePath = writeLocalTempFile(
                    fileType = OutputFileType.IMAGE_DIFF,
                    goldenIdentifier = goldenIdentifier,
                    bitmap = diff
            )


            val file = File("/tmp", "${goldenIdentifier}.html")
            println("file://${file.absolutePath}")
            val fOut = FileOutputStream(file)
            val printStream = PrintStream(fOut)
            printStream.println("<!DOCTYPE html>")
            printStream.println("<meta charset=\"utf-8\">")
            printStream.println("<title>${goldenIdentifier}</title>")
            printStream.println("<p><h1>${testIdentifier}</h1></p>")
            if (expectedFilePath != null) {
                printStream.println(
                    "<p><h2><a href=\"file://${expectedFilePath}\">Expected</a></h2>" +
                        "<img src=\"local_golden_${goldenIdentifier}.png\" alt=\"Golden\"></p>")
            }
            if (actualFilePath != null) {
                printStream.println(
                    "<p><h2><a href=\"file://${actualFilePath}\">Actual</a></h2>" +
                        "<img src=\"local_actual_${goldenIdentifier}.png\" alt=\"Actual\"></p>")
            }

            if (diffFilePath != null) {
                printStream.println(
                    "<p><h2><a href=\"file://${diffFilePath}\">Diff</a></h2>" +
                        "<img src=\"local_diff_${goldenIdentifier}.png\" alt=\"Diff\"></p>")
            }
            printStream.flush()
            printStream.close()
        }
    }

    internal fun getPathOnDeviceFor(fileType: OutputFileType, goldenIdentifier: String): File {
        val imageSuffix = getOnDeviceImageSuffix(goldenIdentifier)
        val protoSuffix = getOnDeviceArtifactsSuffix(goldenIdentifier, resultProtoFileSuffix)
        val binProtoSuffix =
            getOnDeviceArtifactsSuffix(goldenIdentifier, resultBinaryProtoFileSuffix)
        val succinctTestIdentifier = getSuccinctTestIdentifier(testIdentifier)
        val fileName = when (fileType) {
            OutputFileType.IMAGE_ACTUAL ->
                "${succinctTestIdentifier}_actual_$imageSuffix"
            OutputFileType.IMAGE_EXPECTED ->
                "${succinctTestIdentifier}_expected_$imageSuffix"
            OutputFileType.IMAGE_DIFF ->
                "${succinctTestIdentifier}_diff_$imageSuffix"
            OutputFileType.RESULT_PROTO ->
                "${succinctTestIdentifier}_$protoSuffix"
            OutputFileType.RESULT_BIN_PROTO ->
                "${succinctTestIdentifier}_$binProtoSuffix"
        }
        return File(goldenImagePathManager.deviceLocalPath, fileName)
    }

    open fun getOnDeviceImageSuffix(goldenIdentifier: String): String {
        val resolvedGoldenIdentifier =
            goldenImagePathManager.goldenIdentifierResolver(goldenIdentifier)
                .replace('/', '_')
                .replace(imageExtension, "")
        return "$resolvedGoldenIdentifier$imageExtension"
    }

    open fun getOnDeviceArtifactsSuffix(goldenIdentifier: String, suffix: String): String {
        val resolvedGoldenIdentifier =
            goldenImagePathManager.goldenIdentifierResolver(goldenIdentifier)
                .replace('/', '_')
                .replace(imageExtension, "")
        return "${resolvedGoldenIdentifier}_$suffix"
    }

    open fun getSuccinctTestIdentifier(identifier: String): String {
        val pattern = Regex("\\[([A-Za-z0-9_]+)\\]")
        return pattern.replace(identifier, "")
    }

    private fun Bitmap.writeToDevice(fileType: OutputFileType, goldenIdentifier: String): File {
        return writeToDevice(fileType, goldenIdentifier) {
            compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, it)
        }
    }

    private fun writeToDevice(
        fileType: OutputFileType,
        goldenIdentifier: String,
        writeAction: (FileOutputStream) -> Unit
    ): File {
        val fileGolden = File(goldenImagePathManager.deviceLocalPath)
        if (!fileGolden.exists() && !fileGolden.mkdirs()) {
            throw IOException("Could not create folder $fileGolden.")
        }

        val file = getPathOnDeviceFor(fileType, goldenIdentifier)
        if (!file.exists()) {
            // file typically exists when in one test, the same golden image was repeatedly
            // compared with. In this scenario, multiple actual/expected/diff images with same
            // names will be attempted to write to the device.
            try {
                FileOutputStream(file).use {
                    writeAction(it)
                }
            } catch (e: Exception) {
                throw IOException(
                        "Could not write file to storage (path: ${file.absolutePath}). ", e)
            }
        }

        return file
    }

    /** This will create a new Bitmap with the output (not modifying the [original] Bitmap */
    private fun highlightedBitmap(original: Bitmap, regions: List<Rect>): Bitmap {
        if (regions.isEmpty()) return original

        val outputBitmap = original.copy(original.config!!, true)
        val imageRect = Rect(0, 0, original.width, original.height)
        val regionLineWidth = 2
        for (region in regions) {
            val regionToDraw = Rect(region)
                    .apply {
                        inset(-regionLineWidth, -regionLineWidth)
                        intersect(imageRect)
                    }

            repeat(regionLineWidth) {
                drawRectOnBitmap(outputBitmap, regionToDraw, Color.RED)
                regionToDraw.inset(1, 1)
                regionToDraw.intersect(imageRect)
            }
        }
        return outputBitmap
    }

    private fun drawRectOnBitmap(bitmap: Bitmap, rect: Rect, @ColorInt color: Int) {
        // Draw top and bottom edges
        for (x in rect.left until rect.right) {
            bitmap.setPixel(x, rect.top, color)
            bitmap.setPixel(x, rect.bottom - 1, color)
        }
        // Draw left and right edge
        for (y in rect.top until rect.bottom) {
            bitmap.setPixel(rect.left, y, color)
            bitmap.setPixel(rect.right - 1, y, color)
        }
    }
}

typealias BitmapSupplier = () -> Bitmap

/**
 * Implements a screenshot asserter based on the ScreenshotRule
 */
class ScreenshotRuleAsserter private constructor(
    private val rule: ScreenshotTestRule
) : ScreenshotAsserter {
    // use the most constraining matcher as default
    private var matcher: BitmapMatcher = PixelPerfectMatcher()
    private var beforeScreenshot: Runnable? = null
    private var afterScreenshot: Runnable? = null

    // use the instrumentation screenshot as default
    private var screenShotter: BitmapSupplier = { Screenshot.capture().bitmap }

    private var pointerLocationSetting: Int
        get() = shell("settings get system ${System.POINTER_LOCATION}").trim().toIntOrNull() ?: 0
        set(value) { shell("settings put system ${System.POINTER_LOCATION} $value") }

    private var showTouchesSetting
        get() = shell("settings get system ${System.SHOW_TOUCHES}").trim().toIntOrNull() ?: 0
        set(value) { shell("settings put system ${System.SHOW_TOUCHES} $value") }

    private var prevPointerLocationSetting: Int? = null
    private var prevShowTouchesSetting: Int? = null
    override fun assertGoldenImage(goldenId: String) {
        runBeforeScreenshot()
        try {
            rule.assertBitmapAgainstGolden(screenShotter(), goldenId, matcher)
        } finally {
            runAfterScreenshot()
        }
    }

    override fun assertGoldenImage(goldenId: String, areas: List<Rect>) {
        runBeforeScreenshot()
        try {
            rule.assertBitmapAgainstGolden(screenShotter(), goldenId, matcher, areas)
        } finally {
            runAfterScreenshot()
        }
    }

    private fun runBeforeScreenshot() {
        prevPointerLocationSetting = pointerLocationSetting
        prevShowTouchesSetting = showTouchesSetting

        if (prevPointerLocationSetting != 0) pointerLocationSetting = 0
        if (prevShowTouchesSetting != 0) showTouchesSetting = 0

        beforeScreenshot?.run()
    }

    private fun runAfterScreenshot() {
        afterScreenshot?.run()

        prevPointerLocationSetting?.let { pointerLocationSetting = it }
        prevShowTouchesSetting?.let { showTouchesSetting = it }
    }

    class Builder(private val rule: ScreenshotTestRule) {
        private var asserter = ScreenshotRuleAsserter(rule)
        fun withMatcher(matcher: BitmapMatcher): Builder = apply { asserter.matcher = matcher }

        fun setScreenshotProvider(screenshotProvider: BitmapSupplier): Builder =
                apply { asserter.screenShotter = screenshotProvider }

        fun setOnBeforeScreenshot(run: Runnable): Builder =
                apply { asserter.beforeScreenshot = run }

        fun setOnAfterScreenshot(run: Runnable): Builder = apply { asserter.afterScreenshot = run }

        fun build(): ScreenshotAsserter = asserter.also { asserter = ScreenshotRuleAsserter(rule) }
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
    matcher: BitmapMatcher = MSSIMMatcher(),
    regions: List<Rect> = emptyList()
) {
    rule.assertBitmapAgainstGolden(this, goldenIdentifier, matcher = matcher, regions = regions)
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
