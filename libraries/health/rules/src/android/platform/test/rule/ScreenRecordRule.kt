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
package android.platform.test.rule

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.platform.test.rule.ScreenRecordRule.Companion.SCREEN_RECORDING_ALWAYS_ENABLED_KEY
import android.platform.uiautomator_helpers.DeviceHelpers.shell
import android.platform.uiautomator_helpers.DeviceHelpers.uiDevice
import android.platform.uiautomator_helpers.WaitUtils.ensureThat
import android.util.Log
import androidx.test.InstrumentationRegistry.getInstrumentation
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.nio.file.Files
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule which captures a screen record for a test.
 *
 * After adding this rule to the test class either:
 *
 * - apply the annotation [@ScreenRecord] to individual tests or
 * - pass the [SCREEN_RECORDING_ALWAYS_ENABLED_KEY] instrumentation argument. e.g. `adb shell am
 * instrument -w -e screen-recording-always-enabled true <test>`).
 */
class ScreenRecordRule : TestRule {

    private val automation: UiAutomation = getInstrumentation().uiAutomation

    override fun apply(base: Statement, description: Description): Statement {
        if (!shouldRecordScreen(description)) {
            log("Not recording the screen.")
            return base
        }
        return object : Statement() {
            override fun evaluate() {
                runWithRecording(description) { base.evaluate() }
            }
        }
    }

    private fun shouldRecordScreen(description: Description): Boolean {
        return description.getAnnotation(ScreenRecord::class.java) != null ||
            screenRecordOverrideEnabled()
    }

    /**
     * This is needed to enable screen recording when a parameter is passed to the instrumentation,
     * avoid having to recompile the test.
     */
    private fun screenRecordOverrideEnabled(): Boolean {
        val args = InstrumentationRegistry.getArguments()
        val override = args.getString(SCREEN_RECORDING_ALWAYS_ENABLED_KEY, "false").toBoolean()
        if (override) {
            log("Screen recording enabled due to $SCREEN_RECORDING_ALWAYS_ENABLED_KEY param.")
        }
        return override
    }

    private fun runWithRecording(description: Description?, runnable: () -> Unit) {
        val outputFile = ArtifactSaver.artifactFile(description, "ScreenRecord", "mp4")
        log("Executing test with screen recording. Output file=$outputFile")

        uiDevice.shell("killall screenrecord")
        // --bugreport adds the timestamp as overlay
        val screenRecordingFileDescriptor =
            automation.executeShellCommand("screenrecord --verbose --bugreport $outputFile")
        val screenRecordPid = uiDevice.shell("pidof screenrecord")
        try {
            runnable()
        } finally {
            ensureThat("Recording output created") { outputFile.exists() }
            val killOutput = uiDevice.shell("kill -INT $screenRecordPid")
            val screenRecordOutput = screenRecordingFileDescriptor.readAllAndClose()
            log(
                """
                screenrecord killed (kill command output="$killOutput")
                Screen recording captured at: $outputFile
                File size: ${Files.size(outputFile.toPath()) / 1024} KB
                screenrecord command output:

                """.trimIndent() +
                    screenRecordOutput.prependIndent("   ")
            )
        }
        // automation.executeShellCommand("rm $outputFile")
    }

    /** Interface to indicate that the test should capture screenrecord */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
    annotation class ScreenRecord

    private fun log(s: String) = Log.d(TAG, s)

    // Reads all from the stream and closes it.
    private fun ParcelFileDescriptor.readAllAndClose(): String =
        AutoCloseInputStream(this).use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }

    companion object {
        private const val TAG = "ScreenRecordRule"
        private const val SCREEN_RECORDING_ALWAYS_ENABLED_KEY = "screen-recording-always-enabled"
    }
}
