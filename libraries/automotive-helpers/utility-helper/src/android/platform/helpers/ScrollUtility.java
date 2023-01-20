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

import android.platform.spectatio.exceptions.MissingUiElementException;
import android.platform.spectatio.utils.SpectatioUiUtil;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

/** Utility file for scroll functions */
public class ScrollUtility {

    private static ScrollUtility sScrollUtilityInstance;
    private SpectatioUiUtil mSpectatioUiUtil;

    private ScrollUtility(SpectatioUiUtil spectatioUiUtil) {
        mSpectatioUiUtil = spectatioUiUtil;
    }

    public enum ScrollActions {
        USE_BUTTON,
        USE_GESTURE;
    }

    public enum ScrollDirection {
        VERTICAL,
        HORIZONTAL;
    }

    /** Get ScrollUtility Instance */
    public static ScrollUtility getInstance(SpectatioUiUtil spectatioUiUtil) {
        if (sScrollUtilityInstance == null) {
            sScrollUtilityInstance = new ScrollUtility(spectatioUiUtil);
        }
        return sScrollUtilityInstance;
    }

    /** Scroll using backward buttons or guesture on device screen */
    public boolean scrollBackward(
            ScrollActions scrollAction,
            ScrollDirection scrollDirection,
            BySelector backwardButtonSelector,
            BySelector scrollableElementSelector,
            String action) {
        boolean scrollResult = false;
        try {
            switch (scrollAction) {
                case USE_BUTTON:
                    scrollResult = mSpectatioUiUtil.scrollUsingButton(backwardButtonSelector);
                    break;
                case USE_GESTURE:
                    scrollResult =
                            mSpectatioUiUtil.scrollBackward(
                                    scrollableElementSelector,
                                    (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Unable to %s, unknown scroll action %s.",
                                    action, scrollAction));
            }
            return scrollResult;
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException(
                    String.format("Unable to %s. Error: %s", action, ex.getMessage()));
        }
    }

    /** Scroll using forward buttons or guesture on device screen */
    public boolean scrollForward(
            ScrollActions scrollAction,
            ScrollDirection scrollDirection,
            BySelector forwardButtonSelector,
            BySelector scrollableElementSelector,
            String action) {
        boolean scrollResult = false;
        try {
            switch (scrollAction) {
                case USE_BUTTON:
                    scrollResult = mSpectatioUiUtil.scrollUsingButton(forwardButtonSelector);
                    break;
                case USE_GESTURE:
                    scrollResult =
                            mSpectatioUiUtil.scrollForward(
                                    scrollableElementSelector,
                                    (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Unable to %s, unknown scroll action %s.",
                                    action, scrollAction));
            }
            return scrollResult;
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException(
                    String.format("Unable to %s. Error: %s", action, ex.getMessage()));
        }
    }

    /**
     * Scroll using forward and backward buttons or use guesture on device screen to find the object
     */
    public UiObject2 scrollAndFindUiObject(
            ScrollActions scrollAction,
            ScrollDirection scrollDirection,
            BySelector forwardButtonSelector,
            BySelector backwardButtonSelector,
            BySelector scrollElementSelector,
            BySelector target,
            String action) {
        UiObject2 scrollResult = null;
        try {
            switch (scrollAction) {
                case USE_BUTTON:
                    scrollResult =
                            mSpectatioUiUtil.scrollAndFindUiObject(
                                    forwardButtonSelector, backwardButtonSelector, target);
                    break;
                case USE_GESTURE:
                    scrollResult =
                            mSpectatioUiUtil.scrollAndFindUiObject(
                                    scrollElementSelector,
                                    target,
                                    (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Unable to %s, unknown scroll action %s.",
                                    action, scrollAction));
            }
            return scrollResult;
        } catch (MissingUiElementException ex) {
            throw new IllegalStateException(
                    String.format("Unable to %s. Error: %s", action, ex.getMessage()));
        }
    }
}
