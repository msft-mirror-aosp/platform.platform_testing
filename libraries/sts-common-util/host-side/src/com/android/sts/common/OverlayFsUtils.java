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

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OverlayFsUtils {
    // output of `stat`, e.g. "root shell 755 u:object_r:vendor_file:s0"
    static final Pattern PERM_PATTERN =
            Pattern.compile(
                    "^(?<user>[a-zA-Z0-9_-]+) (?<group>[a-zA-Z0-9_-]+) (?<perm>[0-7]+)"
                            + " (?<secontext>.*)$");

    /**
     * Mounts an OverlayFS dir over the top most common dir in the list.
     *
     * <p>The directory should be writable after this returns successfully. To cleanup, reboot the
     * device as unfortunately unmounting overlayfs is complicated.
     *
     * @param device The test device to setup overlayfs for.
     * @param dir The directory to make writable. Directories with single quotes are not supported.
     */
    public static void makeWritable(ITestDevice device, String dir)
            throws DeviceNotAvailableException, IOException {
        // TODO(duytruong): This should ideally be made into a TestRule that also handles cleanups
        // However, test devices initiation is done in one of the @Before, after a rule's setup.
        assertTrue("Can't acquire root for " + device.getSerialNumber(), device.enableAdbRoot());

        String statOut =
                CommandUtil.runAndCheck(device, "stat -c '%U %G %a %C' '" + dir + "'").getStdout();
        Matcher m = PERM_PATTERN.matcher(statOut);
        assertTrue("Bad stats output: " + statOut, m.find());
        String user = m.group("user");
        String group = m.group("group");
        String unixPerm = m.group("perm");
        String seContext = m.group("secontext");

        Path tempdir = Paths.get("/mnt", "stsoverlayfs", dir);
        String upperdir = tempdir.resolve("upper").toString();
        String workdir = tempdir.resolve("workdir").toString();

        CommandUtil.runAndCheck(device, String.format("mkdir -p '%s' '%s'", upperdir, workdir));
        CommandUtil.runAndCheck(device, String.format("chown %s:%s '%s'", user, group, upperdir));
        CommandUtil.runAndCheck(device, String.format("chcon '%s' '%s'", seContext, upperdir));
        CommandUtil.runAndCheck(device, String.format("chmod %s '%s'", unixPerm, upperdir));

        String mountCmd =
                String.format(
                        "mount -t overlay overlay -o lowerdir='%s',upperdir='%s',workdir='%s' '%s'",
                        dir, upperdir, workdir, dir);
        CommandUtil.runAndCheck(device, mountCmd);
    }
}
