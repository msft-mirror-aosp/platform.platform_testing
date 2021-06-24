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

import static android.media.AudioAttributes.USAGE_ALARM;
import static android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
import static android.media.AudioAttributes.USAGE_MEDIA;
import static android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.car.Car;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.os.SystemClock;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import androidx.test.InstrumentationRegistry;

import java.util.List;

public class SettingsSoundsHelperImpl extends AbstractAutoStandardAppHelper
        implements IAutoSoundsSettingHelper {
    private static final int UI_RESPONSE_WAIT_MS = 5000;
    private static final int SHORT_UI_RESPONSE_TIME = 1000;
    private static final int VOLUME_FLAGS = 0;
    private static final int USAGE_INVALID = -1;
    private static final int MINIMUM_NUMBER_OF_CHILDREN = 2;

    private final BySelector UP_BUTTON =
            By.res(
                    getApplicationConfig(AutoConfigConstants.SETTINGS_PACKAGE),
                    "car_ui_scrollbar_page_up");
    private final BySelector DOWN_BUTTON =
            By.res(
                    getApplicationConfig(AutoConfigConstants.SETTINGS_PACKAGE),
                    "car_ui_scrollbar_page_down");

    protected UiDevice mDevice;
    private Instrumentation mInstrumentation;
    private Context mContext;
    private CarAudioManager mCarAudioManager;
    private final UiAutomation mUiAutomation =
            InstrumentationRegistry.getInstrumentation().getUiAutomation();

    public SettingsSoundsHelperImpl(Instrumentation instr) {
        super(instr);
        mInstrumentation = instr;
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mContext = InstrumentationRegistry.getContext();
        Car car = Car.createCar(mContext);
        mUiAutomation.adoptShellPermissionIdentity(
                "android.car.permission.CAR_CONTROL_AUDIO_VOLUME",
                "android.car.permission.CAR_CONTROL_AUDIO_SETTINGS");
        mCarAudioManager = (CarAudioManager) car.getCarManager(Car.AUDIO_SERVICE);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getApplicationConfig(AutoConfigConstants.SETTINGS_PACKAGE);
    }

    /** {@inheritDoc} */
    @Override
    public void setVolume(VolumeType volumeType, int index) {
        int audioAttribute = USAGE_INVALID;
        switch (volumeType) {
            case MEDIA:
                audioAttribute = USAGE_MEDIA;
                break;
            case ALARM:
                audioAttribute = USAGE_ALARM;
                break;
            case NAVIGATION:
                audioAttribute = USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
                break;
            case INCALL:
                audioAttribute = USAGE_VOICE_COMMUNICATION;
                break;
        }
        int volumeGroupId = mCarAudioManager.getVolumeGroupIdForUsage(audioAttribute);
        mCarAudioManager.setGroupVolume(volumeGroupId, index, VOLUME_FLAGS);
    }

    /** {@inheritDoc} */
    @Override
    public int getVolume(VolumeType volumeType) {
        int audioAttribute = USAGE_INVALID;
        switch (volumeType) {
            case MEDIA:
                audioAttribute = USAGE_MEDIA;
                break;
            case ALARM:
                audioAttribute = USAGE_ALARM;
                break;
            case NAVIGATION:
                audioAttribute = USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
                break;
            case INCALL:
                audioAttribute = USAGE_VOICE_COMMUNICATION;
                break;
        }
        int volumeGroupId = mCarAudioManager.getVolumeGroupIdForUsage(audioAttribute);
        int volume = mCarAudioManager.getGroupVolume(volumeGroupId);
        return volume;
    }

    /** {@inheritDoc} */
    @Override
    public void setSound(SoundType soundType, String sound) {
        String type = "";
        switch (soundType) {
            case ALARM:
                type = "Default alarm sound";
                break;
            case NOTIFICATION:
                type = "Default notification sound";
                break;
            case RINGTONE:
                type = "Phone ringtone";
                break;
        }
        UiObject2 object = scrollAndFindUiObject(By.text(type));
        String currentSound = getSound(soundType);
        object.click();
        SystemClock.sleep(SHORT_UI_RESPONSE_TIME);
        List<UiObject2> downButtons =
                mDevice.wait(Until.findObjects(DOWN_BUTTON), UI_RESPONSE_WAIT_MS);
        int subSettingScrollBarIndex = downButtons.size() - 1;
        UiObject2 downButton = downButtons.get(subSettingScrollBarIndex);
        List<UiObject2> upButtons = mDevice.wait(Until.findObjects(UP_BUTTON), UI_RESPONSE_WAIT_MS);
        subSettingScrollBarIndex = upButtons.size() - 1;
        UiObject2 upButton = upButtons.get(subSettingScrollBarIndex);
        UiObject2 scroll = null;
        if (currentSound.compareToIgnoreCase(sound) < 0) {
            scroll = downButton;
        } else if (currentSound.compareToIgnoreCase(sound) > 0) {
            scroll = upButton;
        }
        UiObject2 soundObject = mDevice.wait(Until.findObject(By.text(sound)), UI_RESPONSE_WAIT_MS);
        while (soundObject == null) {
            scroll.click();
            soundObject = mDevice.wait(Until.findObject(By.text(sound)), UI_RESPONSE_WAIT_MS);
            if (!scroll.isEnabled()) {
                break;
            }
        }
        if (soundObject == null) {
            throw new RuntimeException(String.format("Unable to find sound %s", sound));
        }
        soundObject.click();
        UiObject2 saveButton = mDevice.wait(Until.findObject(By.desc("Save")), UI_RESPONSE_WAIT_MS);
        saveButton.click();
    }

    /** {@inheritDoc} */
    @Override
    public String getSound(SoundType soundType) {
        String type = "";
        switch (soundType) {
            case ALARM:
                type = "Default alarm sound";
                break;
            case NOTIFICATION:
                type = "Default notification sound";
                break;
            case RINGTONE:
                type = "Phone ringtone";
                break;
        }
        UiObject2 object = scrollAndFindUiObject(By.text(type));
        List<UiObject2> list = object.getParent().getChildren();
        if (list.size() < 2) {
            scrollDownOnePage(1);
            list = object.getParent().getChildren();
        }
        UiObject2 summary = list.get(1);
        return summary.getText();
    }
}
