package android.platform.helpers

import android.platform.uiautomator_helpers.DeviceHelpers.shell
import android.platform.uiautomator_helpers.WaitUtils.ensureThat
import android.util.Log

/** Allows to execute operations such as restart on a process identififed by [packageName]. */
class ProcessUtil(private val packageName: String) {

    /** Restart [packageName] running `am crash <package-name>`. */
    fun restart() {
        val initialPids = pids
        // make sure the lock screen is enable.
        Log.d(TAG, "Old $packageName PIDs=$initialPids)")
        initialPids
            .map { pid -> "kill $pid" }
            .forEach { killCmd ->
                val result = shell(killCmd)
                Log.d(TAG, "Result of \"$killCmd\": \"$result\"")
            }
        ensureThat("All sysui process stopped") { allProcessesStopped(initialPids) }
        ensureThat("All sysui process restarted") { hasProcessRestarted(initialPids) }
    }

    private val pids: List<String>
        get() {
            val pidofResult = shell("pidof $packageName").trim()
            return if (pidofResult.isEmpty()) {
                emptyList()
            } else pidofResult.split("\\s".toRegex())
        }

    private fun allProcessesStopped(initialPidsList: List<String>): Boolean =
        (pids intersect initialPidsList).isEmpty()

    /**
     * We can only test if one process has restarted. If we match against the number of killed
     * processes, one may have spawned another process later, and this check would fail.
     *
     * @param initialPidsList list of pidof $packageName
     * @return true if there is a new process with name $packageName
     */
    private fun hasProcessRestarted(initialPidsList: List<String>): Boolean =
        (pids subtract initialPidsList).isNotEmpty()

    private companion object {
        const val TAG = "ProcessUtils"
    }
}
