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

package com.android.sts.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeThat;

import static java.util.stream.Collectors.joining;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Setup and run a native PoC, asserting exit conditions */
public class NativePoc {
    static final long DEFAULT_POC_TIMEOUT_SECONDS = 60;
    static final String TMP_PATH = "/data/local/tmp/";
    static final String RESOURCE_ROOT = "/";
    static final int BUF_SIZE = 65536;

    private final String pocName;
    private final ImmutableList<String> args;
    private final ImmutableMap<String, String> envVars;
    private final boolean useDefaultLdLibraryPath;
    private final long timeoutSeconds;
    private final ImmutableList<String> resources;
    private final String resourcePushLocation;
    private final boolean only32;
    private final boolean only64;
    private final NativePoc.AfterFunction after;
    private final NativePocAsserter asserter;
    private final boolean assumePocExitSuccess;

    private NativePoc(
            String pocName,
            ImmutableList<String> args,
            ImmutableMap<String, String> envVars,
            boolean useDefaultLdLibraryPath,
            long timeoutSeconds,
            ImmutableList<String> resources,
            String resourcePushLocation,
            boolean only32,
            boolean only64,
            NativePoc.AfterFunction after,
            NativePocAsserter asserter,
            boolean assumePocExitSuccess) {
        this.pocName = pocName;
        this.args = args;
        this.envVars = envVars;
        this.useDefaultLdLibraryPath = useDefaultLdLibraryPath;
        this.timeoutSeconds = timeoutSeconds;
        this.resources = resources;
        this.resourcePushLocation = resourcePushLocation;
        this.only32 = only32;
        this.only64 = only64;
        this.after = after;
        this.asserter = asserter;
        this.assumePocExitSuccess = assumePocExitSuccess;
    }

    String pocName() {
        return pocName;
    }

    ImmutableList<String> args() {
        return args;
    }

    ImmutableMap<String, String> envVars() {
        return envVars;
    }

    boolean useDefaultLdLibraryPath() {
        return useDefaultLdLibraryPath;
    }

    long timeoutSeconds() {
        return timeoutSeconds;
    }

    ImmutableList<String> resources() {
        return resources;
    }

    String resourcePushLocation() {
        return resourcePushLocation;
    }

    boolean only32() {
        return only32;
    }

    boolean only64() {
        return only64;
    }

    NativePoc.AfterFunction after() {
        return after;
    }

    NativePocAsserter asserter() {
        return asserter;
    }

    boolean assumePocExitSuccess() {
        return assumePocExitSuccess;
    }

    public String toString() {
        return "NativePoc{"
                + "pocName="
                + pocName
                + ", "
                + "args="
                + args
                + ", "
                + "envVars="
                + envVars
                + ", "
                + "useDefaultLdLibraryPath="
                + useDefaultLdLibraryPath
                + ", "
                + "timeoutSeconds="
                + timeoutSeconds
                + ", "
                + "resources="
                + resources
                + ", "
                + "resourcePushLocation="
                + resourcePushLocation
                + ", "
                + "only32="
                + only32
                + ", "
                + "only64="
                + only64
                + ", "
                + "after="
                + after
                + ", "
                + "asserter="
                + asserter
                + ", "
                + "assumePocExitSuccess="
                + assumePocExitSuccess
                + "}";
    }

    public static Builder builder() {
        return new Builder()
                .args(ImmutableList.of())
                .envVars(ImmutableMap.of())
                .useDefaultLdLibraryPath(false)
                .timeoutSeconds(DEFAULT_POC_TIMEOUT_SECONDS)
                .resources(ImmutableList.of())
                .resourcePushLocation(TMP_PATH)
                .after((res) -> {})
                .only32(false)
                .only64(false)
                .asserter(new NativePocAsserter() {})
                .assumePocExitSuccess(true);
    }

    public static class Builder {
        private String pocName;
        private ImmutableList<String> args;
        private ImmutableMap<String, String> envVars;
        private Boolean useDefaultLdLibraryPath;
        private Long timeoutSeconds;
        private ImmutableList<String> resources;
        private String resourcePushLocation;
        private Boolean only32;
        private Boolean only64;
        private NativePoc.AfterFunction after;
        private NativePocAsserter asserter;
        private Boolean assumePocExitSuccess;

        Builder() {}

        public Builder pocName(String pocName) {
            this.pocName = pocName;
            return this;
        }

        String pocName() {
            return pocName;
        }

        public Builder args(List<String> args) {
            this.args = ImmutableList.copyOf(args);
            return this;
        }

        public Builder args(String... args) {
            this.args = ImmutableList.copyOf(args);
            return this;
        }

        public Builder envVars(Map<String, String> envVars) {
            this.envVars = ImmutableMap.copyOf(envVars);
            return this;
        }

        ImmutableMap<String, String> envVars() {
            return envVars;
        }

        public Builder useDefaultLdLibraryPath(boolean useDefaultLdLibraryPath) {
            this.useDefaultLdLibraryPath = useDefaultLdLibraryPath;
            return this;
        }

        boolean useDefaultLdLibraryPath() {
            return useDefaultLdLibraryPath;
        }

        Builder timeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder timeoutSeconds(long value, String reason) {
            return timeoutSeconds(value);
        }

        public Builder resources(List<String> resources) {
            this.resources = ImmutableList.copyOf(resources);
            return this;
        }

        public Builder resources(String... resources) {
            this.resources = ImmutableList.copyOf(resources);
            return this;
        }

        public Builder resourcePushLocation(String resourcePushLocation) {
            this.resourcePushLocation = resourcePushLocation;
            return this;
        }

        String resourcePushLocation() {
            return resourcePushLocation;
        }

        Builder only32(boolean only32) {
            this.only32 = only32;
            return this;
        }

        public Builder only32() {
            return only32(true);
        }

        Builder only64(boolean only64) {
            this.only64 = only64;
            return this;
        }

        public Builder only64() {
            return only64(true);
        }

        public Builder after(NativePoc.AfterFunction after) {
            this.after = after;
            return this;
        }

        public Builder asserter(NativePocAsserter asserter) {
            this.asserter = asserter;
            return this;
        }

        public Builder assumePocExitSuccess(boolean assumePocExitSuccess) {
            this.assumePocExitSuccess = assumePocExitSuccess;
            return this;
        }

        public NativePoc build() {
            if (useDefaultLdLibraryPath()) {
                updateLdLibraryPath();
            }
            if (!resourcePushLocation().endsWith("/")) {
                resourcePushLocation(resourcePushLocation() + "/");
            }
            NativePoc nativePoc =
                    new NativePoc(
                            this.pocName,
                            this.args,
                            this.envVars,
                            this.useDefaultLdLibraryPath,
                            this.timeoutSeconds,
                            this.resources,
                            this.resourcePushLocation,
                            this.only32,
                            this.only64,
                            this.after,
                            this.asserter,
                            this.assumePocExitSuccess);
            assertFalse("both only32 & only64 are set!", nativePoc.only32() && nativePoc.only64());
            assertNotNull("pocName not set!", nativePoc.pocName());
            return nativePoc;
        }

        private void updateLdLibraryPath() {
            String key = "LD_LIBRARY_PATH";
            String newVal;
            if (envVars().containsKey(key)) {
                newVal = envVars().get(key) + ":/system/lib64:/system/lib";
            } else {
                newVal = "/system/lib64:/system/lib";
            }
            Map<String, String> newMap =
                    new HashMap<String, String>() {
                        {
                            putAll(envVars());
                            put(key, newVal);
                        }
                    };
            envVars(ImmutableMap.copyOf(newMap));
        }
    }

    /**
     * Execute the PoC with the given parameters and assertions.
     *
     * @param test the instance of BaseHostJUnit4Test this is running in
     */
    public void run(final BaseHostJUnit4Test test) throws Exception {
        CLog.d("Trying to start NativePoc: %s", this.toString());
        CommandResult res = runPocAndAssert(test);
        assumeThat(
                "PoC timed out. You may want to make it faster or specify timeout amount",
                res.getStatus(),
                not(CommandStatus.TIMED_OUT));
        if (assumePocExitSuccess()) {
            assumeThat(
                    "PoC did not exit with success. stderr: " + res.getStderr(),
                    res.getStatus(),
                    is(CommandStatus.SUCCESS));
        }
    }

    private CommandResult runPocAndAssert(final BaseHostJUnit4Test test) throws Exception {
        ITestDevice device = test.getDevice();

        try (AutoCloseable aPoc = withPoc(test, device);
                AutoCloseable aRes = withResourcesUpload(device);
                AutoCloseable aAssert = asserter().withAutoCloseable(this, device)) {
            // Setup environment variable shell command prefix
            String envStr =
                    envVars().keySet().stream()
                            .map(k -> String.format("%s='%s'", k, escapeQuote(envVars().get(k))))
                            .collect(joining(" "));

            // Setup command arguments string for shell
            String argStr = args().stream().map(s -> escapeQuote(s)).collect(joining(" "));

            // Run the command
            CommandResult res =
                    device.executeShellV2Command(
                            String.format("cd %s; %s ./%s %s", TMP_PATH, envStr, pocName(), argStr),
                            timeoutSeconds(),
                            TimeUnit.SECONDS,
                            0 /* retryAttempts */);
            CLog.d(
                    "PoC exit code: %d\nPoC stdout:\n%s\nPoC stderr:\n%s\n",
                    res.getExitCode(), res.getStdout(), res.getStderr());

            after().run(res);
            asserter().checkCmdResult(res);
            return res;
        }
    }

    private static String escapeQuote(String s) {
        return s.replace("'", "'\"'\"'");
    }

    private AutoCloseable withPoc(final BaseHostJUnit4Test test, final ITestDevice device)
            throws DeviceNotAvailableException, FileNotFoundException {
        PocPusher pocPusher =
                new PocPusher().setDevice(device).setBuild(test.getBuild()).setAbi(test.getAbi());
        if (only32()) {
            pocPusher.only32();
        }
        if (only64()) {
            pocPusher.only64();
        }
        final String remoteFile = TMP_PATH + pocName();
        pocPusher.pushFile(pocName(), remoteFile);
        device.executeShellV2Command(String.format("chmod 777 '%s'", remoteFile));
        CommandUtil.runAndCheck(device, String.format("test -r '%s'", remoteFile));
        CommandUtil.runAndCheck(device, String.format("test -w '%s'", remoteFile));
        CommandUtil.runAndCheck(device, String.format("test -x '%s'", remoteFile));

        return new AutoCloseable() {
            @Override
            public void close() throws DeviceNotAvailableException {
                device.deleteFile(remoteFile);
            }
        };
    }

    private AutoCloseable withResourcesUpload(final ITestDevice device)
            throws DeviceNotAvailableException, IOException {
        for (String resource : resources()) {
            File resTmpFile = File.createTempFile("STSNativePoc", "");
            try {
                try (InputStream in =
                                NativePoc.class.getResourceAsStream(RESOURCE_ROOT + resource);
                        OutputStream out =
                                new BufferedOutputStream(new FileOutputStream(resTmpFile))) {
                    byte[] buf = new byte[BUF_SIZE];
                    int chunkSize;
                    while ((chunkSize = in.read(buf)) != -1) {
                        out.write(buf, 0, chunkSize);
                    }
                }

                device.pushFile(resTmpFile, resourcePushLocation() + resource);
            } finally {
                resTmpFile.delete();
            }
        }

        return new AutoCloseable() {
            @Override
            public void close() throws DeviceNotAvailableException {
                tryRemoveResources(device);
            }
        };
    }

    private void tryRemoveResources(ITestDevice device) throws DeviceNotAvailableException {
        for (String resource : resources()) {
            device.deleteFile(resourcePushLocation() + resource);
        }
    }

    public static interface AfterFunction {
        void run(CommandResult res) throws Exception;
    }
}
