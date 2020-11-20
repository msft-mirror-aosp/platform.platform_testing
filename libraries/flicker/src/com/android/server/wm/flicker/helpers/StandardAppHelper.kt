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

package com.android.server.wm.flicker.helpers

import android.app.ActivityManager
import android.app.Instrumentation
import android.platform.helpers.AbstractStandardAppHelper
import android.support.test.launcherhelper.ILauncherStrategy
import android.support.test.launcherhelper.LauncherStrategyFactory

/**
 * Class to take advantage of {@code IAppHelper} interface so the same test can be run against first
 * party and third party apps.
 */
open class StandardAppHelper @JvmOverloads constructor(
    instr: Instrumentation,
    protected val packageName: String,
    protected val appName: String,
    protected val launcherStrategy: ILauncherStrategy =
            LauncherStrategyFactory.getInstance(instr).launcherStrategy
) : AbstractStandardAppHelper(instr) {
    constructor(
        instr: Instrumentation,
        appName: String,
        launcherStrategy: ILauncherStrategy =
                LauncherStrategyFactory.getInstance(instr).launcherStrategy
    ) : this(instr, sFlickerPackage, appName, launcherStrategy)

    private val activityManager: ActivityManager?
        get() = mInstrumentation.context.getSystemService(ActivityManager::class.java)

    override fun open() {
        launcherStrategy.launch(appName, packageName)
    }

    /** {@inheritDoc}  */
    override fun getPackage(): String {
        return packageName
    }

    /** {@inheritDoc}  */
    override fun getLauncherName(): String {
        return appName
    }

    /** {@inheritDoc}  */
    override fun dismissInitialDialogs() {}

    /** {@inheritDoc}  */
    override fun exit() {
        super.exit()

        // Ensure all testing components end up being closed.
        activityManager?.forceStopPackage(packageName)
    }

    companion object {
        private val sFlickerPackage = "com.android.server.wm.flicker.testapp"
    }
}
