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

@file:JvmName("CommonConstants")
package com.android.server.wm.flicker

import android.content.ComponentName

val CHROME_COMPONENT = ComponentName("com.android.chrome",
        "org.chromium.chrome.browser.firstrun.FirstRunActivity")
val CHROME_SPLASH_SCREEN_COMPONENT = ComponentName("", "Splash Screen com.android.chrome")
val DOCKER_STACK_DIVIDER_COMPONENT = ComponentName("", "DockedStackDivider")
val IMAGINARY_COMPONENT = ComponentName("", "ImaginaryWindow")
val IME_ACTIVITY_COMPONENT = ComponentName("com.android.server.wm.flicker.testapp",
        "com.android.server.wm.flicker.testapp.ImeActivity")
val LAUNCHER_COMPONENT = ComponentName("com.google.android.apps.nexuslauncher",
        "com.google.android.apps.nexuslauncher.NexusLauncherActivity")
val PIP_DISMISS_COMPONENT = ComponentName("", "pip-dismiss-overlay")

val SIMPLE_APP_COMPONENT = ComponentName("com.android.server.wm.flicker.testapp",
        "com.android.server.wm.flicker.testapp.SimpleActivity")
private const val SHELL_PKG_NAME = "com.android.wm.shell.flicker.testapp"
val SHELL_SPLIT_SCREEN_PRIMARY_COMPONENT = ComponentName(SHELL_PKG_NAME,
        "$SHELL_PKG_NAME.SplitScreenActivity")
val SHELL_SPLIT_SCREEN_SECONDARY_COMPONENT = ComponentName(SHELL_PKG_NAME,
        "$SHELL_PKG_NAME.SplitScreenSecondaryActivity")

val SCREEN_DECOR_COMPONENT = ComponentName("", "ScreenDecorOverlay")
val WALLPAPER_COMPONENT = ComponentName(
        "", "com.breel.wallpapers18.soundviz.wallpaper.variations.SoundVizWallpaperV2")
