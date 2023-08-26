package platform.test.screenshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class MaterialYouColorsRuleTest {
    // A custom rule that saves the value of system_primary_dark before the MaterialYouColors rule
    // is applied.
    private var primaryDarkBefore = 0
    @get:Rule
    val savePreviousValueRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                primaryDarkBefore = systemPrimaryDark()
                base.evaluate()
            }
        }
    }

    @get:Rule val colorsRule = MaterialYouColorsRule()

    @Test
    fun testMaterialYouColorsChanged() {
        val systemPrimaryDark = systemPrimaryDark()

        // primaryDarkBefore was set by our custom rule.
        assertThat(primaryDarkBefore).isNotEqualTo(0)

        // The value of system_primary_dark was changed by the MaterialYouColorsRule.
        assertThat(systemPrimaryDark).isNotEqualTo(primaryDarkBefore)
    }

    private fun systemPrimaryDark(): Int {
        return InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getColor(android.R.color.system_primary_dark)
    }
}
