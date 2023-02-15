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

package com.android.server.wm.traces.common.component.matchers

import com.android.server.wm.traces.common.component.ComponentName
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer
import kotlin.js.JsName

/**
 * ComponentMatcher based on name It is open so that it can be extended by [ComponentTypeMatcher]
 */
open class ComponentNameMatcher(open var component: ComponentName) : IComponentNameMatcher {

    override val packageName: String
        get() = component.packageName
    override val className: String
        get() = component.className
    override fun toActivityName(): String = component.toActivityName()
    override fun toWindowName(): String = component.toWindowName()
    override fun toLayerName(): String = component.toLayerName()

    constructor(
        packageName: String,
        className: String
    ) : this(ComponentName(packageName, className))

    constructor(className: String) : this("", className)

    @JsName("matchesAnyOf")
    private fun <T> matchesAnyOf(
        values: Array<T>,
        valueProducer: (T) -> String,
        regexProducer: (ComponentName) -> Regex,
    ): Boolean {
        val componentRegex = regexProducer.invoke(component)
        val targets = values.map { valueProducer.invoke(it) }
        return targets.any { value -> componentRegex.matches(value) }
    }

    override fun componentNameMatcherToString(): String {
        return "ComponentNameMatcher(\"${this.packageName}\", " + "\"${this.className}\")"
    }

    /** {@inheritDoc} */
    override fun windowMatchesAnyOf(windows: Array<WindowContainer>): Boolean =
        matchesAnyOf(windows, { it.title }, { it.toWindowNameRegex() })

    /** {@inheritDoc} */
    override fun activityMatchesAnyOf(activities: Array<Activity>): Boolean =
        matchesAnyOf(activities, { it.name }, { it.toActivityNameRegex() })

    /** {@inheritDoc} */
    override fun layerMatchesAnyOf(layers: Array<Layer>): Boolean =
        matchesAnyOf(layers, { it.name }, { it.toLayerNameRegex() })

    /** {@inheritDoc} */
    override fun check(
        layers: Collection<Layer>,
        condition: (Collection<Layer>) -> Boolean
    ): Boolean = condition(layers.filter { layerMatchesAnyOf(it) })

    /** {@inheritDoc} */
    override fun toActivityIdentifier(): String = component.toActivityName()

    /** {@inheritDoc} */
    override fun toWindowIdentifier(): String = component.toWindowName()

    /** {@inheritDoc} */
    override fun toLayerIdentifier(): String = component.toLayerName()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentNameMatcher) return false
        return component == other.component
    }

    override fun hashCode(): Int = component.hashCode()

    override fun toString(): String = component.toString()

    companion object {
        @JsName("NAV_BAR") val NAV_BAR = ComponentNameMatcher("", "NavigationBar0")
        @JsName("TASK_BAR") val TASK_BAR = ComponentNameMatcher("", "Taskbar")
        @JsName("STATUS_BAR") val STATUS_BAR = ComponentNameMatcher("", "StatusBar")
        @JsName("ROTATION") val ROTATION = ComponentNameMatcher("", "RotationLayer")
        @JsName("BACK_SURFACE") val BACK_SURFACE = ComponentNameMatcher("", "BackColorSurface")
        @JsName("IME") val IME = ComponentNameMatcher("", "InputMethod")
        @JsName("IME_SNAPSHOT") val IME_SNAPSHOT = ComponentNameMatcher("", "IME-snapshot-surface")
        @JsName("SPLASH_SCREEN") val SPLASH_SCREEN = ComponentNameMatcher("", "Splash Screen")
        @JsName("SNAPSHOT") val SNAPSHOT = ComponentNameMatcher("", "SnapshotStartingWindow")
        @JsName("TRANSITION_SNAPSHOT")
        val TRANSITION_SNAPSHOT = ComponentNameMatcher("", "transition snapshot")
        @JsName("LETTERBOX") val LETTERBOX = ComponentNameMatcher("", "Letterbox")
        @JsName("WALLPAPER_BBQ_WRAPPER")
        val WALLPAPER_BBQ_WRAPPER = ComponentNameMatcher("", "Wallpaper BBQ wrapper")
        @JsName("PIP_CONTENT_OVERLAY")
        val PIP_CONTENT_OVERLAY = ComponentNameMatcher("", "PipContentOverlay")
        @JsName("LAUNCHER")
        val LAUNCHER =
            ComponentNameMatcher(
                "com.google.android.apps.nexuslauncher",
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity"
            )
        @JsName("AOSP_LAUNCHER")
        val AOSP_LAUNCHER =
            ComponentNameMatcher(
                "com.android.launcher3",
                "com.android.launcher3.uioverrides.QuickstepLauncher"
            )
        @JsName("SPLIT_DIVIDER")
        val SPLIT_DIVIDER = ComponentNameMatcher("", "StageCoordinatorSplitDivider")
        @JsName("DEFAULT_TASK_DISPLAY_AREA")
        val DEFAULT_TASK_DISPLAY_AREA = ComponentNameMatcher("", "DefaultTaskDisplayArea")

        /**
         * Creates a component matcher from a window or layer name.
         *
         * Requires the [str] to contain both the package and class name (with a / separator)
         *
         * @param str Value to parse
         */
        @JsName("unflattenFromString")
        fun unflattenFromString(str: String): ComponentNameMatcher {
            val sep = str.indexOf('/')
            if (sep < 0 || sep + 1 >= str.length) {
                error("Missing package/class separator")
            }
            val pkg = str.substring(0, sep)
            var cls = str.substring(sep + 1)
            if (cls.isNotEmpty() && cls[0] == '.') {
                cls = pkg + cls
            }
            return ComponentNameMatcher(pkg, cls)
        }

        /**
         * Creates a component matcher from a window or layer name. The name might contain junk,
         * which will be removed to only extract package and class name (e.g. other words before
         * package name, separated by spaces, #id in the end after the class name)
         *
         * Requires the [str] to contain both the package and class name (with a / separator)
         *
         * @param str Value to parse
         */
        @JsName("unflattenFromStringWithJunk")
        fun unflattenFromStringWithJunk(str: String): ComponentNameMatcher {
            val sep = str.indexOf('/')
            if (sep < 0 || sep + 1 >= str.length) {
                error("Missing package/class separator")
            }

            var pkg = str.substring(0, sep)
            var pkgSep: Int = -1
            val pkgCharArr = pkg.toCharArray()
            for (index in (0..pkgCharArr.lastIndex).reversed()) {
                val currentChar = pkgCharArr[index]
                if (currentChar !in 'A'..'Z' && currentChar !in 'a'..'z' && currentChar != '.') {
                    pkgSep = index
                    break
                }
            }
            if (!(pkgSep < 0 || pkgSep + 1 >= pkg.length)) {
                pkg = pkg.substring(pkgSep, pkg.length)
            }

            var cls = str.substring(sep + 1)
            var clsSep = -1 // cls.indexOf('#')
            val clsCharArr = cls.toCharArray()
            for (index in (0..clsCharArr.lastIndex)) {
                val currentChar = clsCharArr[index]
                if (currentChar !in 'A'..'Z' && currentChar !in 'a'..'z' && currentChar != '.') {
                    clsSep = index
                    break
                }
            }
            if (!(clsSep < 0 || clsSep + 1 >= cls.length)) {
                cls = cls.substring(0, clsSep)
            }

            if (cls.isNotEmpty() && cls[0] == '.') {
                cls = pkg + cls
            }
            return ComponentNameMatcher(pkg, cls)
        }
    }
}
