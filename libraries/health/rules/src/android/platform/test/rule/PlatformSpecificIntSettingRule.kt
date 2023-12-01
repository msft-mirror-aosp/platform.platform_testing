package android.platform.test.rule

import android.platform.uiautomator_helpers.DeviceHelpers.context
import android.provider.Settings

/**
 * Test Rule to set integer values specific to a platform (from Build.HARDWARE)
 *
 * @param settingName string name from Settings.Secure
 * @param platformNames list of platforms, e.g. [ "cutf_cvm" ]
 * @param platformValue value to set if Build.HARDWARE is in the platform list. Ignored if null.
 * @param nonPlatformValue value to set if Build.HARDWARE isn't in the list. Ignored if null.
 */
class PlatformSpecificIntSettingRule<T>(
    settingName: String,
    platformNames: Array<String>,
    platformValue: Int,
    nonPlatformValue: Int? = null
) : PlatformSpecificSettingRule<Int>(settingName, platformNames, platformValue, nonPlatformValue) {

    override fun getSettingValue() = Settings.Secure.getInt(context.contentResolver, settingName)

    override fun setSettingValue(value: Int) {
        Settings.Secure.putInt(context.contentResolver, settingName, value)
    }
}
