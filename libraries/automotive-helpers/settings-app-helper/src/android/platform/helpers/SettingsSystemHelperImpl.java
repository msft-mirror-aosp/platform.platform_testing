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
import android.content.res.Resources;
import android.platform.helpers.ScrollUtility.ScrollActions;
import android.platform.helpers.ScrollUtility.ScrollDirection;
import android.platform.helpers.exceptions.UnknownUiException;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.Date;

/** Helper class for functional tests of System settings */
public class SettingsSystemHelperImpl extends AbstractStandardAppHelper
        implements IAutoSystemSettingsHelper {

    private ScrollUtility mScrollUtility;

    public SettingsSystemHelperImpl(Instrumentation instr) {
        super(instr);
        mScrollUtility = ScrollUtility.getInstance(getSpectatioUiUtil());
        mScrollUtility.setScrollValues(
                Integer.valueOf(
                        getActionFromConfig(
                                AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_MARGIN)),
                Integer.valueOf(
                        getActionFromConfig(
                                AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_WAIT_TIME)));
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.SETTINGS_PACKAGE);
    }

    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void setDisplayLanguage(String language) {
        openLanguageMenu();
        BySelector languageSelector = By.clickable(true).hasDescendant(By.textStartsWith(language));
        UiObject2 languageObject = getMenu(languageSelector);
        validateUiObject(
                languageObject, String.format("Unable to find UI Element for language selector"));
        getSpectatioUiUtil().clickAndWait(languageObject);
        String systemLanguage =
                Resources.getSystem().getConfiguration().getLocales().get(0).getDisplayLanguage();
        if (!language.toLowerCase().contains(systemLanguage.toLowerCase())) {
            throw new RuntimeException("System language is different from selected language");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentLanguage() {
        UiObject2 object =
                getBtnByText(
                        AutomotiveConfigConstants.LANGUAGES_MENU,
                        AutomotiveConfigConstants.LANGUAGES_MENU_IN_SELECTED_LANGUAGE);
        validateUiObject(object, String.format("Unable to find UI Element for current language"));
        String currentLanguage = getSummeryText(object);
        return currentLanguage;
    }

    /** {@inheritDoc} */
    @Override
    public String getDeviceModel() {
        openAboutMenu();
        UiObject2 object = getMenu(getUiElementFromConfig(AutomotiveConfigConstants.DEVICE_MODEL));
        validateUiObject(object, String.format("Unable to find UI Element for device model"));
        String modelName = getSummeryText(object);
        return modelName;
    }

    /** {@inheritDoc} */
    @Override
    public String getAndroidVersion() {
        openAboutMenu();
        UiObject2 object =
                getMenu(getUiElementFromConfig(AutomotiveConfigConstants.ANDROID_VERSION));
        validateUiObject(object, String.format("Unable to find UI Element for current language"));
        String androidVersion = getSummeryText(object);
        return androidVersion;
    }

    /** {@inheritDoc} */
    @Override
    public Date getAndroidSecurityPatchLevel() {
        openAboutMenu();
        UiObject2 object =
                getMenu(
                        getUiElementFromConfig(
                                AutomotiveConfigConstants.ANDROID_SECURITY_PATCH_LEVEL));
        validateUiObject(
                object,
                String.format("Unable to find UI Element for android security patch level"));
        String androidSecurityPatchLevel = getSummeryText(object);
        Date patchDate = parseDate(androidSecurityPatchLevel, "MMMM dd, yyyy");
        if (patchDate == null) {
            patchDate = parseDate(androidSecurityPatchLevel, "dd MMMM yyyy");
        }
        if (patchDate == null) {
            throw new RuntimeException("Cannot find date from UI");
        }
        return formatDate(patchDate, "MMMM dd, yyyy"); // return locale independent date
    }

    private Date formatDate(Date date, String format) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(format);
        String dateString = dateFormatter.format(date);
        String[] arr = dateString.split(" ");
        int year = Integer.valueOf(arr[2]);
        int month = Month.valueOf(arr[0].toUpperCase()).getValue() - 1;
        int day = Integer.valueOf(arr[1].substring(0, arr[1].length() - 1));
        return new Date(year, month, day);
    }

    private Date parseDate(String date, String format) {
        Date parsedDate = null;
        try {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(format);
            parsedDate = dateFormatter.parse(date);
        } catch (Exception e) {
            // do nothing
        }
        return parsedDate;
    }

    /** {@inheritDoc} */
    @Override
    public String getKernelVersion() {
        openAboutMenu();
        UiObject2 object =
                getMenu(getUiElementFromConfig(AutomotiveConfigConstants.KERNEL_VERSION));
        validateUiObject(object, String.format("Unable to find UI Element for kernel version"));
        String kernelVersion = getSummeryText(object);
        return kernelVersion;
    }

    /** {@inheritDoc} */
    @Override
    public String getBuildNumber() {
        openAboutMenu();
        UiObject2 object = getMenu(getUiElementFromConfig(AutomotiveConfigConstants.BUILD_NUMBER));
        validateUiObject(object, String.format("Unable to find UI Element for build number"));
        String buildNumber = getSummeryText(object);
        return buildNumber;
    }

    /** {@inheritDoc} */
    @Override
    public void resetNetwork() {
        openResetOptionsMenu();
        BySelector resetNetworkSelector =
                By.clickable(true)
                        .hasDescendant(
                                getUiElementFromConfig(AutomotiveConfigConstants.RESET_NETWORK));
        UiObject2 resetNetworkMenu = getMenu(resetNetworkSelector);
        validateUiObject(resetNetworkMenu, AutomotiveConfigConstants.RESET_NETWORK);
        getSpectatioUiUtil().clickAndWait(resetNetworkMenu);
        BySelector resetSettingsSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.RESET_SETTINGS);

        UiObject2 resetSettingsButton1 = getMenu(resetSettingsSelector);
        validateUiObject(resetSettingsButton1, AutomotiveConfigConstants.RESET_SETTINGS);
        getSpectatioUiUtil().clickAndWait(resetSettingsButton1);

        UiObject2 resetSettingsButton2 =
                getMenu(getUiElementFromConfig(AutomotiveConfigConstants.RESET_SETTINGS));
        validateUiObject(resetSettingsButton2, AutomotiveConfigConstants.RESET_SETTINGS);
        getSpectatioUiUtil().clickAndWait(resetSettingsButton2);
    }

    /** {@inheritDoc} */
    @Override
    public void resetAppPreferences() {
        openResetOptionsMenu();
        BySelector selector =
                By.clickable(true)
                        .hasDescendant(
                                getUiElementFromConfig(
                                        AutomotiveConfigConstants.RESET_APP_PREFERENCES));
        UiObject2 object = getMenu(selector);
        validateUiObject(object, AutomotiveConfigConstants.RESET_APP_PREFERENCES);
        getSpectatioUiUtil().clickAndWait(object);
        BySelector reset_apps_selector =
                getUiElementFromConfig(AutomotiveConfigConstants.RESET_APPS);

        UiObject2 reset_apps_button = getMenu(reset_apps_selector);
        validateUiObject(reset_apps_button, AutomotiveConfigConstants.RESET_APPS);
        getSpectatioUiUtil().clickAndWait(reset_apps_button);
    }

    /** {@inheritDoc} */
    @Override
    public void openLanguagesInputMenu() {
        UiObject2 languagesInputMenu =
                getMenu(getUiElementFromConfig(AutomotiveConfigConstants.LANGUAGES_AND_INPUT_MENU));
        validateUiObject(
                languagesInputMenu,
                String.format("Unable to find UI Element for language input menu"));
        getSpectatioUiUtil().clickAndWait(languagesInputMenu);
    }

    private String getSummeryText(UiObject2 object) {
        UiObject2 parent = object.getParent();
        if (parent.getChildren().size() < 2) {
            BySelector swipeSelector =
                    getUiElementFromConfig(
                            AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_ELEMENT);
            UiObject2 swipeObject = getSpectatioUiUtil().findUiObject(swipeSelector);
            validateUiObject(swipeObject, AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_ELEMENT);
            getSpectatioUiUtil().swipeUp(swipeObject);
        }
        return object.getParent().getChildren().get(1).getText();
    }

    private void openResetOptionsMenu() {
        UiObject2 resetOptionMenu =
                getMenu(getUiElementFromConfig(AutomotiveConfigConstants.RESET_OPTIONS_MENU));
        validateUiObject(
                resetOptionMenu, String.format("Unable to find UI Element for reset menu"));
        getSpectatioUiUtil().clickAndWait(resetOptionMenu);
    }

    private void openAboutMenu() {
        getSpectatioUiUtil().wait1Second();
        UiObject2 aboutMenu = getMenu(getUiElementFromConfig(AutomotiveConfigConstants.ABOUT_MENU));
        validateUiObject(aboutMenu, String.format("Unable to find UI Element for about menu"));
        getSpectatioUiUtil().clickAndWait(aboutMenu);
    }

    private void openLanguageMenu() {
        UiObject2 languageMenu =
                getBtnByText(
                        AutomotiveConfigConstants.LANGUAGES_MENU,
                        AutomotiveConfigConstants.LANGUAGES_MENU_IN_SELECTED_LANGUAGE);
        validateUiObject(
                languageMenu, String.format("Unable to find UI Element for language menu"));
        getSpectatioUiUtil().clickAndWait(languageMenu);
    }

    private UiObject2 getBtnByText(String... texts) {
        for (String text : texts) {
            BySelector btnSelector = By.text(text);
            UiObject2 btn = getSpectatioUiUtil().findUiObject(btnSelector);
            if (btn != null) {
                return btn;
            }
        }
        throw new RuntimeException("Cannot find button");
    }

    private UiObject2 getMenu(BySelector selector) {
        ScrollActions scrollAction =
                ScrollActions.valueOf(
                        getActionFromConfig(
                                AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_ACTION));

        BySelector forwardButtonSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_BACKWARD_BUTTON);
        BySelector backwardButtonSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_BACKWARD_BUTTON);

        BySelector scrollableElementSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_ELEMENT);
        ScrollDirection scrollDirection =
                ScrollDirection.valueOf(
                        getActionFromConfig(
                                AutomotiveConfigConstants.SYSTEM_SETTINGS_SCROLL_DIRECTION));

        UiObject2 object =
                mScrollUtility.scrollAndFindUiObject(
                        scrollAction,
                        scrollDirection,
                        forwardButtonSelector,
                        backwardButtonSelector,
                        scrollableElementSelector,
                        selector,
                        String.format("Scroll on system setting to find %s", selector));

        validateUiObject(
                object, String.format("Unable to find UI Element %s.", selector.toString()));
        return object;
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }
}
