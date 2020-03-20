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
package android.platform.test.rule;

import android.support.test.uiautomator.UiDevice;
import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.runner.Description;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.SystemClock;
import android.util.Log;

/**
 * This rule will drop caches before running each test method.
 */
public class DropCachesRule extends TestWatcher {
    private static final String LOG_TAG = DropCachesRule.class.getSimpleName();

    @VisibleForTesting static final String KEY_DROP_CACHE = "drop-cache";
    private static boolean mDropCache = true;

    /**
     * Shell equivalent of $(echo 3 > /proc/sys/vm/drop_caches)
     *
     * Clears out the system pagecache for files and inodes metadata.
     */
    public static void executeDropCaches() {
        // Create a temporary file which contains the dropCaches command.
        // Do this because we cannot write to /proc/sys/vm/drop_caches directly,
        // as executeShellCommand parses the '>' character as a literal.
        try {
            File outputDir =
                    InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
            File outputFile = File.createTempFile("drop_cache_script", "sh", outputDir);
            outputFile.setWritable(true);
            outputFile.setExecutable(true, /*ownersOnly*/false);

            String outputFilePath = outputFile.toString();

            // If this works correctly, the next log-line will print 'Success'.
            String str = "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure";
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
            writer.write(str);
            writer.close();

            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            String result = device.executeShellCommand(outputFilePath);
            Log.v(LOG_TAG, "dropCaches output was: " + result);
            outputFile.delete();
        } catch (IOException e) {
            throw new AssertionError (e);
        }
    }

    @Override
    protected void starting(Description description) {
        // Identify the filter option to use.
        mDropCache = Boolean.parseBoolean(getArguments().getString(KEY_DROP_CACHE, "true"));
        if (mDropCache == false) {
            return;
        }

        executeDropCaches();
        // TODO: b/117868612 to identify the root cause for additional wait.
        SystemClock.sleep(3000);
    }
}
