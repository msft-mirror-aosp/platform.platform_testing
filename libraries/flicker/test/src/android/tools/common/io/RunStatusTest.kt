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

package android.tools.common.io

import android.tools.CleanFlickerEnvironmentRule
import android.tools.TEST_SCENARIO
import android.tools.device.traces.deleteIfExists
import android.tools.device.traces.getDefaultFlickerOutputDir
import android.tools.device.traces.io.Artifact
import com.google.common.truth.Truth
import java.io.File
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class RunStatusTest {
    @Test
    fun fromFileNameGetsCorrectStatus() {
        for (status in RunStatus.values()) {
            val artifact =
                Artifact(
                    status,
                    TEST_SCENARIO,
                    getDefaultFlickerOutputDir(),
                    emptyMap<ResultArtifactDescriptor, File>()
                )
            val statusFromFile = RunStatus.fromFileName(artifact.file.name)
            Truth.assertThat(statusFromFile).isEqualTo(status)
            artifact.deleteIfExists()
        }
    }

    companion object {
        @Rule @ClassRule @JvmField val cleanFlickerEnvironmentRule = CleanFlickerEnvironmentRule()
    }
}
