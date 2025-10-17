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

import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExecutableCommand {
    private final StringBuilder mCommand;

    public ExecutableCommand(String baseCommand) {
        mCommand = new StringBuilder(baseCommand);
    }

    public ExecutableCommand append(String s) {
        mCommand.append(s);
        return this;
    }

    public void execute(UiAutomation uiAutomation) {
        String command = mCommand.toString();

        //noinspection EmptyTryBlock
        try (ParcelFileDescriptor ignored = uiAutomation.executeShellCommand(command)) {

        } catch (IOException e) {
            throw new RuntimeException("IOException while executing command: " + command, e);
        }
    }

    public String executeWithOutput(UiAutomation uiAutomation) {
        String command = mCommand.toString();
        try (ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(command)) {
            try (FileInputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
                byte[] bytes = stream.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException while executing command: " + command, e);
        }
    }
}