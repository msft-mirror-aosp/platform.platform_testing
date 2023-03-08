/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.device.traces.io

import android.tools.common.CrossPlatform
import android.tools.common.IScenario
import android.tools.common.ScenarioBuilder
import android.tools.common.Tag
import android.tools.common.Timestamp
import android.tools.common.io.BUFFER_SIZE
import android.tools.common.io.FLICKER_IO_TAG
import android.tools.common.io.ResultArtifactDescriptor
import android.tools.common.io.RunStatus
import android.tools.common.io.TraceType
import android.tools.common.io.TransitionTimeRange
import android.tools.device.traces.deleteIfExists
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Helper class to create run result artifact files */
open class ResultWriter {
    protected var scenario: IScenario = ScenarioBuilder().createEmptyScenario()
    private var runStatus: RunStatus = RunStatus.UNDEFINED
    private val files = mutableMapOf<ResultArtifactDescriptor, File>()
    private var transitionStartTime = CrossPlatform.timestamp.min()
    private var transitionEndTime = CrossPlatform.timestamp.max()
    private var executionError: Throwable? = null
    private var outputDir: File? = null

    /** Sets the artifact scenario to [_scenario] */
    fun forScenario(_scenario: IScenario) = apply { scenario = _scenario }

    /** Sets the artifact transition start time to [time] */
    fun setTransitionStartTime(time: Timestamp) = apply { transitionStartTime = time }

    /** Sets the artifact transition end time to [time] */
    fun setTransitionEndTime(time: Timestamp) = apply { transitionEndTime = time }

    /** Sets the artifact status as successfully executed transition ([RunStatus.RUN_EXECUTED]) */
    fun setRunComplete() = apply { runStatus = RunStatus.RUN_EXECUTED }

    /** Sets the dir where the artifact file will be stored to [dir] */
    fun withOutputDir(dir: File) = apply { outputDir = dir }

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
     * Adds [artifact] to the result artifact
     *
     * @param traceType used when adding [artifact] to the result artifact
     * @param tag used when adding [artifact] to the result artifact
     */
    fun addTraceResult(traceType: TraceType, artifact: File, tag: String = Tag.ALL) = apply {
        CrossPlatform.log.d(
            FLICKER_IO_TAG,
            "Add trace result file=$artifact type=$traceType tag=$tag scenario=$scenario"
        )
        val fileDescriptor = ResultArtifactDescriptor(traceType, tag)
        files[fileDescriptor] = artifact
    }

    private fun addFile(zipOutputStream: ZipOutputStream, artifact: File, nameInArchive: String) {
        CrossPlatform.log.v(FLICKER_IO_TAG, "Adding $artifact with name $nameInArchive to zip")
        val fi = FileInputStream(artifact)
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
        artifact.deleteIfExists()
    }

    private fun createZipFile(file: File): ZipOutputStream {
        return ZipOutputStream(BufferedOutputStream(FileOutputStream(file), BUFFER_SIZE))
    }

    /** @return writes the result artifact to disk and returns it */
    open fun write(): IResultData {
        return CrossPlatform.log.withTracing("write") {
            val outputDir = outputDir
            requireNotNull(outputDir) { "Output dir not configured" }
            require(!scenario.isEmpty) { "Scenario shouldn't be empty" }
            // Ensure output directory exists
            outputDir.mkdirs()

            if (runStatus == RunStatus.UNDEFINED) {
                CrossPlatform.log.w(FLICKER_IO_TAG, "Writing result with $runStatus run status")
            }

            val newFileName = "${runStatus.prefix}_$scenario.zip"
            val dstFile = outputDir.resolve(newFileName)
            CrossPlatform.log.d(FLICKER_IO_TAG, "Writing artifact file $dstFile")
            createZipFile(dstFile).use { zipOutputStream ->
                files.forEach { (descriptor, artifact) ->
                    addFile(
                        zipOutputStream,
                        artifact,
                        nameInArchive = descriptor.fileNameInArtifact
                    )
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
