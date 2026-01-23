/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.platform.systemui_tapl.ui.quicksettings

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertTrue

/**
 * System UI test automation object representing the quick settings' internet details tile. All
 * elements are searched within the provided [qsPanel] scope.
 */
class InternetQSDetailsTile(private val qsPanel: UiObject2) {

    /** Verifies the header and waits for the modal transition to complete. */
    fun verifyLoaded() {
        qsPanel.waitForObj(By.text("Internet")) {
            "Internet header not found after clicking Wi-Fi tile."
        }

        // Synchronization: Wait for the searching progress bar to disappear, which indicates
        // network stability.
        assertTrue(
            "Progress Bar $UI_PROGRESS_BAR_ID did not disappear within $WIFI_SEARCH_TIMEOUT.",
            uiDevice.wait(
                Until.gone(sysuiResSelector(UI_PROGRESS_BAR_ID)),
                WIFI_SEARCH_TIMEOUT.inWholeMilliseconds,
            ),
        )

        qsPanel.waitForObj(By.text("Tap a network to connect")) {
            "Instructional label 'Tap a network to connect' not found in the Internet details tile."
        }
    }

    /** Asserts the presence of elements specific to a connected WiFi state. */
    fun verifyConnectedLayout() {
        qsPanel.waitForObj(sysuiResSelector(UI_CONNECTED_LAYOUT_ID)) {
            "Connected Wi-Fi layout $UI_CONNECTED_LAYOUT_ID missing despite toggle being Checked."
        }

        qsPanel.waitForObj(By.text("Connected")) { "Status text 'Connected' not found." }
    }

    /** Asserts the presence of elements specific to a disconnected WiFi state. */
    fun verifyDisconnectedLayout() {
        qsPanel.waitForObj(sysuiResSelector(UI_INTERNET_CONTAINER_ID)) {
            "Internet container $UI_INTERNET_CONTAINER_ID missing in disconnected state."
        }
    }

    /**
     * Verifies that a specific network is visible in the list.
     *
     * @param networkName The SSID (text) of the network to look for.
     */
    fun hasNetwork(networkName: String) {
        qsPanel.waitForObj(sysuiResSelector(UI_WIFI_TITLE_ID).text(networkName)) {
            "Network '$networkName' not found with ID '$UI_WIFI_TITLE_ID' inside the Internet details tile."
        }
    }

    fun verifyCommonActions() {
        qsPanel.waitForObj(By.text("Add network")) { "'Add network' option not found." }

        qsPanel.waitForObj(By.desc("Go to Settings")) { "'Go to Settings' icon not found." }

        qsPanel.waitForObj(By.desc("Back to QS panel")) { "'Back to QS panel' icon not found." }
    }

    companion object {
        private const val UI_PROGRESS_BAR_ID = "wifi_searching_progress"
        private const val UI_CONNECTED_LAYOUT_ID = "wifi_connected_layout"
        private const val UI_INTERNET_CONTAINER_ID = "internet_container"
        private const val UI_WIFI_TITLE_ID = "wifi_title"

        private val WIFI_SEARCH_TIMEOUT: Duration = 30.seconds
    }
}
