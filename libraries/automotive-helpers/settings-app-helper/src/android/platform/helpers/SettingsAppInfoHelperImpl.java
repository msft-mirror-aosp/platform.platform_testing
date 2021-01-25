/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.platform.helpers;

import static junit.framework.Assert.assertTrue;

import android.app.Instrumentation;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;

import java.util.regex.Pattern;

public class SettingsAppInfoHelperImpl extends AbstractAutoStandardAppHelper
        implements IAutoAppInfoSettingsHelper {
    private static final int DEFAULT_WAIT_TIME = 5000;
    private static final int MAX_SWIPE = 55;

    private static final String ANDROIDX_RECYCLERVIEW_WIDGET_RECYCLERVIEW =
            "androidx.recyclerview.widget.RecyclerView";
    private static final String ALLOW_TEXT = "Allow";
    private static final String DENY_TEXT = "Deny";
    private static final String DISABLE_TEXT = "Disable";
    private static final String ENABLE_TEXT = "Enable";
    private static final String FORCE_STOP = "Force stop";
    private static final String PERMISSIONS_TEXT = "Permissions";
    private static final String SETTINGS_PACKAGE = "com.android.car.settings";
    private static final String CLASS_FRAMELAYOUT = "android.widget.FrameLayout";
    private static final String SHOW_ALL_APPS = "Show all apps";

    public SettingsAppInfoHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public void open() {
        String cmd = "am start -a android.settings.SETTINGS";
        executeShellCommand(cmd);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return SETTINGS_PACKAGE;
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Not Supported");
    }

    /** {@inheritDoc} */
    @Override
    public void showAllApps() {
        BySelector selector = By.clickable(true).hasDescendant(By.text(SHOW_ALL_APPS));
        UiObject2 show_all_apps_menu = getMenu(selector, DEFAULT_WAIT_TIME);
        clickAndWaitForWindowUpdate(show_all_apps_menu);
    }

    /** {@inheritDoc} */
    @Override
    public void enableDisableApplication(State state) {
        BySelector enableDisableBtnSelector = By.res(SETTINGS_PACKAGE, "button1Text");
        UiObject2 enableDisableBtn = getMenu(enableDisableBtnSelector, DEFAULT_WAIT_TIME);
        clickAndWaitForWindowUpdate(enableDisableBtn.getParent());
        if (state == State.ENABLE) {
            assertTrue(
                    "application is not enabled",
                    enableDisableBtn.getText().matches("(?i)" + DISABLE_TEXT));
        } else {
            Pattern disableButtonPattern = Pattern.compile("DISABLE APP", Pattern.CASE_INSENSITIVE);
            BySelector disableAppBtnSelector = By.text(disableButtonPattern);
            UiObject2 disableAppBtn = getMenu(disableAppBtnSelector, DEFAULT_WAIT_TIME);
            clickAndWaitForWindowUpdate(disableAppBtn);
            assertTrue(
                    "application is not disabled",
                    enableDisableBtn.getText().matches("(?i)" + ENABLE_TEXT));
        }
    }

    private UiObject2 getBtnByText(String... texts) {
        for (String text : texts) {
            BySelector btnSelector = By.text(text);
            UiObject2 btn = mDevice.wait(Until.findObject(btnSelector), DEFAULT_WAIT_TIME);
            if (btn != null) {
                return btn;
            }
        }
        throw new RuntimeException("Cannot find button");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCurrentApplicationRunning() {
        UiObject2 forceStopButton = getForceStopButton();
        if (forceStopButton == null) {
            throw new RuntimeException("Cannot find force stop button");
        }
        return forceStopButton.isEnabled() ? true : false;
    }

    /** {@inheritDoc} */
    @Override
    public void forceStop() {
        UiObject2 forceStopButton = getForceStopButton();
        if (forceStopButton == null) {
            throw new RuntimeException("Cannot find force stop button");
        }
        clickAndWaitForWindowUpdate(forceStopButton);
        BySelector okBtnSelector = By.text("OK");
        UiObject2 okBtn = getMenu(okBtnSelector, DEFAULT_WAIT_TIME);
        clickAndWaitForWindowUpdate(okBtn);
    }

    /** {@inheritDoc} */
    @Override
    public void setAppPermission(String permission, State state) {
        Pattern permissionsButtonPattern =
                Pattern.compile(PERMISSIONS_TEXT + "?", Pattern.CASE_INSENSITIVE);
        BySelector permissions_selector = By.text(permissionsButtonPattern);
        UiObject2 permissions_menu = getMenu(permissions_selector, DEFAULT_WAIT_TIME);
        clickAndWaitForWindowUpdate(permissions_menu);
        BySelector permission_selector = By.text(permission);
        UiObject2 permission_menu = getMenu(permission_selector, DEFAULT_WAIT_TIME);
        clickAndWaitForWindowUpdate(permission_menu);
        if (state == State.ENABLE) {
            UiObject2 allow_btn = getMenu(By.text(ALLOW_TEXT), DEFAULT_WAIT_TIME);
            clickAndWaitForWindowUpdate(allow_btn);
        } else {
            UiObject2 deny_btn = getMenu(By.text(DENY_TEXT), DEFAULT_WAIT_TIME);
            clickAndWaitForWindowUpdate(deny_btn);
            UiObject2 deny_anyway_btn = getBtnByText("Deny anyway", "DENY ANYWAY");
            clickAndWaitForWindowUpdate(deny_anyway_btn);
        }
        BySelector back_button_selector = By.clazz(CLASS_FRAMELAYOUT).clickable(true);
        UiObject2 back_button = getMenu(back_button_selector, DEFAULT_WAIT_TIME);
        clickAndWaitForWindowUpdate(back_button);
        back_button_selector = By.clazz(CLASS_FRAMELAYOUT).clickable(true);
        back_button = getMenu(back_button_selector, DEFAULT_WAIT_TIME);
        clickAndWaitForWindowUpdate(back_button);
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentPermissions() {
        Pattern permissionsButtonPattern =
                Pattern.compile(PERMISSIONS_TEXT + "?", Pattern.CASE_INSENSITIVE);
        BySelector permission_selector = By.text(permissionsButtonPattern);
        UiObject2 permission_menu = getMenu(permission_selector, DEFAULT_WAIT_TIME);
        String currentPermissions = permission_menu.getParent().getChildren().get(1).getText();
        return currentPermissions;
    }

    /** {@inheritDoc} */
    @Override
    public void selectApp(String application) {
        BySelector selector = By.textContains(application);
        UiObject2 object = getMenu(selector, DEFAULT_WAIT_TIME);
        if (object == null) {
            throw new RuntimeException("Cannot find the app menu");
        }
        clickAndWaitForWindowUpdate(object);
    }

    private UiObject2 getForceStopButton() {
        Pattern forceStopButtonPattern = Pattern.compile(FORCE_STOP, Pattern.CASE_INSENSITIVE);
        BySelector forceStopSelector = By.text(forceStopButtonPattern);
        UiObject2 forceStopButton = getMenu(forceStopSelector, DEFAULT_WAIT_TIME);
        return forceStopButton;
    }

    private UiObject2 getMenu(BySelector selector, int waitTime) {
        UiObject2 object = getDevice().wait(Until.findObject(selector), waitTime);
        UiSelector uiSelector =
                new UiSelector().className(ANDROIDX_RECYCLERVIEW_WIDGET_RECYCLERVIEW);
        UiScrollable scrollable = new UiScrollable(uiSelector);
        scrollable.setAsVerticalList();
        if (object == null) {
            try {
                scrollable.flingToBeginning(MAX_SWIPE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        object = getDevice().wait(Until.findObject(selector), waitTime);
        while (object == null) {
            try {
                scrollable.scrollForward();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            object = getDevice().wait(Until.findObject(selector), waitTime);
        }
        if (object == null) {
            throw new IllegalStateException("Unable to retrieve menu from UI");
        }
        return object;
    }

    private void clickAndWaitForWindowUpdate(UiObject2 object) {
        if (object == null) {
            throw new RuntimeException("Cannot find object");
        }
        object.click();
        getDevice().waitForWindowUpdate(SETTINGS_PACKAGE, DEFAULT_WAIT_TIME);
    }

    private UiDevice getDevice() {
        return mDevice;
    }
}
