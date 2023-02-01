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
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;

public class ContactDetailsHelperImpl extends AbstractStandardAppHelper
        implements IAutoDialContactDetailsHelper {
    private static final String LOG_TAG = DialHelperImpl.class.getSimpleName();

    private static enum ScrollActions {
        USE_BUTTON,
        USE_GESTURE;
    }

    private static enum ScrollDirection {
        VERTICAL,
        HORIZONTAL;
    }

    public ContactDetailsHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.DIAL_PACKAGE);
    }

    /** {@inheritDoc} */
    public void open() {
        getSpectatioUiUtil().pressHome();
        getSpectatioUiUtil().wait1Second();
        getSpectatioUiUtil()
                .executeShellCommand(
                        getCommandFromConfig(
                                AutomotiveConfigConstants.OPEN_PHONE_ACTIVITY_COMMAND));
        getSpectatioUiUtil().wait1Second();
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    public void addRemoveFavoriteContact() {
        BySelector favoriteButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.ADD_CONTACT_TO_FAVORITE);
        UiObject2 favoriteButton = getSpectatioUiUtil().findUiObject(favoriteButtonSelector);
        validateUiObject(favoriteButton, AutomotiveConfigConstants.ADD_CONTACT_TO_FAVORITE);
        getSpectatioUiUtil().clickAndWait(favoriteButton);
    }

    private UiObject2 getNumberByContactType(String type) {
        try {
            ScrollActions scrollAction =
                    ScrollActions.valueOf(
                            getActionFromConfig(
                                    AutomotiveConfigConstants.CONTACT_DETAILS_SCROLL_ACTION));
            UiObject2 contactByType = null;
            switch (scrollAction) {
                case USE_BUTTON:
                    BySelector forwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.CONTACT_DETAILS_SCROLL_FORWARD);
                    BySelector backwardButtonSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.CONTACT_DETAILS_SCROLL_BACKWARD);
                    contactByType =
                            getSpectatioUiUtil()
                                    .scrollAndFindUiObject(
                                            forwardButtonSelector,
                                            backwardButtonSelector,
                                            getUiElementFromConfig(type));
                    break;
                case USE_GESTURE:
                    BySelector scrollElementSelector =
                            getUiElementFromConfig(
                                    AutomotiveConfigConstants.CONTACT_DETAILS_SCROLL_ELEMENT);
                    ScrollDirection scrollDirection =
                            ScrollDirection.valueOf(
                                    getActionFromConfig(
                                            AutomotiveConfigConstants
                                                    .CONTACT_DETAILS_SCROLL_DIRECTION));
                    contactByType =
                            getSpectatioUiUtil()
                                    .scrollAndFindUiObject(
                                            scrollElementSelector,
                                            getUiElementFromConfig(type),
                                            (scrollDirection == ScrollDirection.VERTICAL));
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Cannot scroll through contact details. Unknown Scroll Action"
                                            + " %s.",
                                    scrollAction));
            }
            validateUiObject(contactByType, String.format("Given type %s", type));
            return contactByType;
        } catch (MissingUiElementException ex) {
            throw new RuntimeException("Unable to scroll through contact details.", ex);
        }
    }

    /** {@inheritDoc} */
    public void makeCallFromDetailsPageByType(ContactType contactType) {
        UiObject2 number = null;
        switch (contactType) {
            case HOME:
                number = getNumberByContactType(AutomotiveConfigConstants.CONTACT_TYPE_HOME);
                break;
            case WORK:
                number = getNumberByContactType(AutomotiveConfigConstants.CONTACT_TYPE_WORK);
                break;
            case MOBILE:
                number = getNumberByContactType(AutomotiveConfigConstants.CONTACT_TYPE_MOBILE);
                break;
            default:
                number = getSpectatioUiUtil().findUiObject("undefined");
        }
        validateUiObject(number, String.format("Contact Type %s", contactType));
        getSpectatioUiUtil().clickAndWait(number);
    }

    /** {@inheritDoc} */
    public void closeDetailsPage() {
        // count is used to avoid infinite loop in case someone invokes
        // after exiting settings application
        int count = 5;
        while (count > 0
                && getSpectatioUiUtil()
                                .findUiObject(
                                        getUiElementFromConfig(
                                                AutomotiveConfigConstants.DIAL_PAD_MENU))
                        == null) {
            getSpectatioUiUtil().pressBack();
            getSpectatioUiUtil().wait5Seconds();
            count--;
        }
    }

    private void validateUiObject(UiObject2 uiObject, String action) {
        if (uiObject == null) {
            throw new UnknownUiException(
                    String.format("Unable to find UI Element for %s.", action));
        }
    }
}
