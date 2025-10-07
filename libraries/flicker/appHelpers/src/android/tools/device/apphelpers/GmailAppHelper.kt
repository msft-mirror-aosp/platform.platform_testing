/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.device.apphelpers

import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.tools.traces.component.ComponentNameMatcher
import android.tools.traces.component.IComponentNameMatcher
import androidx.test.platform.app.InstrumentationRegistry

/** Helper to launch the Gmail app (not compatible with AOSP) */
class GmailAppHelper
@JvmOverloads
constructor(
    instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    pkgManager: PackageManager = instrumentation.context.packageManager,
    appName: String = getGmailAppName(pkgManager),
    appComponent: IComponentNameMatcher = getGmailComponent(pkgManager),
) : StandardAppHelper(instrumentation, appName, appComponent) {

    override val openAppIntent =
        pkgManager.getLaunchIntentForPackage(packageName)
            ?: error("Unable to find intent for Gmail")

    companion object {
        private fun getGmailIntent(): Intent {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }

        private fun getResolveInfo(pkgManager: PackageManager): ResolveInfo =
            pkgManager.resolveActivity(getGmailIntent(), PackageManager.MATCH_DEFAULT_ONLY)
                ?: error("unable to resolve Gmail activity")

        private fun getGmailComponent(pkgManager: PackageManager): ComponentNameMatcher =
            ComponentNameMatcher(packageName = "com.google.android.gm", className = "")

        private fun getGmailAppName(pkgManager: PackageManager): String =
            getResolveInfo(pkgManager).loadLabel(pkgManager).toString()
    }
}
