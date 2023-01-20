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
import android.platform.helpers.exceptions.UnknownUiException;
import android.platform.spectatio.exceptions.MissingUiElementException;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

/** Helper for Notifications on Automotive device */
public class AutoNotificationHelperImpl extends AbstractStandardAppHelper
        implements IAutoNotificationHelper {
    private enum ScrollActions {
        USE_BUTTON,
        USE_GESTURE;
    }

    private enum ScrollDirection {
        VERTICAL,
        HORIZONTAL;
    }

    public AutoNotificationHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public void exit() {
        getSpectatioUiUtil().pressHome();
        getSpectatioUiUtil().wait1Second();
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }
    /**
     * Setup expectation: None.
     *
     * <p>Open notification, do nothing if notification is already open.
     */
    @Override
    public void open() {
        if (!isAppInForeground()) {
            getSpectatioUiUtil()
                    .executeShellCommand(
                            getCommandFromConfig(
                                    AutomotiveConfigConstants.OPEN_NOTIFICATIONS_COMMAND));
            getSpectatioUiUtil().wait1Second();
        }
    }

    /**
     * Setup expectations: None
     *
     * <p>Check if notification app is in foreground by checking if the notification list exists.
     */
    @Override
    public boolean isAppInForeground() {
        BySelector notificationViewSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_VIEW);
        return getSpectatioUiUtil().hasUiElement(notificationViewSelector);
    }

    /** {@inheritDoc} */
    @Override
    public void tapClearAllBtn() {
        BySelector clearButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.CLEAR_ALL_BUTTON);
        if (checkIfClearAllButtonExist(clearButtonSelector)) {
            UiObject2 clear_all_btn = getSpectatioUiUtil().findUiObject(clearButtonSelector);
            getSpectatioUiUtil().clickAndWait(clear_all_btn);
        } else {
            throw new RuntimeException("Cannot find Clear All button");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkNotificationExists(String title) {
        open();
        UiObject2 notification_list =
                getSpectatioUiUtil()
                        .findUiObject(
                                getUiElementFromConfig(
                                        AutomotiveConfigConstants.NOTIFICATION_LIST));
        BySelector selector = By.text(title);
        UiObject2 postedNotification = getSpectatioUiUtil().findUiObject(By.text(title));
        if (postedNotification != null) return true;
        if (isAppInForeground() && notification_list != null) {
            getSpectatioUiUtil().wait5Seconds();
            if (!notification_list.isScrollable()) return false;
            postedNotification = findInNotificationList(selector);
        }
        return postedNotification != null;
    }

    /** {@inheritDoc} */
    @Override
    public void removeNotification(String title) {
        getSpectatioUiUtil().wait5Seconds();
        open();
        UiObject2 postedNotification = getSpectatioUiUtil().findUiObject(By.text(title));
        validateUiObject(
                postedNotification, String.format("Unable to get the posted notification."));
        getSpectatioUiUtil().swipeLeft(postedNotification);
    }

    /** {@inheritDoc} */
    @Override
    public void openNotification() {
        // Swipe Down From top of screen to the bottom in one step
        getSpectatioUiUtil().swipeDown();
    }

    private boolean checkIfClearAllButtonExist(BySelector selector) {
        open();
        UiObject2 notification_list =
                getSpectatioUiUtil()
                        .findUiObject(
                                getUiElementFromConfig(
                                        AutomotiveConfigConstants.NOTIFICATION_LIST));

        UiObject2 clr_btn = getSpectatioUiUtil().findUiObject(selector);
        if (clr_btn != null) return true;
        if (isAppInForeground() && notification_list != null) {
            getSpectatioUiUtil().wait5Seconds();
            if (!notification_list.isScrollable()) return false;
            clr_btn = findInNotificationList(selector);
        }
        return clr_btn != null;
    }

    @Override
    public boolean scrollDownOnePage() {
        try {
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_ACTION));
            ScrollDirection scrollDirection =
                    ScrollDirection.valueOf(
                            getActionFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_DIRECTION));
            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector forwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_FORWARD);
                    getSpectatioUiUtil().scrollForward(forwardButtonSelector);
                    break;
                case USE_GESTURE:
                    BySelector scrollElementSelector =
                            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_LIST);
                    UiObject2 notification_list =
                            getSpectatioUiUtil().findUiObject(scrollElementSelector);
                    if (notification_list.isScrollable()) {
                        getSpectatioUiUtil()
                                .scrollForward(
                                        scrollElementSelector,
                                        scrollDirection == ScrollDirection.VERTICAL);
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through notification list. Unknown Scroll Action"
                                            + " %s.",
                                    scrollAction));
            }
        } catch (MissingUiElementException ex) {
            throw new RuntimeException("Unable to scroll through notification list ", ex);
        }
        return true;
    }

    @Override
    public boolean scrollUpOnePage() {
        try {
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_ACTION));
            ScrollDirection scrollDirection =
                    ScrollDirection.valueOf(
                            getActionFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_DIRECTION));

            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector backwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_BACKWARD);
                    getSpectatioUiUtil().scrollBackward(backwardButtonSelector);
                    break;
                case USE_GESTURE:
                    BySelector scrollElementSelector =
                            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_LIST);
                    UiObject2 notification_list =
                            getSpectatioUiUtil().findUiObject(scrollElementSelector);
                    if (notification_list.isScrollable()) {
                        getSpectatioUiUtil()
                                .scrollBackward(
                                        scrollElementSelector,
                                        scrollDirection == ScrollDirection.VERTICAL);
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through notification list. Unknown Scroll Action"
                                            + " %s.",
                                    scrollAction));
            }
        } catch (MissingUiElementException ex) {
            throw new RuntimeException("Unable to scroll through notification list ", ex);
        }
        return true;
    }

    private UiObject2 findInNotificationList(BySelector selector) {
        try {
            UiObject2 notification_list =
                    getSpectatioUiUtil()
                            .findUiObject(
                                    getUiElementFromConfig(
                                            AutomotiveConfigConstants.NOTIFICATION_LIST));

            UiObject2 object = null;

            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_ACTION));
            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector forwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_FORWARD);
                    BySelector backwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_BACKWARD);
                    object =
                            getSpectatioUiUtil()
                                    .scrollAndFindUiObject(
                                            forwardButtonSelector,
                                            backwardButtonSelector,
                                            selector);
                    break;
                case USE_GESTURE:
                    BySelector scrollElementSelector =
                            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_LIST);
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants
                                                    .NOTIFICATION_LIST_SCROLL_DIRECTION));
                    if (notification_list.isScrollable()) {
                        object =
                                getSpectatioUiUtil()
                                        .scrollAndFindUiObject(
                                                scrollElementSelector,
                                                selector,
                                                (scrollDirection == ScrollDirection.VERTICAL));
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through notification list. "
                                            + "Unknown Scroll Action %s.",
                                    scrollAction));
            }
            return object;
        } catch (MissingUiElementException ex) {
            throw new RuntimeException("Unable to scroll through notification list ", ex);
        }
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }
}
