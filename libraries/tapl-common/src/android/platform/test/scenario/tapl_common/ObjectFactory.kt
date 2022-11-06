/** Copyright 2022 Google Inc. All Rights Reserved. */
package android.platform.test.scenario.tapl_common

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.time.Duration

/**
 * Object finder for a specific app, represented by its package name. It can be used to find objects
 * that belong to this specific app.
 */
class ObjectFactory(private val packageName: String) {
    /**
     * Waits for a UI object with a given resource id. Fails if the object is not visible.
     *
     * @param [resourceId] Resource id for the app.
     * @param [objectName] Name of the object for diags
     * @return The found UI object.
     */
    fun waitForObject(resourceId: String, objectName: String): TaplUiObject {
        val selector = By.res(packageName, resourceId)
        val uiObject =
            device.wait(Until.findObject(selector), WAIT_TIME.toMillis())
                ?: throw AssertionError(
                    "UI object '$objectName' is not visible; selector: $selector."
                )
        return TaplUiObject(uiObject, objectName)
    }

    companion object {
        private val WAIT_TIME = Duration.ofSeconds(10)
        private val device: UiDevice
            get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
}
