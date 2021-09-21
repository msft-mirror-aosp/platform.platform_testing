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

@file:JvmName("CommonAssertions")
package com.android.server.wm.flicker.service.assertors

import android.content.ComponentName
import android.view.Surface
import com.android.server.wm.flicker.helpers.WindowUtils
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.FlickerComponentName

val LAUNCHER_COMPONENT = ComponentName("com.google.android.apps.nexuslauncher",
        "com.google.android.apps.nexuslauncher.NexusLauncherActivity")

fun WindowManagerTraceSubject.statusBarWindowIsVisible() {
    isAboveAppWindowVisible(FlickerComponentName.STATUS_BAR)
}

fun WindowManagerTraceSubject.navBarWindowIsVisible() {
    isAboveAppWindowVisible(FlickerComponentName.NAV_BAR)
}

/**
 * If [allStates] is true, checks if the stack space of all displays is fully covered
 * by any visible layer, during the whole transitions
 *
 * Otherwise, checks if the stack space of all displays is fully covered
 * by any visible layer, at the start and end of the transition
 *
 * @param allStates if all states should be checked, otherwise, just initial and final
 */
@JvmOverloads
fun LayersTraceSubject.entireScreenCovered(allStates: Boolean = true) {
    if (allStates) {
        this.invoke("entireScreenCovered") { entry ->
            entry.entry.displays.forEach { display ->
                entry.visibleRegion().coversAtLeast(display.layerStackSpace)
            }
        }
    } else {
        this.first().entry.displays.forEach { display ->
            this.first().visibleRegion().coversAtLeast(display.layerStackSpace)
        }
        this.last().entry.displays.forEach { display ->
            this.last().visibleRegion().coversAtLeast(display.layerStackSpace)
        }
    }
}

fun LayersTraceSubject.navBarLayerIsVisible() {
    this.first().isVisible(FlickerComponentName.NAV_BAR)
    this.last().isVisible(FlickerComponentName.NAV_BAR)
}

fun LayersTraceSubject.statusBarLayerIsVisible() {
    this.first().isVisible(FlickerComponentName.STATUS_BAR)
    this.last().isVisible(FlickerComponentName.STATUS_BAR)
}

fun LayersTraceSubject.navBarLayerRotatesAndScales() {
    val startRotation = this.first().entry.displays.sortedBy { it.id }.firstOrNull()
            ?.transform?.getRotation() ?: Surface.ROTATION_0
    val endRotation = this.last().entry.displays.sortedBy { it.id }.firstOrNull()
            ?.transform?.getRotation() ?: Surface.ROTATION_0

    this.first().visibleRegion(FlickerComponentName.NAV_BAR)
            .coversExactly(WindowUtils.getNavigationBarPosition(startRotation))

    this.last().visibleRegion(FlickerComponentName.NAV_BAR)
            .coversExactly(WindowUtils.getNavigationBarPosition(endRotation))
}

fun LayersTraceSubject.statusBarLayerRotatesScales() {
    val startRotation = this.first().entry.displays.sortedBy { it.id }.firstOrNull()
            ?.transform?.getRotation() ?: Surface.ROTATION_0
    val endRotation = this.last().entry.displays.sortedBy { it.id }.firstOrNull()
            ?.transform?.getRotation() ?: Surface.ROTATION_0

    this.first().visibleRegion(FlickerComponentName.STATUS_BAR)
            .coversExactly(WindowUtils.getNavigationBarPosition(startRotation))

    this.last().visibleRegion(FlickerComponentName.STATUS_BAR)
            .coversExactly(WindowUtils.getNavigationBarPosition(endRotation))
}

fun WindowManagerTraceSubject.statusBarWindowIsAlwaysInvisible() {
    this.isAboveAppWindowInvisible(FlickerComponentName.STATUS_BAR)
}

fun LayersTraceSubject.statusBarLayerIsAlwaysInvisible() {
    this.isInvisible(FlickerComponentName.STATUS_BAR)
}

/**
 * Asserts that:
 *     [originalLayer] is visible at the start of the trace
 *     [originalLayer] becomes invisible during the trace and (in the same entry) [newLayer]
 *         becomes visible
 *     [newLayer] remains visible until the end of the trace
 *
 * @param originalLayer Layer that should be visible at the start
 * @param newLayer Layer that should be visible at the end
 * @param ignoreSnapshot If the snapshot layer should be ignored during the transition
 *     (useful mostly for app launch)
 */
@JvmOverloads
fun LayersTraceSubject.replacesLayer(
    originalLayer: FlickerComponentName,
    newLayer: FlickerComponentName,
    ignoreSnapshot: Boolean = false
) {
    val assertion = isVisible(originalLayer)
    if (ignoreSnapshot) {
        assertion.then()
                .isVisible(FlickerComponentName.SNAPSHOT, isOptional = true)
    }
    assertion.then().isVisible(newLayer)

    this.first().isInvisible(originalLayer)
            .isInvisible(newLayer)

    this.last().isInvisible(originalLayer)
            .isVisible(newLayer)
}