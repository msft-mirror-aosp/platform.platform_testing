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

package com.android.server.wm.traces.common.windowmanager.windows

import com.android.server.wm.traces.common.Insets
import com.android.server.wm.traces.common.Rect

/** Representation of a display cutout from a WM trace */
data class DisplayCutout(
    val insets: Insets,
    val boundLeft: Rect,
    val boundTop: Rect,
    val boundRight: Rect,
    val boundBottom: Rect,
    val waterfallInsets: Insets
)
