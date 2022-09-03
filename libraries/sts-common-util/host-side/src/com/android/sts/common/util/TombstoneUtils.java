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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Contains helper functions and shared constants for crash parsing. */
public class TombstoneUtils {
    public static final String SIGSEGV = "SIGSEGV";
    public static final String SIGBUS = "SIGBUS";
    public static final String SIGABRT = "SIGABRT";

    public static void assertNoSecurityCrashes(List<Tombstone> tombstones, Config config) {
        List<Tombstone> securityCrashes = getSecurityCrashes(tombstones, config);
        assertThat(securityCrashes).isEqualTo(List.of());
    }

    /**
     * @param tombstones a list of tombstones to check
     * @param config crash detection configuration object
     * @return a list of tombstones that are security-related
     */
    public static List<Tombstone> getSecurityCrashes(List<Tombstone> tombstones, Config config) {
        return tombstones.stream()
                .filter(tombstone -> isSecurityCrash(tombstone, config))
                .collect(Collectors.toList());
    }

    /**
     * Determines if a tombstone is likely to be security-related against the given configuration.
     *
     * @param tombstone the tombstone to check
     * @param config crash detection configuration object
     * @return if the tombstone is security-related
     */
    public static boolean isSecurityCrash(Tombstone tombstone, Config config) {

        // match process patterns
        Optional<String> processFilename = getProcessFilename(tombstone);
        if (processFilename.isPresent() && !config.processPatterns.isEmpty()) {
            if (!matchesAny(processFilename.get(), config.processPatterns)) {
                return false;
            }
        }

        // always fail for ASAN/HWASAN crashes for our process
        {
            // ASAN abort message example:
            // ==5661==ERROR: AddressSanitizer: heap-buffer-overflow on address 0x005354830382...
            //
            // HWASAN abort message example:
            // ==13248==ERROR: HWAddressSanitizer: tag-mismatch on address 0x004d84460302...
            String abortMessage = tombstone.getAbortMessage(); // empty proto field returns ""
            if (abortMessage.contains("AddressSanitizer")) {
                return true;
            }
        }

        // always fail for MTE crashes for our process
        if (tombstone.hasSignalInfo()) {
            // https://patchwork.kernel.org/project/linux-mm/patch/20200715170844.30064-5-catalin.marinas@arm.com/
            Signal signalInfo = tombstone.getSignalInfo();
            if (List.of("SEGV_MTEAERR", "SEGV_MTESERR").contains(signalInfo.getName())) {
                return true;
            }
        }

        // match signal
        if (tombstone.hasSignalInfo() && !config.signals.isEmpty()) {
            Signal signalInfo = tombstone.getSignalInfo();
            if (!config.signals.contains(signalInfo.getName())) {
                return false;
            }

            // if check specified, reject crash if address is unlikely to be security-related
            if (config.ignoreLowFaultAddress) {
                if (signalInfo.getHasFaultAddress()) {
                    Long faultAddress = signalInfo.getFaultAddress();
                    if (Long.compareUnsigned(faultAddress, config.maxLowFaultAddress) < 0) {
                        return false;
                    }
                }
            }
        }

        {
            String abortMessage = tombstone.getAbortMessage();
            if (!config.abortMessageIncludes.isEmpty()) {
                if (!config.abortMessageIncludes.stream()
                        .filter(p -> p.matcher(abortMessage).find())
                        .findFirst()
                        .isPresent()) {
                    return false;
                }
            }
            if (config.abortMessageExcludes.stream()
                    .filter(p -> p.matcher(abortMessage).find())
                    .findFirst()
                    .isPresent()) {
                return false;
            }
        }

        /* if backtrace "includes" patterns are present, ignore this crash if there is no
         * frame that matches any of the patterns
         */
        List<Config.BacktraceFilterPattern> backtraceIncludes = config.getBacktraceIncludes();
        if (!backtraceIncludes.isEmpty()) {
            Optional<com.android.server.os.TombstoneProtos.Thread> thread =
                    getMainThread(tombstone);
            if (thread.isPresent()) {
                if (!thread.get().getCurrentBacktraceList().stream()
                        .flatMap(frame -> backtraceIncludes.stream().map(p -> p.match(frame)))
                        .anyMatch(matched -> matched)) {
                    return false;
                }
            }
        }

        /* if backtrace "excludes" patterns are present, ignore this crash if there is any
         * frame that matches any of the patterns
         */
        List<Config.BacktraceFilterPattern> backtraceExcludes = config.getBacktraceExcludes();
        if (!backtraceExcludes.isEmpty()) {
            Optional<com.android.server.os.TombstoneProtos.Thread> thread =
                    getMainThread(tombstone);
            if (thread.isPresent()) {
                if (thread.get().getCurrentBacktraceList().stream()
                        .flatMap(frame -> backtraceExcludes.stream().map(p -> p.match(frame)))
                        .anyMatch(matched -> matched)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** returns true if the input matches any of the patterns. */
    private static boolean matchesAny(String input, Collection<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }

    /** returns the filename of the process. e.g. "/system/bin/mediaserver" returns "mediaserver" */
    public static Optional<String> getProcessFilename(Tombstone tombstone) {
        List<String> commands = tombstone.getCommandLineList();
        if (commands.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new File(commands.get(0)).getName());
    }

    public static Optional<com.android.server.os.TombstoneProtos.Thread> getMainThread(
            Tombstone tombstone) {
        int tid = tombstone.getTid();
        Map<Integer, com.android.server.os.TombstoneProtos.Thread> threadMap =
                tombstone.getThreads();
        if (!threadMap.containsKey(tid)) {
            return Optional.empty();
        }
        return Optional.of(threadMap.get(tid));
    }

    public static class Config {
        private boolean ignoreLowFaultAddress;
        private Long maxLowFaultAddress;
        private List<String> signals;
        private List<Pattern> processPatterns;
        private List<Pattern> abortMessageIncludes;
        private List<Pattern> abortMessageExcludes;
        private List<BacktraceFilterPattern> backtraceIncludes;
        private List<BacktraceFilterPattern> backtraceExcludes;

        public Config() {
            ignoreLowFaultAddress = true;
            maxLowFaultAddress = 0x8000L;
            setSignals(SIGSEGV, SIGBUS);
            abortMessageIncludes = new ArrayList<>();
            setAbortMessageExcludes("CHECK_", "CANNOT LINK EXECUTABLE");
            processPatterns = new ArrayList<>();
            backtraceIncludes = new ArrayList<>();
            backtraceExcludes = new ArrayList<>();
        }

        public Config setMinAddress(String maxLowFaultAddress) {
            this.maxLowFaultAddress = TombstoneParser.parsePointer(maxLowFaultAddress);
            return this;
        }

        public Config setIgnoreLowFaultAddress(boolean ignoreLowFaultAddress) {
            this.ignoreLowFaultAddress = ignoreLowFaultAddress;
            return this;
        }

        public Config setSignals(String... signals) {
            this.signals = new ArrayList(Arrays.asList(signals));
            return this;
        }

        public Config appendSignals(String... signals) {
            Collections.addAll(this.signals, signals);
            return this;
        }

        public Config setAbortMessageIncludes(String... abortMessages) {
            this.abortMessageIncludes = new ArrayList<>(toPatterns(abortMessages));
            return this;
        }

        public Config setAbortMessageIncludes(Pattern... abortMessages) {
            this.abortMessageIncludes = new ArrayList<>(Arrays.asList(abortMessages));
            return this;
        }

        public Config appendAbortMessageIncludes(String... abortMessages) {
            this.abortMessageIncludes.addAll(toPatterns(abortMessages));
            return this;
        }

        public Config appendAbortMessageIncludes(Pattern... abortMessages) {
            Collections.addAll(this.abortMessageIncludes, abortMessages);
            return this;
        }

        public Config setAbortMessageExcludes(String... abortMessages) {
            this.abortMessageExcludes = new ArrayList<>(toPatterns(abortMessages));
            return this;
        }

        public Config setAbortMessageExcludes(Pattern... abortMessages) {
            this.abortMessageExcludes = new ArrayList<>(Arrays.asList(abortMessages));
            return this;
        }

        public Config appendAbortMessageExcludes(String... abortMessages) {
            this.abortMessageExcludes.addAll(toPatterns(abortMessages));
            return this;
        }

        public Config appendAbortMessageExcludes(Pattern... abortMessages) {
            Collections.addAll(this.abortMessageExcludes, abortMessages);
            return this;
        }

        public Config setProcessPatterns(String... processPatternStrings) {
            this.processPatterns = new ArrayList<>(toPatterns(processPatternStrings));
            return this;
        }

        public Config setProcessPatterns(Pattern... processPatterns) {
            this.processPatterns = new ArrayList(Arrays.asList(processPatterns));
            return this;
        }

        public List<Pattern> getProcessPatterns() {
            return Collections.unmodifiableList(processPatterns);
        }

        public Config appendProcessPatterns(String... processPatternStrings) {
            this.processPatterns.addAll(toPatterns(processPatternStrings));
            return this;
        }

        public Config appendProcessPatterns(Pattern... processPatterns) {
            Collections.addAll(this.processPatterns, processPatterns);
            return this;
        }

        public Config setBacktraceIncludes(BacktraceFilterPattern... patterns) {
            this.backtraceIncludes = new ArrayList<>(Arrays.asList(patterns));
            return this;
        }

        public List<BacktraceFilterPattern> getBacktraceIncludes() {
            return Collections.unmodifiableList(this.backtraceIncludes);
        }

        public Config appendBacktraceIncludes(BacktraceFilterPattern... patterns) {
            Collections.addAll(this.backtraceIncludes, patterns);
            return this;
        }

        public Config setBacktraceExcludes(BacktraceFilterPattern... patterns) {
            this.backtraceExcludes = new ArrayList<>(Arrays.asList(patterns));
            return this;
        }

        public List<BacktraceFilterPattern> getBacktraceExcludes() {
            return Collections.unmodifiableList(this.backtraceExcludes);
        }

        public Config appendBacktraceExcludes(BacktraceFilterPattern... patterns) {
            Collections.addAll(this.backtraceExcludes, patterns);
            return this;
        }

        private static List<Pattern> toPatterns(String... patternStrings) {
            return Stream.of(patternStrings).map(Pattern::compile).collect(Collectors.toList());
        }

        /**
         * A utility class that contains patterns to filter backtraces on.
         *
         * <p>A filter matches if any of the backtrace frame matches any of the patterns.
         *
         * <p>Either filenamePattern or methodPattern can be null, in which case it will act like a
         * wildcard pattern and matches anything.
         *
         * <p>A null filename or method name will not match any non-null pattern.
         */
        public static class BacktraceFilterPattern {
            private final Pattern filenamePattern;
            private final Pattern methodPattern;

            /**
             * Constructs a BacktraceFilterPattern with the given file and method name patterns.
             *
             * <p>Null patterns are interpreted as wildcards and match anything.
             *
             * @param filenamePattern Regex string for the filename pattern. Can be null.
             * @param methodPattern Regex string for the method name pattern. Can be null.
             */
            public BacktraceFilterPattern(String filenamePattern, String methodPattern) {
                if (filenamePattern == null) {
                    this.filenamePattern = null;
                } else {
                    this.filenamePattern = Pattern.compile(filenamePattern);
                }

                if (methodPattern == null) {
                    this.methodPattern = null;
                } else {
                    this.methodPattern = Pattern.compile(methodPattern);
                }
            }

            /** Returns true if the current patterns match a backtrace frame. */
            public boolean match(BacktraceFrame frame) {
                if (frame == null) return false;

                String filename = frame.getFileName(); // empty proto field returns ""
                String method = frame.getFunctionName(); // empty proto field returns ""

                boolean filenameMatches =
                        filenamePattern == null || filenamePattern.matcher(filename).find();
                boolean methodMatches =
                        methodPattern == null || methodPattern.matcher(method).find();
                return filenameMatches && methodMatches;
            }
        }
    }
}
