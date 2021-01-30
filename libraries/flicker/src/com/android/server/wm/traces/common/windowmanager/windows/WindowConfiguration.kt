/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.traces.common.windowmanager.windows

import com.android.server.wm.traces.common.Rect

/**
 * Represents the configuration of a WM window
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot
 * access internal Java/Android functionality
 *
 */
open class WindowConfiguration(
    private val _appBounds: Rect?,
    private val _bounds: Rect?,
    private val _maxBounds: Rect?,
    val windowingMode: Int,
    val activityType: Int
) {
    val appBounds: Rect get() = _appBounds ?: Rect()
    val bounds: Rect get() = _bounds ?: Rect()
    val maxBounds: Rect get() = _maxBounds ?: Rect()

    val isEmpty: Boolean
        get() = _appBounds?.isEmpty ?: true &&
            _bounds?.isEmpty ?: true &&
            _maxBounds?.isEmpty ?: true &&
            windowingMode == 0 &&
            activityType == 0
}