/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.display.color;

import static com.google.common.truth.Truth.assertWithMessage;

import com.android.cuttlefish.tests.CfVkmsEdidHelper;
import com.android.cuttlefish.tests.CfVkmsTester;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(DeviceJUnit4ClassRunner.class)
public class ToggleDisplayPowerTest extends BaseHostJUnit4Test {
    private static final long WAIT_TIME_MS = 2000;
    private ITestDevice mDevice;
    private CfVkmsTester mVkmsTester;

    @Before
    public void setup() throws Exception {
        mDevice = getDevice();

        // Setup VKMS if running on Cuttlefish
        if (mDevice.getProperty("ro.product.name").contains("cf_")) {
            List<CfVkmsTester.VkmsConnectorSetup> connectorConfigs =
                    List.of(
                            CfVkmsTester.VkmsConnectorSetup.builder()
                                    .setType(CfVkmsTester.ConnectorType.EDP)
                                    .setMonitor(CfVkmsEdidHelper.EdpDisplay.REDRIX)
                                    .setEnabledAtStart(true)
                                    .build());
            mVkmsTester = CfVkmsTester.createWithConfig(mDevice, connectorConfigs);
        }

        List<String> ids = getDisplayIds();
        assertWithMessage("Expected exactly one display active at start").that(ids).hasSize(1);

        // Ensure device is in a known state (ON)
        powerReset(ids.get(0));
    }

    @After
    public void tearDown() throws Exception {
        if (mDevice != null) {
            // Restore state
            try {
                powerReset(getPrimaryDisplayId());
            } catch (Exception e) {
                // Ignore if display ID cannot be found during teardown
            }
        }
        if (mVkmsTester != null) {
            mVkmsTester.close();
            mVkmsTester = null;
        }
    }

    private void powerOff(String displayId)
            throws DeviceNotAvailableException, InterruptedException {
        mDevice.executeShellCommand("cmd display power-off " + displayId);
        Thread.sleep(WAIT_TIME_MS);
    }

    private void powerReset(String displayId)
            throws DeviceNotAvailableException, InterruptedException {
        mDevice.executeShellCommand("cmd display power-reset " + displayId);
        Thread.sleep(WAIT_TIME_MS);
    }

    @Test
    public void testInitialState() throws DeviceNotAvailableException {
        ConnectorInfo connector = getConnectedConnector();
        assertWithMessage("No connected connector found").that(connector).isNotNull();
        assertWithMessage("Connected connector should have an active encoder")
                .that(connector.mEncoderId)
                .isNotEqualTo(0);

        int initialEncoderId = connector.mEncoderId;
        EncoderInfo encoder = getEncoder(initialEncoderId);
        assertWithMessage("Could not find encoder with ID %s", initialEncoderId)
                .that(encoder)
                .isNotNull();
        assertWithMessage("Encoder %s should have an active CRTC", initialEncoderId)
                .that(encoder.mCrtcId)
                .isNotEqualTo(0);
    }

    @Test
    public void testPowerOff() throws DeviceNotAvailableException, InterruptedException {
        String displayId = getPrimaryDisplayId();
        ConnectorInfo initialConnector = getConnectedConnector();
        assertWithMessage("No connected connector found initially")
                .that(initialConnector)
                .isNotNull();
        int initialEncoderId = initialConnector.mEncoderId;

        powerOff(displayId);

        ConnectorInfo connector = getConnectedConnector();
        assertWithMessage("No connected connector found after power off")
                .that(connector)
                .isNotNull();
        assertWithMessage("Connector should have no active encoder after power off")
                .that(connector.mEncoderId)
                .isEqualTo(0);

        // The encoder that WAS active should now have CRTC 0
        EncoderInfo encoder = getEncoder(initialEncoderId);
        assertWithMessage("Could not find encoder %s", initialEncoderId).that(encoder).isNotNull();
        assertWithMessage("Encoder %s should have no active CRTC after power off", initialEncoderId)
                .that(encoder.mCrtcId)
                .isEqualTo(0);
    }

    @Test
    public void testPowerReset() throws DeviceNotAvailableException, InterruptedException {
        String displayId = getPrimaryDisplayId();
        // Setup: Turn it off first to verify reset works
        powerOff(displayId);

        powerReset(displayId);

        ConnectorInfo connector = getConnectedConnector();
        assertWithMessage("No connected connector found after power reset")
                .that(connector)
                .isNotNull();
        assertWithMessage("Connector should have an active encoder after power reset")
                .that(connector.mEncoderId)
                .isNotEqualTo(0);

        EncoderInfo encoder = getEncoder(connector.mEncoderId);
        assertWithMessage("Could not find encoder %s", connector.mEncoderId)
                .that(encoder)
                .isNotNull();
        assertWithMessage(
                        "Encoder %s should have an active CRTC after power reset",
                        connector.mEncoderId)
                .that(encoder.mCrtcId)
                .isNotEqualTo(0);
    }

    private List<String> getDisplayIds() throws DeviceNotAvailableException {
        String output = mDevice.executeShellCommand("cmd display get-displays");
        List<String> ids = new ArrayList<>();
        // Matches "Display id <id>:"
        Pattern p = Pattern.compile("Display id (\\d+):");
        Matcher m = p.matcher(output);
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }

    private String getPrimaryDisplayId() throws DeviceNotAvailableException {
        List<String> ids = getDisplayIds();
        if (ids.isEmpty()) {
            throw new RuntimeException("No displays found");
        }
        return ids.get(0);
    }

    private static class ConnectorInfo {
        int mEncoderId;
    }

    private ConnectorInfo getConnectedConnector() throws DeviceNotAvailableException {
        String output = mDevice.executeShellCommand("modetest -c");
        // Format: id encoder status ...
        // Example: 709 708 connected ...
        Pattern p = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(connected|disconnected)\\s+.*");
        for (String line : output.split("\n")) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String status = m.group(3);
                if ("connected".equals(status)) {
                    ConnectorInfo info = new ConnectorInfo();
                    info.mEncoderId = Integer.parseInt(m.group(2));
                    return info;
                }
            }
        }
        return null;
    }

    private static class EncoderInfo {
        int mCrtcId;
    }

    private EncoderInfo getEncoder(int encoderId) throws DeviceNotAvailableException {
        String output = mDevice.executeShellCommand("modetest -e");
        // Format: id crtc type ...
        // Example: 708 199 TMDS ...
        Pattern p = Pattern.compile("^\\s*(\\d+)\\s+(\\d+).*");
        for (String line : output.split("\n")) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                int id = Integer.parseInt(m.group(1));
                if (id == encoderId) {
                    EncoderInfo info = new EncoderInfo();
                    info.mCrtcId = Integer.parseInt(m.group(2));
                    return info;
                }
            }
        }
        return null;
    }
}
