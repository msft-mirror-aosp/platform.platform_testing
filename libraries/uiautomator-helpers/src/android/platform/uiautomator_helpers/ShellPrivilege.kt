package android.platform.uiautomator_helpers

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Adopt shell permissions for the target context.
 *
 * @param[permissions] the permission to adopt. Adopt all available permission is it's empty.
 */
class ShellPrivilege(vararg permissions: String) : AutoCloseable {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val uiAutomation = instrumentation.uiAutomation
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
        if (permissionsGranted) instrumentation.uiAutomation.dropShellPermissionIdentity()
        permissionsGranted = false
    }

    private fun String.isGranted(): Boolean =
        targetContext.checkCallingPermission(this) == PackageManager.PERMISSION_GRANTED
}
