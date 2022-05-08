/*
 * Copyright 2022 The Android Open Source Project
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

package platform.test.screenshot

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class GoldenImagePathManagerTest {

    @Test
    fun goldenWithContextTest() {
        val localGoldenRoot = "/localgoldenroot/"
        val remoteGoldenRoot = "http://remotegoldenroot/"
        val context = InstrumentationRegistry.getInstrumentation().getContext()
        val gim = GoldenImagePathManager(
            context,
            GoldenImageLocationConfig(localGoldenRoot, remoteGoldenRoot))
        // Test for resolving device local paths.
        val localGoldenFullImagePath = gim.goldenIdentifierResolver(
            testName = "test1", relativePathOnly = false, localPath = true)
        assertThat(localGoldenFullImagePath).startsWith(localGoldenRoot)
        assertThat(localGoldenFullImagePath).endsWith("dpi/test1.png")
        assertThat(localGoldenFullImagePath.split("/").size).isEqualTo(9)
        // Test for resolving repo paths.
        val repoGoldenFullImagePath = gim.goldenIdentifierResolver(
            testName = "test2", relativePathOnly = false, localPath = false)
        assertThat(repoGoldenFullImagePath).startsWith(remoteGoldenRoot)
        assertThat(repoGoldenFullImagePath).endsWith("dpi/test2.png")
        assertThat(repoGoldenFullImagePath.split("/").size).isEqualTo(10)
    }

    private fun pathContextExtractor(context: Context): String {
        return when {
            (context.resources.displayMetrics.densityDpi.toString().length > 0) -> "context"
            else -> "invalidcontext"
        }
    }

    private fun pathNoContextExtractor() = "nocontext"

    @Test
    fun pathConfigTest() {
        val pc = PathConfig(
            PathElementNoContext("nocontext1", true, ::pathNoContextExtractor),
            PathElementNoContext("nocontext2", true, ::pathNoContextExtractor),
            PathElementWithContext("context1", true, ::pathContextExtractor),
            PathElementWithContext("context2", true, ::pathContextExtractor)
        )
        val context = InstrumentationRegistry.getInstrumentation().getContext()
        val pcResolvedRelativePath = pc.resolveRelativePath(context)
        assertThat(pcResolvedRelativePath).isEqualTo("nocontext/nocontext/context/context/")
    }
}
