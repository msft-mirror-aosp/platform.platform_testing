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
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

/** Helper class for functional test for App Grid test */
public class AppGridHelperImpl extends AbstractStandardAppHelper implements IAutoAppGridHelper {

    public AppGridHelperImpl(Instrumentation instr) {
        super(instr);
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
        try {
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ACTION));
            UiObject2 app = null;
            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector backwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_GRID_SCROLL_BACKWARD_BUTTON);
                    BySelector forwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_GRID_SCROLL_FORWARD_BUTTON);
                    app =
                            getSpectatioUiUtil()
                                    .scrollAndFindUiObject(
                                            forwardButtonSelector, backwardButtonSelector, appName);
                    break;
                case USE_GESTURE:
                    BySelector scrollElementSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
                    app =
                            getSpectatioUiUtil()
                                    .scrollAndFindUiObject(
                                            scrollElementSelector,
                                            appName,
                                            (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through App Grid. Unknown Scroll Action %s.",
                                    scrollAction));
            }
            validateUiObject(app, String.format("Given app %s", appName));
            getSpectatioUiUtil().clickAndWait(app);
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException(String.format("App %s cannot be found", appName));
        }
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
        boolean scrollUp = false;
        try {
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ACTION));
            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector backwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_GRID_SCROLL_BACKWARD_BUTTON);
                    scrollUp = getSpectatioUiUtil().scrollUsingButton(backwardButtonSelector);
                    break;
                case USE_GESTURE:
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
                    BySelector scrollableElementSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
                    scrollUp =
                            getSpectatioUiUtil()
                                    .scrollBackward(
                                            scrollableElementSelector,
                                            (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through App Grid. Unknown Scroll Action %s.",
                                    scrollAction));
            }
            return scrollUp;
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException(String.format("Scrolling did not work."));
        }
    }

    @Override
    public boolean scrollDownOnePage() {
        boolean scrollDown = false;
        try {
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(AutomotiveConfigConstants.APP_LIST_SCROLL_ACTION));
            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector forwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_GRID_SCROLL_FORWARD_BUTTON);
                    scrollDown = getSpectatioUiUtil().scrollUsingButton(forwardButtonSelector);
                    break;
                case USE_GESTURE:
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants.APP_LIST_SCROLL_DIRECTION));
                    BySelector scrollableElementSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.APP_LIST_SCROLL_ELEMENT);
                    scrollDown =
                            getSpectatioUiUtil()
                                    .scrollForward(
                                            scrollableElementSelector,
                                            (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through App Grid. Unknown Scroll Action %s.",
                                    scrollAction));
            }
            return scrollDown;
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException(String.format("Scrolling did not work."));
        }
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }

    private enum ScrollActions {
        USE_BUTTON,
        USE_GESTURE;
    }

    private enum ScrollDirection {
        VERTICAL,
        HORIZONTAL;
    }
}
