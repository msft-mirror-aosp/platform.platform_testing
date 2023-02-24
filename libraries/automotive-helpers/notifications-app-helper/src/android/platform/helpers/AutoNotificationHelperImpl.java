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
import android.platform.helpers.ScrollUtility.ScrollActions;
import android.platform.helpers.ScrollUtility.ScrollDirection;
import android.platform.helpers.exceptions.UnknownUiException;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

/** Helper for Notifications on Automotive device */
public class AutoNotificationHelperImpl extends AbstractStandardAppHelper
        implements IAutoNotificationHelper {

    private ScrollUtility mScrollUtility;
    private ScrollActions mScrollAction;
    private BySelector mBackwardButtonSelector;
    private BySelector mForwardButtonSelector;
    private BySelector mScrollableElementSelector;
    private ScrollDirection mScrollDirection;

    public AutoNotificationHelperImpl(Instrumentation instr) {
        super(instr);
        mScrollUtility = ScrollUtility.getInstance(getSpectatioUiUtil());
        mScrollAction =
                ScrollActions.valueOf(
                        getActionFromConfig(
                                AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_ACTION));
        mBackwardButtonSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_BACKWARD_BUTTON);
        mForwardButtonSelector =
                getUiElementFromConfig(
                        AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_FORWARD_BUTTON);
        mScrollableElementSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_LIST);
        mScrollDirection =
                ScrollDirection.valueOf(
                        getActionFromConfig(
                                AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_DIRECTION));
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
        open();
        getSpectatioUiUtil().wait5Seconds();
        UiObject2 empty_notification =
                getSpectatioUiUtil()
                        .findUiObject(
                                getUiElementFromConfig(
                                        AutomotiveConfigConstants.NOTIFICATION_LIST_EMPTY));
        if (empty_notification == null) {
            BySelector clearButtonSelector =
                    getUiElementFromConfig(AutomotiveConfigConstants.CLEAR_ALL_BUTTON);
            if (checkIfClearAllButtonExist(clearButtonSelector)) {
                UiObject2 clear_all_btn = getSpectatioUiUtil().findUiObject(clearButtonSelector);
                getSpectatioUiUtil().clickAndWait(clear_all_btn);
            } else {
                throw new RuntimeException("Cannot find Clear All button");
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean checkNotificationExists(String title) {
        open();
        BySelector selector = By.text(title);
        UiObject2 postedNotification = findInNotificationList(selector);
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
        getSpectatioUiUtil().wait5Seconds();
    }

    /** {@inheritDoc} */
    @Override
    public void openNotification() {
        // Swipe Down From top of screen to the bottom in one step
        getSpectatioUiUtil().swipeDown();
    }

    private boolean checkIfClearAllButtonExist(BySelector selector) {
        open();
        UiObject2 clr_btn = findInNotificationList(selector);
        return clr_btn != null;
    }

    @Override
    public boolean scrollDownOnePage() {
        UiObject2 notification_list = getSpectatioUiUtil().findUiObject(mScrollableElementSelector);
        if (notification_list.isScrollable()) {
            mScrollUtility.scrollForward(
                    mScrollAction,
                    mScrollDirection,
                    mBackwardButtonSelector,
                    mScrollableElementSelector,
                    String.format("Scroll up one page on notification list"));
        }
        return true;
    }

    @Override
    public boolean scrollUpOnePage() {
        UiObject2 notification_list = getSpectatioUiUtil().findUiObject(mScrollableElementSelector);
        if (notification_list.isScrollable()) {
            mScrollUtility.scrollBackward(
                    mScrollAction,
                    mScrollDirection,
                    mBackwardButtonSelector,
                    mScrollableElementSelector,
                    String.format("Scroll up one page on notification list"));
        }
        return true;
    }

    private UiObject2 findInNotificationList(BySelector selector) {
        UiObject2 notification_list = getSpectatioUiUtil().findUiObject(mScrollableElementSelector);
        UiObject2 object = null;
        if (isAppInForeground() && notification_list != null) {
            object = getSpectatioUiUtil().findUiObject(selector);
            if (object != null) return object;
            if (notification_list.isScrollable()) {
                object =
                        mScrollUtility.scrollAndFindUiObject(
                                mScrollAction,
                                mScrollDirection,
                                mForwardButtonSelector,
                                mBackwardButtonSelector,
                                mScrollableElementSelector,
                                selector,
                                String.format("Scroll on notification list to find %s", selector));
            }
        }
        return object;
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }
}
