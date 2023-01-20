/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.platform.spectatio.exceptions.MissingUiElementException;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

/** Helper class for functional test for App Grid test */
public class AppGridHelperImpl extends AbstractStandardAppHelper implements IAutoAppGridHelper {
    private ScrollUtility mScrollUtility;

    public AppGridHelperImpl(Instrumentation instr) {
        super(instr);
        mScrollUtility = ScrollUtility.getInstance(getSpectatioUiUtil());
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.APP_GRID_PACKAGE);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
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
     * <p>Open app grid by pressing app grid facet ony if app grid is not open.
     */
    @Override
    public void open() {
        if (!isAppInForeground()) {
            getSpectatioUiUtil()
                    .executeShellCommand(
                            getCommandFromConfig(AutomotiveConfigConstants.OPEN_APP_GRID_COMMAND));
            getSpectatioUiUtil().wait5Seconds();
        }
    }

    /**
     * Setup expectations: None
     *
     * <p>Check if app grid is in foreground.
     */
    @Override
    public boolean isAppInForeground() {
        BySelector appGridViewIdSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.APP_GRID_VIEW_ID);
        return getSpectatioUiUtil().hasUiElement(appGridViewIdSelector);
    }

    /**
     * Setup expectation: None.
     *
     * <p>Exit app grid by pressing home facet only if app grid is open.
     */
    @Override
    public void exit() {
        if (isAppInForeground()) {
            mDevice.pressHome();
        }
    }

    @Override
    public void openApp(String appName) {
        BySelector appNameSelector = By.text(appName);
        ScrollActions scrollAction =
                ScrollActions.valueOf(
                        getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ACTION));
        BySelector backwardButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.APP_GRID_SCROLL_BACKWARD_BUTTON);
        BySelector forwardButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.APP_GRID_SCROLL_FORWARD_BUTTON);
        BySelector scrollableElementSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
        ScrollDirection scrollDirection =
                ScrollDirection.valueOf(
                        getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
        UiObject2 app =
                mScrollUtility.scrollAndFindUiObject(
                        scrollAction,
                        scrollDirection,
                        forwardButtonSelector,
                        backwardButtonSelector,
                        scrollableElementSelector,
                        appNameSelector,
                        String.format("Scroll on app grid to find %s", appName));

        validateUiObject(app, String.format("Given app %s", appName));
        getSpectatioUiUtil().clickAndWait(app);
    }

    /** {@inherticDoc} */
    @Override
    public boolean isTop() {
        boolean isOnTop = false;
        try {
            if (isAppInForeground()) {
                BySelector pageUpSelector =
                        getUiElementFromConfig(
                                AutomotiveConfigConstants.APP_GRID_SCROLL_BACKWARD_BUTTON);
                UiObject2 pageUp = getSpectatioUiUtil().findUiObject(pageUpSelector);
                if (pageUp != null) {
                    isOnTop = !pageUp.isEnabled();
                } else {
                    BySelector scrollableElementSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
                    boolean isScrollable =
                            getSpectatioUiUtil()
                                    .findUiObject(scrollableElementSelector)
                                    .isScrollable();
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
                    if (isScrollable) {
                        isOnTop =
                                !getSpectatioUiUtil()
                                        .scrollBackward(
                                                scrollableElementSelector,
                                                (scrollDirection == ScrollDirection.VERTICAL));
                        if (!isOnTop) {
                            // To place the scroll in previous position
                            getSpectatioUiUtil()
                                    .scrollForward(
                                            scrollableElementSelector,
                                            (scrollDirection == ScrollDirection.VERTICAL));
                        }
                    } else {
                        // Number of apps fits in one page, at top by default
                        isOnTop = true;
                    }
                }
            }
            return isOnTop;
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException("App grid is not open.");
        }
    }

    /** {@inherticDoc} */
    @Override
    public boolean isBottom() {
        boolean isAtBotton = false;
        try {
            if (isAppInForeground()) {
                BySelector pageDownSelector =
                        getUiElementFromConfig(
                                AutomotiveConfigConstants.APP_GRID_SCROLL_FORWARD_BUTTON);
                UiObject2 pageDown = getSpectatioUiUtil().findUiObject(pageDownSelector);
                if (pageDown != null) {
                    isAtBotton = !pageDown.isEnabled();
                } else {
                    BySelector scrollableElementSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
                    boolean isScrollable =
                            getSpectatioUiUtil()
                                    .findUiObject(scrollableElementSelector)
                                    .isScrollable();
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
                    if (isScrollable) {
                        isAtBotton =
                                !getSpectatioUiUtil()
                                        .scrollForward(
                                                scrollableElementSelector,
                                                (scrollDirection == ScrollDirection.VERTICAL));
                        if (!isAtBotton) {
                            // To place the scroll in previous position
                            getSpectatioUiUtil()
                                    .scrollBackward(
                                            scrollableElementSelector,
                                            (scrollDirection == ScrollDirection.VERTICAL));
                        }
                    } else {
                        // Number of apps fits in one page, at top by default
                        isAtBotton = true;
                    }
                }
            }
            return isAtBotton;
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException("App grid is not open.");
        }
    }

    @Override
    public boolean scrollUpOnePage() {
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ACTION));
                    BySelector backwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_GRID_SCROLL_BACKWARD_BUTTON);
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
        BySelector scrollableElementSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
        return mScrollUtility.scrollBackward(
                scrollAction,
                scrollDirection,
                backwardButtonSelector,
                scrollableElementSelector,
                String.format("Scroll up one page on app grid"));
    }

    @Override
    public boolean scrollDownOnePage() {
        ScrollActions scrollAction =
                ScrollActions.valueOf(
                        getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ACTION));
        BySelector forwardButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.APP_GRID_SCROLL_FORWARD_BUTTON);
        ScrollDirection scrollDirection =
                ScrollDirection.valueOf(
                        getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
        BySelector scrollableElementSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
        return mScrollUtility.scrollForward(
                scrollAction,
                scrollDirection,
                forwardButtonSelector,
                scrollableElementSelector,
                String.format("Scroll down one page on app grid"));
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }
}
