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

package android.platform.helpers;

import android.app.Instrumentation;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Until;
import android.content.ActivityNotFoundException;

import java.io.IOException;

public abstract class AbstractAutoStandardAppHelper extends AbstractStandardAppHelper {
    private static final String LOG_TAG = AbstractAutoStandardAppHelper.class.getSimpleName();

    protected Instrumentation mInstrumentation;
    protected UiDevice mDevice;

    public AbstractAutoStandardAppHelper(Instrumentation instrumentation) {
        super(instrumentation);
        mInstrumentation = instrumentation;
        mDevice = UiDevice.getInstance(instrumentation);
    }

    /** {@inheritDoc} */
    @Override
    public void open() {
        // Launch the application as normal.
        String pkg = getPackage();

        String output = null;
        try {
            Log.i(LOG_TAG, String.format("Sending command to launch: %s", pkg));
            mInstrumentation.getContext().startActivity(getOpenAppIntent());
        } catch (ActivityNotFoundException e) {
            throw new RuntimeException(String.format("Failed to find package: %s", pkg), e);
        }

        // Ensure the package is in the foreground for success.
        if (!mDevice.wait(Until.hasObject(By.pkg(pkg).depth(0)), 30000)) {
            throw new IllegalStateException(
                    String.format("Did not find package, %s, in foreground.", pkg));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void exit() {
        mDevice.pressHome();
        mDevice.waitForIdle();
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }

    /**
     * Executes a shell command on device, and return the standard output in string.
     *
     * @param command the command to run
     * @return the standard output of the command, or empty string if failed without throwing an
     *     IOException
     */
    protected String executeShellCommand(String command) {
        try {
            return mDevice.executeShellCommand(command);
        } catch (IOException e) {
            // ignore
            Log.e(
                    LOG_TAG,
                    String.format(
                            "The shell command failed to run: %s exception: %s",
                            command, e.getMessage()));
            return "";
        }
    }
}
