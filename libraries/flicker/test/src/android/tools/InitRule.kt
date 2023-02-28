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

package android.tools

import android.annotation.SuppressLint
import android.tools.common.Cache
import android.tools.common.CrossPlatform
import android.tools.common.TimestampFactory
import android.tools.common.io.RunStatus
import android.tools.device.flicker.datastore.DataStore
import android.tools.device.traces.ANDROID_LOGGER
import android.tools.device.traces.deleteIfExists
import android.tools.device.traces.formatRealTimestamp
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Standard initialization rule for all flicker tests */
@SuppressLint("VisibleForTests")
class InitRule : TestRule {
    override fun apply(base: Statement, description: Description?): Statement {
        CrossPlatform.setLogger(ANDROID_LOGGER)
            .setTimestampFactory(TimestampFactory { formatRealTimestamp(it) })
        DataStore.clear()
        Cache.clear()
        RunStatus.ALL.forEach { outputFileName(it).deleteIfExists() }
        return base
    }
}
