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

package android.tools.common.assertions

import android.tools.InitRule
import android.tools.assertThrows
import android.tools.common.Tag
import android.tools.common.flicker.assertions.SubjectsParser
import android.tools.common.io.RunStatus
import android.tools.device.traces.DEFAULT_TRACE_CONFIG
import android.tools.device.traces.deleteIfExists
import android.tools.device.traces.io.ResultReader
import android.tools.newTestResultWriter
import android.tools.outputFileName
import java.io.FileNotFoundException
import org.junit.ClassRule
import org.junit.Test

/** Tests for [SubjectsParser] */
class SubjectsParserTest {

    @Test
    fun failFileNotFound() {
        val data = newTestResultWriter().write()
        outputFileName(RunStatus.RUN_EXECUTED).deleteIfExists()
        val parser = SubjectsParser(ResultReader(data, DEFAULT_TRACE_CONFIG))
        assertThrows<FileNotFoundException> { parser.getSubjects(Tag.ALL) }
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
