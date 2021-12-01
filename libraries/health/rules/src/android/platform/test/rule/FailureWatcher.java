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

import android.os.FileUtils;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** A rule that generates debug artifact files for failed tests. */
public class FailureWatcher extends TestWatcher {
    private static final String TAG = "FailureWatcher";
    private final UiDevice mDevice;

    public FailureWatcher() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    FailureWatcher.super.apply(base, description).evaluate();
                } catch (Throwable t) {
                    final String systemAnomalyMessage = getSystemAnomalyMessage(mDevice);
                    if (systemAnomalyMessage != null) {
                        throw new AssertionError(systemAnomalyMessage, t);
                    } else {
                        throw t;
                    }
                }
            }
        };
    }

    @Override
    protected void failed(Throwable e, Description description) {
        onError(mDevice, description, e);
    }

    public static File diagFile(String fileName) {
        return new File(getInstrumentation().getTargetContext().getFilesDir(), fileName);
    }

    public static File diagFile(Description description, String prefix, String ext) {
        return diagFile(
                prefix
                        + "-"
                        + description.getTestClass().getSimpleName()
                        + "."
                        + description.getMethodName()
                        + "."
                        + ext);
    }

    private static BySelector getAnyObjectSelector() {
        return By.textStartsWith("");
    }

    private static String getSystemAnomalyMessage(UiDevice device) {
        if (!device.wait(Until.hasObject(getAnyObjectSelector()), 10000)) {
            return "Screen is empty";
        }

        final StringBuilder sb = new StringBuilder();

        UiObject2 object = device.findObject(By.res("android", "alertTitle"));
        if (object != null) {
            sb.append("TITLE: ").append(object.getText());
        }

        object = device.findObject(By.res("android", "message"));
        if (object != null) {
            sb.append(" PACKAGE: ")
                    .append(object.getApplicationPackage())
                    .append(" MESSAGE: ")
                    .append(object.getText());
        }

        if (sb.length() != 0) {
            return "System alert popup is visible: " + sb;
        }

        return null;
    }

    public static void runWithArtifacts(ThrowingRunnable runnable, Description description)
            throws Throwable {
        try {
            runnable.run();
        } catch (Throwable t) {
            onError(UiDevice.getInstance(getInstrumentation()), description, t);
            throw t;
        }
    }

    private static void onError(UiDevice device, Description description, Throwable e) {
        if (device == null) return;
        final File sceenshot = diagFile(description, "TestScreenshot", "png");
        final File hierarchy = diagFile(description, "Hierarchy", "zip");

        // Dump window hierarchy
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(hierarchy))) {
            out.putNextEntry(new ZipEntry("bugreport.txt"));
            dumpStringCommand("dumpsys window windows", out);
            dumpStringCommand("dumpsys package", out);
            out.closeEntry();

            out.putNextEntry(new ZipEntry("visible_windows.zip"));
            dumpCommand("cmd window dump-visible-window-views", out);
            out.closeEntry();
        } catch (IOException ex) {
        }

        Log.e(
                TAG,
                "Failed test "
                        + description.getMethodName()
                        + ",\nscreenshot will be saved to "
                        + sceenshot
                        + ",\nUI dump at: "
                        + hierarchy
                        + " (use go/web-hv to open the dump file)",
                e);
        device.takeScreenshot(sceenshot);

        // Dump accessibility hierarchy
        try {
            device.dumpWindowHierarchy(diagFile(description, "AccessibilityHierarchy", "uix"));
        } catch (IOException ex) {
            Log.e(TAG, "Failed to save accessibility hierarchy", ex);
        }

        // Dump bugreport
        if (getSystemAnomalyMessage(device) != null) {
            dumpCommand("bugreportz -s", diagFile(description, "Bugreport", "zip"));
        }
    }

    private static void dumpStringCommand(String cmd, OutputStream out) throws IOException {
        out.write(("\n\n" + cmd + "\n").getBytes());
        dumpCommand(cmd, out);
    }

    public static void dumpCommand(String cmd, File out) {
        try (BufferedOutputStream buffered = new BufferedOutputStream(new FileOutputStream(out))) {
            dumpCommand(cmd, buffered);
        } catch (IOException ex) {
        }
    }

    private static void dumpCommand(String cmd, OutputStream out) throws IOException {
        try (AutoCloseInputStream in =
                new AutoCloseInputStream(
                        getInstrumentation().getUiAutomation().executeShellCommand(cmd))) {
            FileUtils.copy(in, out);
        }
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
