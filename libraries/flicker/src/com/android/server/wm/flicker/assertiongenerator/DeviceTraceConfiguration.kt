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

package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.service.assertors.ComponentBuilder
import com.android.server.wm.flicker.service.assertors.Components

/**
 * Contains the configuration of a [DeviceTraceDump]
 *
 * In the future, more values can be added if necessary,
 * requiring minimum modifications in the assertion production pipeline
 */
data class DeviceTraceConfiguration(
    /**
     * Map from component name -> component type
     * e.g. "openingName" -> "OPENING_APP"
     *      "closingName" -> "CLOSING_APP"
     */
    val componentToTypeMap: Map<String, ComponentBuilder>
) {
    override fun equals(other: Any?): Boolean {
        return other is DeviceTraceConfiguration &&
            componentToTypeMap == other.componentToTypeMap
    }

    override fun hashCode(): Int {
        return componentToTypeMap.hashCode()
    }

    companion object{
        /**
         * Converts a simplified trace config to the real trace config
         */
        fun fromSimplifiedTrace(
            traceConfigurationFromFile: DeviceTraceConfigurationSimplified
        ): DeviceTraceConfiguration {
            val actualComponentToType = traceConfigurationFromFile.componentToTypeMap.map{
                (component, componentType) ->
                val componentBuilder = Components.byType[componentType]
                if (componentBuilder != null) {
                    component to componentBuilder
                } else throw RuntimeException("Component builder is null")
            }.toMap()
            return DeviceTraceConfiguration(actualComponentToType)
        }
    }
}

/**
 * A format of [DeviceTraceConfiguration] that can be easily read from a JSON file.
 * Example of a difference:
 * componentToTypeMap is Map<String, String> instead of Map<String, ComponentBuilder>,
 * because ComponentBuilder is too complicated to encode in JSON format.
 */
class DeviceTraceConfigurationSimplified(
    val componentToTypeMap: Map<String, String>
)
