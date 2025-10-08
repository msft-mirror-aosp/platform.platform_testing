/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.platform.systemui_tapl.ui

import android.graphics.Point
import androidx.test.uiautomator.UiObject2

/**
 * System UI test automation object representing an item in the status bar like date, clock,
 * connectivity icon, battery icon, etc.
 */
class StatusBarItem internal constructor(val name: String, private val uiObject: UiObject2) {

    /** Returns center position of the item */
    val visibleCenter: Point
        get() = uiObject.visibleCenter
}
