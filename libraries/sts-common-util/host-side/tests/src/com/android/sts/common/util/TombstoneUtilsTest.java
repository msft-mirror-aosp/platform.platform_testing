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

package com.android.sts.common.util;

import static com.google.common.truth.Truth.*;

import com.android.server.os.TombstoneProtos.*;
import com.android.sts.common.util.TombstoneUtils.Config.BacktraceFilterPattern;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Unit tests for {@link TombstoneUtils}. */
@RunWith(JUnit4.class)
public class TombstoneUtilsTest {

    private static List<Tombstone> sTombstones;

    @BeforeClass
    public static void setUp() throws IOException {
        String logcat = null;
        try (InputStream is =
                TombstoneUtilsTest.class.getClassLoader().getResourceAsStream("logcat.txt")) {
            logcat = new String(is.readAllBytes());
        }
        sTombstones = TombstoneParser.parseLogcat(logcat);
    }

    @Test
    @Ignore
    public void dumpCurrentTombstones() throws Exception {
        File tmp = new File("/tmp/tombstones");
        if (!tmp.exists()) {
            tmp.mkdir();
        }
        for (File f : tmp.listFiles()) {
            f.delete();
        }

        int index = 0;
        for (Tombstone tombstone : sTombstones) {
            try (FileOutputStream os =
                    new FileOutputStream(String.format("%s/tombstone-%03d.pb", tmp, index))) {
                tombstone.writeTo(os);
            }
            index++;
        }
    }

    @Test
    // If this test fails, update the res/tombstones files with the results of
    // `dumpCurrentTombstones`. Diff the files to ensure that the fields are updating as expected
    // and we're not losing data.
    public void testGetAllCrashes() throws Exception {
        List<Tombstone> expectedResults = new ArrayList<>();
        int index = 0;
        while (true) {
            String filename = String.format("tombstones/tombstone-%03d.pb", index);
            index++;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
                if (is == null) {
                    // couldn't find resource; stopping
                    break;
                }
                Tombstone tombstone = Tombstone.parseFrom(is.readAllBytes());
                expectedResults.add(tombstone);
            }
        }
        assertThat(expectedResults).isEqualTo(sTombstones);
    }

    @Test
    public void testValidCrash() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0")))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testMissingName() throws Exception {
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("")));
    }

    @Test
    public void testSIGABRT() throws Exception {
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones, new TombstoneUtils.Config().setProcessPatterns("installd"));
    }

    @Test
    public void testFaultAddressBelowMin() throws Exception {
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("synthetic_process_1")));
    }

    @Test
    public void testIgnoreMinAddressCheck() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(false)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_1")))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testBadAbortMessage() throws Exception {
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("generic")));
    }

    @Test
    public void testGoodAndBadCrashes() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"),
                                                        Pattern.compile("generic")))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testNullFaultAddress() throws Exception {
        List<Tombstone> crashes = new ArrayList<>();
        crashes.add(
                Tombstone.newBuilder()
                        .addCommandLine("com.android.bluetooth")
                        .setSignalInfo(Signal.newBuilder().setCodeName("SIGSEGV").build())
                        .build());
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        crashes,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile(
                                                                "com\\.android\\.bluetooth")))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testAbortMessageInclude() throws Exception {
        List<Tombstone> crashes = new ArrayList<>();
        crashes.add(
                Tombstone.newBuilder()
                        .addCommandLine("com.android.bluetooth")
                        .setSignalInfo(Signal.newBuilder().setCodeName("SIGABRT").build())
                        .setAbortMessage(
                                "'[FATAL:allocation_tracker.cc(143)] Check failed: map_entry !="
                                        + " allocations.end().")
                        .build());

        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        crashes,
                                        new TombstoneUtils.Config()
                                                .appendSignals(TombstoneUtils.SIGABRT)
                                                .appendAbortMessageIncludes("Check failed:")
                                                .setProcessPatterns(
                                                        Pattern.compile(
                                                                "com\\.android\\.bluetooth")))
                                .size())
                .isNotEqualTo(List.of());

        TombstoneUtils.assertNoSecurityCrashes(
                crashes,
                new TombstoneUtils.Config()
                        .appendSignals(TombstoneUtils.SIGABRT)
                        .appendAbortMessageIncludes("include not matches")
                        .setProcessPatterns(Pattern.compile("com\\.android\\.bluetooth")));
    }

    @Test
    public void testAbortMessageExclude() throws Exception {
        List<Tombstone> crashes = new ArrayList<>();
        crashes.add(
                Tombstone.newBuilder()
                        .addCommandLine("com.android.bluetooth")
                        .setSignalInfo(Signal.newBuilder().setCodeName("SIGABRT").build())
                        .setAbortMessage(
                                "'[FATAL:allocation_tracker.cc(143)] Check failed: map_entry !="
                                        + " allocations.end().")
                        .build());

        TombstoneUtils.assertNoSecurityCrashes(
                crashes,
                new TombstoneUtils.Config()
                        .appendSignals(TombstoneUtils.SIGABRT)
                        .appendAbortMessageExcludes("Check failed:")
                        .setProcessPatterns(Pattern.compile("com\\.android\\.bluetooth")));

        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        crashes,
                                        new TombstoneUtils.Config()
                                                .appendSignals(TombstoneUtils.SIGABRT)
                                                .appendAbortMessageExcludes("exclude not matches")
                                                .setProcessPatterns(
                                                        Pattern.compile(
                                                                "com\\.android\\.bluetooth")))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testAbortMessageExcludeCannotLink() throws Exception {
        List<Tombstone> crashes = new ArrayList<>();
        crashes.add(
                Tombstone.newBuilder()
                        .addCommandLine("/data/local/tmp/CVE-2020-0073")
                        .setSignalInfo(Signal.newBuilder().setCodeName("SIGABRT").build())
                        .setAbortMessage(
                                "'CANNOT LINK EXECUTABLE \"/data/local/tmp/CVE-2020-0073\": library"
                                        + " \"libnfc-nci.so\" (\"(default)\","
                                        + " \"/data/local/tmp/CVE-2020-0073\", \"\") not found'")
                        .build());

        TombstoneUtils.assertNoSecurityCrashes(
                crashes,
                new TombstoneUtils.Config()
                        .appendSignals(TombstoneUtils.SIGABRT)
                        .setProcessPatterns(Pattern.compile("CVE-2020-0073")));

        crashes.add(
                Tombstone.newBuilder()
                        .addCommandLine("/data/local/tmp/CVE-2015-6616-2")
                        .setSignalInfo(Signal.newBuilder().setCodeName("SIGABRT").build())
                        .setAbortMessage(
                                "'CANNOT LINK EXECUTABLE \"/data/local/tmp/CVE-2015-6616-2\":"
                                    + " cannot locate symbol \""
                                    + "_ZN7android14MediaExtractor17CreateFromServiceERKNS_2spINS_10DataSourceEEEPKc\""
                                    + " referenced by \"/data/local/tmp/CVE-2015-6616-2\"...'")
                        .build());

        TombstoneUtils.assertNoSecurityCrashes(
                crashes,
                new TombstoneUtils.Config()
                        .appendSignals(TombstoneUtils.SIGABRT)
                        .setProcessPatterns(Pattern.compile("CVE-2015-6616-2")));
    }

    @Test
    public void testBacktraceFilterIncludeFilename() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceIncludes(
                                                        new BacktraceFilterPattern(
                                                                "libaudioutils", null)))
                                .size())
                .isNotEqualTo(List.of());

        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceIncludes(
                                                        new BacktraceFilterPattern(
                                                                "libstagefright", null),
                                                        new BacktraceFilterPattern(
                                                                "libaudioflinger\\.so", null)))
                                .size())
                .isNotEqualTo(List.of());

        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setProcessPatterns(Pattern.compile("synthetic_process_0"))
                        .setBacktraceIncludes(new BacktraceFilterPattern("libstagefright", null)));
    }

    @Test
    public void testBacktraceFilterExcludeFilename() throws Exception {
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("synthetic_process_0"))
                        .setBacktraceExcludes(new BacktraceFilterPattern("libaudioutils", null)));
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("synthetic_process_0"))
                        .setBacktraceExcludes(
                                new BacktraceFilterPattern("libstagefright", null),
                                new BacktraceFilterPattern("libaudioflinger\\.so", null)));

        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceExcludes(
                                                        new BacktraceFilterPattern(
                                                                "libstagefright", null)))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testBacktraceFilterIncludeMethodName() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceIncludes(
                                                        new BacktraceFilterPattern(
                                                                null, "memcpy_to_float")))
                                .size())
                .isNotEqualTo(List.of());
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceIncludes(
                                                        new BacktraceFilterPattern(null, "strlen"),
                                                        new BacktraceFilterPattern(
                                                                null, "memcpy_[^_]+_float")))
                                .size())
                .isNotEqualTo(List.of());

        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("synthetic_process_0"))
                        .setBacktraceIncludes(new BacktraceFilterPattern(null, "strlen")));
    }

    @Test
    public void testBacktraceFilterExcludeMethodName() throws Exception {
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("synthetic_process_0"))
                        .setBacktraceExcludes(new BacktraceFilterPattern(null, "memcpy_to_float")));
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("synthetic_process_0"))
                        .setBacktraceExcludes(
                                new BacktraceFilterPattern(null, "strlen"),
                                new BacktraceFilterPattern(null, "memcpy_[^_]+_float")));
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceExcludes(
                                                        new BacktraceFilterPattern(null, "strlen")))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testBacktraceFilterCombinations() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceIncludes(
                                                        new BacktraceFilterPattern(null, null)))
                                .size())
                .isNotEqualTo(List.of());
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setIgnoreLowFaultAddress(true)
                                                .setProcessPatterns(
                                                        Pattern.compile("synthetic_process_0"))
                                                .setBacktraceIncludes(
                                                        new BacktraceFilterPattern(
                                                                "libaudioutils", "memcpy")))
                                .size())
                .isNotEqualTo(List.of());
        TombstoneUtils.assertNoSecurityCrashes(
                sTombstones,
                new TombstoneUtils.Config()
                        .setIgnoreLowFaultAddress(true)
                        .setProcessPatterns(Pattern.compile("synthetic_process_0"))
                        .setBacktraceIncludes(
                                new BacktraceFilterPattern("libaudioutils", "strlen")));
    }

    @Test
    public void testMteAlwaysFails() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setProcessPatterns(
                                                        Pattern.compile("com.redacted.mte-fail")))
                                .size())
                .isNotEqualTo(List.of());
    }

    @Test
    public void testAsanAlwaysFails() throws Exception {
        assertThat(
                        TombstoneUtils.getSecurityCrashes(
                                        sTombstones,
                                        new TombstoneUtils.Config()
                                                .setProcessPatterns(
                                                        Pattern.compile("/data/data/avrc_poc")))
                                .size())
                .isNotEqualTo(List.of());
    }
}
