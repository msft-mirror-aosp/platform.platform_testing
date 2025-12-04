/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package android.platform.uiautomatorhelpers

import android.app.UiAutomation
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Adopt shell permissions for the target context.
 *
 * @param uiAutomation UiAutomation to adopt permissions with. It's important to not fetch
 *   uiAutomation from instrumentation#getUiAutomation() everytime ShellPrivilege gets created, as
 *   tests might rely on UiAutomation state to perform some actions. One of the most common one is
 *   how accessibility services are interrupted b/435029163 when getUiAutomation() is called without
 *   [android.app.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES]
 * @param[permissions] the permission to adopt. Adopt all available permission is it's empty.
 */
class ShellPrivilege(private val uiAutomation: UiAutomation, vararg permissions: String) :
    AutoCloseable {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private var permissionsGranted = false

    init {
        permissionsGranted = grantMissingPermissions(*permissions)
    }

    /**
     * @return[Boolean] True is there are any missing permission and we've successfully granted all
     * of them.
     */
    private fun grantMissingPermissions(vararg permissions: String): Boolean {
        if (permissions.isEmpty()) {
            uiAutomation.adoptShellPermissionIdentity()
            return true
        }
        val missingPermissions = permissions.filter { !it.isGranted() }.toTypedArray()
        if (missingPermissions.isEmpty()) return false
        uiAutomation.adoptShellPermissionIdentity(*missingPermissions)
        return true
    }

    override fun close() {
        if (permissionsGranted) uiAutomation.dropShellPermissionIdentity()
        permissionsGranted = false
    }

    private fun String.isGranted(): Boolean =
        targetContext.checkCallingPermission(this) == PackageManager.PERMISSION_GRANTED
}
