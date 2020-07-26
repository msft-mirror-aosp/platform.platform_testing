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
import static com.google.common.truth.Truth.assertWithMessage;

import android.graphics.Region;

import androidx.test.runner.AndroidJUnit4;

import com.android.server.wm.nano.WindowStateProto;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.Set;

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

    @Test
    public void canDetectAboveAppWindowVisibility() {
        WindowManagerTraceEntry entry = mTrace.getEntry(9213763541297L);
        entry.isAboveAppWindow("NavigationBar").assertPassed();
        entry.isAboveAppWindow("ScreenDecorOverlay").assertPassed();
        entry.isAboveAppWindow("StatusBar").assertPassed();

        entry.isAboveAppWindow("pip-dismiss-overlay").assertFailed("is invisible");
        entry.isAboveAppWindow("NotificationShade").assertFailed("is invisible");
        entry.isAboveAppWindow("InputMethod").assertFailed("is invisible");
        entry.isAboveAppWindow("AssistPreviewPanel").assertFailed("is invisible");
    }

    @Test
    public void canDetectWindowCoversAtLeastRegion() {
        WindowManagerTraceEntry entry = mTrace.getEntry(9213763541297L);
        // Exact size
        entry.coversAtLeastRegion("StatusBar", new Region(0, 0, 1440, 171)).assertPassed();
        entry.coversAtLeastRegion(
                        "com.google.android.apps.nexuslauncher", new Region(0, 0, 1440, 2960))
                .assertPassed();

        // Smaller region
        entry.coversAtLeastRegion("StatusBar", new Region(0, 0, 100, 100)).assertPassed();
        entry.coversAtLeastRegion(
                        "com.google.android.apps.nexuslauncher", new Region(0, 0, 100, 100))
                .assertPassed();

        // Larger region
        entry.coversAtLeastRegion("StatusBar", new Region(0, 0, 1441, 171))
                .assertFailed("Uncovered region: SkRegion((1440,0,1441,171))");
        entry.coversAtLeastRegion(
                        "com.google.android.apps.nexuslauncher", new Region(0, 0, 1440, 2961))
                .assertFailed("Uncovered region: SkRegion((0,2960,1440,2961))");
    }

    @Test
    public void canDetectWindowCoversAtMostRegion() {
        WindowManagerTraceEntry entry = mTrace.getEntry(9213763541297L);
        // Exact size
        entry.coversAtMostRegion("StatusBar", new Region(0, 0, 1440, 171)).assertPassed();
        entry.coversAtMostRegion(
                        "com.google.android.apps.nexuslauncher", new Region(0, 0, 1440, 2960))
                .assertPassed();

        // Smaller region
        entry.coversAtMostRegion("StatusBar", new Region(0, 0, 100, 100))
                .assertFailed("Out-of-bounds region: SkRegion((100,0,1440,100)(0,100,1440,171))");
        entry.coversAtMostRegion(
                        "com.google.android.apps.nexuslauncher", new Region(0, 0, 100, 100))
                .assertFailed("Out-of-bounds region: SkRegion((100,0,1440,100)(0,100,1440,2960))");

        // Larger region
        entry.coversAtMostRegion("StatusBar", new Region(0, 0, 1441, 171)).assertPassed();
        entry.coversAtMostRegion(
                        "com.google.android.apps.nexuslauncher", new Region(0, 0, 1440, 2961))
                .assertPassed();
    }

    @Test
    public void canDetectBelowAppWindowVisibility() {
        mTrace.getEntry(9213763541297L).hasNonAppWindow("wallpaper").assertPassed();
    }

    @Test
    public void canDetectAppWindow() {
        Set<WindowStateProto> appWindows = mTrace.getEntry(9213763541297L).getAppWindows();
        assertWithMessage("Unable to detect app windows").that(appWindows.size()).isEqualTo(2);
    }

    @Test
    public void canDetectAppWindowVisibility() {
        mTrace.getEntry(9213763541297L)
                .isAppWindowVisible("com.google.android.apps.nexuslauncher").assertPassed();

        mTrace.getEntry(9215551505798L).isAppWindowVisible("com.android.chrome").assertPassed();
    }

    @Test
    public void canFailWithReasonForVisibilityChecks_windowNotFound() {
        mTrace.getEntry(9213763541297L)
                .hasNonAppWindow("ImaginaryWindow")
                .assertFailed("ImaginaryWindow cannot be found");
    }

    @Test
    public void canFailWithReasonForVisibilityChecks_windowNotVisible() {
        mTrace.getEntry(9213763541297L)
                .hasNonAppWindow("InputMethod")
                .assertFailed("InputMethod is invisible");
    }

    @Test
    public void canDetectAppZOrder() {
        mTrace.getEntry(9215551505798L)
                .isAppWindowVisible("com.google.android.apps.nexuslauncher")
                .assertPassed();

        mTrace.getEntry(9215551505798L)
                .isVisibleAppWindowOnTop("com.android.chrome").assertPassed();
    }

    @Test
    public void canFailWithReasonForZOrderChecks_windowNotOnTop() {
        mTrace.getEntry(9215551505798L)
                .isVisibleAppWindowOnTop("com.google.android.apps.nexuslauncher")
                .assertFailed("wanted=com.google.android.apps.nexuslauncher");

        mTrace.getEntry(9215551505798L)
                .isVisibleAppWindowOnTop("com.google.android.apps.nexuslauncher")
                .assertFailed("found=Splash Screen com.android.chrome");
    }
}
