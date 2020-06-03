/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.wm.flicker;

import androidx.annotation.Nullable;

import com.android.server.wm.flicker.Assertions.Result;
import com.android.server.wm.nano.WindowContainerChildProto;
import com.android.server.wm.nano.WindowContainerProto;
import com.android.server.wm.nano.WindowManagerTraceFileProto;
import com.android.server.wm.nano.WindowManagerTraceProto;
import com.android.server.wm.nano.WindowStateProto;

import com.google.protobuf.nano.InvalidProtocolBufferNanoException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contains a collection of parsed WindowManager trace entries and assertions to apply over a single
 * entry.
 *
 * <p>Each entry is parsed into a list of {@link WindowManagerTrace.Entry} objects.
 */
public class WindowManagerTrace {
    private final List<Entry> mEntries;
    @Nullable private final Path mSource;
    @Nullable private final String mSourceChecksum;

    private WindowManagerTrace(List<Entry> entries, Path source, String sourceChecksum) {
        this.mEntries = entries;
        this.mSource = source;
        this.mSourceChecksum = sourceChecksum;
    }

    /**
     * Parses {@code WindowManagerTraceFileProto} from {@code data} and uses the proto to generates
     * a list of trace entries.
     *
     * @param data binary proto data
     * @param source Path to source of data for additional debug information
     */
    public static WindowManagerTrace parseFrom(byte[] data, Path source, String checksum) {
        List<Entry> entries = new ArrayList<>();

        WindowManagerTraceFileProto fileProto;
        try {
            fileProto = WindowManagerTraceFileProto.parseFrom(data);
        } catch (InvalidProtocolBufferNanoException e) {
            throw new RuntimeException(e);
        }
        for (WindowManagerTraceProto entryProto : fileProto.entry) {
            entries.add(new Entry(entryProto));
        }
        return new WindowManagerTrace(entries, source, checksum);
    }

    public static WindowManagerTrace parseFrom(byte[] data) {
        return parseFrom(data, null /* source */, null /* checksum */);
    }

    public List<Entry> getEntries() {
        return mEntries;
    }

    public Entry getEntry(long timestamp) {
        Optional<Entry> entry =
                mEntries.stream().filter(e -> e.getTimestamp() == timestamp).findFirst();
        if (!entry.isPresent()) {
            throw new RuntimeException("Entry does not exist for timestamp " + timestamp);
        }
        return entry.get();
    }

    public Optional<Path> getSource() {
        return Optional.ofNullable(mSource);
    }

    public String getSourceChecksum() {
        return mSourceChecksum;
    }

    /** Represents a single WindowManager trace entry. */
    public static class Entry implements ITraceEntry {
        private final WindowManagerTraceProto mProto;

        public Entry(WindowManagerTraceProto proto) {
            mProto = proto;
        }

        private List<WindowStateProto> getWindows(WindowContainerProto windowContainer) {
            return Arrays.stream(windowContainer.children)
                    .flatMap(p -> getWindows(p).stream())
                    .collect(Collectors.toList());
        }

        private List<WindowStateProto> getWindows(WindowContainerChildProto windowContainer) {
            if (windowContainer.displayArea != null) {
                return getWindows(windowContainer.displayArea.windowContainer);
            } else if (windowContainer.displayContent != null
                    && windowContainer.displayContent.windowContainer == null) {
                return getWindows(windowContainer.displayContent.rootDisplayArea.windowContainer);
            } else if (windowContainer.displayContent != null) {
                return getWindows(windowContainer.displayContent.windowContainer);
            } else if (windowContainer.task != null) {
                return getWindows(windowContainer.task.windowContainer);
            } else if (windowContainer.activity != null) {
                return getWindows(windowContainer.activity.windowToken.windowContainer);
            } else if (windowContainer.windowToken != null) {
                return getWindows(windowContainer.windowToken.windowContainer);
            } else if (windowContainer.window != null) {
                return Collections.singletonList(windowContainer.window);
            } else {
                return getWindows(windowContainer.windowContainer);
            }
        }

        private List<WindowStateProto> getWindows() {
            return getWindows(mProto.windowManagerService.rootWindowContainer.windowContainer);
        }

        /** Checks if non app window with {@code windowTitle} is visible. */
        private Optional<WindowStateProto> getNonAppWindowByIdentifier(
                WindowStateProto windowState, String windowTitle) {
            if (windowState.identifier.title.contains(windowTitle)) {
                return Optional.of(windowState);
            }

            return Arrays.stream(windowState.childWindows)
                    .filter(child -> getNonAppWindowByIdentifier(child, windowTitle).isPresent())
                    .findFirst();
        }

        /** Checks if non app window with {@code windowTitle} is visible. */
        public Result isNonAppWindowVisible(String windowTitle) {
            boolean titleFound = false;
            List<WindowStateProto> windows = getWindows();
            if (windows.isEmpty()) {
                return new Result(
                        false /* success */,
                        getTimestamp(),
                        "isNonAppWindowVisible" /* assertionName */,
                        "No windows found");
            }

            for (WindowStateProto windowState : windows) {
                Optional<WindowStateProto> foundWindow =
                        getNonAppWindowByIdentifier(windowState, windowTitle);

                if (foundWindow.isPresent()) {
                    titleFound = true;
                    if (isVisible(foundWindow.get())) {
                        return new Result(
                                true /* success */,
                                foundWindow.get().identifier.title + " is visible");
                    }
                }
            }

            String reason;
            if (!titleFound) {
                reason = windowTitle + " cannot be found";
            } else {
                reason = windowTitle + " is invisible";
            }
            return new Result(
                    false /* success */,
                    getTimestamp(),
                    "isNonAppWindowVisible" /* assertionName */,
                    reason);
        }

        private static boolean isVisible(WindowStateProto windowState) {
            return windowState.windowContainer.visible;
        }

        @Override
        public long getTimestamp() {
            return mProto.elapsedRealtimeNanos;
        }

        /** Returns window title of the top most visible app window. */
        private String getTopVisibleAppWindow() {
            List<WindowStateProto> windows = getWindows();
            if (windows.isEmpty()) {
                return "";
            }

            final String topVisible = getTopVisibleAppWindow(windows);
            if (topVisible != null) {
                return topVisible;
            }

            return "";
        }

        private String getTopVisibleAppWindow(List<WindowStateProto> windows) {
            for (WindowStateProto windowState : windows) {
                if (windowState.windowContainer.visible) {
                    return windowState.identifier.title;
                }
            }

            return null;
        }

        /** Checks if app window with {@code windowTitle} is on top. */
        public Result isVisibleAppWindowOnTop(String windowTitle) {
            String topAppWindow = getTopVisibleAppWindow();
            boolean success = topAppWindow.contains(windowTitle);
            String reason = "wanted=" + windowTitle + " found=" + topAppWindow;
            return new Result(success, getTimestamp(), "isAppWindowOnTop", reason);
        }

        /** Checks if app window with {@code windowTitle} is visible. */
        public Result isAppWindowVisible(String windowTitle) {
            final String assertionName = "isAppWindowVisible";
            boolean[] titleFound = { false };
            List<WindowStateProto> windows = getWindows();
            if (windows.isEmpty()) {
                return new Result(
                        false /* success */, getTimestamp(), assertionName, "No windows found");
            }

            final Result result = isAppWindowVisible(windowTitle, titleFound, windows);
            if (result != null) {
                return result;
            }

            String reason;
            if (!titleFound[0]) {
                reason = "Window " + windowTitle + " cannot be found";
            } else {
                reason = "Window " + windowTitle + " is invisible";
            }
            return new Result(false /* success */, getTimestamp(), assertionName, reason);
        }

        private Result isAppWindowVisible(
                String windowTitle, boolean[] titleFound, List<WindowStateProto> windows) {
            for (WindowStateProto windowState : windows) {
                if (windowState.identifier.title.contains(windowTitle)) {
                    titleFound[0] = true;
                    if (windowState.windowContainer.visible) {
                        return new Result(
                                true /* success */,
                                getTimestamp(),
                                "isAppWindowVisible" /* assertionName */,
                                "Window " + windowState.identifier.title + "is visible");
                    }
                }
            }

            return null;
        }
    }
}
