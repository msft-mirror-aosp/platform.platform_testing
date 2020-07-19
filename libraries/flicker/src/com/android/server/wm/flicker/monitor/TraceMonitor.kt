/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker.monitor

import androidx.annotation.VisibleForTesting
import com.android.compatibility.common.util.SystemUtil
import com.google.common.io.BaseEncoding
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Base class for monitors containing common logic to read the trace as a byte array and save the
 * trace to another location.
 */
abstract class TraceMonitor internal constructor(
    @VisibleForTesting
    protected var outputPath: Path,
    protected var sourceTraceFilePath: Path
) : ITransitionMonitor {
    override var checksum: String = ""
        protected set

    abstract val isEnabled: Boolean

    internal constructor(
        outputDir: Path,
        traceFileName: String
    ) : this(outputDir, TRACE_DIR.resolve(traceFileName))

    override fun save(testTag: String): Path {
        outputPath.toFile().mkdirs()
        val savedTrace = getOutputTraceFilePath(testTag)
        moveFile(sourceTraceFilePath, savedTrace)
        checksum = calculateChecksum(savedTrace)
        return savedTrace
    }

    private fun moveFile(src: Path, dst: Path) {
        // Move the  file to the output directory
        // Note: Due to b/141386109, certain devices do not allow moving the files between
        //       directories with different encryption policies, so manually copy and then
        //       remove the original file
        SystemUtil.runShellCommand("cp $src $dst")
        SystemUtil.runShellCommand("rm $src")
    }

    @VisibleForTesting
    fun getOutputTraceFilePath(testTag: String?): Path {
        return outputPath.resolve("${testTag}_${sourceTraceFilePath.fileName}")
    }

    companion object {
        private val TRACE_DIR = Paths.get("/data/misc/wmtrace/")

        @VisibleForTesting
        @JvmStatic
        fun calculateChecksum(traceFile: Path): String {
            return try {
                val messageDigest = MessageDigest.getInstance("SHA-256")
                val inputStream = FileInputStream(traceFile.toFile())
                val channel = inputStream.channel
                val buffer = ByteBuffer.allocate(2048)
                while (channel.read(buffer) != -1) {
                    buffer.flip()
                    messageDigest.update(buffer)
                    buffer.clear()
                }
                val hash = messageDigest.digest()
                BaseEncoding.base16().encode(hash).toLowerCase()
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalArgumentException("Checksum algorithm SHA-256 not found", e)
            } catch (e: IOException) {
                throw IllegalArgumentException("File not found", e)
            }
        }
    }
}
