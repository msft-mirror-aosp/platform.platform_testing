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

import android.os.SystemClock;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IYouTubeHelper;

import androidx.annotation.VisibleForTesting;

import org.junit.runner.Description;

/** This rule allows to execute CUJ while YouTube in pip state. */
public class YouTubePipRule extends TestWatcher {

    @VisibleForTesting static final String SEARCH_KEYWORD = "youtube-search-keyword";
    String searchKeyword = "no ads video";

    @VisibleForTesting static final String YOUTUBE_SEARCH_TIMEOUT = "youtube-search-timeout";
    long searchTimeout = 5000;

    @VisibleForTesting static final String YOUTUBE_PLAYBACK_TIMEOUT = "youtube-playback-time";
    long playbackTimeout = 2000;

    private static HelperAccessor<IYouTubeHelper> sYouTubeHelper =
            new HelperAccessor<>(IYouTubeHelper.class).withPrefix("YouTubeHelper");

    @Override
    protected void starting(Description description) {
        searchKeyword = getArguments().getString(SEARCH_KEYWORD, "no ads video");
        searchTimeout = Long.valueOf(getArguments().getString(YOUTUBE_SEARCH_TIMEOUT, "5000"));
        playbackTimeout = Long.valueOf(getArguments().getString(YOUTUBE_PLAYBACK_TIMEOUT, "2000"));

        sYouTubeHelper.get().open();
        sYouTubeHelper.get().goToSearchPage();
        sYouTubeHelper.get().searchVideo(searchKeyword);
        sYouTubeHelper.get().waitForSearchResults(searchTimeout);
        sYouTubeHelper.get().playSearchResultPageVideo();
        sYouTubeHelper.get().goToYouTubePip();
        SystemClock.sleep(playbackTimeout);
    }

    @Override
    protected void finished(Description description) {
        sYouTubeHelper.get().backFromYouTubeFromPip();
        sYouTubeHelper.get().exit();
    }
}
