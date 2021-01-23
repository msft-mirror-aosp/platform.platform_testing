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
import android.content.Context;
import android.platform.helpers.exceptions.UnknownUiException;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;

import java.util.regex.Pattern;

public class DialHelperImpl extends AbstractAutoStandardAppHelper implements IAutoDialHelper {

    private static final String LOG_TAG = DialHelperImpl.class.getSimpleName();
    private static final String DIAL_APP_PACKAGE = "com.android.car.dialer";
    private static final String CALL_LISTVIEW_CLASSNAME = android.widget.TextView.class.getName();
    private static final String PHONE = "Phone";
    private static final String CAR_SPEAKERS = "Car speakers";
    private static final String FIRST_NAME = "First name";
    private static final String LAST_NAME = "Last name";

    private static final int UI_RESPONSE_WAIT_MS = 10000;

    private static final BySelector MENU_DIAL_A_NUMBER =
            By.clickable(true).hasDescendant(By.text("Dialpad"));
    private static final BySelector MENU_CALL_HISTORY =
            By.clickable(true).hasDescendant(By.text("Recents"));
    private static final BySelector MENU_CONTACT =
            By.clickable(true).hasDescendant(By.text("Contacts"));
    private static final BySelector MENU_FAVORITES =
            By.clickable(true)
                    .hasDescendant(
                            By.text(Pattern.compile("favo.?rite.?", Pattern.CASE_INSENSITIVE)));
    private static final BySelector CALL_TIME = By.text("00:00");

    private static final BySelector CALL_BUTTON = By.res(DIAL_APP_PACKAGE, "call_button");
    private static final BySelector END_BUTTON = By.res(DIAL_APP_PACKAGE, "end_call_button");
    private static final BySelector DELETE_BUTTON = By.res(DIAL_APP_PACKAGE, "delete_button");
    private static final BySelector DIAL_CONTACT = By.res(DIAL_APP_PACKAGE, "user_profile_title");
    private static final BySelector DIAL_CONTACT_IN_DIALPAD = By.res(DIAL_APP_PACKAGE, "title");
    private static final BySelector IN_CALL_DIALPAD =
            By.res(DIAL_APP_PACKAGE, "toggle_dialpad_button");
    private static final BySelector MUTE_BUTTON = By.res(DIAL_APP_PACKAGE, "mute_button");
    private static final BySelector DIAL_PAD = By.res(DIAL_APP_PACKAGE, "dialpad_fragment");
    private static final BySelector CALL_HISTORY = By.res(DIAL_APP_PACKAGE, "call_action_id");
    private static final BySelector VOICE_CHANNEL_BUTTON =
            By.res(DIAL_APP_PACKAGE, "voice_channel_view");
    private static final BySelector CONTACT_NAME_IN_DIALER =
            By.res(DIAL_APP_PACKAGE, "user_profile_title");
    private static final BySelector CONTACT_TYPE =
            By.res(DIAL_APP_PACKAGE, "user_profile_phone_number");
    private static final BySelector CONTACT_LIST =
            By.res(DIAL_APP_PACKAGE, "list_view").scrollable(true);
    private static final BySelector PAGE_UP_BUTTON =
            By.res(DIAL_APP_PACKAGE, "car_ui_scrollbar_page_up");
    private static final BySelector PAGE_DOWN_BUTTON =
            By.res(DIAL_APP_PACKAGE, "car_ui_scrollbar_page_down");
    private static final BySelector CONTACT_NAME_IN_CONTACTS = By.res(DIAL_APP_PACKAGE, "title");
    private static final BySelector DIALER_SEARCH_BUTTON =
            By.res(DIAL_APP_PACKAGE, "menu_item_search");
    private static final BySelector DIALER_SETTINGS_BUTTON =
            By.res(DIAL_APP_PACKAGE, "menu_item_setting");
    private static final BySelector BACK_BUTTON =
            By.res(DIAL_APP_PACKAGE, "car_ui_toolbar_nav_icon_container");
    private static final BySelector CONTACT_ORDER_BUTTON =
            By.clickable(true).hasDescendant(By.text("Contact order"));
    private static final BySelector SEARCH_CONTACTS_BOX =
            By.res(DIAL_APP_PACKAGE, "car_ui_toolbar_search_bar");
    private static final BySelector SEARCH_RESULT = By.res(DIAL_APP_PACKAGE, "contact_name");

    private static final String PHONE_LAUNCH_COMMAND =
            "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "
                    + "-n com.android.car.dialer/.ui.TelecomActivity";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    public DialHelperImpl(Instrumentation instr) {
        super(instr);
        mBluetoothManager =
                (BluetoothManager)
                        mInstrumentation.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    /** {@inheritDoc} */
    public void open() {
        mDevice.pressHome();
        mDevice.waitForIdle();
        executeShellCommand(PHONE_LAUNCH_COMMAND);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return DIAL_APP_PACKAGE;
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        return "Phone";
    }

    /** {@inheritDoc} */
    public void makeCall() {
        UiObject2 dialButton = waitUntilFindUiObject(CALL_BUTTON);
        if (dialButton != null) {
            clickAndWaitForGone(dialButton, CALL_BUTTON);
        } else {
            throw new UnknownUiException("Unable to find call button.");
        }
        mDevice.waitForWindowUpdate(DIAL_APP_PACKAGE, UI_RESPONSE_WAIT_MS);
        mDevice.wait(Until.findObject(CALL_TIME), UI_RESPONSE_WAIT_MS);
    }

    /** {@inheritDoc} */
    public void endCall() {
        UiObject2 endButton = waitUntilFindUiObject(END_BUTTON);
        if (endButton != null) {
            clickAndWaitForGone(endButton, END_BUTTON);
        } else {
            throw new UnknownUiException("Unable to find end call button.");
        }
        mDevice.waitForWindowUpdate(DIAL_APP_PACKAGE, UI_RESPONSE_WAIT_MS);
    }

    /** {@inheritDoc} */
    public void dialANumber(String phoneNumber) {
        enterNumber(phoneNumber);
        mDevice.waitForIdle();
    }

    /** {@inheritDoc} */
    public void openCallHistory() {
        UiObject2 historyMenuButton = waitUntilFindUiObject(MENU_CALL_HISTORY);
        if (historyMenuButton != null) {
            clickAndWaitForIdle(historyMenuButton);
        } else {
            throw new UnknownUiException("Unable to find call history menu.");
        }
    }

    /** {@inheritDoc} */
    public void callContact(String contactName) {
        openContacts();
        dialFromList(contactName);
        mDevice.wait(Until.findObject(CALL_TIME), UI_RESPONSE_WAIT_MS);
    }

    /** {@inheritDoc} */
    public String getRecentCallHistory() {
        UiObject2 recentCallHistory = getCallHistory();
        if (recentCallHistory != null) {
            return recentCallHistory.getText();
        } else {
            throw new UnknownUiException("Unable to find history");
        }
    }

    /** {@inheritDoc} */
    public void callMostRecentHistory() {
        UiObject2 recentCallHistory = getCallHistory();
        clickAndWaitForIdle(recentCallHistory);
    }

    /** {@inheritDoc} */
    public void deleteDialedNumber() {
        String phoneNumber = getDialInNumber();
        UiObject2 deleteButton = waitUntilFindUiObject(DELETE_BUTTON);
        for (int index = 0; index < phoneNumber.length(); ++index) {
            if (deleteButton != null) {
                clickAndWaitForIdle(deleteButton);
            } else {
                throw new UnknownUiException("Unable to find delete button");
            }
        }
    }

    /** {@inheritDoc} */
    public String getDialInNumber() {
        UiObject2 dialInNumber = waitUntilFindUiObject(DIAL_CONTACT_IN_DIALPAD);
        String phoneNumber = dialInNumber.getText();
        return phoneNumber;
    }

    /** {@inheritDoc} */
    public String getDialedNumber() {
        UiObject2 dialedNumber = waitUntilFindUiObject(DIAL_CONTACT);
        String phoneNumber = dialedNumber.getText();
        return phoneNumber;
    }

    /** {@inheritDoc} */
    public String getDialedContactName() {
        UiObject2 dialedContactName = waitUntilFindUiObject(DIAL_CONTACT);
        String callerName = dialedContactName.getText();
        return callerName;
    }

    /** {@inheritDoc} */
    public void inCallDialPad(String phoneNumber) {
        UiObject2 dialPad = waitUntilFindUiObject(IN_CALL_DIALPAD);
        if (dialPad != null) {
            clickAndWaitForIdle(dialPad);
        } else {
            throw new UnknownUiException("Unable to find in-call dial pad");
        }
        enterNumber(phoneNumber);
        mDevice.waitForIdle();
    }

    /** {@inheritDoc} */
    public void muteCall() {
        UiObject2 muteButton = waitUntilFindUiObject(MUTE_BUTTON);
        if (muteButton != null) {
            clickAndWaitForIdle(muteButton);
        } else {
            throw new UnknownUiException("Unable to find mute call button.");
        }
    }

    /** {@inheritDoc} */
    public void unmuteCall() {
        UiObject2 muteButton = waitUntilFindUiObject(MUTE_BUTTON);
        if (muteButton != null) {
            clickAndWaitForIdle(muteButton);
        } else {
            throw new UnknownUiException("Unable to find unmute call button.");
        }
    }

    /** {@inheritDoc} */
    public void dialFromList(String contact) {
        UiObject2 contactToCall = waitUntilFindUiObject(By.text(contact));
        if (contactToCall != null) {
            clickAndWaitForIdle(contactToCall);
        } else {
            scrollThroughCallList(contact);
        }
    }

    /** {@inheritDoc} */
    public void changeAudioSource(AudioSource source) {
        UiObject2 voiceChannelButton = waitUntilFindUiObject(VOICE_CHANNEL_BUTTON);
        clickAndWaitForIdle(voiceChannelButton);
        String channelString = CAR_SPEAKERS;
        if (source == AudioSource.PHONE) {
            channelString = PHONE;
        }
        UiObject2 channelButton = waitUntilFindUiObject(By.text(channelString));
        clickAndWaitForIdle(channelButton);
    }

    /** {@inheritDoc} */
    public String getContactName() {
        UiObject2 contactName = waitUntilFindUiObject(CONTACT_NAME_IN_DIALER);
        if (contactName != null) {
            return contactName.getText();
        } else {
            throw new UnknownUiException("Unable to find contact name.");
        }
    }

    /** {@inheritDoc} */
    public String getContactType() {
        UiObject2 contactDetail = waitUntilFindUiObject(CONTACT_TYPE);
        if (contactDetail != null) {
            return contactDetail.getText();
        } else {
            throw new UnknownUiException("Unable to find contact details.");
        }
    }

    /** {@inheritDoc} */
    public void searchContactsByName(String contact) {
        openSearchContact();
        UiObject2 searchBox = waitUntilFindUiObject(SEARCH_CONTACTS_BOX);
        if (searchBox != null) {
            searchBox.setText(contact);
        } else {
            throw new UnknownUiException("Unable to find the search box.");
        }
    }

    /** {@inheritDoc} */
    public void searchContactsByNumber(String number) {
        openSearchContact();
        UiObject2 searchBox = waitUntilFindUiObject(SEARCH_CONTACTS_BOX);
        if (searchBox != null) {
            searchBox.setText(number);
        } else {
            throw new UnknownUiException("Unable to find the search box.");
        }
    }

    /** {@inheritDoc} */
    public String getFirstSearchResult() {
        UiObject2 searchResult = waitUntilFindUiObject(SEARCH_RESULT);
        String result;
        if (searchResult != null) {
            result = searchResult.getText();
        } else {
            throw new UnknownUiException("Unable to find the search result");
        }
        UiObject2 backButton = waitUntilFindUiObject(BACK_BUTTON);
        if (backButton != null) {
            clickAndWaitForIdle(backButton);
        } else {
            throw new UnknownUiException("Unable to find back button");
        }
        return result;
    }

    /** {@inheritDoc} */
    public void sortContactListBy(OrderType orderType) {
        openContacts();
        openSettings();
        UiObject2 contactOrderButton = waitUntilFindUiObject(CONTACT_ORDER_BUTTON);
        clickAndWaitForIdle(contactOrderButton);
        String order;
        if (orderType == OrderType.FIRST_NAME) {
            order = FIRST_NAME;
        } else {
            order = LAST_NAME;
        }
        UiObject2 orderButton = waitUntilFindUiObject(By.text(order));
        if (orderButton != null) {
            clickAndWaitForIdle(orderButton);
        } else {
            throw new UnknownUiException("Unable to find dialer settings button");
        }
        UiObject2 contactsMenu = waitUntilFindUiObject(MENU_CONTACT);
        while (contactsMenu == null) {
            UiObject2 backButton = waitUntilFindUiObject(BACK_BUTTON);
            clickAndWaitForIdle(backButton);
            contactsMenu = waitUntilFindUiObject(MENU_CONTACT);
        }
    }

    /** {@inheritDoc} */
    public String getFirstContactFromContactList() {
        openContacts();
        UiObject2 scrollObject = waitUntilFindUiObject(CONTACT_LIST);
        // Contacts has more than one page
        if (scrollObject != null) {
            UiObject2 pageUpButton = waitUntilFindUiObject(PAGE_UP_BUTTON);
            // go back to the top of the contacts page
            while (pageUpButton.isEnabled()) {
                pageUpButton.click();
                pageUpButton = waitUntilFindUiObject(PAGE_UP_BUTTON);
            }
        }
        UiObject2 firstContact = waitUntilFindUiObject(CONTACT_NAME_IN_CONTACTS);
        if (firstContact != null) {
            return firstContact.getText();
        } else {
            throw new UnknownUiException("Unable to find first contact from contact list");
        }
    }

    /** {@inheritDoc} */
    public boolean isContactInFavorites(String contact) {
        openFavorites();
        UiObject2 obj = waitUntilFindUiObject(By.text(contact));
        return obj != null;
    }

    /** {@inheritDoc} */
    public void openDetailsPage(String contactName) {
        openContacts();
        boolean isContactFound = false;
        UiObject2 scrollObject = waitUntilFindUiObject(CONTACT_LIST);
        if (scrollObject != null) {
            UiObject2 pageUpButton = waitUntilFindUiObject(PAGE_UP_BUTTON);
            // go back to the top of the contacts page
            while (pageUpButton.isEnabled()) {
                pageUpButton.click();
                pageUpButton = waitUntilFindUiObject(PAGE_UP_BUTTON);
            }
            UiObject2 pageDownButton = waitUntilFindUiObject(PAGE_DOWN_BUTTON);
            while (pageDownButton.isEnabled()) {
                UiObject2 contact = waitUntilFindUiObject(By.text(contactName));
                if (contact != null) {
                    UiObject2 contactDetailButton = contact.getParent().getChildren().get(4);
                    clickAndWaitForIdle(contactDetailButton);
                    isContactFound = true;
                    break;
                }
                pageDownButton.click();
            }
        }
        if (!isContactFound) {
            UiObject2 contact = waitUntilFindUiObject(By.text(contactName));
            if (contact != null) {
                UiObject2 contactDetailButton = contact.getParent().getChildren().get(4);
                clickAndWaitForIdle(contactDetailButton);
            } else {
                throw new UnknownUiException(
                        String.format("Unable to find contact name %s.", contactName));
            }
        }
    }

    /** This method is used to get the first history in the Recents tab. */
    private UiObject2 getCallHistory() {
        UiObject2 callHistory = waitUntilFindUiObject(CALL_HISTORY).getParent();
        UiObject2 recentCallHistory = callHistory.getChildren().get(2);
        return recentCallHistory;
    }

    /** This method is used to open the contacts menu */
    private void openContacts() {
        UiObject2 contactMenuButton = waitUntilFindUiObject(MENU_CONTACT);
        if (contactMenuButton != null) {
            clickAndWaitForIdle(contactMenuButton);
        } else {
            throw new UnknownUiException("Unable to find Contacts menu.");
        }
        UiObject2 pageUpButton = waitUntilFindUiObject(PAGE_UP_BUTTON);
        while (pageUpButton != null && pageUpButton.isEnabled()) {
            pageUpButton.click();
            pageUpButton = waitUntilFindUiObject(PAGE_UP_BUTTON);
        }
    }

    /** This method opens the contact search window. */
    private void openSearchContact() {
        UiObject2 searchContact =
                mDevice.wait(Until.findObject(DIALER_SEARCH_BUTTON), UI_RESPONSE_WAIT_MS);
        if (searchContact != null) {
            clickAndWaitForIdle(searchContact);
        } else {
            throw new UnknownUiException("Unable to find the search contact button.");
        }
    }
    /** This method opens the settings for contact. */
    private void openSettings() {
        UiObject2 settingButton =
                mDevice.wait(Until.findObject(DIALER_SETTINGS_BUTTON), UI_RESPONSE_WAIT_MS);
        if (settingButton != null) {
            clickAndWaitForIdle(settingButton);
        } else {
            throw new UnknownUiException("Unable to find dialer settings button");
        }
    }

    /** This method opens the Favorites tab. */
    private void openFavorites() {
        UiObject2 favoritesMenuButton = waitUntilFindUiObject(MENU_FAVORITES);
        if (favoritesMenuButton != null) {
            clickAndWaitForIdle(favoritesMenuButton);
        } else {
            throw new UnknownUiException("Unable to find Favorites menu.");
        }
    }

    public boolean isPhonePaired() {
        return mBluetoothAdapter.getBondedDevices().size() != 0;
    }

    /**
     * This method is used to scroll through the list (Favorites, Call History, Contact)
     *
     * <p>in search of the contact number or name and click if found
     *
     * @param contact contact number or name to be dialed
     */
    private void scrollThroughCallList(String contact) {
        try {
            UiScrollable callList = new UiScrollable(new UiSelector().scrollable(true));
            callList.setAsVerticalList();
            UiObject callListItem =
                    callList.getChildByText(
                            new UiSelector().className(CALL_LISTVIEW_CLASSNAME), contact);
            if (callListItem != null) {
                callListItem.clickAndWaitForNewWindow(UI_RESPONSE_WAIT_MS);
            }
        } catch (UiObjectNotFoundException exception) {
            throw new UnknownUiException(
                    "Unable to find provided contact in the call list " + contact);
        }
    }

    /**
     * This method is used to enter phonenumber from the on-screen numberpad
     *
     * @param phoneNumber number to be dialed
     */
    private void enterNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new UnknownUiException("No phone number provided");
        }
        mDevice.pressHome();
        mDevice.waitForIdle();
        executeShellCommand("am start -a android.intent.action.DIAL");
        UiObject2 dial_pad = waitUntilFindUiObject(DIAL_PAD);
        if (dial_pad == null) {
            throw new UnknownUiException("Unable to find dial pad");
        }
        char[] array = phoneNumber.toCharArray();
        for (char ch : array) {
            UiObject2 numberButton = waitUntilFindUiObject(By.text(Character.toString(ch)));
            if (numberButton == null) {
                throw new UnknownUiException("Unable to find number" + phoneNumber);
            }
            clickAndWaitForIdle(numberButton);
        }
    }

    /**
     * This method is used to click on an UiObject2 and wait until it is gone
     *
     * @param uiObject2 uiObject to be clicked
     * @param selector BySelector to be gone
     */
    private void clickAndWaitForGone(UiObject2 uiObject2, BySelector selector) {
        uiObject2.click();
        mDevice.wait(Until.gone(selector), UI_RESPONSE_WAIT_MS);
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
