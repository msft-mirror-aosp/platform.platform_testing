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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.FlickerComponentName

/**
 * Class containing the assertions for the rotation transition.
 */
class RotationAssertions {
    // TODO(b/198585124): Add app layer assertions
    fun navBarWindowIsVisible(subject: WindowManagerTraceSubject): FlickerSubjectException? {
        try {
            subject.navBarWindowIsVisible()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun navBarLayerIsVisible(subject: LayersTraceSubject): FlickerSubjectException? {
        try {
            subject.navBarLayerIsVisible()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun navBarLayerRotatesAndScales(subject: LayersTraceSubject): FlickerSubjectException? {
        try {
            subject.navBarLayerRotatesAndScales()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun statusBarLayerRotatesScales(subject: LayersTraceSubject): FlickerSubjectException? {
        try {
            subject.statusBarLayerRotatesScales()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun statusBarLayerIsAlwaysInvisible(subject: LayersTraceSubject): FlickerSubjectException? {
        try {
            subject.statusBarLayerIsAlwaysInvisible()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun statusBarWindowIsAlwaysInvisible(
        subject: WindowManagerTraceSubject
    ): FlickerSubjectException? {
        try {
            subject.statusBarWindowIsAlwaysInvisible()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun visibleLayersShownMoreThanOneConsecutiveEntry(
        subject: LayersTraceSubject
    ): FlickerSubjectException? {
        try {
            subject.visibleLayersShownMoreThanOneConsecutiveEntry(
                ignoreLayers = listOf(FlickerComponentName.SPLASH_SCREEN,
                    FlickerComponentName.SNAPSHOT,
                    FlickerComponentName("", "SecondaryHomeHandle")
                )
            )
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun visibleWindowsShownMoreThanOneConsecutiveEntry(
        subject: WindowManagerTraceSubject
    ): FlickerSubjectException? {
        try {
            subject.visibleWindowsShownMoreThanOneConsecutiveEntry()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }

    fun entireScreenCovered(subject: LayersTraceSubject): FlickerSubjectException? {
        try {
            subject.entireScreenCovered()
        } catch (error: FlickerSubjectException) {
            return error
        }
        return null
    }
}