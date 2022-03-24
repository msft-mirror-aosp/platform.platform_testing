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

import static android.platform.test.util.HealthTestingUtils.waitForNullDiag;

import android.graphics.Rect;
import android.os.RemoteException;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;

import com.android.launcher3.tapl.LauncherInstrumentation;

import org.junit.runner.Description;

/**
 * Locks landscape orientation before running a test and goes back to natural orientation
 * afterwards.
 */
public class LandscapeOrientationRule extends TestWatcher {
    private final LauncherInstrumentation mLauncher = new LauncherInstrumentation();

    private interface Condition {
        boolean isTrue() throws Throwable;
    }

    @Override
    protected void starting(Description description) {
        try {
            getUiDevice().pressHome();
            mLauncher.setEnableRotation(true);
            getUiDevice().setOrientationLeft();

            waitForNullDiag(
                    () -> {
                        final UiObject2 launcher =
                                getUiDevice()
                                        .findObject(
                                                By.res("android", "content")
                                                        .pkg(
                                                                getUiDevice()
                                                                        .getLauncherPackageName()));
                        if (launcher == null) return "Launcher is not found";

                        final Rect launcherRectangle = launcher.getVisibleBounds();
                        if (launcherRectangle.width() < launcherRectangle.height()) {
                            return "Visible orientation is not landscape";
                        }

                        return null; // No error == success.
                    });
        } catch (RemoteException e) {
            String message = "RemoteException when forcing landscape rotation on the device";
            throw new RuntimeException(message, e);
        }
    }

    @Override
    protected void finished(Description description) {
        try {
            getUiDevice().setOrientationNatural();
            mLauncher.setEnableRotation(false);
            getUiDevice().unfreezeRotation();
        } catch (RemoteException e) {
            String message = "RemoteException when restoring natural rotation of the device";
            throw new RuntimeException(message, e);
        }
    }
}
