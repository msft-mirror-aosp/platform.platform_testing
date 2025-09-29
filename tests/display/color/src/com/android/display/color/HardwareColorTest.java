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

import static com.google.common.truth.Truth.assertThat;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.testtype.junit4.BeforeClassWithInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(DeviceJUnit4ClassRunner.class)
public class HardwareColorTest extends BaseHostJUnit4Test {
    private ITestDevice mDevice;
    private List<String> mCtmValues;

    private static final String NIGHT_LIGHT_SETTING = "night_display_activated";
    private static final String COLOR_ACCESSIBILITY_SETTING =
            "accessibility_display_daltonizer_enabled";
    private static final String COLOR_INVERSION_SETTING = "accessibility_display_inversion_enabled";

    // Matches a hexadecimal blob
    private static final Pattern HEX_DATA_PATTERN = Pattern.compile("^\\s+([0-9a-fA-F]+)\\s*$");
    // Matches a modetest header
    private static final Pattern PROPERTY_HEADER_PATTERN =
            Pattern.compile("^\\s*\\d+\\s+[A-Z0-9_]+:.*");
    private static final String CTM_IDENTITY =
            "00000000010000000000000000000000"
                    + "00000000000000000000000000000000"
                    + "00000000010000000000000000000000"
                    + "00000000000000000000000000000000"
                    + "0000000001000000";
    // The duration of the night light animation
    // See TintController.TRANSITION_DURATION
    private static final long TRANSITION_DURATION = 3000L;

    /**
     * Prepares the device for CTM property testing
     *
     * @param testInfo Test Information
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *     recovered.
     */
    @BeforeClassWithInfo
    public static void init(TestInformation testInfo) throws DeviceNotAvailableException {
        ITestDevice device = testInfo.getDevice();
        device.enableAdbRoot();
        device.setProperty("vendor.hwc.drm.ctm", "DRM_OR_IGNORE");
        device.executeShellCommand("stop && start");
        device.waitForDeviceAvailable();
    }

    @Before
    public void setup() throws DeviceNotAvailableException {
        mDevice = getDevice();
        populateCtmValues(mDevice.executeShellCommand("modetest -p"));
        assertThat(mCtmValues.size()).isGreaterThan(0);
        for (String value : mCtmValues) assertThat(value).isEqualTo(CTM_IDENTITY);
    }

    @After
    public void tearDown() throws DeviceNotAvailableException, InterruptedException {
        disableSecureSetting(NIGHT_LIGHT_SETTING);
        disableSecureSetting(COLOR_ACCESSIBILITY_SETTING);
        disableSecureSetting(COLOR_INVERSION_SETTING);
        Thread.sleep(TRANSITION_DURATION);
        mDevice.waitForDeviceAvailable();
    }

    private void enableSecureSetting(String setting) throws DeviceNotAvailableException {
        mDevice.setSetting("secure", setting, "1");
    }

    private void disableSecureSetting(String setting) throws DeviceNotAvailableException {
        mDevice.setSetting("secure", setting, "null");
    }

    private void populateCtmValues(String modetestOutput) {
        if (modetestOutput == null || modetestOutput.isEmpty()) {
            mCtmValues = null;
        }

        StringBuilder hexData = new StringBuilder();
        boolean inCtmBlock = false;
        boolean inValueSection = false;

        List<String> ctmValues = new ArrayList<>();

        for (String line : modetestOutput.lines().toList()) {
            if (inValueSection) {
                Matcher hexMatcher = HEX_DATA_PATTERN.matcher(line);
                if (hexMatcher.matches()) {
                    hexData.append(hexMatcher.group(1));
                } else {
                    if (hexData.length() > 0) {
                        ctmValues.add(hexData.toString());
                        hexData.setLength(0);
                    }
                    inValueSection = false;
                    inCtmBlock = false;
                }
                continue;
            }

            if (inCtmBlock) {
                if (line.trim().equals("value:")) {
                    inValueSection = true;
                } else if (PROPERTY_HEADER_PATTERN.matcher(line).matches()) {
                    inCtmBlock = false;
                }
                continue;
            }

            if (line.contains("CTM:")) {
                inCtmBlock = true;
            }
        }
        mCtmValues = ctmValues;
    }

    @Test
    public void testSetNightLight() throws DeviceNotAvailableException, InterruptedException {
        enableSecureSetting(NIGHT_LIGHT_SETTING);
        Thread.sleep(TRANSITION_DURATION);
        populateCtmValues(mDevice.executeShellCommand("modetest -p"));
        assertThat(mCtmValues.size()).isGreaterThan(0);
        for (String value : mCtmValues) assertThat(value).isNotEqualTo(CTM_IDENTITY);
    }

    @Test
    public void testSetColorCorrection() throws DeviceNotAvailableException {
        enableSecureSetting(COLOR_ACCESSIBILITY_SETTING);
        populateCtmValues(mDevice.executeShellCommand("modetest -p"));
        assertThat(mCtmValues.size()).isGreaterThan(0);
        for (String value : mCtmValues) assertThat(value).isNotEqualTo(CTM_IDENTITY);
    }

    @Test
    public void testSetColorInversion() throws DeviceNotAvailableException {
        enableSecureSetting(COLOR_INVERSION_SETTING);
        populateCtmValues(mDevice.executeShellCommand("modetest -p"));
        assertThat(mCtmValues.size()).isGreaterThan(0);
        // TODO(406267714): Color inversion is not supported in DRM HWC and must be performed by the
        // client
        for (String value : mCtmValues) assertThat(value).isEqualTo(CTM_IDENTITY);
    }
}
