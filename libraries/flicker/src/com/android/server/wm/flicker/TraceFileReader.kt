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

import android.util.Log
import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.ScenarioConfig
import com.android.server.wm.flicker.io.TraceType
import com.android.server.wm.flicker.service.assertors.ComponentBuilder
import com.android.server.wm.flicker.service.assertors.Components
import com.android.server.wm.flicker.service.assertors.ConfigException
import com.android.server.wm.flicker.service.assertors.scenarioInstanceSlice
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.FlickerServiceScenario
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.service.ScenarioType
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.events.EventLogParser
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.transaction.TransactionsTraceParser
import com.android.server.wm.traces.parser.transition.TransitionsTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import com.google.common.io.ByteStreams
import com.google.gson.Gson
import java.lang.reflect.Type

class TraceFileReader {
    companion object {
        /**
         * Creates a device trace dump containing the WindowManager and Layers trace obtained from
         * the contents in a regular trace file, already read and passed as bytearray The parsed
         * traces may contain a multiple [WindowManagerState] or [LayerTraceEntry].
         *
         * @param wmTraceByteArray [WindowManagerTrace] content
         * @param layersTraceByteArray [LayersTrace] content
         */
        @JvmStatic
        fun fromTraceByteArray(
            wmTraceByteArray: ByteArray?,
            layersTraceByteArray: ByteArray?
        ): DeviceTraceDump {
            val wmTrace =
                wmTraceByteArray?.let { WindowManagerTraceParser().parse(wmTraceByteArray) }
            val layersTrace =
                layersTraceByteArray?.let { LayersTraceParser().parse(layersTraceByteArray) }
            return DeviceTraceDump(wmTrace, layersTrace)
        }

        /** Gets the config directory of golden traces used for automatic test generation */
        fun getGoldenTracesConfigDir() = "/assertiongenerator_config"

        /** Read a resource file in bytearray format Return null if the filename doesn't exist */
        fun readBytesFromResource(filename: String): ByteArray? {
            val inputStream = object {}.javaClass.getResourceAsStream(filename) ?: return null
            return ByteStreams.toByteArray(inputStream)
        }

        /** Read a resource file in String format Return null if the filename doesn't exist */
        fun readTextFromResource(filename: String): String? {
            return object {}.javaClass.getResource(filename)?.readText()
        }

        /**
         * Read a list of objects of type ObjectType from a json String For typeToken, create it
         * beforehand with the actual ObjectType as follows: val typeToken = object :
         * TypeToken<List<ObjectType>>() {}.type
         */
        inline fun <reified ObjectType> readJsonFromString(
            jsonString: String,
            typeToken: Type
        ): List<ObjectType> {
            val gson = Gson()
            val jsonFromStr: List<ObjectType> = gson.fromJson(jsonString, typeToken)
            return jsonFromStr
        }

        /**
         * Read a list of objects of type ObjectType from a json file Return null if the filename
         * doesn't exist
         */
        inline fun <reified ObjectType> readObjectFromResource(
            filename: String,
            typeToken: Type
        ): List<ObjectType>? {
            val str = readTextFromResource(filename)
            str?.run {
                return@readObjectFromResource readJsonFromString(this, typeToken)
            }
            return null
        }

        data class TracePaths(
            val wmTracePath: String?,
            val layersTracePath: String?,
            val transactionsTracePath: String?,
            val transitionsTracePath: String?,
            val eventLogPath: String?
        ) {
            fun getDeviceTraceDump(): DeviceTraceDump {
                val wmTrace =
                    wmTracePath?.let {
                        readBytesFromResource(wmTracePath)?.let {
                            try {
                                WindowManagerTraceParser().parse(it)
                            } catch (err: Exception) {
                                // invalid file
                                null
                            }
                        }
                    }

                val layersTrace =
                    layersTracePath?.let {
                        readBytesFromResource(layersTracePath)?.let {
                            try {
                                LayersTraceParser().parse(it)
                            } catch (err: Exception) {
                                // invalid file
                                null
                            }
                        }
                    }

                val transactionsTrace =
                    transactionsTracePath?.let {
                        readBytesFromResource(transactionsTracePath)?.let {
                            try {
                                TransactionsTraceParser().parse(it)
                            } catch (err: Exception) {
                                // invalid file
                                null
                            }
                        }
                    }

                val transitionsTrace =
                    if (transitionsTracePath != null && transactionsTrace != null) {
                        readBytesFromResource(transitionsTracePath)?.let {
                            try {
                                TransitionsTraceParser(transactionsTrace).parse(it)
                            } catch (err: Exception) {
                                // invalid file
                                null
                            }
                        }
                    } else {
                        null
                    }
                val eventsTrace =
                    eventLogPath?.let {
                        readBytesFromResource(eventLogPath)?.let {
                            try {
                                EventLogParser().parse(it)
                            } catch (err: Exception) {
                                // invalid file
                                null
                            }
                        }
                    }
                return DeviceTraceDump(
                    wmTrace,
                    layersTrace,
                    transactionsTrace,
                    transitionsTrace,
                    eventsTrace
                )
            }
        }

        /** Gets the paths of golden traces for a specified scenario */
        fun getGoldenTracePathsForDirectory(dir: String): TracePaths {
            return TracePaths(
                "$dir/${TraceType.WM.fileName}",
                "$dir/${TraceType.SF.fileName}",
                "$dir/${TraceType.TRANSACTION.fileName}",
                "$dir/${TraceType.TRANSITION.fileName}",
                "$dir/${TraceType.EVENT_LOG.fileName}"
            )
        }

        fun getScenarioInstanceFromScenarioFromGoldenTrace(
            scenarioType: ScenarioType,
            deviceTraceDump: DeviceTraceDump
        ): ScenarioInstance {
            val transitionsTrace = deviceTraceDump.transitionsTrace
            val wmTrace = deviceTraceDump.wmTrace
            if (transitionsTrace == null) {
                throw ConfigException(
                    "Transition trace is missing for scenario $scenarioType, " +
                        "so we can't extract scenario instance"
                )
            }
            if (wmTrace == null) {
                throw ConfigException(
                    "WindowManager trace is missing for scenario $scenarioType, " +
                        "so we can't extract scenario instance"
                )
            }
            val scenarioInstances =
                scenarioType.getInstances(transitionsTrace, wmTrace) { m ->
                    Log.d("TraceFileReader", m)
                }
            assert(scenarioInstances.size == 1)
            val scenarioInstance: ScenarioInstance
            try {
                scenarioInstance = (scenarioInstances as MutableList)[0]
            } catch (err: IndexOutOfBoundsException) {
                throw ConfigException(
                    "No scenario instance for scenario $scenarioType, " +
                        "rotation ${wmTrace.getInitialRotation()}"
                )
            }
            return scenarioInstance
        }

        /** Pre-processing step to trim golden traces for scenario */
        fun trimGoldenTracesForScenario(
            scenarioType: ScenarioType,
            deviceTraceDump: DeviceTraceDump
        ): DeviceTraceDump {
            val scenarioInstance =
                getScenarioInstanceFromScenarioFromGoldenTrace(scenarioType, deviceTraceDump)
            val newLayersTrace =
                deviceTraceDump.layersTrace?.scenarioInstanceSlice(scenarioInstance)
            val newWmTrace = deviceTraceDump.wmTrace?.scenarioInstanceSlice(scenarioInstance)
            return DeviceTraceDump(
                newWmTrace,
                newLayersTrace,
                deviceTraceDump.transactionsTrace,
                deviceTraceDump.transitionsTrace
            )
        }

        fun getDeviceTraceConfiguration(
            scenarioInstance: ScenarioInstance
        ): DeviceTraceConfiguration {
            val componentToTypeMap: MutableMap<String, ComponentBuilder> = mutableMapOf()
            try {
                val openCompMatcher =
                    getComponentMatcherWithType(scenarioInstance, Components.OPENING_APP)
                componentToTypeMap[openCompMatcher.toString()] = Components.OPENING_APP
            } catch (_: Exception) {}
            try {
                val closeCompMatcher =
                    getComponentMatcherWithType(scenarioInstance, Components.CLOSING_APP)
                componentToTypeMap[closeCompMatcher.toString()] = Components.CLOSING_APP
            } catch (_: Exception) {}
            return DeviceTraceConfiguration(componentToTypeMap)
        }

        /**
         * Get the component matcher with a given type (component builder) e.g. OPENING_APP,
         * CLOSING_APP
         */
        fun getComponentMatcherWithType(
            scenarioInstance: ScenarioInstance,
            componentBuilder: ComponentBuilder
        ): ComponentNameMatcher {
            return componentBuilder.build(scenarioInstance.associatedTransition)
                as ComponentNameMatcher
        }

        /**
         * Gets the config of golden traces used for automatic test generation Key -> Scenario Value
         * -> Path where the directory of traces for this specific scenario is located
         *
         * @param configDir the config directory -> if null, the default one is retrieved
         *
         * If a custom config directory is specified, it must be placed under
         * src/com/android/server/wm/flicker/service/resources/ and follow a strict structure TODO:
         * detail the structure
         */
        fun getGoldenTracesConfig(
            configDir: String? = null
        ): Map<FlickerServiceScenario, ScenarioConfig> {
            val configDir = configDir ?: getGoldenTracesConfigDir()
            return ScenarioType.scenariosByDescription
                .flatMap { (scenarioDescription, scenarioType) ->
                    PlatformConsts.Rotation.values().map { rotation ->
                        // for (rotation in PlatformConsts.Rotation.values()) {
                        val scenarioDir = "$configDir/$scenarioDescription/${rotation.description}"
                        var traceCount = 1
                        val scenarioDeviceTraceDumpArray: MutableList<DeviceTraceDump> =
                            mutableListOf()
                        val scenarioTraceConfigurationArray: MutableList<DeviceTraceConfiguration> =
                            mutableListOf()
                        while (true) {
                            val traceDir = "$scenarioDir/trace$traceCount"
                            val tracePaths = getGoldenTracePathsForDirectory(traceDir)
                            var deviceTraceDump = tracePaths.getDeviceTraceDump()
                            if (!deviceTraceDump.isValid) {
                                break
                            }
                            val scenarioInstance =
                                getScenarioInstanceFromScenarioFromGoldenTrace(
                                    scenarioType,
                                    deviceTraceDump
                                )
                            deviceTraceDump =
                                trimGoldenTracesForScenario(scenarioType, deviceTraceDump)
                            val deviceTraceConfiguration =
                                getDeviceTraceConfiguration(scenarioInstance)
                            scenarioDeviceTraceDumpArray.add(deviceTraceDump)
                            scenarioTraceConfigurationArray.add(deviceTraceConfiguration)
                            traceCount++
                        }
                        val scenario = FlickerServiceScenario(scenarioType, rotation)
                        scenario to
                            ScenarioConfig(
                                scenarioDeviceTraceDumpArray.toTypedArray(),
                                scenarioTraceConfigurationArray.toTypedArray()
                            )
                    }
                }
                .toMap()
        }
    }
}
