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

package com.android.server.wm.flicker

import android.device.collectors.BaseMetricListener
import android.device.collectors.DataRecord
import android.device.collectors.util.SendToInstrumentation
import android.os.Bundle
import android.util.Log
import java.nio.file.Path
import org.junit.runner.Description
import org.junit.runner.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Notify that trace files are ready to be pulled
 *
 * This is used to asynchronously fetch files during testing, instead of fetching all
 * files once the test is over
 *
 * In the flicker tests it is not possible to use the async mechanism from
 * [com.android.tradefed.device.metric.FilePullerLogCollector] because it either:
 *   (1) pushes the files form the directory multiple times -- once per test -- creating many
 *   duplicates; or
 *   (2) cleans up the directory without checking for new files, losing some files.
 *
 * This class doesn't ensure all files will be uploaded. It is recommended to additionally use a
 * [com.android.tradefed.device.metric.FilePullerLogCollector] to collect the remaining files at
 * the end.
 */
class TraceFileReadyListener : BaseMetricListener() {
    /**
     * At the start of a test, notifies the host side runner about any existing files to fetch.
     * The files are created by flicker after the previous test finishes and after [onTestEnd]
     * is invoked.
     *
     * This way, while a new test starts interacting with the device, the trace files can be
     * uploaded to the host side.
     */
    override fun onTestStart(testData: DataRecord, description: Description) {
        notifyFilesReady()
        super.onTestStart(testData, description)
    }

    override fun onTestRunEnd(runData: DataRecord, result: Result) {
        notifyFilesReady()
        super.onTestRunEnd(runData, result)
    }

    private fun notifyFilesReady() {
        val bundle = Bundle()
        var hasValues = false
        traceFiles.forEach { path ->
            Log.v(LOG_TAG, "Notifying file ready: $path")
            val fileName = path.fileName.toString()
            bundle.putString(fileName, path.toString())
            hasValues = true
        }
        traceFiles.clear()

        if (hasValues) {
            tasks.add(
                coroutineScope.async {
                    SendToInstrumentation.sendBundle(getInstrumentation(), bundle)
                }
            )
        }
    }

    companion object {
        private val LOG_TAG = this::class.java.simpleName
        private val traceFiles = mutableListOf<Path>()
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val tasks = mutableListOf<Deferred<*>>()

        internal fun notifyFileReady(file: Path) {
            val absPath = file.toAbsolutePath()
            Log.v(LOG_TAG, "Add ready file to queue $absPath")
            traceFiles.add(absPath)
        }
    }
}