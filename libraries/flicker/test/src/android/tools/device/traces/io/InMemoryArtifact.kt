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

import android.tools.common.io.IArtifact
import android.tools.common.io.ResultArtifactDescriptor
import android.tools.common.io.RunStatus

class InMemoryArtifact(override val path: String) : IArtifact {

    override val runStatus = RunStatus.UNDEFINED

    override fun deleteIfExists() {
        // No op
    }

    override fun traceCount(): Int = 0

    override fun hasTrace(descriptor: ResultArtifactDescriptor): Boolean = false

    override fun readBytes(descriptor: ResultArtifactDescriptor): ByteArray? = null

    override fun updateStatus(newStatus: RunStatus) {
        // No op
    }

    companion object {
        internal val EMPTY = InMemoryArtifact("Empty")
    }
}
