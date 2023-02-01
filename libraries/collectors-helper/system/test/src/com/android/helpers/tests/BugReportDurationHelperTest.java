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
package com.android.helpers.tests;

import androidx.test.runner.AndroidJUnit4;

import com.android.helpers.BugReportDurationHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Android Unit tests for {@link BugReportDurationHelper}.
 *
 * <p>atest CollectorsHelperTest:com.android.helpers.BugReportDurationHelperTest
 */
@RunWith(AndroidJUnit4.class)
public class BugReportDurationHelperTest {

    private static final String TAG = BugReportDurationHelperTest.class.getSimpleName();

    // Comparison of floating-point numbers with assertEquals requires a maximum delta.
    private static final double DELTA = 0.00001;

    private BugReportDurationHelper helper;
    private File testDir;

    @Before
    public void setUp() throws IOException {
        testDir = Files.createTempDirectory("test_dir").toFile();
        helper = new BugReportDurationHelper(testDir.getPath());
        helper.startCollecting();
    }

    @After
    public void tearDown() {
        // stopCollecting() is currently a no-op but is included here in case it is updated.
        helper.stopCollecting();

        // Deletes the files in the test directory, then the test directory.
        File[] files = testDir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                f.delete();
            }
        }
        testDir.delete();
    }

    // Creates a .zip archive with an identically-named .txt file containing the input lines.
    private File createArchive(String name, List<String> lines) throws IOException {
        File f = new File(testDir, name + ".zip");
        FileOutputStream fos = new FileOutputStream(f);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ZipOutputStream zos = new ZipOutputStream(bos);
        try {
            zos.putNextEntry(new ZipEntry(name + ".txt"));
            for (String line : lines) {
                zos.write((line + "\n").getBytes());
            }
            zos.closeEntry();
        } finally {
            zos.close();
        }
        return f;
    }

    @Test
    public void testGetMetrics() throws IOException {
        List<String> lines =
                Arrays.asList(
                        "------ 44.619s was the duration of \'dumpstate_board()\' ------",
                        "------ 21.397s was the duration of \'DUMPSYS\' ------",
                        "------ 0.022s was the duration of \'DUMPSYS CRITICAL PROTO\' ------",
                        "unrelated log line");

        createArchive("bugreport", lines);

        Map<String, Double> metrics = helper.getMetrics();
        assertEquals(3, metrics.size());
        assertEquals(44.619, metrics.get("bugreport-duration-dumpstate_board()"), DELTA);
        assertEquals(21.397, metrics.get("bugreport-duration-dumpsys"), DELTA);
        assertEquals(0.022, metrics.get("bugreport-duration-dumpsys-critical-proto"), DELTA);
    }

    @Test
    public void testGetLatestBugReport() throws IOException {
        List<String> empty = new ArrayList<>();
        createArchive("bugreport-2022-04-23-03-12-33", empty);
        createArchive("bugreport-2022-04-20-21-44-11", empty);
        createArchive("bugreport-2021-12-28-10-32-10", empty);
        assertEquals("bugreport-2022-04-23-03-12-33.zip", helper.getLatestBugReport());
    }

    @Test
    public void testExtractAndFilterBugReport() throws IOException {
        String line1 = "------ 44.619s was the duration of \'dumpstate_board()\' ------";
        String line2 = "------ 21.397s was the duration of \'DUMPSYS\' ------";
        String invalidLine = "unrelated log line";
        String showmapLine =
                "------ 0.076s was the duration of \'SHOW MAP 22930 (com.android.chrome)\' ------";
        List<String> lines = Arrays.asList(line1, line2, invalidLine, showmapLine);

        File archive = createArchive("bugreport", lines);

        String zip = archive.getName();

        ArrayList<String> filtered = helper.extractAndFilterBugReport(zip);
        assertTrue(filtered.contains(line1));
        assertTrue(filtered.contains(line2));
        assertFalse(filtered.contains(invalidLine));
        assertFalse(filtered.contains(showmapLine));
    }

    @Test
    public void testParseDuration() {
        String line1 = "------ 44.619s was the duration of \'dumpstate_board()\' ------";
        String line2 = "------ 21.397s was the duration of \'DUMPSYS\' ------";
        // This isn't a "real" case, since parseDuration() is only ever passed valid lines.
        String invalidLine = "unrelated log line";
        assertEquals(44.619, helper.parseDuration(line1), DELTA);
        assertEquals(21.397, helper.parseDuration(line2), DELTA);
        assertEquals(-1, helper.parseDuration(invalidLine), DELTA);
    }

    @Test
    public void testParseSection() {
        String line1 = "------ 44.619s was the duration of \'dumpstate_board()\' ------";
        String line2 = "------ 21.397s was the duration of \'DUMPSYS\' ------";
        // This isn't a "real" case, since parseSection() is only ever passed valid lines.
        String invalidLine = "unrelated log line";
        assertEquals("dumpstate_board()", helper.parseSection(line1));
        assertEquals("DUMPSYS", helper.parseSection(line2));
        assertNull(helper.parseSection(invalidLine));
    }

    @Test
    public void testConvertSectionToKey() {
        String section1 = "PROCRANK";
        String section2 = "PROCESSES AND THREADS";
        String section3 = "dumpstate_board()";
        assertEquals("bugreport-duration-procrank", helper.convertSectionToKey(section1));
        assertEquals(
                "bugreport-duration-processes-and-threads", helper.convertSectionToKey(section2));
        assertEquals("bugreport-duration-dumpstate_board()", helper.convertSectionToKey(section3));
    }
}
