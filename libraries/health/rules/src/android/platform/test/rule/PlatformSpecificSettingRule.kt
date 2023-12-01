package android.platform.test.rule

import android.os.Build
import android.util.Log
import org.junit.rules.ExternalResource

/**
 * Test Rule to set values specific to a platform (from Build.HARDWARE)
 *
 * @param settingName string name from Settings.Secure
 * @param platformNames list of platforms, e.g. [ "cutf_cvm" ]
 * @param platformValue value to set if Build.HARDWARE is in the platform list.
 * @param nonPlatformValue value to set if Build.HARDWARE isn't in the list. Ignored if null.
 */
abstract class PlatformSpecificSettingRule<T>(
    val settingName: String,
    private val platformNames: Array<String>,
    val platformValue: T,
    val nonPlatformValue: T? = null
) : ExternalResource() {

    // retain the original value, so we can restore state.
    private var originalValue: T? = null

    /**
     * Before executing the test, save the setting, and if BUILD.HARDWARE is on the platform list,
     * set the platform value. If not, set the non-platform value.
     */
    override fun before() {
        originalValue = getSettingValue()
        Log.d(TAG, "original $settingName is $originalValue")
        if (Build.HARDWARE in platformNames) {
            setSettingValue(platformValue)
        } else if (nonPlatformValue != null) {
            setSettingValue(nonPlatformValue)
        }
    }

    /** Restore the original value after the test if it wasn't null */
    override fun after() {
        originalValue?.let { setSettingValue(it) }
    }

    abstract fun getSettingValue(): T?

    abstract fun setSettingValue(value: T)

    companion object {
        val TAG = "PlatformSpecificSettingRule"
    }
}
