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

import static junit.framework.Assert.assertTrue;

import android.app.Instrumentation;
import android.app.Notification;
import android.app.Notification.MessagingStyle.Message;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.os.SystemClock;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import java.util.List;
import java.util.stream.Collectors;

public class AutoNotificationMockingHelperImpl extends NotificationHelperImpl
        implements IAutoNotificationMockingHelper {

    private static final int UI_RESPONSE_WAIT_MS = 5000;
    private static final int LONG_UI_RESPONSE_WAIT_MS = 10000;

    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String NOTIFICATION_CHANNEL_ID = "auto_test_channel_id";
    private static final String NOTIFICATION_CHANNEL_NAME = "Test Channel";
    private static final String NOTIFICATION_TITLE_TEXT = "AUTO TEST NOTIFICATION";
    private static final String APP_ICON_ID = "app_icon";
    private static final String APP_NAME_ID = "header_text";
    private static final String NOTIFICATION_TITLE_ID = "notification_body_title";
    private static final String NOTIFICATION_BODY_ID = "notification_body_content";
    private static final String NOTIFICATION_CONTENT_TEXT_FORMAT = "Test notification %d";
    private static final String OPEN_NOTIFICATION = "service call statusbar 1";

    private static final BySelector[] NOTIFICATION_REQUIRED_FIELDS = {
        By.res(SYSTEMUI_PACKAGE, APP_ICON_ID),
        By.res(SYSTEMUI_PACKAGE, APP_NAME_ID),
        By.res(SYSTEMUI_PACKAGE, NOTIFICATION_TITLE_ID),
        By.res(SYSTEMUI_PACKAGE, NOTIFICATION_BODY_ID)
    };

    private static final int NOTIFICATION_DEPTH = 6;
    private static final long SHORT_TRANSITION_WAIT = 1500;

    private NotificationManager mNotificationManager;

    public AutoNotificationMockingHelperImpl(Instrumentation instr) {
        super(instr);
        mNotificationManager = instr.getContext().getSystemService(NotificationManager.class);
        NotificationChannel channel =
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(channel);
    }

    /** {@inheritDoc} */
    @Override
    public void postNotifications(int count) {
        postNotifications(count, null);
        SystemClock.sleep(UI_RESPONSE_WAIT_MS);
        assertTrue(
                "Notification does not have all required fields",
                checkNotificationRequiredFieldsExist(NOTIFICATION_TITLE_TEXT));
    }

    /** {@inheritDoc} */
    @Override
    public void postNotifications(int count, String pkg) {
        postNotifications(count, pkg, false /* interrupting */);
    }

    /** {@inheritDoc} */
    @Override
    public void postNotifications(int count, String pkg, boolean interrupting) {
        int initialCount = mNotificationManager.getActiveNotifications().length;
        Notification.Builder builder = getBuilder(pkg);
        if (interrupting) {
            Person person = new Person.Builder().setName("Marvelous user").build();
            builder.setStyle(
                    new Notification.MessagingStyle(person)
                            .addMessage(
                                    new Message(
                                            "Hello",
                                            SystemClock.currentThreadTimeMillis(),
                                            person)));
        }

        for (int i = initialCount; i < initialCount + count; i++) {
            builder.setContentText(String.format(NOTIFICATION_CONTENT_TEXT_FORMAT, i));
            mNotificationManager.notify(i, builder.build());
        }
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 postMessagingStyleNotification(String pkg) {
        String personName = "John Doe";
        Person person = new Person.Builder().setName(personName).build();
        postNotification(
                getBuilder(pkg)
                        .setStyle(
                                new Notification.MessagingStyle(person)
                                        .addMessage(
                                                new Message(
                                                        "Message 1",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))));

        for (UiObject2 notification : getNotificationStack()) {
            if (notification.hasObject(By.text(personName))) {
                return notification;
            }
        }
        throw new AssertionError("Couldn't find posted notification");
    }

    /** {@inheritDoc} */
    @Override
    public void clearAllNotification() {
        mNotificationManager.cancelAll();
    }

    private boolean checkNotificationRequiredFieldsExist(String title) {
        if (!checkNotificationExists(title)) {
            throw new RuntimeException(
                    String.format("Unable to find notification with title %s", title));
        }
        for (BySelector selector : NOTIFICATION_REQUIRED_FIELDS) {
            UiObject2 obj = mDevice.wait(Until.findObject(selector), UI_RESPONSE_WAIT_MS);
            if (obj == null) {
                throw new RuntimeException("Unable to find required notification field");
            }
        }
        return true;
    }

    private boolean checkNotificationExists(String title) {
        executeShellCommand(OPEN_NOTIFICATION);
        UiObject2 postedNotification =
                mDevice.wait(Until.findObject(By.text(title)), UI_RESPONSE_WAIT_MS);
        return postedNotification != null;
    }

    private Notification.Builder getBuilder(String pkg) {
        Context context = mInstrumentation.getContext();
        Notification.Builder builder =
                new Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(NOTIFICATION_TITLE_TEXT)
                        .setContentText(NOTIFICATION_CONTENT_TEXT)
                        .setSmallIcon(android.R.drawable.stat_notify_chat);
        if (pkg != null) {
            builder.setContentIntent(
                    PendingIntent.getActivity(
                            context,
                            0,
                            context.getPackageManager().getLaunchIntentForPackage(pkg),
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        return builder;
    }

    private List<UiObject2> getNotificationStack() {
        List<UiObject2> objects =
                mDevice.wait(
                        Until.findObjects(
                                By.res(SYSTEMUI_PACKAGE, "card_view").maxDepth(NOTIFICATION_DEPTH)),
                        SHORT_TRANSITION_WAIT);
        return objects.stream().map(o -> o.getParent().getParent()).collect(Collectors.toList());
    }
}
