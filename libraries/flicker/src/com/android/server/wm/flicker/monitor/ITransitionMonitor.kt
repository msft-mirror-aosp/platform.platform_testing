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

import java.nio.file.Path

/** Collects test artifacts during a UI transition.  */
interface ITransitionMonitor {
    /** Starts monitor.  */
    fun start()

    /** Stops monitor.  */
    fun stop()

    /**
     * Saves any monitor artifacts to file adding `testTag` and `iteration` to the file
     * name.
     *
     * @param testTag suffix added to artifact name
     * @param iteration suffix added to artifact name
     * @return Path to saved artifact
     */
    fun save(testTag: String, iteration: Int): Path = save("${testTag}_$iteration")

    /**
     * Saves any monitor artifacts to file adding `testTag` to the file name.
     *
     * @param testTag suffix added to artifact name
     * @return Path to saved artifact
     */
    /**
     * Saves trace file to the external storage directory suffixing the name with the testtag and
     * iteration.
     *
     *
     * Moves the trace file from the default location via a shell command since the test app does
     * not have security privileges to access /data/misc/wmtrace.
     *
     * @param testTag suffix added to trace name used to identify trace
     * @return Path to saved trace file and file checksum (SHA-256)
     */
    fun save(testTag: String): Path

    val checksum: String
}
