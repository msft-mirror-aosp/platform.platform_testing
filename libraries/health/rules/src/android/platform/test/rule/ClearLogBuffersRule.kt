/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.platform.test.rule

import android.app.Instrumentation
import android.system.helpers.CommandsHelper
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/** This rule will execute the adb command for clearing all SysUI log buffers. */
class ClearLogBuffersRule : ExternalResource {
    private val TAG = "ClearLogBuffersRule"
    private val instrumentation: Instrumentation
    private var commandsHelper: CommandsHelper? = null

    // Constructor for dynamic injection by class name (required by TradeFed)
    constructor() {
        this.instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    // Constructor for manual instantiation
    constructor(instrumentation: Instrumentation) {
        this.instrumentation = instrumentation
    }

    override fun before() {
        if (commandsHelper == null) {
            commandsHelper = CommandsHelper.getInstance(instrumentation)
        }

        val command = "cmd statusbar clear-log-buffers"
        try {
            Log.d(TAG, "Executing: $command")
            val output = commandsHelper?.executeShellCommand(command)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
        }
    }

    override fun after() {
        // No action needed after the test
    }
}
