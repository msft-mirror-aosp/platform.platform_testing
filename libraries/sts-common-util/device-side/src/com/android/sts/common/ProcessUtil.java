/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.util.Log;

import androidx.test.uiautomator.UiDevice;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessUtil {
    private static final String TAG = "ProcessUtil";

    /* Matches UserHandle#PER_USER_RANGE */
    private static final int PER_USER_RANGE = 100000;

    /**
     * Get a pids for the passed package name.
     *
     * @param device the device to use
     * @param packageName a String representing the package name
     * @return an List of pid; empty if no such process was found.
     */
    public static List<Integer> pidsOfPackage(UiDevice device, String packageName) {
        final String res;
        try {
            res = device.executeShellCommand("pidof " + packageName).trim();
        } catch (IOException e) {
            // Re-throw unchecked for simplicity
            throw new IllegalStateException("can't execute pidof on device", e);
        }
        String[] pids = res.split(" ");
        return Arrays.stream(pids).map(Integer::parseInt).collect(Collectors.toList());
    }

    /**
     * Queries the uid of the process identifier by `pid`.
     *
     * @param device device to be run on
     * @param pid the id of the process to query
     * @return the uid of the process. on failure, throws an exception instead
     */
    public static int uidFromPid(UiDevice device, int pid) {
        final String command = "stat -c %u /proc/" + pid;
        final String statRes;
        try {
            statRes = device.executeShellCommand(command).trim();
        } catch (IOException e) {
            // Re-throw unchecked for simplicity
            throw new IllegalStateException("can't execute stat on device", e);
        }
        if (statRes.isEmpty()) {
            Log.d(TAG, String.format("'%s' did not provide stdout", command));
            throw new IllegalStateException(
                    "pid " + pid + " could not be queried for uid - not running?");
        }
        return Integer.parseInt(statRes);
    }

    /**
     * Returns the Android user that belongs to the process identified by `pid`.
     *
     * @param device device to be run on
     * @param pid the id of the process to query
     * @return the Android user of the process. on failure, throws an exception instead
     */
    public static int androidUserFromPid(UiDevice device, int pid) {
        final int uid = uidFromPid(device, pid);
        return uid / PER_USER_RANGE;
    }
}
