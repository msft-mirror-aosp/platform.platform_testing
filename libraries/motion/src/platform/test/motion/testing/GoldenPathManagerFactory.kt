/*
 * Copyright (C) 2024 The Android Open Source Project
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

package platform.test.motion.testing

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.PathConfig

/**
 * Creates a [GoldenPathManager] with the recommended configuration for motion tests.
 *
 * @param sourceTreeGoldenLocation path to the golden asset directory, relative to ANDROID_BUILD_TOP
 */
fun createGoldenPathManager(
    sourceTreeGoldenLocation: String,
    pathConfig: PathConfig = PathConfig()
): GoldenPathManager {
    val appContext = InstrumentationRegistry.getInstrumentation().context
    val deviceOutputDirectory = File(appContext.filesDir, "goldens").toString()
    return GoldenPathManager(
        appContext,
        sourceTreeGoldenLocation,
        deviceOutputDirectory,
        pathConfig
    )
}
