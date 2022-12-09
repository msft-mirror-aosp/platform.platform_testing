/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static androidx.test.InstrumentationRegistry.getInstrumentation;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rule which captures a screen record for a test. After adding this rule to the test class, apply
 * the annotation @ScreenRecord to individual tests
 */
public class ScreenRecordRule implements TestRule {

    private static final String TAG = "ScreenRecordRule";

    private static final String SCREEN_RECORDING_ALWAYS_ENABLED_KEY =
            "screen-recording-always-enabled";

    public static void runWithRecording(ThrowingRunnable runnable, Description description)
            throws Throwable {
        Instrumentation inst = getInstrumentation();
        UiAutomation automation = inst.getUiAutomation();
        UiDevice device = UiDevice.getInstance(inst);

        File outputFile = ArtifactSaver.artifactFile(description, "ScreenRecord", "mp4");
        device.executeShellCommand("killall screenrecord");
        ParcelFileDescriptor output = automation.executeShellCommand("screenrecord " + outputFile);
        String screenRecordPid = device.executeShellCommand("pidof screenrecord");
        try {
            runnable.run();
        } finally {
            device.executeShellCommand("kill -INT " + screenRecordPid);
            Log.e(TAG, "Screenrecord captured at: " + outputFile);
            output.close();
        }
        automation.executeShellCommand("rm " + outputFile);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (!shouldRecordScreen(description)) {
            return base;
        }

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                runWithRecording(base::evaluate, description);
            }
        };
    }

    private boolean shouldRecordScreen(Description description) {
        return description.getAnnotation(ScreenRecord.class) != null
                || screenRecordOverrideEnabled();
    }

    /**
     * This is needed to enable screen recording when a parameter is passed to the instrumentation,
     * avoid having to recompile the test.
     */
    private boolean screenRecordOverrideEnabled() {
        Bundle args = InstrumentationRegistry.getArguments();
        boolean overrideEnabled = args.getBoolean(SCREEN_RECORDING_ALWAYS_ENABLED_KEY, false);
        if (overrideEnabled) {
            Log.d(
                    TAG,
                    "Screen recording has been enabled due to "
                            + SCREEN_RECORDING_ALWAYS_ENABLED_KEY
                            + " parameter.");
        }
        return overrideEnabled;
    }

    /** Interface to indicate that the test should capture screenrecord */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ScreenRecord {}

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
