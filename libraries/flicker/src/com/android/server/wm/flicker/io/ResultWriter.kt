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

package com.android.server.wm.flicker.io

import android.util.Log
import com.android.server.wm.flicker.AssertionTag
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.ScenarioBuilder
import com.android.server.wm.flicker.now
import com.android.server.wm.traces.common.IScenario
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.parser.withPerfettoTrace
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Helper class to create run result artifact files */
open class ResultWriter {
    protected var scenario: IScenario = ScenarioBuilder().createEmptyScenario()
    private var runStatus: RunStatus = RunStatus.UNDEFINED
    private val files = mutableMapOf<ResultArtifactDescriptor, File>()
    private var transitionStartTime = Timestamp.MIN
    private var transitionEndTime = Timestamp.MAX
    private var executionError: Throwable? = null
    private var outputDir: Path? = null

    /** Sets the artifact scenario to [_scenario] */
    fun forScenario(_scenario: IScenario) = apply { scenario = _scenario }

    /** Sets the artifact transition start time to [time] */
    fun setTransitionStartTime(time: Timestamp = now()) = apply { transitionStartTime = time }

    /** Sets the artifact transition end time to [time] */
    fun setTransitionEndTime(time: Timestamp = now()) = apply { transitionEndTime = time }

    /** Sets the artifact status as successfully executed transition ([RunStatus.RUN_EXECUTED]) */
    fun setRunComplete() = apply { runStatus = RunStatus.RUN_EXECUTED }

    /** Sets the dir where the artifact file will be stored to [path] */
    fun withOutputDir(path: Path) = apply { outputDir = path }

    /**
     * Sets the artifact status as failed executed transition ([RunStatus.RUN_FAILED])
     *
     * @param error that caused the transition to fail
     */
    fun setRunFailed(error: Throwable) = apply {
        runStatus = RunStatus.RUN_FAILED
        executionError = error
    }

    /**
     * Adds [file] to the result artifact
     *
     * @param traceType used when adding [file] to the result artifact
     * @param tag used when adding [file] to the result artifact
     */
    fun addTraceResult(traceType: TraceType, file: File, tag: String = AssertionTag.ALL) = apply {
        Log.d(
            FLICKER_IO_TAG,
            "Add trace result file=$file type=$traceType tag=$tag scenario=$scenario"
        )
        val fileDescriptor = ResultArtifactDescriptor(traceType, tag)
        files[fileDescriptor] = file
    }

    private fun addFile(zipOutputStream: ZipOutputStream, file: File, nameInArchive: String) {
        Log.v(FLICKER_IO_TAG, "Adding $file with name $nameInArchive to zip")
        val fi = FileInputStream(file)
        val inputStream = BufferedInputStream(fi, BUFFER_SIZE)
        inputStream.use {
            val entry = ZipEntry(nameInArchive)
            zipOutputStream.putNextEntry(entry)
            val data = ByteArray(BUFFER_SIZE)
            var count: Int = it.read(data, 0, BUFFER_SIZE)
            while (count != -1) {
                zipOutputStream.write(data, 0, count)
                count = it.read(data, 0, BUFFER_SIZE)
            }
        }
        zipOutputStream.closeEntry()
        file.delete()
    }

    private fun createZipFile(file: File): ZipOutputStream {
        return ZipOutputStream(BufferedOutputStream(FileOutputStream(file), BUFFER_SIZE))
    }

    /** @return writes the result artifact to disk and returns it */
    open fun write(): ResultData {
        return withPerfettoTrace("write") {
            val outputDir = outputDir
            requireNotNull(outputDir) { "Output dir not configured" }
            require(!scenario.isEmpty) { "Scenario shouldn't be empty" }
            // Ensure output directory exists
            outputDir.toFile().mkdirs()

            if (runStatus == RunStatus.UNDEFINED) {
                Log.w(FLICKER_IO_TAG, "Writing result with $runStatus run status")
            }

            val newFileName = "${runStatus.prefix}_$scenario.zip"
            val dstFile = outputDir.resolve(newFileName)
            Log.d(FLICKER_IO_TAG, "Writing artifact file $dstFile")
            createZipFile(dstFile.toFile()).use { zipOutputStream ->
                files.forEach { (descriptor, file) ->
                    addFile(zipOutputStream, file, nameInArchive = descriptor.fileNameInArtifact)
                }
            }

            ResultData(
                dstFile,
                TransitionTimeRange(transitionStartTime, transitionEndTime),
                executionError,
                runStatus
            )
        }
    }
}
