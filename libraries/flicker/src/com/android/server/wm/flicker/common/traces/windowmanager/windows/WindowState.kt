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

package com.android.server.wm.flicker.common.traces.windowmanager.windows

import com.android.server.wm.flicker.common.Rect
import com.android.server.wm.flicker.common.Region
import com.android.server.wm.flicker.common.WindowRect

class WindowState(
    windowContainer: WindowContainer,
    val childWindows: Array<WindowState>, // deprecated — kept for backward compatibility
    frame: Rect
) : WindowContainer(windowContainer) {
    val rect = WindowRect(frame, this, title)
    override val rects: List<Rect> = listOf(rect)

    val frameRegion: Region = Region(frame)
}