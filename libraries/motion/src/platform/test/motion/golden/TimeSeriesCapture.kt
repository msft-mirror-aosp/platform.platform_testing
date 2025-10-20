/*
 * Copyright (C) 2024 The Android Open Source Project
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

package platform.test.motion.golden

class TimeSeriesCaptureScope<T>(
    private val observing: T?,
    private val valueCollector: MutableMap<String, MutableList<DataPoint<*>>>,
) {

    /**
     * Records a [DataPoint] from [observing], extracted [capture] and stored in the time-series as
     * [name].
     *
     * If the backing [observing] object cannot be resolved during an animation frame,
     * `DataPoint.notFound` is recorded in the time-series.
     *
     * @param name unique, human-readable label under which the feature is stored in the time-series
     * @param capture lambda function that extracts a [DataPoint] from the [observing] object. This
     *   is only invoked if [observing] is not null.
     */
    fun feature(name: String, capture: (T) -> DataPoint<*>) {
        val dataPoint = if (observing != null) capture(observing) else DataPoint.notFound<Any>()
        valueCollector.getOrPut(name) { mutableListOf() }.add(dataPoint)
    }

    /**
     * Captures features on other, related objects.
     *
     * @param resolveRelated finds the related object on which to capture features, invoked once per
     *   animation frame. Can return null if the related object is not currently available in the
     *   scene.
     * @param nestedTimeSeriesCapture captures features on the related object.
     */
    fun <U> on(
        resolveRelated: (T) -> U?,
        nestedTimeSeriesCapture: TimeSeriesCaptureScope<U>.() -> Unit,
    ) {
        with(TimeSeriesCaptureScope(observing?.let(resolveRelated), valueCollector)) {
            nestedTimeSeriesCapture()
        }
    }
}

/**
 * Captures a time-series feature of an observed [T].
 *
 * A [DataPoint] of type [V] is recorded at each frame.
 */
class FeatureCapture<T, V : Any>(val name: String, val capture: (T) -> DataPoint<V>)

/**
 * Records a [DataPoint], extracted [capture] the specified [FeatureCapture] and stored in the
 * time-series as [name].
 *
 * @param name unique, human-readable label under which the feature is stored in the time-series
 * @param capture [FeatureCapture] that extracts a [DataPoint]
 */
fun <T> TimeSeriesCaptureScope<T>.feature(name: String, capture: FeatureCapture<in T, *>) {
    feature(name) { capture.capture(it) }
}

/**
 * Records a [DataPoint], extracted [capture] the specified [FeatureCapture] and stored in the
 * time-series as [FeatureCapture.name] .
 *
 * @param capture [FeatureCapture] that extracts a [DataPoint]
 */
fun <T> TimeSeriesCaptureScope<T>.feature(capture: FeatureCapture<in T, *>) {
    feature(capture.name, capture)
}
