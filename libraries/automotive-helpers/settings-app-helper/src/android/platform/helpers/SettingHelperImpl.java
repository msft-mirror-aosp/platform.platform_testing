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

import android.app.Instrumentation;
import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import androidx.test.InstrumentationRegistry;

import java.util.regex.Pattern;

import junit.framework.Assert;

public class SettingHelperImpl extends AbstractAutoStandardAppHelper implements IAutoSettingHelper {

    private static final String LOG_TAG = SettingHelperImpl.class.getSimpleName();

    // Packages
    private static final String SETTING_APP_PACKAGE = "com.android.car.settings";
    private static final String SETTING_APP_INTELLIGENCE_PACKAGE =
            "com.android.settings.intelligence";
    private static final String PERMISSION_PAGE_PACKAGE = "com.android.permissioncontroller";

    private static final String LAUNCH_QUICK_SETTING_COMMAND =
            "am start -n "
                    + "com.android.car.settings/"
                    + "com.android.car.settings.common.CarSettingActivities$QuickSettingActivity";

    // Wait Time
    private static final int SHORT_UI_RESPONSE_WAIT_MS = 1000;
    private static final int UI_RESPONSE_WAIT_MS = 5000;

    private static final int MAX_SWIPE = 55;
    private static final int NUMBER_OF_SCROLLS = 5;

    // Text and Resource Id's
    private static final BySelector R_ID_LENSPICKER_PAGEDOWN =
            By.res(SETTING_APP_PACKAGE, "page_down");
    private static final BySelector SETTINGS_BUTTON =
            By.res(SETTING_APP_PACKAGE, "toolbar_menu_item_1");
    private static final BySelector HOTSPOT_TOGGLE_SWITCH = By.desc("Hotspot toggle switch");
    private static final BySelector BLUETOOTH_TOGGLE_SWITCH = By.desc("Bluetooth toggle switch");
    private static final BySelector WIFI_TOGGLE_SWITCH =
            By.res(SETTING_APP_PACKAGE, "action_widget_container");
    private static final BySelector BACK_BUTTON =
            By.res(SETTING_APP_PACKAGE, "car_ui_toolbar_nav_icon");
    private static final BySelector NIGHT_MODE_TEXT = By.text("Night mode");
    private static final BySelector UP_BTN =
            By.res(Pattern.compile(".*:id/car_ui_scrollbar_page_up"));
    private static final String SEEK_BAR = "com.android.car.settings:id/seek_bar";
    private static final BySelector SEARCH_BTN = By.desc("Search");
    private static final BySelector SEARCH_BOX =
            By.res(SETTING_APP_INTELLIGENCE_PACKAGE, "car_ui_toolbar_search_bar");
    private static final BySelector SEARCH_RESULTS =
            By.res(SETTING_APP_INTELLIGENCE_PACKAGE, "recycler_view");
    private static final BySelector PAGE_TITLE_IN_SETTING =
            By.res(SETTING_APP_PACKAGE, "car_ui_toolbar_title");
    private static final BySelector PAGE_TITLE_IN_PERMISSION =
            By.res(PERMISSION_PAGE_PACKAGE, "car_ui_toolbar_title");

    private SettingConstants mSettingConstants;
    private UiModeManager mUiModeManager;
    private Context mContext;

    public SettingHelperImpl(Instrumentation instr) {
        super(instr);
        mSettingConstants = new SettingConstants();
        mUiModeManager =
                InstrumentationRegistry.getInstrumentation()
                        .getContext()
                        .getSystemService(UiModeManager.class);
        mContext = InstrumentationRegistry.getContext();
    }

    /** {@inheritDoc} */
    @Override
    public void open() {
        openFullSettings();
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return SETTING_APP_PACKAGE;
    }

    /** {@inheritDoc} */
    @Override
    public void stopSettingsApplication() {
        String cmd = String.format("am force-stop %s", SETTING_APP_PACKAGE);
        executeShellCommand(cmd);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        return "Settings";
    }

    /** {@inheritDoc} */
    @Override
    public void openSetting(String setting) {
        UiObject2 menuButton = findSettingMenu(setting);
        clickAndWaitForIdle(menuButton);
        verifyAvailableOptions(setting);
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 findSettingMenu(String setting) {
        openFullSettings();
        UiObject2 menuButton = getMenu(mSettingConstants.getSettingNameVariations(setting));
        return menuButton;
    }

    /** Get menu item that matches one of the given name variations */
    private UiObject2 getMenu(String... menuItems) {
        UiObject2 object = null;
        for (String menu : menuItems) {
            try {
                Pattern menuPattern = Pattern.compile(menu, Pattern.CASE_INSENSITIVE);
                BySelector selector = By.text(menuPattern);
                object = getMenu(selector);
                if (object != null) {
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }
        if (object == null) {
            throw new RuntimeException("Unable to retrieve menu from UI");
        }
        return object;
    }

    /** Get menu item that matches the given selector */
    private UiObject2 getMenu(BySelector selector) {
        UiObject2 object = waitUntilFindUiObject(selector);
        if (object != null) {
            return object;
        }
        UiObject2 up = waitUntilFindUiObject(UP_BTN);
        while (up.isEnabled()) {
            up.click();
            mDevice.waitForIdle();
        }
        try {
            UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
            scrollable.setAsVerticalList();
            scrollable.flingToBeginning(MAX_SWIPE);
            object = waitUntilFindUiObject(selector);
            int count = 0;
            while (object == null && count < NUMBER_OF_SCROLLS) {
                scrollable.scrollForward();
                mDevice.wait(
                        Until.findObject(By.text("something_not_on_screen")), UI_RESPONSE_WAIT_MS);
                object = waitUntilFindUiObject(selector);
                count++;
            }
            if (object == null) {
                throw new IllegalStateException("Unable to retrieve menu from UI");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return object;
    }

    // Returns the type of testing device
    private String getTypeOfConnectedDevice() {
        String deviceType = "";
        String commandToGetDeviceType = "getprop ro.product.board";
        String commandOutput = executeShellCommand(commandToGetDeviceType);
        if (commandOutput != null) {
            deviceType = commandOutput.trim();
        }
        return deviceType;
    }

    /** {@inheritDoc} */
    @Override
    public void openFullSettings() {
        String cmd = "am start -a android.settings.SETTINGS";
        executeShellCommand(cmd);
    }

    /** {@inheritDoc} */
    @Override
    public void openQuickSettings() {
        executeShellCommand(LAUNCH_QUICK_SETTING_COMMAND);
        UiObject2 settingObject =
                mDevice.wait(Until.findObject(SETTINGS_BUTTON), UI_RESPONSE_WAIT_MS);
        if (settingObject == null) {
            goBackToSettingsScreen();
            executeShellCommand(LAUNCH_QUICK_SETTING_COMMAND);
        }
        settingObject = mDevice.wait(Until.findObject(SETTINGS_BUTTON), UI_RESPONSE_WAIT_MS);
        if (settingObject == null) {
            throw new RuntimeException("Failed to open quick settings.");
        }
    }

    private UiObject2 findEntryInList(BySelector listSelector, BySelector entrySelector) {
        if (mDevice.hasObject(entrySelector)) {
            return mDevice.findObject(entrySelector);
        }
        mDevice.wait(Until.findObject(listSelector), UI_RESPONSE_WAIT_MS);
        Assert.assertTrue("Failed to find list.", mDevice.hasObject(listSelector));
        mDevice.findObject(listSelector).scroll(Direction.UP, 100.0f);
        do {
            mDevice.waitForIdle();
            if (mDevice.hasObject(entrySelector)) {
                break;
            }
        } while (mDevice.findObject(listSelector).scroll(Direction.DOWN, 1.0f));
        UiObject2 entryFound = mDevice.findObject(entrySelector);
        if (entryFound != null) {
            return entryFound;
        }
        mDevice.findObject(listSelector).scroll(Direction.DOWN, 100.0f);
        entryFound = mDevice.findObject(entrySelector);
        if (entryFound != null) {
            return entryFound;
        }
        return mDevice.findObject(entrySelector);
    }

    private void verifyAvailableOptions(String setting) {
        UiObject2 backButton = waitUntilFindUiObject(BACK_BUTTON);
        if (backButton == null) {
            throw new RuntimeException("Back button is not found for the setting: " + setting);
        }

        String[] expectedOptions = mSettingConstants.getExpectedOptions(setting);
        if (expectedOptions == null) {
            return;
        }

        for (String option : expectedOptions) {
            if (mDevice.hasObject(By.clickable(false).textContains(option))) {
                continue;
            }
            BySelector entry = By.clickable(true).hasDescendant(By.textStartsWith(option));
            if (findEntryInList(By.scrollable(true), entry) == null) {
                throw new RuntimeException("Cannot find settings option: " + option);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void turnOnOffWifi(boolean onOff) {
        boolean isOn = isWifiOn();
        if (isOn != onOff) {
            UiObject2 enableOption = waitUntilFindUiObject(WIFI_TOGGLE_SWITCH);
            waitForWindowUpdateAfterClick(enableOption);
        } else {
            throw new RuntimeException("Wi-Fi enabled state is already " + (onOff ? "on" : "off"));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWifiOn() {
        WifiManager wifi = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }

    /** {@inheritDoc} */
    @Override
    public void turnOnOffHotspot(boolean onOff) {
        boolean isOn = isHotspotOn();
        if (isOn != onOff) {
            UiObject2 enableOption = waitUntilFindUiObject(HOTSPOT_TOGGLE_SWITCH);
            clickAndWaitForIdle(enableOption);
        } else {
            throw new RuntimeException(
                    "Hotspot enabled state is already " + (onOff ? "on" : "off"));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHotspotOn() {
        UiObject2 enableOption =
                waitUntilFindUiObject(HOTSPOT_TOGGLE_SWITCH)
                        .getChildren()
                        .get(1)
                        .getChildren()
                        .get(0);
        return enableOption.isChecked();
    }

    /** {@inheritDoc} */
    @Override
    public void turnOnOffBluetooth(boolean onOff) {
        boolean isOn = isBluetoothOn();
        if (isOn != onOff) {
            UiObject2 enableOption = waitUntilFindUiObject(BLUETOOTH_TOGGLE_SWITCH);
            waitForWindowUpdateAfterClick(enableOption);
        } else {
            throw new RuntimeException(
                    "Bluetooth enabled state is already " + (onOff ? "on" : "off"));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBluetoothOn() {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        return ba.isEnabled();
    }

    /** {@inheritDoc} */
    @Override
    public void searchAndSelect(String item) {
        searchAndSelect(item, 0);
    }

    /** {@inheritDoc} */
    @Override
    public void searchAndSelect(String item, int selectedIndex) {
        UiObject2 search_button = waitUntilFindUiObject(SEARCH_BTN);
        clickAndWaitForIdle(search_button);
        UiObject2 search_box = waitUntilFindUiObject(SEARCH_BOX);
        search_box.setText(item);
        SystemClock.sleep(UI_RESPONSE_WAIT_MS);
        // close the keyboard to reveal all search results.
        mDevice.pressBack();

        UiObject2 searchResults = waitUntilFindUiObject(SEARCH_RESULTS);
        int numberOfResults = searchResults.getChildren().size();
        if (numberOfResults == 0) {
            throw new RuntimeException("No results found");
        }
        clickAndWaitForIdle(searchResults.getChildren().get(selectedIndex));
        SystemClock.sleep(UI_RESPONSE_WAIT_MS);

        UiObject2 object = waitUntilFindUiObject(By.textContains(item));
        if (object == null) {
            throw new RuntimeException("Opened page does not contain searched item");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidPageTitle(String item) {
        UiObject2 pageTitle = getPageTitle();
        return pageTitle.getText().contains(item);
    }

    private UiObject2 getPageTitle() {
        BySelector[] selectors = new BySelector[] {PAGE_TITLE_IN_SETTING, PAGE_TITLE_IN_PERMISSION};
        for (BySelector selector : selectors) {
            UiObject2 pageTitle = waitUntilFindUiObject(selector);
            if (pageTitle != null) {
                return pageTitle;
            }
        }
        throw new RuntimeException("Unable to find page title");
    }

    /** {@inheritDoc} */
    @Override
    public void goBackToSettingsScreen() {
        UiObject2 backButton = waitUntilFindUiObject(BACK_BUTTON);
        while (backButton != null) {
            clickAndWaitForIdle(backButton);
            // TODO b/119773913
            mDevice.wait(
                    Until.findObject(By.text("something_not_on_screen")),
                    SHORT_UI_RESPONSE_WAIT_MS);
            backButton = waitUntilFindUiObject(BACK_BUTTON);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void openMenuWith(String... menuOptions) {
        for (String menu : menuOptions) {
            Pattern menuPattern = Pattern.compile(menu, Pattern.CASE_INSENSITIVE);
            UiObject2 menuButton = waitUntilFindUiObject(By.text(menuPattern));
            if (menuButton != null) {
                clickAndWaitForIdle(menuButton);
            } else {
                try {
                    UiScrollable menuList = new UiScrollable(new UiSelector().scrollable(true));
                    menuList.setAsVerticalList();
                    menuList.flingToBeginning(MAX_SWIPE);
                    menuButton = waitUntilFindUiObject(By.text(menuPattern));
                    int count = 0;
                    while (menuButton == null && count < NUMBER_OF_SCROLLS) {
                        menuList.scrollForward();
                        mDevice.wait(
                                Until.findObject(By.text("something_not_on_screen")),
                                UI_RESPONSE_WAIT_MS);
                        menuButton = waitUntilFindUiObject(By.text(menuPattern));
                        count++;
                    }
                    if (menuButton != null) {
                        clickAndWaitForIdle(menuButton);
                    } else {
                        throw new RuntimeException("Unable to find menu item");
                    }
                } catch (UiObjectNotFoundException exception) {
                    throw new RuntimeException("Unable to find menu item");
                }
            }
            mDevice.waitForIdle(UI_RESPONSE_WAIT_MS);
        }
    }

    /**
     * Checks whether a setting menu is enabled or not. When not enabled, the menu item cannot be
     * clicked.
     */
    @Override
    public boolean isSettingMenuEnabled(String menu) {
        UiObject2 menuObject = getMenu(mSettingConstants.getSettingNameVariations(menu));
        return menuObject.isEnabled();
    }

    /** {@inheritDoc} */
    @Override
    public int getValue(String setting) {
        String cmd = String.format("settings get system %s", setting);
        String value = executeShellCommand(cmd);
        return Integer.parseInt(value.replaceAll("\\s", ""));
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(String setting, int value) {
        String cmd = String.format("settings put system %s %d", setting, value);
        executeShellCommand(cmd);
    }

    /** {@inheritDoc} */
    @Override
    public void changeSeekbarLevel(int index, ChangeType changeType) {

        try {
            UiScrollable seekbar =
                    new UiScrollable(new UiSelector().resourceId(SEEK_BAR).instance(index));
            if (changeType == ChangeType.INCREASE) {
                seekbar.scrollForward(1);
            } else {
                seekbar.scrollBackward(1);
            }
            mDevice.waitForWindowUpdate(SETTING_APP_PACKAGE, UI_RESPONSE_WAIT_MS);
        } catch (UiObjectNotFoundException exception) {
            throw new RuntimeException("Unable to find seekbar");
        }
    }

    /**
     * Clicks on an UiObject2 and waits for device idle after click.
     *
     * @param uiObject - UiObject2 to click.
     */
    private void clickAndWaitForIdle(UiObject2 uiObject) {
        uiObject.click();
        mDevice.waitForIdle();
    }

    /**
     * Waits until timeout for the target UiObject to be found and returns the UiObject found
     *
     * @param selector BySelector to be found
     * @return UiObject2 - UiObject2 found
     */
    private UiObject2 waitUntilFindUiObject(BySelector selector) {
        UiObject2 uiObject2 = mDevice.wait(Until.findObject(selector), UI_RESPONSE_WAIT_MS);
        return uiObject2;
    }

    /**
     * Clicks on a UiObject2 and waits for the window to update
     *
     * @param uiObject2 uiObject to be clicked
     */
    private void waitForWindowUpdateAfterClick(UiObject2 uiObject) {
        uiObject.click();
        mDevice.waitForWindowUpdate(SETTING_APP_PACKAGE, UI_RESPONSE_WAIT_MS);
        mDevice.waitForIdle();
    }

    /** {@inheritDoc} */
    @Override
    public void setDayNightMode(DayNightMode mode) {
        if (mode == DayNightMode.DAY_MODE
                        && getDayNightModeStatus().getValue() == mUiModeManager.MODE_NIGHT_YES
                || mode == DayNightMode.NIGHT_MODE
                        && getDayNightModeStatus().getValue() != mUiModeManager.MODE_NIGHT_YES) {
            waitForWindowUpdateAfterClick(getNightModeButton());
        }
    }

    private UiObject2 getNightModeButton() {
        UiObject2 nightModeButton = waitUntilFindUiObject(NIGHT_MODE_TEXT);
        if (nightModeButton != null) {
            return nightModeButton.getParent();
        } else {
            UiObject2 scrollable = waitUntilFindUiObject(By.scrollable(true));
            if (scrollable != null) {
                boolean canScroll = true;
                while (nightModeButton == null && canScroll) {
                    canScroll = scrollable.scroll(Direction.DOWN, 100.0f);
                    nightModeButton = waitUntilFindUiObject(NIGHT_MODE_TEXT);
                }
            }
            if (nightModeButton == null) {
                throw new RuntimeException("Unable to find night mode button");
            }
            return nightModeButton.getParent();
        }
    }

    /** {@inheritDoc} */
    @Override
    public DayNightMode getDayNightModeStatus() {
        return mUiModeManager.getNightMode() == mUiModeManager.MODE_NIGHT_YES
                ? DayNightMode.NIGHT_MODE
                : DayNightMode.DAY_MODE;
    }
}
