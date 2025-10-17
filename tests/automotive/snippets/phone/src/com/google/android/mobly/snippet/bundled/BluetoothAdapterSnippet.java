/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.mobly.snippet.bundled;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.bundled.utils.Utils;
import com.google.android.mobly.snippet.rpc.Rpc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class BluetoothAdapterSnippet implements Snippet {

    private static class BluetoothAdapterSnippetException extends Exception {

        BluetoothAdapterSnippetException(String msg) {
            super(msg);
        }

        BluetoothAdapterSnippetException(String msg, Throwable err) {
            super(msg, err);
        }
    }

    // Timeout to measure consistent BT state.
    private static final int BT_MATCHING_STATE_INTERVAL_SEC = 5;
    // Default timeout in seconds.
    private static final int TIMEOUT_TOGGLE_STATE_SEC = 30;
    // Default timeout in seconds for UI update.
    private static final int TIMEOUT_UI_UPDATE_SEC = 8;
    // Timeout in seconds for Bluetooth profile connections.
    private static final int PROFILE_CONNECTION_TIMEOUT_SEC = 15;
    private final Context mContext;
    private static final BluetoothAdapter sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final Pattern TEXT_PATTERN_ALLOW =
            Pattern.compile("allow", Pattern.CASE_INSENSITIVE);

    static final String BLUETOOTH_A2DP_SINK_ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED";
    static final String BLUETOOTH_HEADSET_CLIENT_ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED";
    static final String BLUETOOTH_MAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED";
    static final String BLUETOOTH_PBAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED";
    static final String BLUETOOTH_PAN_ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    static final String A2DP_SINK = "A2DP_SINK";
    static final String HEADSET_CLIENT = "HEADSET_CLIENT";
    static final String MAP_CLIENT = "MAP_CLIENT";
    static final String PBAP_CLIENT = "PBAP_CLIENT";
    static final String PAN = "PAN";
    private static final Set<String> ALL_PROFILES =
            new HashSet<>(Arrays.asList(A2DP_SINK, HEADSET_CLIENT, MAP_CLIENT, PBAP_CLIENT, PAN));
    Map<String, Long> mConnectedProfiles = new HashMap<>();
    Map<String, Long> mDisconnectedProfiles = new HashMap<>();

    public BluetoothAdapterSnippet() throws Throwable {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        Utils.adaptShellPermissionIfRequired(mContext);
    }

    /* Gets the UiDevice instance for UI operations. */
    private static UiDevice getUiDevice() throws BluetoothAdapterSnippetException {
        try {
            return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        } catch (IllegalStateException e) {
            throw new BluetoothAdapterSnippetException(
                    "Failed to get UiDevice. Please ensure that "
                            + "no other UiAutomation service is running.",
                    e);
        }
    }

    /**
     * Waits until the bluetooth adapter state has stabilized. We consider BT state stabilized if it
     * hasn't changed within 5 sec.
     */
    private static void waitForStableBtState() throws BluetoothAdapterSnippetException {
        long timeoutMs = System.currentTimeMillis() + TIMEOUT_TOGGLE_STATE_SEC * 1000;
        long continuousStateIntervalMs =
                System.currentTimeMillis() + BT_MATCHING_STATE_INTERVAL_SEC * 1000;
        int prevState = sBluetoothAdapter.getState();
        while (System.currentTimeMillis() < timeoutMs) {
            // Delay.
            Utils.waitUntil(() -> false, /* timeout= */ 1);

            int currentState = sBluetoothAdapter.getState();
            if (currentState != prevState) {
                continuousStateIntervalMs =
                        System.currentTimeMillis() + BT_MATCHING_STATE_INTERVAL_SEC * 1000;
            }
            if (continuousStateIntervalMs <= System.currentTimeMillis()) {
                return;
            }
            prevState = currentState;
        }
        throw new BluetoothAdapterSnippetException(
                String.format(
                        "Failed to reach a stable Bluetooth state within %d s",
                        TIMEOUT_TOGGLE_STATE_SEC));
    }

    /**
     * Enable bluetooth with larger tolerance on UI update timeout than btEnable from
     * https://github.com/google/mobly-bundled-snippets/blob/master/src/main/java/com/google/android/mobly/snippet/bundled/bluetooth/BluetoothAdapterSnippet.java.
     */
    @Rpc(description = "Enable bluetooth with a 30s timeout.")
    public void btEnableWithLongerWait()
            throws BluetoothAdapterSnippetException, InterruptedException {
        if (sBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            return;
        }
        waitForStableBtState();

        if (Build.VERSION.SDK_INT >= 33) {
            // BluetoothAdapter#enable is removed from public SDK for 33 and above, so uses an
            // intent instead.
            UiDevice uiDevice = getUiDevice();
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Triggers the system UI popup to ask for explicit permission.
            mContext.startActivity(enableIntent);
            // Clicks the "ALLOW" button.
            BySelector allowButtonSelector = By.text(TEXT_PATTERN_ALLOW).clickable(true);
            if (!Utils.waitUntil(
                    () -> uiDevice.findObject(allowButtonSelector) != null,
                    TIMEOUT_UI_UPDATE_SEC)) {
                throw new BluetoothAdapterSnippetException(
                        String.format(
                                "Bluetooth permission request dialog did not show up within %ss.",
                                TIMEOUT_UI_UPDATE_SEC));
            }
            uiDevice.findObject(allowButtonSelector).click();
        } else if (!sBluetoothAdapter.enable()) {
            throw new BluetoothAdapterSnippetException("Failed to start enabling bluetooth.");
        }
        if (!Utils.waitUntil(
                () -> sBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON,
                TIMEOUT_TOGGLE_STATE_SEC)) {
            throw new BluetoothAdapterSnippetException(
                    String.format(
                            "Bluetooth did not turn on within %ss.", TIMEOUT_TOGGLE_STATE_SEC));
        }
    }

    /**
     * Become discoverable in bluetooth with larger tolerance on UI update timeout than
     * btBecomeDiscoverable from
     * https://github.com/google/mobly-bundled-snippets/blob/master/src/main/java/com/google/android/mobly/snippet/bundled/bluetooth/BluetoothAdapterSnippet.java.
     */
    @Rpc(description = "Become discoverable in Bluetooth.")
    public void btBecomeDiscoverableWithLongerWait(Integer duration) throws Throwable {
        if (!sBluetoothAdapter.isEnabled()) {
            throw new BluetoothAdapterSnippetException(
                    "Bluetooth is not enabled, cannot become discoverable.");
        }
        if (Build.VERSION.SDK_INT >= 31) {
            // BluetoothAdapter#setScanMode is removed from public SDK for 31 and above, so uses an
            // intent instead.
            UiDevice uiDevice = getUiDevice();
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
            // Triggers the system UI popup to ask for explicit permission.
            mContext.startActivity(discoverableIntent);
            // Clicks the "ALLOW" button.
            BySelector allowButtonSelector = By.text(TEXT_PATTERN_ALLOW).clickable(true);
            if (!Utils.waitUntil(
                    () -> uiDevice.findObject(allowButtonSelector) != null,
                    TIMEOUT_UI_UPDATE_SEC)) {
                throw new BluetoothAdapterSnippetException(
                        String.format(
                                "Bluetooth permission request dialog did not show up within %ss.",
                                TIMEOUT_UI_UPDATE_SEC));
            }
            uiDevice.findObject(allowButtonSelector).click();
        } else if (Build.VERSION.SDK_INT >= 30) {
            if (!(boolean)
                    Utils.invokeByReflection(
                            sBluetoothAdapter,
                            "setScanMode",
                            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
                            (long) duration * 1000)) {
                throw new BluetoothAdapterSnippetException("Failed to become discoverable.");
            }
        } else {
            if (!(boolean)
                    Utils.invokeByReflection(
                            sBluetoothAdapter,
                            "setScanMode",
                            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
                            duration)) {
                throw new BluetoothAdapterSnippetException("Failed to become discoverable.");
            }
        }
    }

    /** Grant Bluetooth profiles permissions for phone book, call log, message access. */
    @Rpc(description = "Grant Bluetooth profiles permissions.")
    public void btGrantPermissions(String deviceAddress) throws Throwable {
        BluetoothDevice device =
                com.google.android.mobly.snippet.bundled.bluetooth.BluetoothAdapterSnippet
                        .getKnownDeviceByAddress(deviceAddress);
        Utils.invokeByReflection(device, "setPhonebookAccessPermission", 1);
        Utils.invokeByReflection(device, "setMessageAccessPermission", 1);
    }

    /** Connect Bluetooth profiles. */
    @Rpc(description = "Connect Bluetooth profiles.")
    public void btConnectProfiles(String deviceAddress) throws Throwable {
        BluetoothDevice device =
                com.google.android.mobly.snippet.bundled.bluetooth.BluetoothAdapterSnippet
                        .getKnownDeviceByAddress(deviceAddress);
        mConnectedProfiles = new HashMap<>();
        ProfileConnectionBroadcastReceiver receiver =
                new ProfileConnectionBroadcastReceiver(mContext);
        IntentFilter a2dpSinkFilter =
                new IntentFilter(BLUETOOTH_A2DP_SINK_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter headsetClientFilter =
                new IntentFilter(BLUETOOTH_HEADSET_CLIENT_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter mapClientFilter =
                new IntentFilter(BLUETOOTH_MAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter pbapClientFilter =
                new IntentFilter(BLUETOOTH_PBAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter panFilter = new IntentFilter(BLUETOOTH_PAN_ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, a2dpSinkFilter);
        mContext.registerReceiver(receiver, headsetClientFilter);
        mContext.registerReceiver(receiver, mapClientFilter);
        mContext.registerReceiver(receiver, pbapClientFilter);
        mContext.registerReceiver(receiver, panFilter);
        try {
            int result = (int) Utils.invokeByReflection(device, "connect");
            if (result == BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED) {
                throw new BluetoothAdapterSnippetException(
                        "Failed to initiate the profile connection process to device: "
                                + deviceAddress);
            }
            if (!Utils.waitUntil(
                    () -> mConnectedProfiles.size() == ALL_PROFILES.size(),
                    PROFILE_CONNECTION_TIMEOUT_SEC)) {
                String connectedProfilesAndDelay = "";
                for (Map.Entry<String, Long> entry : mConnectedProfiles.entrySet()) {
                    String profile = entry.getKey();
                    Long delay = entry.getValue();
                    connectedProfilesAndDelay += (profile + ": " + delay + " ms\n");
                }
                throw new BluetoothAdapterSnippetException(
                        "Failed to connect all required bluetooth profiles with device "
                                + deviceAddress
                                + " after "
                                + PROFILE_CONNECTION_TIMEOUT_SEC
                                + " secs.\n"
                                + "profile and the time it took to connect: \n"
                                + connectedProfilesAndDelay);
            }
        } finally {
            mContext.unregisterReceiver(receiver);
        }
    }

    /** Disconnect Bluetooth profiles. */
    @Rpc(description = "Disconnect Bluetooth profiles.")
    public void btDisconnectProfiles(String deviceAddress) throws Throwable {
        BluetoothDevice device =
                com.google.android.mobly.snippet.bundled.bluetooth.BluetoothAdapterSnippet
                        .getKnownDeviceByAddress(deviceAddress);
        mDisconnectedProfiles = new HashMap<>();
        ProfileConnectionBroadcastReceiver receiver =
                new ProfileConnectionBroadcastReceiver(mContext);
        IntentFilter a2dpSinkFilter =
                new IntentFilter(BLUETOOTH_A2DP_SINK_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter headsetClientFilter =
                new IntentFilter(BLUETOOTH_HEADSET_CLIENT_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter mapClientFilter =
                new IntentFilter(BLUETOOTH_MAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter pbapClientFilter =
                new IntentFilter(BLUETOOTH_PBAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED);
        IntentFilter panFilter = new IntentFilter(BLUETOOTH_PAN_ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, a2dpSinkFilter);
        mContext.registerReceiver(receiver, headsetClientFilter);
        mContext.registerReceiver(receiver, mapClientFilter);
        mContext.registerReceiver(receiver, pbapClientFilter);
        mContext.registerReceiver(receiver, panFilter);
        try {
            int result = (int) Utils.invokeByReflection(device, "disconnect");
            if (result == BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED) {
                throw new BluetoothAdapterSnippetException(
                        "Failed to initiate the profile disconnection process to device: "
                                + deviceAddress);
            }
            if (!Utils.waitUntil(
                    () -> mDisconnectedProfiles.size() == ALL_PROFILES.size(),
                    PROFILE_CONNECTION_TIMEOUT_SEC)) {
                String disconnectedProfilesAndDelay = "";
                for (Map.Entry<String, Long> entry : mDisconnectedProfiles.entrySet()) {
                    String profile = entry.getKey();
                    Long delay = entry.getValue();
                    disconnectedProfilesAndDelay += (profile + ": " + delay + " ms\n");
                }
                throw new BluetoothAdapterSnippetException(
                        "Failed to disconnect all required bluetooth profiles with device "
                                + deviceAddress
                                + " after "
                                + PROFILE_CONNECTION_TIMEOUT_SEC
                                + " secs.\n"
                                + "profile and the time it took to disconnect: \n"
                                + disconnectedProfilesAndDelay);
            }
        } finally {
            mContext.unregisterReceiver(receiver);
        }
    }

    class ProfileConnectionBroadcastReceiver extends BroadcastReceiver {

        long mStartTime = SystemClock.elapsedRealtime();

        ProfileConnectionBroadcastReceiver(Context context) throws Throwable {
            Utils.adaptShellPermissionIfRequired(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            switch (action) {
                case BLUETOOTH_A2DP_SINK_ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.d(
                            "ProfileConnectionBroadcastReceiver",
                            " Action " + action + ", new A2DP Sink State :" + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectedProfiles.put(
                                A2DP_SINK, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                A2DP_SINK + " added to mConnectedProfiles");
                    }
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mDisconnectedProfiles.put(
                                A2DP_SINK, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                A2DP_SINK + " added to mDisconnectedProfiles");
                    }
                }
                case BLUETOOTH_HEADSET_CLIENT_ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.d(
                            "ProfileConnectionBroadcastReceiver",
                            " Action " + action + ", new Headset Client State :" + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectedProfiles.put(
                                HEADSET_CLIENT, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                HEADSET_CLIENT + " added to mConnectedProfiles");
                    }
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mDisconnectedProfiles.put(
                                HEADSET_CLIENT, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                HEADSET_CLIENT + " added to mDisconnectedProfiles");
                    }
                }
                case BLUETOOTH_MAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.d(
                            "ProfileConnectionBroadcastReceiver",
                            " Action " + action + ", new MAP Client State :" + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectedProfiles.put(
                                MAP_CLIENT, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                MAP_CLIENT + " added to mConnectedProfiles");
                    }
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mDisconnectedProfiles.put(
                                MAP_CLIENT, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                MAP_CLIENT + " added to mDisconnectedProfiles");
                    }
                }
                case BLUETOOTH_PBAP_CLIENT_ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.d(
                            "ProfileConnectionBroadcastReceiver",
                            " Action " + action + ", new PBAP Client State :" + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectedProfiles.put(
                                PBAP_CLIENT, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                PBAP_CLIENT + " added to mConnectedProfiles");
                    }
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mDisconnectedProfiles.put(
                                PBAP_CLIENT, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                PBAP_CLIENT + " added to mDisconnectedProfiles");
                    }
                }
                case BLUETOOTH_PAN_ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.d(
                            "ProfileConnectionBroadcastReceiver",
                            " Action " + action + ", new PAN State :" + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectedProfiles.put(PAN, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                PAN + " added to mConnectedProfiles");
                    }
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mDisconnectedProfiles.put(PAN, SystemClock.elapsedRealtime() - mStartTime);
                        Log.d(
                                "ProfileConnectionBroadcastReceiver",
                                PAN + " added to mDisconnectedProfiles");
                    }
                }
                default -> {}
            }
        }
    }
}
