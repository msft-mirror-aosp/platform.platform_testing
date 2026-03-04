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

package android.platform.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.platform.helpers.CommonUtils.isLargeScreen
import android.platform.uiautomatorhelpers.DeviceHelpers
import android.platform.uiautomatorhelpers.DeviceHelpers.shell
import android.provider.Settings
import android.util.Log
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION
import com.android.app.tracing.traceSection
import com.android.systemui.Flags

object ShadeUtils {

    /**
     * Whether the device is in a configuration that should display Single Shade.
     *
     * @param context The context containing the configuration of the display that is hosting the
     *   shade.
     */
    @JvmStatic
    @JvmOverloads
    fun isSingleShadeConfig(context: Context = getShadeDisplayContext()): Boolean =
        !isDualShadeConfig(context) && !isSplitShadeConfig(context)

    /**
     * Whether the device is in a configuration that should display Dual Shade.
     *
     * @param context The context containing the configuration of the display that is hosting the
     *   shade.
     */
    @JvmStatic
    @JvmOverloads
    fun isDualShadeConfig(context: Context = getShadeDisplayContext()): Boolean {
        if (!Flags.dualShade()) {
            return false
        }

        if (
            context.getBoolResource(
                resName = "config_useDualShadeSetting",
                packageName = "com.android.settingslib",
            ) == true
        ) {
            return isDualShadeSettingEnabled(context)
        }

        return context.getBoolResource("config_dualShadeEnabledByDefault")
            ?: isDualShadeSettingEnabled(context)
    }

    /**
     * Whether the device is in a configuration that should display Split Shade.
     *
     * @param context The context containing the configuration of the display that is hosting the
     *   shade.
     */
    @JvmStatic
    @JvmOverloads
    fun isSplitShadeConfig(context: Context = getShadeDisplayContext()): Boolean {
        if (Flags.dualShade()) {
            return false // Split shade cannot be shown
        }

        return context.getBoolResource("config_use_split_notification_shade")
            ?:
            // Fallback check, accurate for most but not necessarily all devices.
            isLargeScreen() &&
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun getShadeDisplayContext(): Context {
        val displayIdString = shell("cmd statusbar shade_display_override display_id")
        val displayId =
            try {
                displayIdString.trim().toInt()
            } catch (e: NumberFormatException) {
                error("Couldn't parse shade display ID: $displayIdString")
            }

        return traceSection("getShadeDisplayContext") {
            val displayManager =
                DeviceHelpers.context.getSystemService(DisplayManager::class.java)
                    ?: error("Couldn't get DisplayManager")
            val display = displayManager.getDisplay(displayId)
            DeviceHelpers.context.createWindowContext(display, TYPE_APPLICATION, null)
        }
    }

    private fun isDualShadeSettingEnabled(context: Context): Boolean {
        val defaultValue = if (Flags.dualShade()) 1 else 0
        val resolver = context.contentResolver
        return Settings.Secure.getInt(resolver, Settings.Secure.DUAL_SHADE, defaultValue) == 1
    }

    private fun Context.getBoolResource(
        resName: String,
        packageName: String = "com.android.systemui",
    ): Boolean? {
        try {
            val appResources = packageManager.getResourcesForApplication(packageName)
            val resourceId = appResources.getIdentifier(resName, "bool", packageName)
            return appResources.getBoolean(resourceId)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Couldn't find boolean resource: [$packageName].$resName", e)
            return null
        }
    }
}

private const val TAG = "ShadeUtils"
