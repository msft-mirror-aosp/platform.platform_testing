/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.wm.flicker

import com.android.compatibility.common.util.SystemUtil
import com.android.server.wm.flicker.FlickerRunResult.Companion.RunStatus
import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.layers.LayersElementLifecycle
import com.android.server.wm.flicker.service.assertors.ComponentTypeMatcher
import com.android.server.wm.traces.common.ComponentNameMatcher
import java.nio.file.Path

object Utils {
    fun renameFile(src: Path, dst: Path) {
        SystemUtil.runShellCommand("mv $src $dst")
    }

    fun copyFile(src: Path, dst: Path) {
        SystemUtil.runShellCommand("cp $src $dst")
        SystemUtil.runShellCommand("chmod a+r $dst")
    }

    fun moveFile(src: Path, dst: Path) {
        // Move the  file to the output directory
        // Note: Due to b/141386109, certain devices do not allow moving the files between
        //       directories with different encryption policies, so manually copy and then
        //       remove the original file
        //       Moreover, the copied trace file may end up with different permissions, resulting
        //       in b/162072200, to prevent this, ensure the files are readable after copying
        copyFile(src, dst)
        SystemUtil.runShellCommand("rm $src")
    }

    fun addStatusToFileName(traceFile: Path, status: RunStatus) {
        val newFileName = "${status.prefix}_${traceFile.fileName}"
        val dst = traceFile.resolveSibling(newFileName)
        renameFile(traceFile, dst)
    }

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

    fun componentNameMatcherConfig(
        str: String,
        deviceTraceConfiguration: DeviceTraceConfiguration
    ): ComponentNameMatcher? {
        for ((compName, compBuilder) in deviceTraceConfiguration.componentToTypeMap) {
            val compNameElements = compName.split('/')
            val compPackage = compNameElements[0]
            val compClass = compNameElements[1]
            if (str.contains(compPackage) && str.contains(compClass)) {
                return ComponentTypeMatcher(compName, compBuilder)
            }
        }
        return null
    }

    /**
     * Obtains the component name matcher corresponding to a name (str) Returns null if the name is
     * not found in the hardcoded list or in the config, and it does not contain both the package
     * and class name (with a / separator)
     */
    fun componentNameMatcherFromNameWithConfig(
        str: String,
        deviceTraceConfiguration: DeviceTraceConfiguration
    ): ComponentNameMatcher? {
        return try {
            componentNameMatcherHardcoded(str)
                ?: componentNameMatcherConfig(str, deviceTraceConfiguration)
                    ?: ComponentNameMatcher.unflattenFromStringWithJunk(str)
        } catch (err: IllegalStateException) {
            null
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
        if (componentNameMatcher is ComponentTypeMatcher) {
            return "Components.${componentNameMatcher.componentBuilder.name}"
        }
        return "ComponentNameMatcher(\"${componentNameMatcher.packageName}\", " +
            "\"${componentNameMatcher.className}\")"
    }

    fun componentNameMatcherToStringSimplified(componentNameMatcher: ComponentNameMatcher): String {
        if (componentNameMatcher is ComponentTypeMatcher) {
            return componentNameMatcher.componentBuilder.name
        }
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

    /** Gets the name of the states in a layers lifecycle */
    fun getLayersElementLifecycleName(elementLifecycle: LayersElementLifecycle): String {
        for (state in elementLifecycle.states) {
            if (state != null) {
                return state.name
            }
        }
        return ""
    }
}
