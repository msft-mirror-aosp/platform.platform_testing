/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.rules

import android.app.Instrumentation
import android.content.Context
import android.os.RemoteException
import android.util.Log
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.android.server.wm.traces.parser.withPerfettoTrace
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Changes display orientation before a test
 *
 * @param targetOrientation Target orientation
 * @param instrumentation Instrumentation mechanism to use
 * @param clearCacheAfterParsing If the caching used while parsing the proto should be
 * ```
 *                               cleared or remain in memory
 * ```
 */
data class ChangeDisplayOrientationRule
@JvmOverloads
constructor(
    private val targetOrientation: PlatformConsts.Rotation,
    private val resetOrientationAfterTest: Boolean = true,
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    private val clearCacheAfterParsing: Boolean = true
) : TestWatcher() {
    private var initialOrientation = PlatformConsts.Rotation.ROTATION_0

    override fun starting(description: Description?) {
        withPerfettoTrace("ChangeDisplayOrientationRule:starting") {
            Log.v(
                FLICKER_TAG,
                "Changing display orientation to " +
                    "$targetOrientation ${targetOrientation.description}"
            )
            val wm =
                instrumentation.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            initialOrientation = PlatformConsts.Rotation.getByValue(wm.defaultDisplay.rotation)
            setRotation(targetOrientation, instrumentation, clearCacheAfterParsing)
        }
    }

    override fun finished(description: Description?) {
        withPerfettoTrace("ChangeDisplayOrientationRule:finished") {
            if (resetOrientationAfterTest) {
                setRotation(initialOrientation, instrumentation, clearCacheAfterParsing)
            }
        }
    }

    companion object {
        @JvmOverloads
        fun setRotation(
            rotation: PlatformConsts.Rotation,
            instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
            clearCacheAfterParsing: Boolean = true,
            wmHelper: WindowManagerStateHelper =
                WindowManagerStateHelper(
                    instrumentation,
                    clearCacheAfterParsing = clearCacheAfterParsing
                )
        ) {
            val device: UiDevice = UiDevice.getInstance(instrumentation)

            try {
                when (rotation) {
                    PlatformConsts.Rotation.ROTATION_270 -> device.setOrientationRight()
                    PlatformConsts.Rotation.ROTATION_90 -> device.setOrientationLeft()
                    PlatformConsts.Rotation.ROTATION_0 -> device.setOrientationNatural()
                    else -> device.setOrientationNatural()
                }

                if (wmHelper.currentState.wmState.canRotate) {
                    wmHelper.StateSyncBuilder().withRotation(rotation).waitForAndVerify()
                } else {
                    wmHelper.StateSyncBuilder().withAppTransitionIdle().waitForAndVerify()
                    Log.v(FLICKER_TAG, "Rotation is not allowed in the state")
                    return
                }

                // During seamless rotation the app window is shown
                val currWmState = wmHelper.currentState.wmState
                if (currWmState.visibleWindows.none { it.isFullscreen }) {
                    wmHelper
                        .StateSyncBuilder()
                        .withNavOrTaskBarVisible()
                        .withStatusBarVisible()
                        .waitForAndVerify()
                }
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
    }
}
