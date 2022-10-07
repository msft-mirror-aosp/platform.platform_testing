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

import android.util.Log
import com.android.server.wm.flicker.FlickerRunResult.Companion.RunStatus
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RunResultArtifacts(_filePath: Path) {

    init {
        _filePath.parent.toFile().mkdirs()
        _filePath.toFile().createNewFile()
    }

    var path = _filePath
        private set

    private var archiveLocked = false

    private val zipOutputStream by lazy { openZipOutputStream() }
    private fun openZipOutputStream(): ZipOutputStream {
        return ZipOutputStream(BufferedOutputStream(FileOutputStream(path.toFile()), BUFFER_SIZE))
    }

    internal val traceName = path.fileName ?: "UNNAMED_TRACE"

    var status: RunStatus = RunStatus.UNDEFINED
        internal set(value) {
            if (field != value) {
                require(value != RunStatus.UNDEFINED) {
                    "Can't set status to UNDEFINED after being defined"
                }
                require(!field.isFailure) {
                    "Status of run already set to a failed status $field " +
                        "and can't be changed to $value."
                }
                field = value
                syncFileWithStatus()
            }
        }

    private fun syncFileWithStatus() {
        // Since we don't expect this to run in a multi-threaded context this is fine
        val localTraceFile = path
        try {
            val newFileName = "${status.prefix}_$traceName"
            val dst = localTraceFile.resolveSibling(newFileName)
            Utils.renameFile(localTraceFile, dst)
            path = dst
        } catch (e: IOException) {
            Log.e(FLICKER_TAG, "Unable to update file status $this", e)
        }
    }

    internal fun addFile(file: File, nameInArchive: String = file.name) {
        require(!archiveLocked) { "Archive is locked. Can't add to it." }
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

    internal fun lock() {
        if (!archiveLocked) {
            zipOutputStream.close()
            archiveLocked = true
        }
    }

    internal fun getFileBytes(fileName: String): ByteArray {
        require(archiveLocked) {
            "Can't get files from archive before it is closed, " +
                "maybe you forgot to run `.lock()` on the run result."
        }

        val tmpBuffer = ByteArray(BUFFER_SIZE)
        val zipInputStream: ZipInputStream
        try {
            zipInputStream =
                ZipInputStream(BufferedInputStream(FileInputStream(path.toFile()), BUFFER_SIZE))
        } catch (e: Throwable) {
            return ByteArray(0)
        }
        val outByteArray = ByteArrayOutputStream()
        var foundFile = false

        try {
            var zipEntry: ZipEntry? = zipInputStream.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == fileName) {
                    val outputStream = BufferedOutputStream(outByteArray, BUFFER_SIZE)
                    try {
                        var size = zipInputStream.read(tmpBuffer, 0, BUFFER_SIZE)
                        while (size > 0) {
                            outputStream.write(tmpBuffer, 0, size)
                            size = zipInputStream.read(tmpBuffer, 0, BUFFER_SIZE)
                        }
                        zipInputStream.closeEntry()
                    } finally {
                        outputStream.flush()
                        outputStream.close()
                    }
                    foundFile = true
                    break
                }
                zipEntry = zipInputStream.nextEntry
            }
        } finally {
            zipInputStream.closeEntry()
            zipInputStream.close()
        }

        require(foundFile) { "$fileName not found in archive..." }

        return outByteArray.toByteArray()
    }

    companion object {
        private const val BUFFER_SIZE = 2048
    }
}
