/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.platform.helpers.features.common;

import static android.platform.helpers.VolumeUtils.adjustVolume;

import static java.util.regex.Pattern.compile;

import android.media.AudioManager;
import android.platform.helpers.features.Page2;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;


/**
 * Helper class for Volume dialog.
 * Note: You cannot grab the page hierarchy using UiAutomatorViewer or HSV. Use dumphierarchy to
 * get the ui detail.
 */
public class VolumeDialog implements Page2 {

    private static final String PAGE_TTILE_SELECTOR_RES_ID =
            "com.android.systemui:id/volume_dialog";
    private static final BySelector PAGE_TTILE_SELECTOR = By.res(
            compile(PAGE_TTILE_SELECTOR_RES_ID));
    private static final BySelector SLIDER = By.res("com.android.systemui:id/volume_row_slider");

    /**
     * To get page selector used for determining the given page
     *
     * @return an instance of given page selector identifier.
     */
    @Override
    public BySelector getPageTitleSelector() {
        return PAGE_TTILE_SELECTOR;
    }

    @Override
    public void open() {
        adjustVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME);
    }
}
