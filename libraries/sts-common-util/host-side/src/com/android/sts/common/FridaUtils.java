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

import static com.android.sts.common.CommandUtil.runAndCheck;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.tukaani.xz.XZInputStream;

import com.android.compatibility.common.tradefed.build.CompatibilityBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.RunUtil;

public class FridaUtils implements AutoCloseable {
    private static final String PRODUCT_CPU_ABI_KEY = "ro.product.cpu.abi";
    private static final String PRODUCT_CPU_ABILIST_KEY = "ro.product.cpu.abilist";
    private static final String FRIDA_PACKAGE = "frida-inject";
    private static final String FRIDA_OS = "android";
    private static final String TMP_PATH = "/data/local/tmp/";

    private final ITestDevice device;
    private final CompatibilityBuildHelper buildHelper;
    private final String remoteFridaExeName;
    private List<Integer> runningPids = new ArrayList<>();
    private List<String> fridaFiles = new ArrayList<>();

    private FridaUtils(ITestDevice device, IBuildInfo buildInfo, String fridaVersion)
            throws DeviceNotAvailableException, UnsupportedOperationException, IOException {
        this.device = device;
        this.buildHelper = new CompatibilityBuildHelper(buildInfo);

        // Figure out which Frida arch we should be using for our device
        String fridaAbi = getFridaAbiFor(device);
        String fridaExeName =
                String.format("%s-%s-%s-%s", FRIDA_PACKAGE, fridaVersion, FRIDA_OS, fridaAbi);

        // Download Frida if needed
        File localFridaExe;
        try {
            localFridaExe = buildHelper.getTestFile(fridaExeName);
            CLog.d("%s found at %s", fridaExeName, localFridaExe.getAbsolutePath());
        } catch (FileNotFoundException e) {
            String fridaUrl =
                    String.format(
                            "https://github.com/frida/frida/releases/download/%s/%s.xz",
                            fridaVersion, fridaExeName);
            CLog.d("%s not found. Downloading from %s", fridaExeName, fridaUrl);
            try {
                URL url = new URL(fridaUrl);
                URLConnection conn = url.openConnection();
                XZInputStream in = new XZInputStream(conn.getInputStream());
                File tmpOutput = FileUtil.createTempFile("STS", fridaExeName);
                FileUtil.writeToFile(in, tmpOutput);
                localFridaExe = new File(buildHelper.getTestsDir(), fridaExeName);
                FileUtil.copyFile(tmpOutput, localFridaExe);
                tmpOutput.delete();
            } catch (Exception e2) {
                CLog.e(
                        "Could not download Frida. Please manually download '%s' and extract to "
                                + "'%s', renaming the file to '%s' as necessary.",
                        fridaUrl, buildHelper.getTestsDir(), fridaExeName);
                throw e2;
            }
        }

        // Upload Frida binary to device
        device.enableAdbRoot();
        remoteFridaExeName = new File(TMP_PATH, localFridaExe.getName()).getAbsolutePath();
        device.pushFile(localFridaExe, remoteFridaExeName);
        runAndCheck(device, String.format("chmod a+x '%s'", remoteFridaExeName));
        fridaFiles.add(remoteFridaExeName);
        device.disableAdbRoot();
    }

    /**
     * Find out which Frida binary we need and download it if needed.
     *
     * @param device device to use Frida on
     * @param buildInfo test device build info (from test.getBuild())
     * @return an AutoCloseable FridaUtils object that can be used to run Frida scripts with
     */
    public static FridaUtils withFrida(
            ITestDevice device, IBuildInfo buildInfo, String fridaVersion)
            throws DeviceNotAvailableException, UnsupportedOperationException, IOException {
        return new FridaUtils(device, buildInfo, fridaVersion);
    }

    /**
     * Upload and run frida script on given process.
     *
     * @param fridaJsScriptContent Content of the Frida JS script. Note: this is not a file name
     * @param pid PID of the process to attach Frida to
     * @return ByteArrayOutputStream containing stdout and stderr of frida command
     */
    public ByteArrayOutputStream withFridaScript(final String fridaJsScriptContent, int pid)
            throws DeviceNotAvailableException, FileNotFoundException, IOException,
                    TimeoutException, InterruptedException {
        // Upload Frida script to device
        device.enableAdbRoot();
        String uuid = UUID.randomUUID().toString();
        String remoteFridaJsScriptName =
                new File(TMP_PATH, "frida_" + uuid + ".js").getAbsolutePath();
        device.pushString(fridaJsScriptContent, remoteFridaJsScriptName);
        fridaFiles.add(remoteFridaJsScriptName);

        // Execute Frida, binding to given PID, in the background
        List<String> cmd =
                List.of(
                        "adb",
                        "-s",
                        device.getSerialNumber(),
                        "shell",
                        remoteFridaExeName,
                        "-p",
                        String.valueOf(pid),
                        "-s",
                        remoteFridaJsScriptName,
                        "--runtime=v8");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RunUtil.getDefault().runCmdInBackground(cmd, output);

        // frida can fail to attach after a short pause so wait for that
        TimeUnit.SECONDS.sleep(5);
        try {
            Map<Integer, String> pids =
                    ProcessUtil.waitProcessRunning(device, "^" + remoteFridaExeName);
            assertEquals("Unexpected Frida processes with the same name", 1, pids.size());
            runningPids.add(pids.keySet().iterator().next());
        } catch (Exception e) {
            CLog.e(e);
            CLog.e("Frida attach output: %s", output.toString(StandardCharsets.UTF_8));
            throw e;
        }
        device.disableAdbRoot();
        return output;
    }

    @Override
    /** Kill all running Frida processes and delete all files uploaded. */
    public void close() throws DeviceNotAvailableException, TimeoutException {
        device.enableAdbRoot();
        for (Integer pid : runningPids) {
            ProcessUtil.killPid(device, pid.intValue(), 10_000L);
        }
        for (String file : fridaFiles) {
            device.deleteFile(file);
        }
        device.disableAdbRoot();
    }

    /**
     * Return the best ABI of Frida that we should download for given device.
     *
     * <p>Throw UnsupportedOperationException if Frida does not support device's ABI.
     */
    private String getFridaAbiFor(ITestDevice device)
            throws DeviceNotAvailableException, UnsupportedOperationException {
        for (String abi : getSupportedAbis(device)) {
            if (abi.startsWith("arm64")) {
                return "arm64";
            } else if (abi.startsWith("armeabi")) {
                return "arm";
            } else if (abi.startsWith("x86_64")) {
                return "x86_64";
            } else if (abi.startsWith("x86")) {
                return "x86";
            }
        }
        throw new UnsupportedOperationException(
                String.format("Device %s is not supported by Frida", device.getSerialNumber()));
    }

    /** Return a list of supported ABIs by the device in order of preference. */
    private List<String> getSupportedAbis(ITestDevice device) throws DeviceNotAvailableException {
        String primaryAbi = device.getProperty(PRODUCT_CPU_ABI_KEY);
        String[] supportedAbis = device.getProperty(PRODUCT_CPU_ABILIST_KEY).split(",");
        return Stream.concat(Stream.of(primaryAbi), Arrays.stream(supportedAbis))
                .distinct()
                .collect(toList());
    }
}
