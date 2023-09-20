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

import java.util.function.Supplier;

/**
 * A condition for the {@link ConditionalIgnoreRule} which will return true if the property
 * 'debug.portrait' is present. CATBox will set this property if the device's screen resolution is
 * taller than it is wide.
 */
public class IgnoreOnPortrait implements Supplier<Boolean> {
    @Override
    public Boolean get() {
        return !SystemProperties.get("debug.portrait").isEmpty();
    }
}
