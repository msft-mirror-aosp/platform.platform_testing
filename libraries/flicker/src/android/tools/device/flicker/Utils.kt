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

package android.tools.device.flicker

import android.tools.common.datatypes.component.ComponentNameMatcher

object Utils {
    fun componentMatcherParamsFromName(name: String): Pair<String, String> {
        var packageName = ""
        var className = ""
        if (name.contains("/")) {
            if (name.contains("#")) {
                name.removeSuffix("#")
            }
            val splitString = name.split('/')
            packageName = splitString[0]
            className = splitString[1]
        } else {
            className = name
        }
        return Pair(packageName, className)
    }

    fun componentNameMatcherHardcoded(str: String): ComponentNameMatcher? {
        return when (true) {
            str.contains("NavigationBar0") -> ComponentNameMatcher.NAV_BAR
            str.contains("Taskbar") -> ComponentNameMatcher.TASK_BAR
            str.contains("StatusBar") -> ComponentNameMatcher.STATUS_BAR
            str.contains("RotationLayer") -> ComponentNameMatcher.ROTATION
            str.contains("BackColorSurface") -> ComponentNameMatcher.BACK_SURFACE
            str.contains("InputMethod") -> ComponentNameMatcher.IME
            str.contains("IME-snapshot-surface") -> ComponentNameMatcher.IME_SNAPSHOT
            str.contains("Splash Screen") -> ComponentNameMatcher.SPLASH_SCREEN
            str.contains("SnapshotStartingWindow") -> ComponentNameMatcher.SNAPSHOT
            str.contains("Letterbox") -> ComponentNameMatcher.LETTERBOX
            str.contains("Wallpaper BBQ wrapper") -> ComponentNameMatcher.WALLPAPER_BBQ_WRAPPER
            str.contains("PipContentOverlay") -> ComponentNameMatcher.PIP_CONTENT_OVERLAY
            str.contains("com.google.android.apps.nexuslauncher") -> ComponentNameMatcher.LAUNCHER
            str.contains("StageCoordinatorSplitDivider") -> ComponentNameMatcher.SPLIT_DIVIDER
            else -> null
        }
    }

    /**
     * Obtains the component name matcher corresponding to a name (str) Returns null if the name is
     * not found in the hardcoded list, and it does not contain both the package and class name
     * (with a / separator)
     */
    fun componentNameMatcherFromName(
        str: String,
    ): ComponentNameMatcher? {
        return try {
            componentNameMatcherHardcoded(str)
                ?: ComponentNameMatcher.unflattenFromStringWithJunk(str)
        } catch (err: IllegalStateException) {
            null
        }
    }

    fun componentNameMatcherToString(componentNameMatcher: ComponentNameMatcher): String {
        return "ComponentNameMatcher(\"${componentNameMatcher.packageName}\", " +
            "\"${componentNameMatcher.className}\")"
    }

    fun componentNameMatcherToStringSimplified(componentNameMatcher: ComponentNameMatcher): String {
        var className = componentNameMatcher.className
        val separatedByDots = className.split('.')
        if (separatedByDots.isNotEmpty()) {
            className = separatedByDots[separatedByDots.size - 1]
        }
        className = className.replace(' ', '_')
        return className
    }

    fun componentNameMatcherAsStringFromName(str: String): String? {
        val componentMatcher = componentNameMatcherFromName(str)
        return componentMatcher?.componentNameMatcherToString()
    }
}
