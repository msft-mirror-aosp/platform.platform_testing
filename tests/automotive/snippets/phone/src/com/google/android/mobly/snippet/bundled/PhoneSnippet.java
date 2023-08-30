/*
 * Copyright (C) 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.mobly.snippet.bundled;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoCarSmsMessengerHelper;
import android.platform.helpers.IAutoDialContactDetailsHelper;
import android.platform.helpers.IAutoDialHelper;
import android.platform.helpers.IAutoPrivacySettingsHelper;
import android.platform.helpers.IAutoStatusBarHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

import java.util.List;

/** Snippet class for exposing Phone/Dial App APIs. */
public class PhoneSnippet implements Snippet {
    private final HelperAccessor<IAutoDialHelper> mDialerHelper;
    private final HelperAccessor<IAutoDialContactDetailsHelper> mContactsDetailsHelper;
    private final HelperAccessor<IAutoVehicleHardKeysHelper> mHardKeysHelper;
    private final HelperAccessor<IAutoPrivacySettingsHelper> mPrivacySettingsHelper;
    private final HelperAccessor<IAutoCarSmsMessengerHelper> mCarSmsMessengerHelper;
    private final HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;

    public PhoneSnippet() {
        mDialerHelper = new HelperAccessor<>(IAutoDialHelper.class);
        mContactsDetailsHelper = new HelperAccessor<>(IAutoDialContactDetailsHelper.class);
        mHardKeysHelper = new HelperAccessor<>(IAutoVehicleHardKeysHelper.class);
        mPrivacySettingsHelper = new HelperAccessor<>(IAutoPrivacySettingsHelper.class);
        mCarSmsMessengerHelper = new HelperAccessor<>(IAutoCarSmsMessengerHelper.class);
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Rpc(description = "Open Phone Application.")
    public void openPhoneApp() {
        mDialerHelper.get().open();
    }

    @Rpc(description = "Open Dial Pad and dial in a number using keypad.")
    public void dialANumber(String phoneNumber) {
        mDialerHelper.get().dialANumber(phoneNumber);
    }

    @Rpc(description = "Make a call.")
    public void makeCall() {
        mDialerHelper.get().makeCall();
    }

    @Rpc(description = "End the call.")
    public void endCall() {
        mDialerHelper.get().endCall();
    }

    @Rpc(description = "Press the hardkey for ending the call.")
    public void endCallWithHardkey() {
        mHardKeysHelper.get().pressEndCallKey();
    }

    @Rpc(description = "Open Call History.")
    public void openCallHistory() {
        mDialerHelper.get().openCallHistory();
    }

    @Rpc(description = "Call Contact From Contact List.")
    public void callContact(String contactName) {
        mDialerHelper.get().callContact(contactName);
    }

    @Rpc(description = "Delete the dialed number on Dial Pad.")
    public void deleteDialedNumber() {
        mDialerHelper.get().deleteDialedNumber();
    }

    @Rpc(description = "Get the dialed number when the call is in progress.")
    public String getDialedNumber() {
        return mDialerHelper.get().getDialedNumber();
    }

    @Rpc(description = "Get the entered on dial pad.")
    public String getDialInNumber() {
        return mDialerHelper.get().getDialInNumber();
    }

    @Rpc(description = "Get the contact name for dialed number when the call is in progress.")
    public String getDialedContactName() {
        return mDialerHelper.get().getDialedContactName();
    }

    @Rpc(description = "Get the recent entry from Call History.")
    public String getRecentCallHistory() {
        return mDialerHelper.get().getRecentCallHistory();
    }

    @Rpc(
            description =
                    "Call contact from list open in foreground e.g. Favorites, Recents, Contacts.")
    public void dialFromList(String contact) {
        mDialerHelper.get().dialFromList(contact);
    }

    @Rpc(description = "Dial a number in call dial pad when call is in progress.")
    public void inCallDialPad(String phoneNumber) {
        mDialerHelper.get().inCallDialPad(phoneNumber);
    }

    @Rpc(description = "Mute Call.")
    public void muteCall() {
        mDialerHelper.get().muteCall();
    }

    @Rpc(description = "Unmute Call.")
    public void unmuteCall() {
        mDialerHelper.get().unmuteCall();
    }

    @Rpc(description = "Ongoing Call on homescreen.")
    public boolean isOngoingCallDisplayedOnHome() {
        return mDialerHelper.get().isOngoingCallDisplayedOnHome();
    }

    @Rpc(description = "Open Phone from Home Screen card.")
    public void openPhoneAppFromHome() {
        mDialerHelper.get().openPhoneAppFromHome();
    }

    @Rpc(description = "Change audio source to Phone when the call is in progress.")
    public void changeAudioSourceToPhone() {
        mDialerHelper.get().changeAudioSource(IAutoDialHelper.AudioSource.PHONE);
    }

    @Rpc(description = "Change audio source to Car Speakers when the call is in progress.")
    public void changeAudioSourceToCarSpeakers() {
        mDialerHelper.get().changeAudioSource(IAutoDialHelper.AudioSource.CAR_SPEAKERS);
    }

    @Rpc(description = "Call Most Recent History.")
    public void callMostRecentHistory() {
        mDialerHelper.get().callMostRecentHistory();
    }

    @Rpc(description = "Get contact name while the call is in progress.")
    public String getContactName() {
        return mDialerHelper.get().getContactName();
    }

    @Rpc(description = "Get contact type (Work, Mobile, Home) while the call is in progress.")
    public String getContactType() {
        return mDialerHelper.get().getContactType();
    }

    @Rpc(description = "Search contact by name.")
    public void searchContactsByName(String contact) {
        mDialerHelper.get().searchContactsByName(contact);
    }

    @Rpc(description = "Search contact by number.")
    public void searchContactsByNumber(String number) {
        mDialerHelper.get().searchContactsByNumber(number);
    }

    @Rpc(description = "Get first search result.")
    public String getFirstSearchResult() {
        return mDialerHelper.get().getFirstSearchResult();
    }

    @Rpc(description = "Sort contact list by First Name.")
    public void sortContactListByFirstName() {
        mDialerHelper.get().sortContactListBy(IAutoDialHelper.OrderType.FIRST_NAME);
    }

    @Rpc(description = "Sort contact list by Last Name.")
    public void sortContactListByLastName() {
        mDialerHelper.get().sortContactListBy(IAutoDialHelper.OrderType.LAST_NAME);
    }

    @Rpc(description = "Get first contact from contacts list.")
    public String getFirstContactFromContactList() {
        return mDialerHelper.get().getFirstContactFromContactList();
    }

    @Rpc(description = "Check if given contact is in Favorites.")
    public boolean isContactInFavorites(String contact) {
        return mDialerHelper.get().isContactInFavorites(contact);
    }

    @Rpc(description = "Bluetooth HFP Error")
    public boolean isBluetoothHfpErrorDisplayed() {
        return mDialerHelper.get().isBluetoothHfpErrorDisplayed();
    }

    @Rpc(description = "Open details page for given contact.")
    public void openDetailsPage(String contact) {
        mDialerHelper.get().openDetailsPage(contact);
    }

    @Rpc(description = "Open Contacts List.")
    public void openContacts() {
        mDialerHelper.get().openContacts();
    }

    @Rpc(description = "Press 'Device' on a prompt, if present.")
    public void pressDevice() {
        mDialerHelper.get().pressDeviceOnPrompt();
    }

    @Rpc(description = "Add and remove contact ( contact details are open ) from favorites.")
    public void addRemoveFavoriteContact() {
        mContactsDetailsHelper.get().addRemoveFavoriteContact();
    }

    @Rpc(description = "Make call to number with type Work from contact details page.")
    public void makeCallFromDetailsPageByTypeWork() {
        mContactsDetailsHelper
                .get()
                .makeCallFromDetailsPageByType(IAutoDialContactDetailsHelper.ContactType.WORK);
    }

    @Rpc(description = "Make call to number with type Home from contact details page.")
    public void makeCallFromDetailsPageByTypeHome() {
        mContactsDetailsHelper
                .get()
                .makeCallFromDetailsPageByType(IAutoDialContactDetailsHelper.ContactType.HOME);
    }

    @Rpc(description = "Make call to number with type Mobile from contact details page.")
    public void makeCallFromDetailsPageByTypeMobile() {
        mContactsDetailsHelper
                .get()
                .makeCallFromDetailsPageByType(IAutoDialContactDetailsHelper.ContactType.MOBILE);
    }

    @Rpc(description = "Close contact details page.")
    public void closeDetailsPage() {
        mContactsDetailsHelper.get().closeDetailsPage();
    }

    @Rpc(description = "Get list of visible contacts")
    public List<String> getListOfAllContacts() {
        return mDialerHelper.get().getListOfAllVisibleContacts();
    }

    @Rpc(description = "Microphone Chip.")
    public boolean isMicChipPresentOnStatusBar() {
        return mPrivacySettingsHelper.get().isMicChipPresentOnStatusBar();
    }

    @Rpc(description = "Add Favorites from favorites tab")
    public void addFavoritesFromFavoritesTab(String contact) {
        mDialerHelper.get().addFavoritesFromFavoritesTab(contact);
    }

    @Rpc(description = "Open SMS Application.")
    public void openSmsApp() {
        mCarSmsMessengerHelper.get().open();
    }

    @Rpc(description = "Bluetooth SMS Error")
    public boolean isSmsBluetoothErrorDisplayed() {
        return mCarSmsMessengerHelper.get().isSmsBluetoothErrorDisplayed();
    }

    @Rpc(description = "Open Bluetooth Palette")
    public void openBluetoothPalette() {
        mStatusBarHelper.get().openBluetoothPalette();
    }

    @Rpc(description = "Click Bluetooth Button")
    public void clickBluetoothButton() {
        mStatusBarHelper.get().clickBluetoothButton();
    }

    @Rpc(description = "is Bluetooth Connected")
    public boolean isBluetoothConnected() {
        return mStatusBarHelper.get().isBluetoothConnected();
    }

    @Rpc(description = "Verify Bluetooth")
    public boolean verifyBluetooth() {
        return mStatusBarHelper.get().verifyBluetooth();
    }

    @Rpc(description = "Verify Phone")
    public boolean verifyPhone() {
        return mStatusBarHelper.get().verifyPhone();
    }

    @Rpc(description = "Verify Media")
    public boolean verifyMedia() {
        return mStatusBarHelper.get().verifyMedia();
    }

    @Rpc(description = "Verify Device Name")
    public boolean verifyDeviceName() {
        return mStatusBarHelper.get().verifyDeviceName();
    }

    @Rpc(description = "Verify Disabled Bluetooth Profile")
    public boolean verifyDisabledBluetoothProfile() {
        return mStatusBarHelper.get().verifyDisabledBluetoothProfile();
    }

    @Rpc(description = "Verify Disabled Phone Profile")
    public boolean verifyDisabledPhoneProfile() {
        return mStatusBarHelper.get().verifyDisabledPhoneProfile();
    }

    @Rpc(description = "Verify Disabled Media Profile")
    public boolean verifyDisabledMediaProfile() {
        return mStatusBarHelper.get().verifyDisabledMediaProfile();
    }

    @Override
    public void shutdown() {}
}
