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

import android.graphics.Rect;
import android.os.RemoteException;

/**
 * Locks landscape orientation before running a test and goes back to natural orientation
 * afterwards.
 */
public class LandscapeOrientationRule extends SwitchToOrientationBaseRule {
    @Override
    protected void setOrientation() throws RemoteException {
        getUiDevice().setOrientationLeft();
    }

    @Override
    protected String orientationDescription() {
        return "landscape";
    }

    @Override
    protected boolean isOrientationSuccessfullySet(Rect launcherRectangle) {
        return launcherRectangle.width() > launcherRectangle.height();
    }
}
