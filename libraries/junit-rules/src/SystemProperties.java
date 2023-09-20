/*
 * Copyright (C) 2023 The Android Open Source Project
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
package android.platform.test.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** Utility class for retrieving Android properties from a running device. */
public class SystemProperties {
    /**
     * Retrieve the value of the specified Android property.
     *
     * @param name The name of the property to retrieve.
     * @return The value of the property, or the empty string if it's not set.
     */
    public static String get(String name) {
        Process process = null;
        BufferedReader reader = null;

        try {
            process =
                    new ProcessBuilder()
                            .command("/system/bin/getprop", name)
                            .redirectErrorStream(true)
                            .start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if (line == null) {
                line = "";
            }
            return line;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }
}
