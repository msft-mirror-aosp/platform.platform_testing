/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.platform.test.flag.junit.host;

import android.aconfig.Aconfig;
import android.platform.test.flag.util.Flag;
import android.platform.test.flag.util.FlagReadException;

import com.android.tradefed.log.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Dumps flags from static aconfig.pb in test resources.
 *
 * <p>Flag values are determined at the time the test is built.
 */
record TestResourceFlags(boolean mExists, Map<String, Aconfig.parsed_flag> mAllFlags) {

    public static TestResourceFlags createTestResourceFlags(String testClassLocation) {
        Map<String, Aconfig.parsed_flag> parsedFlags = new HashMap<>();
        boolean exists = loadAconfigPbFromTestResource(testClassLocation, parsedFlags);
        return new TestResourceFlags(exists, parsedFlags);
    }

    /** Returns whether the static aconfig.pb resource is found. */
    public boolean exists() {
        return mExists;
    }

    /**
     * Gets the parsed flag for the given flag name.
     *
     * @param flagName The full aconfig flag name ({packageName}.{flagName}).
     * @return The parsed_flag object, or {@code null} if the flag is not found.
     */
    @Nullable
    public Aconfig.parsed_flag getFlag(String flagName) {
        return mAllFlags.get(flagName);
    }

    /** Loads the static aconfig.pb resource from the given test class location. */
    private static boolean loadAconfigPbFromTestResource(
            String testClassLocation, Map<String, Aconfig.parsed_flag> flagMap) {
        try {
            // Find the static aconfig.pb in the same jar
            URL aconfigUrl = new URL(String.format("jar:%s!/aconfig.pb", testClassLocation));
            LogUtil.CLog.i("Load static aconfig from %s", aconfigUrl);
            try (InputStream stream = aconfigUrl.openStream()) {
                if (stream == null) {
                    LogUtil.CLog.i("The static aconfig.pb does not exist.");
                    return false;
                }
                Aconfig.parsed_flags flags = Aconfig.parsed_flags.parseFrom(stream);
                for (Aconfig.parsed_flag flag : flags.getParsedFlagList()) {
                    String fullFlagName =
                            String.format(
                                    Flag.ACONFIG_FULL_FLAG_FORMAT,
                                    flag.getPackage(),
                                    flag.getName());
                    flagMap.put(fullFlagName, flag);
                }
                return true;
            } catch (IOException e) {
                throw new FlagReadException(
                        "ALL_FLAGS", "Failed to read static flags from aconfig.pb", e);
            }
        } catch (MalformedURLException e) {
            throw new FlagReadException("ALL_FLAGS", "Unable to find the aconfig.pb", e);
        }
    }
}
