/** Copyright 2022 Google Inc. All Rights Reserved. */
package android.platform.test.scenario.tapl_common;

import static org.junit.Assert.assertTrue;

import androidx.test.uiautomator.StaleObjectException;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObject2Condition;
import androidx.test.uiautomator.Until;

import java.time.Duration;

/**
 * A collection of gestures for UI objects that implements flake-proof patterns and adds
 * diagnostics. Don't use these gestures directly from the test, this class should be used only by
 * TAPL.
 */
public class Gestures {
    private static final Duration WAIT_TIME = Duration.ofSeconds(10);

    private static void waitForObjectCondition(
            UiObject2 object,
            String objectName,
            UiObject2Condition<Boolean> condition,
            String conditionName) {
        assertTrue(
                String.format("UI object '%s' is not %s.", objectName, conditionName),
                object.wait(condition, WAIT_TIME.toMillis()));
    }

    private static void waitForObjectEnabled(UiObject2 object, String objectName) {
        waitForObjectCondition(object, objectName, Until.enabled(true), "enabled");
    }

    private static void waitForObjectClickable(UiObject2 object, String waitReason) {
        waitForObjectCondition(object, waitReason, Until.clickable(true), "clickable");
    }

    /**
     * Wait for the object to become clickable and enabled, then clicks the object.
     *
     * @param object The object to click
     * @param objectName Name of the object for diags
     */
    public static void click(UiObject2 object, String objectName) {
        try {
            waitForObjectEnabled(object, objectName);
            waitForObjectClickable(object, objectName);
            object.click();
        } catch (StaleObjectException e) {
            throw new AssertionError(
                    String.format(
                            "UI object '%s' has disappeared from the screen during the click"
                                    + " gesture.",
                            objectName),
                    e);
        }
    }
}
