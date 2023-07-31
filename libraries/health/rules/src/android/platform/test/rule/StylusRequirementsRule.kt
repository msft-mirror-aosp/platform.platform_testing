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
package android.platform.test.rule

import android.Manifest.permission.MANAGE_ROLE_HOLDERS
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.runner.Description

/**
 * This rule allows end-to-end tests to manage requirements pertaining to stylus CUJs. For example
 * android version requirement, managing [RoleManager.ROLE_NOTES].
 *
 * @param requiredAndroidVersion enforce required Android version, default is Android U
 * @param requireNotesRole [RoleManager.ROLE_NOTES] should be enabled for most stylus CUJs, override
 *   to skip the requirement
 * @param defaultNotesRoleHolderPackage should be overridden to set a default
 *   [RoleManager.ROLE_NOTES] holder for the tests. The rule will take care of setting and restoring
 *   the role holder for each test. Test will be skipped if the provided package is not installed.
 *   Notes role is required when overriding this param. The rule will also ensure to kill this app
 *   before and after the test to ensure clean state.
 */
class StylusRequirementsRule
@JvmOverloads
constructor(
    private val requiredAndroidVersion: Int = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    private val requireNotesRole: Boolean = true,
    private val defaultNotesRoleHolderPackage: String = ""
) : TestWatcher() {

    private val roleManager = context.getSystemService(RoleManager::class.java)!!
    private var prevNotesRoleHolder: String? = null

    override fun starting(description: Description?) {
        super.starting(description)

        assumeTrue(
            "Build SDK should be at least $requiredAndroidVersion for this test",
            Build.VERSION.SDK_INT >= requiredAndroidVersion
        )

        if (requireNotesRole) {
            assumeTrue(
                "Notes role should be enabled for this test",
                roleManager.isRoleAvailable(RoleManager.ROLE_NOTES)
            )
        }

        if (defaultNotesRoleHolderPackage.isNotEmpty()) {
            assert(requireNotesRole) {
                "Notes role is required for managing default notes role holder"
            }

            assumeTrue(
                "$defaultNotesRoleHolderPackage should be installed on the device for this test",
                isDefaultNotesRoleHolderInstalled()
            )

            prevNotesRoleHolder = setDefaultNotesRoleHolder(defaultNotesRoleHolderPackage)

            killDefaultNotesApp()
        }
    }

    override fun finished(description: Description?) {
        super.finished(description)

        if (defaultNotesRoleHolderPackage.isNotEmpty()) {
            killDefaultNotesApp()
        }

        prevNotesRoleHolder?.let { setDefaultNotesRoleHolder(it) }
    }

    private fun isDefaultNotesRoleHolderInstalled() =
        runCatching {
                context.packageManager.getPackageInfo(defaultNotesRoleHolderPackage, /* flags= */ 0)
            }
            .isSuccess

    private fun setDefaultNotesRoleHolder(packageName: String): String {
        val notesRoleHolders = getNotesRoleHolders()

        val prevDefaultNotesRoleHolder = notesRoleHolders.firstOrNull().orEmpty()

        clearNotesRoleHolders()

        if (packageName.isEmpty()) {
            return prevDefaultNotesRoleHolder
        }

        val setRoleHolderFuture = CompletableFuture<Boolean>()
        callWithManageRoleHolderPermission {
            roleManager.addRoleHolderAsUser(
                RoleManager.ROLE_NOTES,
                packageName,
                /* flags= */ 0,
                context.user,
                context.mainExecutor,
                setRoleHolderFuture::complete
            )
        }

        assert(setRoleHolderFuture.get(ROLE_MANAGER_VERIFICATION_TIMEOUT, TimeUnit.MILLISECONDS)) {
            "Failed to set $packageName as default notes role holder"
        }

        return prevDefaultNotesRoleHolder
    }

    private fun clearNotesRoleHolders() {
        val clearRoleHoldersFuture = CompletableFuture<Boolean>()
        callWithManageRoleHolderPermission {
            roleManager.clearRoleHoldersAsUser(
                RoleManager.ROLE_NOTES,
                /* flags= */ 0,
                context.user,
                context.mainExecutor,
                clearRoleHoldersFuture::complete
            )
        }

        assert(
            clearRoleHoldersFuture.get(ROLE_MANAGER_VERIFICATION_TIMEOUT, TimeUnit.MILLISECONDS)
        ) {
            "Failed to clear notes role holder"
        }
    }

    private fun getNotesRoleHolders(): List<String> = callWithManageRoleHolderPermission {
        roleManager.getRoleHolders(RoleManager.ROLE_NOTES)
    }

    private fun <T> callWithManageRoleHolderPermission(callable: () -> T): T {
        val uiAutomation = getInstrumentation().uiAutomation
        var shouldDropShellPermissionIdentity = false

        try {
            // Check if permission is already granted.
            if (
                context.packageManager.checkPermission(MANAGE_ROLE_HOLDERS, context.packageName) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                uiAutomation.adoptShellPermissionIdentity(MANAGE_ROLE_HOLDERS)
                shouldDropShellPermissionIdentity = true
            }

            return callable()
        } finally {
            if (shouldDropShellPermissionIdentity) {
                uiAutomation.dropShellPermissionIdentity()
            }
        }
    }

    private fun killDefaultNotesApp() {
        executeShellCommand("am force-stop $defaultNotesRoleHolderPackage")
    }

    private companion object {
        const val ROLE_MANAGER_VERIFICATION_TIMEOUT = 5000L
    }
}
