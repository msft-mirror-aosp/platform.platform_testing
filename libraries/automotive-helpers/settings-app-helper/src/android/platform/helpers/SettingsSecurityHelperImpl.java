/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.platform.helpers.exceptions.UnknownUiException;
import android.platform.spectatio.exceptions.MissingUiElementException;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

import java.util.List;

/** Helper class for functional tests of Security settings */
public class SettingsSecurityHelperImpl extends AbstractStandardAppHelper
        implements IAutoSecuritySettingsHelper {

    private enum ScrollActions {
        USE_BUTTON,
        USE_GESTURE;
    }

    private enum ScrollDirection {
        VERTICAL,
        HORIZONTAL;
    }

    public SettingsSecurityHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_PACKAGE);
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
    public void setLockByPassword(String password) {
        openChooseLockTypeMenu();
        BySelector password_menuSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.SECURITY_SETTINGS_LOCK_TYPE_PASSWORD);
        UiObject2 password_menu = getSpectatioUiUtil().findUiObject(password_menuSelector);
        getSpectatioUiUtil().clickAndWait(password_menu);
        getSpectatioUiUtil().wait5Seconds();
        typePasswordOnTextEditor(password);
        pressEnter();
        typePasswordOnTextEditor(password);
        pressEnter();
    }

    private void openChooseLockTypeMenu() {
        getSpectatioUiUtil().wait5Seconds();
        BySelector titlesSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_TITLE);
        List<UiObject2> titles = getSpectatioUiUtil().findUiObjects(titlesSelector);
        validateUiObject(titles, AutomotiveConfigConstants.SECURITY_SETTINGS_TITLE);
        UiObject2 title = titles.get(titles.size() - 1);
        if (title != null
                && title.getText()
                        .equalsIgnoreCase(
                                AutomotiveConfigConstants.SECURITY_SETTINGS_CHOOSE_LOCK_TYPE)) {
            // CHOOSE_LOCK_TYPE is already open
            return;
        }
        try {
            BySelector profileLockMenuSelector =
                    getUiElementFromConfig(
                            AutomotiveConfigConstants.SECURITY_SETTINGS_PROFILE_LOCK);
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(AutomotiveConfigConstants.SECURITY_SCROLL_ACTION));
            UiObject2 profileLockMenu = null;
            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector forwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.SECURITY_SCROLL_FORWARD_BUTTON);
                    BySelector backwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.SECURITY_SCROLL_BACKWARD_BUTTON);
                    profileLockMenu =
                            getSpectatioUiUtil()
                                    .scrollAndFindUiObject(
                                            forwardButtonSelector,
                                            backwardButtonSelector,
                                            profileLockMenuSelector);
                    break;
                case USE_GESTURE:
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants.SECURITY_SCROLL_DIRECTION));
                    BySelector scrollableElementSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.SECURITY_SETTINGS_SCROLLABLE_ELEMENT);
                    profileLockMenu =
                            getSpectatioUiUtil()
                                    .scrollAndFindUiObject(
                                            scrollableElementSelector,
                                            profileLockMenuSelector,
                                            (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through. Unknown Scroll Action %s.",
                                    scrollAction));
            }
            validateUiObject(
                    profileLockMenu, String.format("Profile Lock is %s", profileLockMenuSelector));
            getSpectatioUiUtil().clickAndWait(profileLockMenu);
            getSpectatioUiUtil().wait5Seconds();
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException(String.format("Unable to find Profile Lock Menu."));
        }
    }

    private void typePasswordOnTextEditor(String password) {
        BySelector textEditorSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_ENTER_PASSWORD);
        UiObject2 textEditor = getSpectatioUiUtil().findUiObject(textEditorSelector);
        validateUiObject(textEditor, AutomotiveConfigConstants.SECURITY_SETTINGS_ENTER_PASSWORD);
        textEditor.setText(password);
    }

    /** {@inheritDoc} */
    @Override
    public void setLockByPin(String pin) {
        openChooseLockTypeMenu();
        BySelector pin_menuSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_LOCK_TYPE_PIN);
        UiObject2 pin_menu = getSpectatioUiUtil().findUiObject(pin_menuSelector);
        validateUiObject(pin_menu, AutomotiveConfigConstants.SECURITY_SETTINGS_LOCK_TYPE_PIN);
        getSpectatioUiUtil().clickAndWait(pin_menu);
        getSpectatioUiUtil().wait5Seconds();
        selectPinOnPinPad(pin);
        BySelector continue_buttonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_CONTINUE_BUTTON);
        UiObject2 continue_button = getSpectatioUiUtil().findUiObject(continue_buttonSelector);
        validateUiObject(
                continue_button, AutomotiveConfigConstants.SECURITY_SETTINGS_CONTINUE_BUTTON);
        getSpectatioUiUtil().clickAndWait(continue_button);
        getSpectatioUiUtil().wait5Seconds();
        selectPinOnPinPad(pin);
        BySelector confirm_buttonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_CONFIRM_BUTTON);
        UiObject2 confirm_button = getSpectatioUiUtil().findUiObject(confirm_buttonSelector);
        validateUiObject(
                confirm_button, AutomotiveConfigConstants.SECURITY_SETTINGS_CONFIRM_BUTTON);
        getSpectatioUiUtil().clickAndWait(confirm_button);
        getSpectatioUiUtil().wait5Seconds();
    }

    private void selectPinOnPinPad(String pin) {
        int length = pin.length();
        for (int i = 0; i < length; i++) {
            char c = pin.charAt(i);
            UiObject2 number =
                    getSpectatioUiUtil()
                            .findUiObject(getUiElementFromConfig(Character.toString(c)));
            if (number == null) {
                number = getSpectatioUiUtil().findUiObject(By.text(Character.toString(c)));
            }
            validateUiObject(
                    number,
                    String.format("Unable to find number on pin pad: " + Character.toString(c)));
            getSpectatioUiUtil().clickAndWait(number);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unlockByPassword(String password) {
        BySelector textEditorSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_ENTER_PASSWORD);
        UiObject2 textEditor = getSpectatioUiUtil().findUiObject(textEditorSelector);
        validateUiObject(textEditor, AutomotiveConfigConstants.SECURITY_SETTINGS_ENTER_PASSWORD);
        textEditor.setText(password);
        pressEnter();
    }

    /** {@inheritDoc} */
    @Override
    public void unlockByPin(String pin) {

        selectPinOnPinPad(pin);
        BySelector enter_buttonSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.SECURITY_SETTINGS_ENTER_PIN_BUTTON);
        UiObject2 enter_button = getSpectatioUiUtil().findUiObject(enter_buttonSelector);
        validateUiObject(
                enter_button, AutomotiveConfigConstants.SECURITY_SETTINGS_ENTER_PIN_BUTTON);
        getSpectatioUiUtil().clickAndWait(enter_button);
        BySelector pinPadSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_PIN_PAD);
        UiObject2 pinPad = getSpectatioUiUtil().findUiObject(pinPadSelector);
        if (pinPad != null) {
            throw new RuntimeException("PIN input is not corrected");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeLock() {
        openChooseLockTypeMenu();
        BySelector none_menuSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_LOCK_TYPE_NONE);
        UiObject2 none_menu = getSpectatioUiUtil().findUiObject(none_menuSelector);
        validateUiObject(none_menu, AutomotiveConfigConstants.SECURITY_SETTINGS_LOCK_TYPE_NONE);
        getSpectatioUiUtil().clickAndWait(none_menu);
        getSpectatioUiUtil().wait5Seconds();
        BySelector remove_buttonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_REMOVE_BUTTON);
        UiObject2 remove_button = getSpectatioUiUtil().findUiObject(remove_buttonSelector);
        validateUiObject(remove_button, AutomotiveConfigConstants.SECURITY_SETTINGS_REMOVE_BUTTON);
        getSpectatioUiUtil().clickAndWait(remove_button);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeviceLocked() {
        openChooseLockTypeMenu();
        BySelector textEditorSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_ENTER_PASSWORD);
        UiObject2 textEditor = getSpectatioUiUtil().findUiObject(textEditorSelector);

        BySelector pinPadSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SECURITY_SETTINGS_PIN_PAD);
        UiObject2 pinPad = getSpectatioUiUtil().findUiObject(pinPadSelector);

        return textEditor != null || pinPad != null;
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }

    private void validateUiObject(List<UiObject2> uiObjects, String action) {
        if (uiObjects == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }

    private void pressEnter() {
        mDevice.pressEnter();
    }
}
