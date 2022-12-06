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

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.AssertionTag

/** Descriptor for files inside flicker result artifacts */
class ResultArtifactDescriptor
internal constructor(
    /** Trace or dump type */
    @VisibleForTesting val traceType: TraceType,
    /** If the trace/dump is associated with a tag */
    @VisibleForTesting val tag: String = AssertionTag.ALL
) {
    private val isTagTrace: Boolean
        get() = tag != AssertionTag.ALL

    /** Name of the trace file in the result artifact (e.g. zip) */
    val fileNameInArtifact: String = buildString {
        if (isTagTrace) {
            append(tag)
            append("__")
        }
        append(traceType.fileName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResultArtifactDescriptor) return false

        if (traceType != other.traceType) return false
        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = traceType.hashCode()
        result = 31 * result + tag.hashCode()
        return result
    }

    override fun toString(): String = fileNameInArtifact

    companion object {
        /**
         * Creates a [ResultArtifactDescriptor] based on the [fileNameInArtifact]
         *
         * @param fileNameInArtifact Name of the trace file in the result artifact (e.g. zip)
         */
        fun fromFileName(fileNameInArtifact: String): ResultArtifactDescriptor {
            val tagSplit = fileNameInArtifact.split("__")
            require(tagSplit.size <= 2) {
                "File name format should match '{tag}__{filename}' but was $fileNameInArtifact"
            }
            val tag = if (tagSplit.size > 1) tagSplit.first() else AssertionTag.ALL
            val fileName = tagSplit.last()
            return ResultArtifactDescriptor(TraceType.fromFileName(fileName), tag)
        }

        @VisibleForTesting
        fun newTestInstance(traceType: TraceType, tag: String = AssertionTag.ALL) =
            ResultArtifactDescriptor(traceType, tag)
    }
}
