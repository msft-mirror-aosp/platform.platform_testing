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

import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.ScenarioConfig
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceConfigurationSimplified
import com.android.server.wm.flicker.assertiongenerator.windowmanager.WmTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.windowmanager.WmTraceConfigurationSimplified
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.transaction.TransactionsTraceParser
import com.android.server.wm.traces.parser.transition.TransitionsTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import com.google.common.io.ByteStreams
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

class TraceFileReader {
    companion object {
        /**
         * Creates a device trace dump containing the WindowManager and Layers trace
         * obtained from the contents in a regular trace file, already read and passed as bytearray
         * The parsed traces may contain a multiple
         * [WindowManagerState] or [LayerTraceEntry].
         *
         * @param wmTraceByteArray [WindowManagerTrace] content
         * @param layersTraceByteArray [LayersTrace] content
         */
        @JvmStatic
        fun fromTraceByteArray(wmTraceByteArray: ByteArray?, layersTraceByteArray: ByteArray?):
        DeviceTraceDump {
            val wmTrace = wmTraceByteArray?.let {
                WindowManagerTraceParser.parseFromTrace(wmTraceByteArray)
            }
            val layersTrace = layersTraceByteArray?.let {
                LayersTraceParser.parseFromTrace(data = layersTraceByteArray)
            }
            return DeviceTraceDump(wmTrace, layersTrace)
        }

        /**
         * Gets the config directory of golden traces used for automatic test generation
         */
        fun getGoldenTracesConfigDir() = "/assertiongenerator_config"

        /**
         * Read a resource file in bytearray format
         * Return null if the filename doesn't exist
         */
        fun readBytesFromResource(filename: String): ByteArray? {
            val inputStream = object {}.javaClass.getResourceAsStream(filename) ?: return null
            return ByteStreams.toByteArray(inputStream)
        }

        /**
         * Read a resource file in String format
         * Return null if the filename doesn't exist
         */
        fun readTextFromResource(filename: String): String? {
            return object {}.javaClass.getResource(filename)?.readText()
        }

        fun <ObjectType> readJsonFromStringBad(jsonString: String): ObjectType {
            val gson = Gson()
            val objType = object : TypeToken<ObjectType>() {}.type
            var obj: ObjectType = gson.fromJson(jsonString, objType)
            return obj
        }

        /**
         * Read a list of objects of type ObjectType from a json String
         * For typeToken, create it beforehand with the actual ObjectType as follows:
         * val typeToken = object : TypeToken<List<ObjectType>>() {}.type
         */
        inline fun <reified ObjectType> readJsonFromString(jsonString: String, typeToken: Type):
            List<ObjectType> {
            val gson = Gson()
            val jsonFromStr: List<ObjectType> = gson.fromJson(jsonString, typeToken)
            return jsonFromStr
        }

        /**
         * Read a list of objects of type ObjectType from a json file
         * Return null if the filename doesn't exist
         */
        inline fun <reified ObjectType> readObjectFromResource(filename: String, typeToken: Type):
            List<ObjectType>? {
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
        ) {
            fun getDeviceTraceDump(): DeviceTraceDump {
                val wmTrace = wmTracePath?.let{
                    readBytesFromResource(wmTracePath)?.let {
                        try {
                            WindowManagerTraceParser.parseFromTrace(it)
                        } catch (err: Exception) {
                            // invalid file
                            null
                        }
                    }
                }

                val layersTrace = layersTracePath?.let {
                    readBytesFromResource(layersTracePath)?.let {
                        try {
                            LayersTraceParser.parseFromTrace(it)
                        } catch (err: Exception) {
                            // invalid file
                            null
                        }
                    }
                }

                val transactionsTrace = transactionsTracePath?.let {
                    readBytesFromResource(transactionsTracePath)?.let {
                        try {
                            TransactionsTraceParser.parseFromTrace(it)
                        } catch (err: Exception) {
                            // invalid file
                            null
                        }
                    }
                }

                val transitionsTrace = if (
                    transitionsTracePath != null &&
                    transactionsTrace != null
                ) {
                    readBytesFromResource(transitionsTracePath)?.let {
                        try {
                            TransitionsTraceParser.parseFromTrace(it, transactionsTrace)
                        } catch (err: Exception) {
                            // invalid file
                            null
                        }
                    }
                } else {
                    null
                }
                return DeviceTraceDump(wmTrace, layersTrace, transactionsTrace, transitionsTrace)
            }
        }

        data class TraceConfigurationPaths(
            val wmConfigPath: String?,
            val layersConfigPath: String?,
        ) {
            fun getDeviceTraceConfiguration(): DeviceTraceConfiguration {
                val wmTraceConfiguration = wmConfigPath?.let{
                    val wmType = object : TypeToken<List<WmTraceConfigurationSimplified>>() {}.type
                    val simplifiedConfig = readObjectFromResource<WmTraceConfigurationSimplified>(
                        wmConfigPath, wmType)?.let{ it[0] }
                    simplifiedConfig?.let{ WmTraceConfiguration.fromSimplifiedTrace(it) }
                }

                val layersTraceConfiguration = layersConfigPath?.let{
                    val layersType =
                        object : TypeToken<List<LayersTraceConfigurationSimplified>>() {}.type
                    val simplifiedConfig =
                        readObjectFromResource<LayersTraceConfigurationSimplified>(
                            layersConfigPath, layersType)?.let{ it[0] }
                    simplifiedConfig?.let{ LayersTraceConfiguration.fromSimplifiedTrace(it) }
                }

                return DeviceTraceConfiguration(wmTraceConfiguration, layersTraceConfiguration)
            }
        }

        /**
         * Gets the paths of golden traces for a specified scenario
         */
        fun getGoldenTracePathsForDirectory(dir: String): TracePaths {
            return TracePaths(
                "$dir/wm_trace.winscope",
                "$dir/layers_trace.winscope",
                "$dir/transactions_trace.winscope",
                "$dir/transitions_trace.winscope"
            )
        }

        /**
         * Gets the paths of golden trace configs for a specified scenario
         *
         * We should handle if these files don't exist and just return null when reading,
         * because assertions for hardcoded components can be generated even without configuration
         */
        fun getGoldenTraceConfigPathsForDirectory(dir: String): TraceConfigurationPaths {
            return TraceConfigurationPaths(
                "$dir/wm_trace_configuration.json",
                "$dir/layers_trace_configuration.json",
            )
        }

        /**
         * Gets the config of golden traces used for automatic test generation
         * Key -> Scenario
         * Value -> Path where the directory of traces for this specific scenario is located
         *
         * @param configDir the config directory -> if null, the default one is retrieved
         *
         * If a custom config directory is specified, it must be placed under
         * src/com/android/server/wm/flicker/service/resources/ and follow a strict structure
         * TODO: detail the structure
         */
        fun getGoldenTracesConfig(configDir: String? = null): Map<Scenario, ScenarioConfig> {
            val configDir = configDir ?: getGoldenTracesConfigDir()
            return Scenario.scenariosByDescription.map { (scenarioDescription, scenario) ->
                val scenarioDir = "$configDir/$scenarioDescription"
                var traceCount = 1
                val scenarioDeviceTraceDumpArray: MutableList<DeviceTraceDump> = mutableListOf()
                val scenarioTraceConfigurationArray: MutableList<DeviceTraceConfiguration> =
                    mutableListOf()
                while (true) {
                    val traceDir = "$scenarioDir/trace$traceCount"
                    val tracePaths = getGoldenTracePathsForDirectory(traceDir)
                    val deviceTraceDump = tracePaths.getDeviceTraceDump()
                    if (!deviceTraceDump.isValid) {
                        break
                    }
                    val traceConfigPaths = getGoldenTraceConfigPathsForDirectory(traceDir)
                    val deviceTraceConfiguration =
                        traceConfigPaths.getDeviceTraceConfiguration()
                    scenarioDeviceTraceDumpArray.add(deviceTraceDump)
                    scenarioTraceConfigurationArray.add(deviceTraceConfiguration)
                    traceCount++
                }
                scenario to ScenarioConfig(
                    scenarioDeviceTraceDumpArray.toTypedArray(),
                    scenarioTraceConfigurationArray.toTypedArray()
                )
            }.toMap()
        }

        fun getAllFilesInDirectory(directoryPath: String): Stream<Path>? {
            val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
            val resourcesPath = Paths.get(
                projectDirAbsolutePath,
                "src/com/android/server/wm/flicker/service/resources/",
                directoryPath
            )
            return Files.walk(resourcesPath, 1)
        }
    }
}
