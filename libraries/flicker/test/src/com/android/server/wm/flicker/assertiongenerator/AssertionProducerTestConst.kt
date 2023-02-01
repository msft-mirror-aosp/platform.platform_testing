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

import com.android.server.wm.flicker.assertiongenerator.layers.LayersAssertion
import com.android.server.wm.flicker.service.assertors.ComponentTypeMatcher
import com.android.server.wm.flicker.service.assertors.Components
import com.android.server.wm.traces.common.ComponentNameMatcher

class AssertionProducerTestConst {
    companion object {
        val componentMatcher_id1 = ComponentNameMatcher.NAV_BAR
        val componentMatcher_id2 = ComponentNameMatcher.STATUS_BAR
        val componentMatcher_id4 =
            ComponentNameMatcher(
                "com.google.android.apps.nexuslauncher",
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity"
            )
        var componentMatcher_openApp = ComponentTypeMatcher("openPackage/openApp")

        val expectedAssertionNamesFileTrace =
            "AutomaticallyGenerated_LayersVisibility_NavigationBar0\n" +
                "AutomaticallyGenerated_LayersVisibility_StatusBar\n" +
                "AutomaticallyGenerated_LayersVisibility_OPENING_APP\n" +
                "AutomaticallyGenerated_LayersVisibility_Wallpaper_BBQ_wrapper\n" +
                "AutomaticallyGenerated_LayersVisibility_NexusLauncherActivity\n" +
                "AutomaticallyGenerated_LayersVisibility_Splash_Screen\n" +
                "AutomaticallyGenerated_LayersVisibility_InputMethod\n" +
                "AutomaticallyGenerated_WmFocusedApp\n"

        val expectedLayersAssertionStringsFileTrace =
            listOf(
                ".isVisible(ComponentNameMatcher(\"\", \"NavigationBar0\"))",
                ".isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))",
                ".isInvisible(Components.OPENING_APP).then()" +
                    ".isVisible(Components.OPENING_APP)",
                ".isVisible(ComponentNameMatcher(\"\", \"Wallpaper BBQ wrapper\")).then()" +
                    ".isInvisible(ComponentNameMatcher(\"\", \"Wallpaper BBQ wrapper\"))",
                ".isVisible(ComponentNameMatcher(\"com.google.android.apps.nexuslauncher\"," +
                    " \"com.google.android.apps.nexuslauncher.NexusLauncherActivity\"))" +
                    ".then().isInvisible(ComponentNameMatcher(" +
                    "\"com.google.android.apps.nexuslauncher\", " +
                    "\"com.google.android.apps.nexuslauncher.NexusLauncherActivity\"))",
                ".isInvisible(ComponentNameMatcher(\"\", \"Splash Screen\")).then()" +
                    ".isVisible(ComponentNameMatcher(\"\", \"Splash Screen\"))",
                ".isInvisible(ComponentNameMatcher(\"\", \"InputMethod\"))"
            )

        val expectedAssertionStringsFileTrace =
            expectedLayersAssertionStringsFileTrace +
                listOf(
                    ".isFocusedApp(\"com.google.android.apps.nexuslauncher/" +
                        ".NexusLauncherActivity\").then()" +
                        ".isFocusedApp(\"com.android.server.wm.flicker.testapp/.SimpleActivity\")"
                )

        private fun createExpectedAssertion_id1(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("isVisible(NavigationBar0)", isOptional = false) {
                it.isVisible(componentMatcher_id1)
            }
            assertion.assertionsChecker.add("notContains(NavigationBar0)", isOptional = false) {
                it.notContains(componentMatcher_id1)
            }
            assertion.name = "LayersVisibility_NavigationBar0"
            assertion.assertionString =
                ".isVisible(ComponentNameMatcher(\"\", \"NavigationBar0\"))" +
                    ".then().notContains(ComponentNameMatcher(\"\", \"NavigationBar0\"))"
            return assertion
        }

        private fun createExpectedAssertion_id2(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("isVisible(StatusBar)", isOptional = false) {
                it.isVisible(componentMatcher_id2)
            }
            assertion.assertionsChecker.add("isInvisible(StatusBar)", isOptional = false) {
                it.isInvisible(componentMatcher_id2)
            }
            assertion.assertionsChecker.add("isVisible(StatusBar)", isOptional = false) {
                it.isVisible(componentMatcher_id2)
            }
            assertion.name = "LayersVisibility_StatusBar"
            assertion.assertionString =
                ".isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))" +
                    ".then().isInvisible(ComponentNameMatcher(\"\", \"StatusBar\"))" +
                    ".then().isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))"
            return assertion
        }

        private fun createExpectedAssertion_sameComponentMatcher(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("isVisible(StatusBar)", isOptional = false) {
                it.isVisible(componentMatcher_id2)
            }
            assertion.name = "LayersVisibility_StatusBar"
            assertion.assertionString = ".isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))"
            return assertion
        }

        private fun createExpectedAssertion_id4(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add(
                "isInvisible(NexusLauncherActivity)",
                isOptional = false
            ) { it.isVisible(componentMatcher_id4) }
            assertion.assertionsChecker.add(
                "isVisible(NexusLauncherActivity)",
                isOptional = false
            ) { it.isInvisible(componentMatcher_id4) }
            assertion.name = "LayersVisibility_NexusLauncherActivity"
            assertion.assertionString =
                ".isInvisible(ComponentNameMatcher(\"com.google.android.apps.nexuslauncher\"," +
                    " \"com.google.android.apps.nexuslauncher.NexusLauncherActivity\"))" +
                    ".then().isVisible(ComponentNameMatcher(" +
                    "\"com.google.android.apps.nexuslauncher\"," +
                    " \"com.google.android.apps.nexuslauncher.NexusLauncherActivity\"))"
            return assertion
        }

        private fun createExpectedAssertion_OpenApp(): LayersAssertion {
            componentMatcher_openApp.componentBuilder = Components.OPENING_APP
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("notContains(OPENING_APP)", isOptional = false) {
                it.isVisible(componentMatcher_id4)
            }
            assertion.assertionsChecker.add("isInvisible(OPENING_APP)", isOptional = false) {
                it.isInvisible(componentMatcher_id4)
            }
            assertion.assertionsChecker.add("isVisible(OPENING_APP)", isOptional = false) {
                it.isInvisible(componentMatcher_id4)
            }
            assertion.name = "LayersVisibility_OPENING_APP"
            assertion.assertionString =
                ".notContains(Components.OPENING_APP)" +
                    ".then().isInvisible(Components.OPENING_APP)" +
                    ".then().isVisible(Components.OPENING_APP)"
            return assertion
        }

        private val expected_layer_id1_assertion = createExpectedAssertion_id1()
        private val expected_layer_id2_assertion = createExpectedAssertion_id2()
        private val expected_layer_id4_assertion = createExpectedAssertion_id4()
        private val expected_layer_sameComponentMatcher_assertion =
            createExpectedAssertion_sameComponentMatcher()
        private val expected_layer_openApp_assertion = createExpectedAssertion_OpenApp()

        val expected_layer_visibility_assertions =
            listOf(
                expected_layer_id1_assertion,
                expected_layer_id2_assertion,
                expected_layer_id4_assertion
            )

        val expected_layer_visibility_assertions_sameComponentMatcher =
            listOf(expected_layer_id2_assertion)

        val expected_layer_visibility_assertions_openApp = listOf(expected_layer_openApp_assertion)
        val openAppComponentTypeMatcher =
            ComponentTypeMatcher("openPackage/openApp", Components.OPENING_APP)
        val openAppConfig =
            DeviceTraceConfiguration(mapOf("openPackage/openApp" to Components.OPENING_APP))

        val expected_layer_visibility_assertions_id1 = listOf(expected_layer_id1_assertion)

        private fun createExpectedAllVisibilityAssertions_id2(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("notContains(StatusBar)", isOptional = false) {
                it.notContains(componentMatcher_id2)
            }
            assertion.assertionsChecker.add("isVisible(StatusBar)", isOptional = false) {
                it.isVisible(componentMatcher_id2)
            }
            assertion.assertionsChecker.add("isInvisible(StatusBar)", isOptional = false) {
                it.isInvisible(componentMatcher_id2)
            }
            assertion.name = "LayersVisibility_StatusBar"
            assertion.assertionString =
                ".notContains(ComponentNameMatcher(\"\", \"StatusBar\"))" +
                    ".then().isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))" +
                    ".then().isInvisible(ComponentNameMatcher(\"\", \"StatusBar\"))"
            return assertion
        }

        private fun createExpectedFailAssertion1_id2(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("isVisible(StatusBar)", isOptional = false) {
                it.isVisible(componentMatcher_id2)
            }
            assertion.assertionsChecker.add(".isInvisible(StatusBar)", isOptional = false) {
                it.isInvisible(componentMatcher_id2)
            }
            assertion.name = "LayersVisibility_StatusBar"
            assertion.assertionString =
                ".isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))" +
                    ".then().isInvisible(ComponentNameMatcher(\"\", \"StatusBar\"))"
            return assertion
        }

        private fun createExpectedFailAssertion2_id2(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("isVisible(StatusBar)", isOptional = false) {
                it.isVisible(componentMatcher_id2)
            }
            assertion.name = "LayersVisibility_StatusBar"
            assertion.assertionString = ".isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))"
            return assertion
        }

        private fun createExpectedFailAssertion3_id2(): LayersAssertion {
            val assertion = LayersAssertion()
            assertion.assertionsChecker.add("notContains(StatusBar)", isOptional = false) {
                it.notContains(componentMatcher_id2)
            }
            assertion.assertionsChecker.add("isVisible(StatusBar)", isOptional = false) {
                it.isVisible(componentMatcher_id2)
            }
            assertion.name = "LayersVisibility_StatusBar"
            assertion.assertionString =
                ".notContains(ComponentNameMatcher(\"\", \"StatusBar\"))" +
                    ".then().isVisible(ComponentNameMatcher(\"\", \"StatusBar\"))"
            return assertion
        }
    }
}
