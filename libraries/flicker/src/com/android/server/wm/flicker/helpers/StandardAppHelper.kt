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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.helpers.AbstractStandardAppHelper
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.android.launcher3.tapl.LauncherInstrumentation
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.Condition
import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.IComponentNameMatcher
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper

/**
 * Class to take advantage of {@code IAppHelper} interface so the same test can be run against first
 * party and third party apps.
 */
open class StandardAppHelper(
    instr: Instrumentation,
    val appName: String,
    val componentMatcher: IComponentNameMatcher
) : AbstractStandardAppHelper(instr), IComponentNameMatcher by componentMatcher {
    constructor(
        instr: Instrumentation,
        appName: String,
        packageName: String,
        activity: String
    ) : this(instr, appName, ComponentNameMatcher(packageName, ".$activity"))

    protected val tapl: LauncherInstrumentation = LauncherInstrumentation()

    private val activityManager: ActivityManager?
        get() = mInstrumentation.context.getSystemService(ActivityManager::class.java)

    protected val context: Context
        get() = mInstrumentation.context

    override val packageName: String = componentMatcher.packageName

    override val className: String = componentMatcher.className

    protected val uiDevice: UiDevice = UiDevice.getInstance(mInstrumentation)

    private fun getAppSelector(expectedPackageName: String): BySelector {
        val expected = expectedPackageName.ifEmpty { packageName }
        return By.pkg(expected).depth(0)
    }

    override fun open() {
        open(`package`)
    }

    protected fun open(expectedPackageName: String) {
        tapl.goHome().switchToAllApps().getAppIcon(launcherName).launch(expectedPackageName)
    }

    /** {@inheritDoc} */
    override fun getPackage(): String {
        return packageName
    }

    /** {@inheritDoc} */
    override fun getOpenAppIntent(): Intent {
        val intent = Intent()
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.component = ComponentName(packageName, className)
        return intent
    }

    /** {@inheritDoc} */
    override fun getLauncherName(): String {
        return appName
    }

    /** {@inheritDoc} */
    override fun dismissInitialDialogs() {}

    /** {@inheritDoc} */
    override fun exit() {
        // Ensure all testing components end up being closed.
        activityManager?.forceStopPackage(packageName)
    }

    /** Exits the activity and wait for activity destroyed */
    fun exit(wmHelper: WindowManagerStateHelper) {
        exit()
        waitForActivityDestroyed(wmHelper)
    }

    /** Waits the activity until state change to {link WindowManagerState.STATE_DESTROYED} */
    private fun waitForActivityDestroyed(wmHelper: WindowManagerStateHelper) {
        val waitMsg =
            "state of ${componentMatcher.toActivityIdentifier()} to be " +
                WindowManagerState.STATE_DESTROYED
        wmHelper
            .StateSyncBuilder()
            .add(waitMsg) {
                !it.wmState.containsActivity(componentMatcher) ||
                    it.wmState.hasActivityState(
                        componentMatcher,
                        WindowManagerState.STATE_DESTROYED
                    )
            }
            .withAppTransitionIdle()
            .waitForAndVerify()
    }

    private fun launchAppViaIntent(
        action: String? = null,
        stringExtras: Map<String, String> = mapOf()
    ) {
        val intent = openAppIntent
        intent.action = action
        stringExtras.forEach { intent.putExtra(it.key, it.value) }
        context.startActivity(intent)
    }

    /**
     * Launches the app through an intent instead of interacting with the launcher.
     *
     * Uses UiAutomation to detect when the app is open
     */
    @JvmOverloads
    open fun launchViaIntent(
        expectedPackageName: String = "",
        action: String? = null,
        stringExtras: Map<String, String> = mapOf()
    ) {
        launchAppViaIntent(action, stringExtras)
        val appSelector = getAppSelector(expectedPackageName)
        uiDevice.wait(Until.hasObject(appSelector), APP_LAUNCH_WAIT_TIME_MS)
    }

    /**
     * Launches the app through an intent instead of interacting with the launcher and waits until
     * the app window is visible
     */
    @JvmOverloads
    open fun launchViaIntent(
        wmHelper: WindowManagerStateHelper,
        expectedWindowName: String = "",
        action: String? = null,
        stringExtras: Map<String, String> = mapOf(),
        waitConditions: Array<Condition<DeviceStateDump>> = emptyArray()
    ) =
        launchViaIntentAndWaitShown(
            wmHelper,
            expectedWindowName,
            action,
            stringExtras,
            waitConditions
        )

    /**
     * Launches the app through an intent instead of interacting with the launcher and waits until
     * the app window is visible
     */
    protected fun launchViaIntentAndWaitShown(
        wmHelper: WindowManagerStateHelper,
        expectedWindowName: String = "",
        action: String? = null,
        stringExtras: Map<String, String> = mapOf(),
        waitConditions: Array<Condition<DeviceStateDump>> = emptyArray()
    ) {
        launchAppViaIntent(action, stringExtras)

        val expectedWindow =
            if (expectedWindowName.isNotEmpty()) {
                ComponentNameMatcher("", expectedWindowName)
            } else {
                componentMatcher
            }
        val builder =
            wmHelper
                .StateSyncBuilder()
                .add(WindowManagerConditionsFactory.isWMStateComplete())
                .withAppTransitionIdle()
                .withWindowSurfaceAppeared(expectedWindow)

        waitConditions.forEach { builder.add(it) }
        builder.waitForAndVerify()

        // During seamless rotation the app window is shown
        val currWmState = wmHelper.currentState.wmState
        if (currWmState.visibleWindows.none { it.isFullscreen }) {
            wmHelper
                .StateSyncBuilder()
                .withNavOrTaskBarVisible()
                .withStatusBarVisible()
                .waitForAndVerify()
        }
    }

    companion object {
        private const val APP_LAUNCH_WAIT_TIME_MS = 10000L
    }
}
