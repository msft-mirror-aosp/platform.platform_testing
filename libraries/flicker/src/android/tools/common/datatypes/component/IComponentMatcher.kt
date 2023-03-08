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

package android.tools.common.datatypes.component

import android.tools.common.traces.surfaceflinger.Layer
import android.tools.common.traces.wm.Activity
import android.tools.common.traces.wm.WindowContainer
import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
interface IComponentMatcher {
    @JsName("or")
    fun or(other: IComponentMatcher): IComponentMatcher {
        return OrComponentMatcher(arrayOf(this, other))
    }

    /**
     * @param window to search
     * @return if any of the [components] matches [window]
     */
    @JsName("windowMatchesAnyOf")
    fun windowMatchesAnyOf(window: WindowContainer): Boolean = windowMatchesAnyOf(arrayOf(window))

    /**
     * @param windows to search
     * @return if any of the [components] matches any of [windows]
     */
    @JsName("windowMatchesAnyOfCollection")
    fun windowMatchesAnyOf(windows: Collection<WindowContainer>): Boolean =
        windowMatchesAnyOf(windows.toTypedArray())

    /**
     * @param windows to search
     * @return if any of the [windows] fit the matching conditions of the matcher
     */
    @JsName("windowMatchesAnyOfArray")
    fun windowMatchesAnyOf(windows: Array<WindowContainer>): Boolean

    /**
     * @param activity to search
     * @return if any of the [components] matches [activity]
     */
    @JsName("activityMatchesAnyOf")
    fun activityMatchesAnyOf(activity: Activity): Boolean = activityMatchesAnyOf(arrayOf(activity))

    /**
     * @param activities to search
     * @return if any of the [components] matches any of [activities]
     */
    @JsName("activityMatchesAnyOfCollection")
    fun activityMatchesAnyOf(activities: Collection<Activity>): Boolean =
        activityMatchesAnyOf(activities.toTypedArray())

    /**
     * @param activities to search
     * @return if any of the [components] matches any of [activities]
     */
    @JsName("activityMatchesAnyOfArray")
    fun activityMatchesAnyOf(activities: Array<Activity>): Boolean

    /**
     * @param layer to search
     * @return if any of the [components] matches [layer]
     */
    @JsName("layerMatchesAnyOf")
    fun layerMatchesAnyOf(layer: Layer): Boolean = layerMatchesAnyOf(arrayOf(layer))

    /**
     * @param layers to search
     * @return if any of the [components] matches any of [layers]
     */
    @JsName("layerMatchesAnyOfCollection")
    fun layerMatchesAnyOf(layers: Collection<Layer>): Boolean =
        layerMatchesAnyOf(layers.toTypedArray())

    @JsName("layerMatchesAnyOfArray") fun layerMatchesAnyOf(layers: Array<Layer>): Boolean

    /**
     * @return an identifier string that provides enough information to determine which activities
     *
     * ```
     *         the matcher is looking to match. Mostly used for debugging purposes in error messages
     * ```
     */
    fun toActivityIdentifier(): String

    /**
     * @return an identifier string that provides enough information to determine which windows the
     *
     * ```
     *         matcher is looking to match. Mostly used for debugging purposes in error messages.
     * ```
     */
    @JsName("toWindowIdentifier") fun toWindowIdentifier(): String

    /**
     * @return an identifier string that provides enough information to determine which layers the
     *
     * ```
     *         matcher is looking to match. Mostly used for debugging purposes in error messages.
     * ```
     */
    @JsName("toLayerIdentifier") fun toLayerIdentifier(): String

    /**
     * @param layers Collection of layers check for matches
     * @param condition A function taking the matched layers of a base level component and returning
     *
     * ```
     *              true or false base on if the check succeeded.
     * @return
     * ```
     *
     * true iff all the check condition is satisfied according to the ComponentMatcher's
     *
     * ```
     *         defined execution of it.
     * ```
     */
    @JsName("check")
    fun check(layers: Collection<Layer>, condition: (Collection<Layer>) -> Boolean): Boolean
}
