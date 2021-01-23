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
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

public class ContactDetailsHelperImpl extends AbstractAutoStandardAppHelper
        implements IAutoDialContactDetailsHelper {

    private static final String LOG_TAG = DialHelperImpl.class.getSimpleName();
    private static final String DIAL_APP_PACKAGE = "com.android.car.dialer";

    private static final int UI_RESPONSE_WAIT_MS = 10000;

    private static final BySelector MENU_CONTACT =
            By.clickable(true).hasDescendant(By.text("Contacts"));

    private static final BySelector CONTACT_LIST =
            By.res(DIAL_APP_PACKAGE, "list_view").scrollable(true);
    private static final BySelector PAGE_UP_BUTTON = By.res(DIAL_APP_PACKAGE, "page_up");
    private static final BySelector CONTACT_FAVORITE_BUTTON =
            By.res(DIAL_APP_PACKAGE, "contact_details_favorite_button");
    private static final BySelector CONTACT_NUMBER = By.res(DIAL_APP_PACKAGE, "call_action_id");
    private static final BySelector BACK_BUTTON =
            By.res(DIAL_APP_PACKAGE, "car_ui_toolbar_nav_icon_container");

    private static final String PHONE_LAUNCH_COMMAND =
            "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "
                    + "-n com.android.car.dialer/.ui.TelecomActivity";

    public ContactDetailsHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return DIAL_APP_PACKAGE;
    }

    /** {@inheritDoc} */
    public void open() {
        mDevice.pressHome();
        mDevice.waitForIdle();
        executeShellCommand(PHONE_LAUNCH_COMMAND);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        return "Phone";
    }

    /** {@inheritDoc} */
    public void addRemoveFavoriteContact() {
        UiObject2 favoriteButton = waitUntilFindUiObject(CONTACT_FAVORITE_BUTTON);
        if (favoriteButton != null) {
            clickAndWaitForIdle(favoriteButton);
        } else {
            throw new UnknownUiException("Unable to find add favorite button");
        }
    }

    /** {@inheritDoc} */
    public void makeCallFromDetailsPageByType(ContactType contactType) {
        String numberType;
        switch (contactType) {
            case HOME:
                numberType = "Home";
                break;
            case WORK:
                numberType = "Work";
                break;
            case MOBILE:
                numberType = "Mobile";
                break;
            default:
                numberType = "Undefined";
        }
        UiObject2 number = waitUntilFindUiObject(By.text(numberType));
        if (number != null) {
            clickAndWaitForIdle(number.getParent());
        } else {
            throw new UnknownUiException("Unable to find number in contact details");
        }
    }

    /** {@inheritDoc} */
    public void closeDetailsPage() {
        UiObject2 backButton = waitUntilFindUiObject(BACK_BUTTON);
        if (backButton != null) {
            clickAndWaitForIdle(backButton);
        } else {
            throw new UnknownUiException("Unable to find back button");
        }
    }

    /**
     * This method is used to click on an UiObject2 and wait for device idle after click.
     *
     * @param uiObject UiObject2 to click.
     */
    private void clickAndWaitForIdle(UiObject2 uiObject2) {
        uiObject2.click();
        mDevice.waitForIdle();
    }

    /**
     * This method is used to find UiObject2 corresponding to the selector
     *
     * @param selector BySelector to be found
     * @return UiObject2 UiObject2 found for the corresponding selector
     */
    private UiObject2 waitUntilFindUiObject(BySelector selector) {
        UiObject2 uiObject2 = mDevice.wait(Until.findObject(selector), UI_RESPONSE_WAIT_MS);
        return uiObject2;
    }
}
