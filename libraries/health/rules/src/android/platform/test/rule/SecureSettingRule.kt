package android.platform.test.rule

import android.provider.Settings
import android.provider.Settings.Secure.getString
import android.provider.Settings.Secure.putFloat
import android.provider.Settings.Secure.putInt
import android.provider.Settings.Secure.putLong
import android.provider.Settings.Secure.putString
import org.junit.runner.Description

/** Base rule to set values in [Settings.Secure]. The value is then reset at the end of the test. */
open class SecureSettingRule<T : Any>(
    private val settingName: String,
    private val initialValue: T? = null,
) : TestWatcher() {

    private var originalValue: String? = null

    override fun starting(description: Description?) {
        originalValue = getSettingValueAsString()
        if (initialValue != null) {
            setSettingValue(initialValue)
        }
    }

    override fun finished(description: Description?) {
        setSettingValueAsString(originalValue)
    }

    fun setSettingValue(value: T) {
        val contentResolver = context.contentResolver
        when (value) {
            is Boolean -> putInt(contentResolver, settingName, if (value) 1 else 0)
            is Int -> putInt(contentResolver, settingName, value)
            is Long -> putLong(contentResolver, settingName, value)
            is Float -> putFloat(contentResolver, settingName, value)
            is String -> putString(contentResolver, settingName, value)
            else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
        }
    }

    private fun getSettingValueAsString(): String? = getString(context.contentResolver, settingName)

    private fun setSettingValueAsString(value: String?) {
        putString(context.contentResolver, settingName, value)
    }
}
