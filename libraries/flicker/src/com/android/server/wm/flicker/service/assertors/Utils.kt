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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.service.FlickerService
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.errors.Error
import com.android.server.wm.traces.common.errors.ErrorState
import com.android.server.wm.traces.common.errors.ErrorTrace
import java.io.FileNotFoundException

/**
 * Reads the assertions configuration for the configuration file.
 *
 * @param path the location of the configuration file
 * @return a list with assertors configuration
 *
 * @throws FileNotFoundException when there is no config file
 */
internal fun readConfigurationFile(fileName: String): Array<AssertorConfigModel> {
    val fileContent = FlickerService::class.java.classLoader.getResource(fileName)
        ?.readText(Charsets.UTF_8)
        ?: throw FileNotFoundException("A configuration file must exist!")
    return AssertionConfigParser.parseConfigFile(fileContent)
}

internal fun buildErrorTrace(errors: MutableMap<Long, MutableList<Error>>): ErrorTrace {
    val errorStates = errors.map { entry ->
        val timestamp = entry.key
        val stateTags = entry.value
        ErrorState(stateTags.toTypedArray(), timestamp.toString())
    }
    return ErrorTrace(errorStates.toTypedArray(), source = "")
}

internal fun createError(
    subject: FlickerTraceSubject<out FlickerSubject>,
    error: FlickerSubjectException
): Error {
    if (subject is LayersTraceSubject) {
        return createError(subject, error)
    }

    if (subject is WindowManagerTraceSubject) {
        return createError(subject, error)
    }

    return Error(stacktrace = "", message = "")
}

internal fun createError(
    subject: WindowManagerTraceSubject,
    error: FlickerSubjectException
): Error {
    val taskId = subject.trace.entries
        .firstOrNull { it.timestamp == error.timestamp }
        ?.homeTask?.taskId

    return Error(
        stacktrace = error.stackTraceToString(),
        message = error.message,
        taskId = taskId ?: 0
    )
}

internal fun createError(subject: LayersTraceSubject, error: FlickerSubjectException): Error {
    val layerId = subject.trace.entries
        .firstOrNull {
            it.timestamp == error.timestamp
        }
        ?.visibleLayers?.firstOrNull()?.id

    return Error(
        stacktrace = error.stackTraceToString(),
        message = error.message,
        layerId = layerId ?: 0
    )
}