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

import static android.app.Notification.CATEGORY_SYSTEM;
import static android.app.NotificationManager.IMPORTANCE_HIGH;

import static com.google.common.truth.Truth.assertThat;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import android.app.Instrumentation;
import android.app.Notification;
import android.app.Notification.MessagingStyle.Message;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Icon;
import android.os.SystemClock;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationHelperImpl extends AbstractAutoStandardAppHelper
        implements INotificationHelper {
    private static final String LOG_TAG = NotificationHelperImpl.class.getSimpleName();

    private static final String APP_NAME = "android.platform.test.scenario";
    private static final String UI_PACKAGE_NAME_SYSUI = "com.android.systemui";
    private static final String UI_PACKAGE_NAME_ANDROID = "android";
    private static final String UI_NOTIFICATION_ID = "status_bar_latest_event_content";
    private static final String NOTIFICATION_TITLE_ID = "title";
    private static final String NOTIFICATION_TITLE_TEXT = "TEST NOTIFICATION";
    private static final String NOTIFICATION_BIG_TEXT =
            "lorem ipsum dolor sit amet\n"
                    + "lorem ipsum dolor sit amet\n"
                    + "lorem ipsum dolor sit amet\n"
                    + "lorem ipsum dolor sit amet";
    private static final String NOTIFICATION_CONTENT_TEXT_FORMAT = "Test notification %d";
    private static final String NOTIFICATION_GROUP_KEY_FORMAT = "Test group %d";
    private static final String NOTIFICATION_CHANNEL_ID = "test_channel_id";
    private static final String NOTIFICATION_CHANNEL_NAME = "Test Channel";
    private static final String UI_NOTIFICATION_LIST_ID = "notification_stack_scroller";
    private static final String UI_CLEAR_ALL_BUTTON_ID = "dismiss_text";
    private static final String GUTS_ID = "notification_guts";
    private static final String GUTS_SETTINGS_ID = "info";
    private static final String GUTS_CLOSE_ID = "done";
    private static final String UI_QS_STATUS_BAR_ID = "quick_status_bar_system_icons";
    private static final String UI_SCROLLABLE_ELEMENT_ID = "notification_stack_scroller";

    private static final long SHORT_TRANSITION_WAIT = 1500;
    // Time to wait for notifications to post and inflate fully.
    private static final long NOTIFICATION_POST_TIMEOUT = 2000;
    // Time to wait for notifications to be cancelled and their views removed.
    private static final long NOTIFICATION_CANCEL_TIMEOUT = 2000;
    private static final long NOTIFICATION_POLL_INTERVAL = 100;
    private static final long UI_RESPONSE_TIMEOUT_MSECS = 3000;
    private static final long LAUNCH_APP_TIMEOUT_MSECS = 10000;
    private static final long LONG_PRESS_DURATION = 3000;
    private static final int MAX_FIND_NOTIFICATION_ATTEMPTS = 15;
    // How deep to go on the hierarchy when looking for notifications.
    private static final int NOTIFICATION_DEPTH = 6;
    private static final int DEFAULT_FLING_SPEED = 3000;

    private final NotificationManager mNotificationManager;

    public NotificationHelperImpl(Instrumentation instr) {
        super(instr);
        mNotificationManager = instr.getContext().getSystemService(NotificationManager.class);
        NotificationChannel channel =
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(channel);
    }

    /** {@inheritDoc} */
    @Override
    public void open() {
        mDevice.openNotification();
        // Wait for the notification shade to finish open. This is determined by the appearance of
        // the status bar within Quick Settings.
        mDevice.wait(
                Until.hasObject(By.res(UI_PACKAGE_NAME_SYSUI, UI_QS_STATUS_BAR_ID)),
                LAUNCH_APP_TIMEOUT_MSECS);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return UI_PACKAGE_NAME_SYSUI;
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("There is no explicit launcher for Notification");
    }

    private List<UiObject2> getNotificationStack() {
        List<UiObject2> objects =
                mDevice.wait(
                        Until.findObjects(
                                By.res(UI_PACKAGE_NAME_ANDROID, UI_NOTIFICATION_ID)
                                        .maxDepth(NOTIFICATION_DEPTH)),
                        SHORT_TRANSITION_WAIT);
        return objects.stream().map(o -> o.getParent().getParent()).collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public void openNotificationbyIndex(int index) {
        List<UiObject2> notificationStack = getNotificationStack();
        UiObject2 targetNotification = notificationStack.get(index);
        targetNotification.clickAndWait(Until.newWindow(), SHORT_TRANSITION_WAIT);
    }

    /** {@inheritDoc} */
    @Override
    public void postNotifications(int count) {
        postNotifications(count, null);
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
            builder.setGroup(String.format(NOTIFICATION_GROUP_KEY_FORMAT, i));
            mNotificationManager.notify(i, builder.build());
        }

        // Wait for notifications to inflate.
        if (!waitUntilPostedNotificationsCountMatches(
                initialCount + count, NOTIFICATION_POST_TIMEOUT)) {
            int activeNotifsCount = mNotificationManager.getActiveNotifications().length;
            throw new AssertionError(
                    String.format(
                            "Timed out trying to post notifications. Posted %d out of %d requested"
                                    + " successfully. %d notifications total.",
                            activeNotifsCount - initialCount, count, activeNotifsCount));
        }
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 postBigTextNotification(String pkg) {
        postNotification(
                getBuilder(pkg)
                        .setStyle(new Notification.BigTextStyle().bigText(NOTIFICATION_BIG_TEXT))
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentText(NOTIFICATION_CONTENT_TEXT));
        return getNotificationByTitle(NOTIFICATION_TITLE_TEXT);
    }

    @Override
    public UiObject2 postBigPictureNotification(String pkg) {
        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        postNotification(
                getBuilder(pkg)
                        .setStyle(new Notification.BigPictureStyle().bigPicture(bitmap))
                        .setContentText(NOTIFICATION_CONTENT_TEXT));
        return getNotificationByTitle(NOTIFICATION_TITLE_TEXT);
    }

    @Override
    public UiObject2 postMessagingStyleNotification(String pkg) {
        String personName = "Person Name";
        Person person = new Person.Builder().setName(personName).build();
        postNotification(
                getBuilder(pkg)
                        .setStyle(
                                new Notification.MessagingStyle(person)
                                        .addMessage(
                                                new Message(
                                                        "Message 4",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))
                                        .addMessage(
                                                new Message(
                                                        "Message 3",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))
                                        .addMessage(
                                                new Message(
                                                        "Message 2",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))
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

    @Override
    public void postBubbleNotification(String senderName, int count) {
        String shortcutId = "test_shortcut_id";
        String pkg = "android.platform.test.scenario";

        Context context = mInstrumentation.getContext();
        Person person = new Person.Builder().setName(senderName).build();
        long currentTimeMillis = SystemClock.currentThreadTimeMillis();
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        ShortcutInfo shortcutInfo =
                new ShortcutInfo.Builder(context, shortcutId)
                        .setShortLabel(senderName)
                        .setLongLabel(senderName)
                        .setIntent(intent)
                        .setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                        .setPerson(person)
                        .setLongLived(true)
                        .build();
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.pushDynamicShortcut(shortcutInfo);

        Notification.BubbleMetadata bubbleMetadata =
                new Notification.BubbleMetadata.Builder(shortcutInfo.getId())
                        .setAutoExpandBubble(false /* autoExpand */)
                        .setSuppressNotification(false /* suppressNotif */)
                        .build();

        Notification.Builder builder =
                getBuilder(pkg)
                        .setStyle(
                                new Notification.MessagingStyle(person)
                                        .addMessage(
                                                new Message("Message", currentTimeMillis, person)))
                        .setShortcutId(shortcutId)
                        .setBubbleMetadata(bubbleMetadata);

        int initialCount = mNotificationManager.getActiveNotifications().length;
        for (int i = initialCount; i < initialCount + count; i++) {
            mNotificationManager.notify(i, builder.build());
        }
    }

    @Override
    public UiObject2 postConversationNotification(String pkg) {
        String shortcutId = "test_shortcut_id";
        String personName = "Person Name";

        Context context = mInstrumentation.getContext();
        Person person = new Person.Builder().setName(personName).build();
        long currentTimeMillis = SystemClock.currentThreadTimeMillis();
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        ShortcutInfo shortcutInfo =
                new ShortcutInfo.Builder(context, shortcutId)
                        .setShortLabel(personName)
                        .setLongLabel(personName)
                        .setIntent(intent)
                        .setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                        .setPerson(person)
                        .setLongLived(true)
                        .build();
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.pushDynamicShortcut(shortcutInfo);

        Notification.Builder builder =
                getBuilder(pkg)
                        .setStyle(
                                new Notification.MessagingStyle(person)
                                        .addMessage(
                                                new Message("Message", currentTimeMillis, person)))
                        .setShortcutId(shortcutId);
        postNotification(builder);

        for (UiObject2 notification : getNotificationStack()) {
            if (notification.hasObject(By.text(personName))) {
                return notification;
            }
        }

        throw new AssertionError("Couldn't find posted notification");
    }

    /** {@inheritDoc} */
    @Override
    public void postNotification(Notification.Builder builder) {
        final int initialCount = mNotificationManager.getActiveNotifications().length;
        mNotificationManager.notify(initialCount, builder.build());

        // Wait for notifications to inflate.
        if (!waitUntilPostedNotificationsCountMatches(
                initialCount + 1, NOTIFICATION_POST_TIMEOUT)) {
            throw new AssertionError("Timed out trying to post notifications.");
        }
    }

    private Notification.Builder getBuilder(String pkg) {
        Context context = mInstrumentation.getContext();

        Notification.Builder builder =
                new Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(NOTIFICATION_TITLE_TEXT)
                        .setCategory(CATEGORY_SYSTEM)
                        .setGroupSummary(false)
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

    /** {@inheritDoc} */
    @Override
    public void cancelNotifications() {
        mNotificationManager.cancelAll();

        // Wait for notifications to cancel and views to be removed.
        if (!waitUntilPostedNotificationsCountMatches(0, NOTIFICATION_CANCEL_TIMEOUT)) {
            throw new AssertionError("Timed out trying to cancel all notifications.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void expandNotification(UiObject2 targetNotification, boolean dragging) {
        int height = targetNotification.getVisibleBounds().height();

        if (dragging) {
            Point dragTarget = targetNotification.getVisibleCenter();
            dragTarget.y += 300;
            targetNotification.drag(dragTarget);
        } else {
            UiObject2 chevron =
                    targetNotification.findObject(
                            By.res(UI_PACKAGE_NAME_ANDROID, EXPAND_BUTTON_ID));
            chevron.click();
        }

        // There isn't an explicit contract for notification expansion, so let's assert
        // that the content height changed, which is likely.
        long startTime = SystemClock.elapsedRealtime();
        while (SystemClock.elapsedRealtime() - startTime < UI_RESPONSE_TIMEOUT_MSECS) {
            if (targetNotification.getVisibleBounds().height() != height) {
                return;
            }
            SystemClock.sleep(NOTIFICATION_POLL_INTERVAL);
        }
        throw new AssertionError("Notification height didn't change");
    }

    /** {@inheritDoc} */
    @Override
    public void showGuts(UiObject2 notification) {
        notification.click(LONG_PRESS_DURATION);
        UiObject2 guts =
                notification.wait(
                        Until.findObject(By.res(UI_PACKAGE_NAME_SYSUI, GUTS_ID)),
                        UI_RESPONSE_TIMEOUT_MSECS);

        assertThat(guts.hasObject(By.text(APP_NAME))).isTrue();
        assertThat(guts.hasObject(By.text(NOTIFICATION_CHANNEL_NAME))).isTrue();

        // Confirmation/Settings buttons
        assertThat(guts.hasObject(By.res(UI_PACKAGE_NAME_SYSUI, GUTS_SETTINGS_ID))).isTrue();
        assertThat(guts.hasObject(By.res(UI_PACKAGE_NAME_SYSUI, GUTS_CLOSE_ID))).isTrue();
    }

    /** {@inheritDoc} */
    @Override
    public void hideGuts(UiObject2 notification) {
        BySelector gutsSelector = By.res(UI_PACKAGE_NAME_SYSUI, GUTS_ID);
        assertThat(notification.hasObject(gutsSelector)).isTrue();

        notification.findObject(By.text("Done")).click();
        notification.wait(Until.gone(gutsSelector), UI_RESPONSE_TIMEOUT_MSECS);
    }

    /**
     * Waits until the number of notifications posted by this helper matches the specified count.
     *
     * @param count The count to wait for.
     * @param timeout The maximum time to wait in ms before timing out.
     * @return true if the number of posted notifications matches count, false if it timed out
     */
    private boolean waitUntilPostedNotificationsCountMatches(int count, long timeout) {
        long startTime = SystemClock.elapsedRealtime();
        boolean countMatches = false;
        while (SystemClock.elapsedRealtime() - startTime < timeout) {
            SystemClock.sleep(NOTIFICATION_POLL_INTERVAL);
            countMatches = mNotificationManager.getActiveNotifications().length == count;
        }
        return countMatches;
    }

    /** {@inheritDoc} */
    @Override
    public void openNotificationByTitle(String title, String expectedPkg) {
        UiObject2 targetNotification = getNotificationByTitle(title);
        for (int retries = 0;
                retries < MAX_FIND_NOTIFICATION_ATTEMPTS && targetNotification == null;
                retries++) {
            // Checks the notification list has scrolled to the bottom or not
            boolean clearAllAppears =
                    mDevice.hasObject(By.res(UI_PACKAGE_NAME_SYSUI, UI_CLEAR_ALL_BUTTON_ID));
            if (clearAllAppears) {
                throw new IllegalStateException(
                        String.format("Did not find notification with title, %s", title));
            }

            UiObject2 notificationList =
                    mDevice.findObject(By.res(UI_PACKAGE_NAME_SYSUI, UI_NOTIFICATION_LIST_ID));
            int notificationListY = notificationList.getVisibleBounds().height();
            notificationList.setGestureMargin((int) Math.floor(notificationListY * 0.1));
            notificationList.scroll(Direction.DOWN, 1.0f, 2000);
            mDevice.waitForIdle();
            targetNotification = getNotificationByTitle(title);
        }
        targetNotification.click();
        mDevice.waitForIdle();

        // Won't check if the expected package is in foreground or not if the application isn't
        // specified
        if (expectedPkg != null
                && !mDevice.wait(
                        Until.hasObject(By.pkg(expectedPkg).depth(0)), LAUNCH_APP_TIMEOUT_MSECS)) {
            throw new IllegalStateException(
                    String.format("Did not find application, %s, in foreground", expectedPkg));
        }
    }

    @Override
    public UiObject2 getNotificationByText(String text) {
        for (UiObject2 notification : getNotificationStack()) {
            if (notification.hasObject(By.text(text))) {
                return notification;
            }
        }
        throw new AssertionError("Couldn't find posted notification");
    }

    private UiObject2 getNotificationByTitle(String title) {
        for (UiObject2 notification : getNotificationStack()) {
            if (notification.hasObject(
                    By.res(UI_PACKAGE_NAME_ANDROID, NOTIFICATION_TITLE_ID).text(title))) {
                return notification;
            }
        }
        throw new IllegalStateException(
                String.format("Did not find notification with title %s", title));
    }

    @Override
    public void shareScreenshotFromNotification(BySelector pageSelector) {
        BySelector shareSelector = By.text(compile("Share", CASE_INSENSITIVE));
        UiObject2 screenshotNotification = getNotificationByTitle("Screenshot saved");
        if (!screenshotNotification.hasObject(shareSelector)) {
            expandNotification(screenshotNotification, false);
        }
        assertThat(screenshotNotification.hasObject(shareSelector)).isTrue();

        screenshotNotification.findObject(shareSelector).click();
        if (!mDevice.wait(Until.hasObject(pageSelector), UI_RESPONSE_TIMEOUT_MSECS)) {
            throw new IllegalStateException(
                    String.format("Did not find share screenshot page of %s", pageSelector));
        }
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 getNotificationShadeScrollContainer() {
        return mDevice.wait(
                Until.findObject(By.res(UI_PACKAGE_NAME_SYSUI, UI_SCROLLABLE_ELEMENT_ID)),
                UI_RESPONSE_TIMEOUT_MSECS);
    }

    /** {@inheritDoc} */
    @Override
    public void flingFeed(UiObject2 container, Direction dir) {
        flingFeed(container, dir, DEFAULT_FLING_SPEED);
    }

    /** {@inheritDoc} */
    @Override
    public void flingFeed(UiObject2 container, Direction dir, int speed) {
        if (dir == Direction.LEFT || dir == Direction.RIGHT) {
            throw new IllegalArgumentException("Can only fling Notification up and down");
        }

        // Set a 30% margin of scroller height to avoid scrolling from the screen border
        int scrollerHeight = container.getVisibleBounds().height();
        container.setGestureMargin((int) Math.floor(scrollerHeight * 0.3));
        container.fling(dir, speed);
    }

    /** {@inheritDoc} */
    @Override
    public void swipeToOpen() {
        mDevice.swipe(
                mDevice.getDisplayWidth() / 2 /* startX */,
                mDevice.getDisplayHeight() / 3 /* startY */,
                mDevice.getDisplayWidth() / 2 /* endX */,
                mDevice.getDisplayHeight() * 2 / 3 /* endY */,
                10 /* steps */);
        mDevice.waitForIdle();
    }
}
