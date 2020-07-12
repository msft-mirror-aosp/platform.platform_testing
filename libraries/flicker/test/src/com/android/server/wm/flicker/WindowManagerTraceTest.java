/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.wm.flicker;

import static com.android.server.wm.flicker.TestFileUtils.readTestFile;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Contains {@link WindowManagerTrace} tests. To run this test: {@code atest
 * FlickerLibTest:WindowManagerTraceTest}
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WindowManagerTraceTest {
    private WindowManagerTrace mTrace;

    private static WindowManagerTrace readWindowManagerTraceFromFile(String relativePath) {
        try {
            return WindowManagerTrace.parseFrom(readTestFile(relativePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup() {
        mTrace = readWindowManagerTraceFromFile("wm_trace_openchrome.pb");
    }

    @Test
    public void canParseAllEntries() {
        WindowManagerTraceEntry firstEntry = mTrace.getEntries().get(0);
        assertThat(firstEntry.getTimestamp()).isEqualTo(9213763541297L);
        assertThat(firstEntry.getWindows().size()).isEqualTo(10);
        assertThat(firstEntry.getVisibleWindows().size()).isEqualTo(6);
        assertThat(mTrace.getEntries().get(mTrace.getEntries().size() - 1).getTimestamp())
                .isEqualTo(9216093628925L);
    }

    @Ignore
    @Test
    public void canDetectAboveAppWindowVisibility() {
        WindowManagerTraceEntry entry = mTrace.getEntry(9213763541297L);
        entry.isNonAppWindowVisible("NavigationBar").assertPassed();
        entry.isNonAppWindowVisible("ScreenDecorOverlay").assertPassed();
        entry.isNonAppWindowVisible("StatusBar").assertPassed();
    }

    @Test
    public void canDetectBelowAppWindowVisibility() {
        mTrace.getEntry(9213763541297L)
                .isNonAppWindowVisible("wallpaper").assertPassed();
    }

    @Ignore
    @Test
    public void canDetectAppWindowVisibility() {
        mTrace.getEntry(9213763541297L)
                .isAppWindowVisible("com.google.android.apps.nexuslauncher").assertPassed();
    }

    @Test
    public void canFailWithReasonForVisibilityChecks_windowNotFound() {
        mTrace.getEntry(9213763541297L)
                .isNonAppWindowVisible("ImaginaryWindow")
                .assertFailed("ImaginaryWindow cannot be found");
    }

    @Test
    public void canFailWithReasonForVisibilityChecks_windowNotVisible() {
        mTrace.getEntry(9213763541297L)
                .isNonAppWindowVisible("InputMethod")
                .assertFailed("InputMethod is invisible");
    }

    @Ignore
    @Test
    public void canDetectAppZOrder() {
        mTrace.getEntry(9215551505798L)
                .isVisibleAppWindowOnTop("com.android.chrome").assertPassed();
    }

    @Ignore
    @Test
    public void canFailWithReasonForZOrderChecks_windowNotOnTop() {
        mTrace.getEntry(9215551505798L)
                .isVisibleAppWindowOnTop("com.google.android.apps.nexuslauncher")
                .assertFailed("wanted=com.google.android.apps.nexuslauncher");

        mTrace.getEntry(9215551505798L)
                .isVisibleAppWindowOnTop("com.google.android.apps.nexuslauncher")
                .assertFailed("found=com.android.chrome");
    }
}
