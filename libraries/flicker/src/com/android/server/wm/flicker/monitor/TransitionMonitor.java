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

package com.android.server.wm.flicker.monitor;

import android.os.Environment;

import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Collects test artifacts during a UI transition. */
public abstract class TransitionMonitor {
    static final String TAG = "FLICKER";
    public static final Path OUTPUT_DIR =
            Paths.get(Environment.getExternalStorageDirectory().toString(), "flicker");
    protected String mChecksum;
    protected Path mOutputPath;

    /** Starts monitor. */
    public abstract void start();

    /** Stops monitor. */
    public abstract void stop();

    /**
     * Saves any monitor artifacts to file adding {@code testTag} and {@code iteration} to the file
     * name.
     *
     * @param testTag suffix added to artifact name
     * @param iteration suffix added to artifact name
     * @return Path to saved artifact
     */
    public Path save(String testTag, int iteration) {
        return save(testTag + "_" + iteration);
    }

    /**
     * Saves any monitor artifacts to file adding {@code testTag} to the file name.
     *
     * @param testTag suffix added to artifact name
     * @return Path to saved artifact
     */
    /**
     * Saves trace file to the external storage directory suffixing the name with the testtag and
     * iteration.
     *
     * <p>Moves the trace file from the default location via a shell command since the test app does
     * not have security privileges to access /data/misc/wmtrace.
     *
     * @param testTag suffix added to trace name used to identify trace
     * @return Path to saved trace file and file checksum (SHA-256)
     */
    public Path save(String testTag) {
        mOutputPath.toFile().mkdirs();
        Path savedTrace = saveTrace(testTag);
        mChecksum = calculateChecksum(savedTrace);
        return savedTrace;
    }

    protected Path saveTrace(String testTag) {
        throw new UnsupportedOperationException("Save not implemented for this monitor");
    }

    public String getChecksum() {
        return mChecksum;
    }

    static String calculateChecksum(Path traceFile) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileData = Files.readAllBytes(traceFile);
            byte[] hash = digest.digest(fileData);
            return BaseEncoding.base16().encode(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Checksum algorithm SHA-256 not found", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("File not found", e);
        }
    }
}
