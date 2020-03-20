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

import static com.android.compatibility.common.util.SystemUtil.runShellCommand;

import android.os.RemoteException;

import androidx.annotation.VisibleForTesting;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Base class for monitors containing common logic to read the trace as a byte array and save the
 * trace to another location.
 */
public abstract class TraceMonitor extends TransitionMonitor {
    private static final Path TRACE_DIR = Paths.get("/data/misc/wmtrace/");

    protected Path mTraceFile;

    public abstract boolean isEnabled() throws RemoteException;

    TraceMonitor(Path outputDir, Path traceFile) {
        mTraceFile = traceFile;
        mOutputPath = outputDir;
    }

    TraceMonitor(Path outputDir, String traceFileName) {
        this(outputDir, TRACE_DIR.resolve(traceFileName));
    }

    protected Path saveTrace(String testTag) {
        Path traceFileCopy = getOutputTraceFilePath(testTag);
        moveFile(mTraceFile, traceFileCopy);

        return traceFileCopy;
    }

    protected void moveFile(Path src, Path dst) {
        // Move the  file to the output directory
        // Note: Due to b/141386109, certain devices do not allow moving the files between
        //       directories with different encryption policies, so manually copy and then
        //       remove the original file
        String copyCommand =
                String.format(Locale.getDefault(), "cp %s %s", src.toString(), dst.toString());
        runShellCommand(copyCommand);
        String removeCommand = String.format(Locale.getDefault(), "rm %s", src.toString());
        runShellCommand(removeCommand);
    }

    @VisibleForTesting
    public Path getOutputTraceFilePath(String testTag) {
        return mOutputPath.resolve(testTag + "_" + mTraceFile.getFileName());
    }
}
