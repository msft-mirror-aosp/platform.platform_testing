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

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IGoogleCameraHelper;
import android.platform.helpers.IPhotosHelper;

import androidx.annotation.VisibleForTesting;

import org.junit.runner.Description;

/** This rule allows to execute CUJ while new picures uploading in cloud. */
public class PhotoUploadRule extends TestWatcher {

    @VisibleForTesting static final String PHOTO_COUNT = "photo-count";
    int photoCount = 5;

    @VisibleForTesting static final String TAKE_PHOTO_DELAY = "take-photo-delay";
    long takePhotoDelay = 1000;

    @VisibleForTesting static final String PHOTO_TIMEOUT = "photo-timeout";
    long photoTimeout = 10000;

    private static HelperAccessor<IPhotosHelper> sPhotosHelper =
            new HelperAccessor<>(IPhotosHelper.class);

    private static HelperAccessor<IGoogleCameraHelper> sGoogleCameraHelper =
            new HelperAccessor<>(IGoogleCameraHelper.class);

    @Override
    protected void starting(Description description) {
        photoCount = Integer.valueOf(getArguments().getString(PHOTO_COUNT, "5"));
        photoTimeout = Long.valueOf(getArguments().getString(PHOTO_TIMEOUT, "10000"));
        takePhotoDelay = Long.valueOf(getArguments().getString(TAKE_PHOTO_DELAY, "1000"));

        sPhotosHelper.get().open();
        sPhotosHelper.get().disableBackupMode();
        sGoogleCameraHelper.get().open();
        sGoogleCameraHelper.get().takeMultiplePhotos(photoCount, takePhotoDelay);
        sGoogleCameraHelper.get().timeoutAfterTakingPhoto(photoTimeout);
        sPhotosHelper.get().open();
        sPhotosHelper.get().enableBackupMode();
        sPhotosHelper.get().verifyPhotosStartedUploading();
        sPhotosHelper.get().exit();
    }

    @Override
    protected void finished(Description description) {
        sPhotosHelper.get().open();
        sPhotosHelper.get().disableBackupMode();
        sPhotosHelper.get().exit();
    }
}
