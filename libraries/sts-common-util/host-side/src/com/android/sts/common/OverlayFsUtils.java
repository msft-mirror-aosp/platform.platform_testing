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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.google.common.hash.Hashing;

/** TestWatcher that enables writing to read-only partitions and reboots device when done. */
public class OverlayFsUtils extends TestWatcher {
    private static final String OVERLAYFS_PREFIX = "overlay_sts_";

    private final BaseHostJUnit4Test test;

    // output of `stat`, e.g. "root shell 755 u:object_r:vendor_file:s0"
    static final Pattern PERM_PATTERN =
            Pattern.compile(
                    "^(?<user>[a-zA-Z0-9_-]+) (?<group>[a-zA-Z0-9_-]+) (?<perm>[0-7]+)"
                            + " (?<secontext>.*)$");

    public OverlayFsUtils(BaseHostJUnit4Test test) {
        assertNotNull("Need to pass in a valid testcase object.", test);
        this.test = test;
    }

    /**
     * Mounts an OverlayFS dir over the top most common dir in the list.
     *
     * <p>The directory should be writable after this returns successfully. To cleanup, reboot the
     * device as unfortunately unmounting overlayfs is complicated.
     *
     * @param dir The directory to make writable. Directories with single quotes are not supported.
     */
    public void makeWritable(final String dir)
            throws DeviceNotAvailableException, IOException, IllegalStateException {
        ITestDevice device = test.getDevice();
        assertNotNull("device not set.", device);
        assertTrue("dir needs to be an absolute path.", dir.startsWith("/"));

        // Check and make sure we have not already mounted over this dir. We do that by hashing
        // the lower dir path and put that as part of the device ID for `mount`.
        String dirHash = Hashing.md5().hashString(dir, StandardCharsets.UTF_8).toString();
        String id = OVERLAYFS_PREFIX + dirHash;
        CommandResult res = device.executeShellV2Command("mount | grep -q " + id);
        if (res.getStatus() == CommandStatus.SUCCESS) {
            // a mount with the same ID already exists
            throw new IllegalStateException(dir + " has already been made writable.");
        }

        assertTrue("Can't acquire root for " + device.getSerialNumber(), device.enableAdbRoot());

        // Match permissions of upper dir to lower dir
        String statOut =
                CommandUtil.runAndCheck(device, "stat -c '%U %G %a %C' '" + dir + "'").getStdout();
        Matcher m = PERM_PATTERN.matcher(statOut);
        assertTrue("Bad stats output: " + statOut, m.find());
        String user = m.group("user");
        String group = m.group("group");
        String unixPerm = m.group("perm");
        String seContext = m.group("secontext");

        Path tempdir = Paths.get("/mnt", "stsoverlayfs", id);
        String upperdir = tempdir.resolve("upper").toString();
        String workdir = tempdir.resolve("workdir").toString();

        CommandUtil.runAndCheck(device, String.format("mkdir -p '%s' '%s'", upperdir, workdir));
        CommandUtil.runAndCheck(device, String.format("chown %s:%s '%s'", user, group, upperdir));
        CommandUtil.runAndCheck(device, String.format("chcon '%s' '%s'", seContext, upperdir));
        CommandUtil.runAndCheck(device, String.format("chmod %s '%s'", unixPerm, upperdir));

        String mountCmd =
                String.format(
                        "mount -t overlay '%s' -o lowerdir='%s',upperdir='%s',workdir='%s' '%s'",
                        id, dir, upperdir, workdir, dir);
        CommandUtil.runAndCheck(device, mountCmd);
    }

    public boolean anyOverlayFsMounted() throws DeviceNotAvailableException {
        ITestDevice device = test.getDevice();
        assertNotNull("Device not set", device);
        CommandResult res = device.executeShellV2Command("mount | grep -q " + OVERLAYFS_PREFIX);
        return res.getStatus() == CommandStatus.SUCCESS;
    }

    @Override
    public void finished(Description d) {
        ITestDevice device = test.getDevice();
        assertNotNull("Device not set", device);
        try {
            if (anyOverlayFsMounted()) {
                device.rebootUntilOnline();
                device.waitForDeviceAvailable();
            }
        } catch (DeviceNotAvailableException e) {
            throw new AssertionError("Device unavailable when cleaning up", e);
        }
    }
}
