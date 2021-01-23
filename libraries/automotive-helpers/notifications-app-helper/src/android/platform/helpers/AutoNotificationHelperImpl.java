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
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

/** This helper extends NotificationHelperImpl to accommodate for Auto specific UI */
public class AutoNotificationHelperImpl extends NotificationHelperImpl
        implements IAutoNotificationHelper {
    private static final int UI_RESPONSE_WAIT_MS = 5000;

    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String NOTIFICATION_LIST_ID = "notifications";
    private static final String CLEAR_ALL_BUTTON_ID = "clear_all_button";
    private static final String STATUS_BAR_ID = "car_top_bar";
    private static final String RECYCLER_VIEW_CLASS = "androidx.recyclerview.widget.RecyclerView";
    private static final String NOTIFICATION_VIEW = "notification_view";
    private static final String OPEN_NOTIFICATION = "service call statusbar 1";

    private static final BySelector NOTIFICATION_LIST =
            By.res(SYSTEMUI_PACKAGE, NOTIFICATION_LIST_ID).clazz(RECYCLER_VIEW_CLASS);
    private static final BySelector NOTIFICATIONS = By.res(SYSTEMUI_PACKAGE, NOTIFICATION_VIEW);
    private static final BySelector CLEAR_ALL_BUTTON =
            By.res(SYSTEMUI_PACKAGE, CLEAR_ALL_BUTTON_ID);
    private static final BySelector STATUS_BAR_GROUND = By.res(SYSTEMUI_PACKAGE, STATUS_BAR_ID);

    public AutoNotificationHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public void openNotificationbyIndex(int index) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /** {@inheritDoc} */
    @Override
    public void openNotificationByTitle(String title, String expectedPkg) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: None
     *
     * <p>Check if notification app is in foreground by checking if the notification list exists.
     */
    @Override
    public boolean isAppInForeground() {
        return mDevice.hasObject(NOTIFICATIONS);
    }

    /** {@inheritDoc} */
    @Override
    public void tapClearAllBtn() {
        scrollThroughNotifications();
        if (clearAllBtnExist()) {
            UiObject2 clear_all_btn =
                    mDevice.wait(Until.findObject(CLEAR_ALL_BUTTON), UI_RESPONSE_WAIT_MS);
            clear_all_btn.click();
        } else {
            throw new RuntimeException("Cannot find Clear All button");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkNotificationExists(String title) {
        executeShellCommand(OPEN_NOTIFICATION);
        UiObject2 notification_list =
                mDevice.wait(Until.findObject(NOTIFICATION_LIST), UI_RESPONSE_WAIT_MS);
        UiObject2 postedNotification =
                mDevice.wait(Until.findObject(By.text(title)), UI_RESPONSE_WAIT_MS);
        // This scrolls the notification list until the notification is found
        // or reaches to the bottom when "Clear All" button is presented.
        while (postedNotification == null && isAppInForeground()) {
            if (clearAllBtnExist()) {
                break;
            }
            notification_list.scroll(Direction.DOWN, 20, 300);
            postedNotification =
                    mDevice.wait(Until.findObject(By.text(title)), UI_RESPONSE_WAIT_MS);
        }
        return postedNotification != null;
    }

    /** {@inheritDoc} */
    @Override
    public void removeNotification(String title) {
        mDevice.wait(Until.gone(By.text(title)), UI_RESPONSE_WAIT_MS);
        executeShellCommand(OPEN_NOTIFICATION);
        UiObject2 postedNotification =
                mDevice.wait(Until.findObject(By.text(title)), UI_RESPONSE_WAIT_MS);
        postedNotification.swipe(Direction.LEFT, 1.0f);
    }

    /** {@inheritDoc} */
    @Override
    public void openNotification() {
        UiObject2 statusBar =
                mDevice.wait(Until.findObject(STATUS_BAR_GROUND), UI_RESPONSE_WAIT_MS);
        statusBar.swipe(Direction.DOWN, 1.0f, 500);
    }

    private void scrollThroughNotifications() {
        executeShellCommand(OPEN_NOTIFICATION);
        UiObject2 notification_list =
                mDevice.wait(Until.findObject(NOTIFICATION_LIST), UI_RESPONSE_WAIT_MS);
        int max_swipe = 5;
        while (max_swipe > 0 && isAppInForeground()) {
            if (clearAllBtnExist()) {
                break;
            } else {
                max_swipe--;
            }
            notification_list.scroll(Direction.DOWN, 20, 300);
        }
    }

    private boolean clearAllBtnExist() {
        UiObject2 clear_all_btn =
                mDevice.wait(Until.findObject(CLEAR_ALL_BUTTON), UI_RESPONSE_WAIT_MS);
        return clear_all_btn != null;
    }
}
