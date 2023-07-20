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

package android.platform.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a specific test or class should be run on certain feature flag enabled.
 *
 * <p>With {@code CheckFlagsRule}, test(s) will be skipped with 'assumption failed' when any of the
 * required flag on the target Android platform is disabled.
 *
 * <p>If {@code RequiresFlagsEnabled} is applied at both the class and test method, the test method
 * annotation takes precedence, and the class level {@code RequiresFlagsEnabled} is ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresFlagsEnabled {
    /**
     * The list of the feature flags that require to be enabled. Each item is the full flag name
     * with the format {package_name}.{flag_name}.
     */
    String[] value();
}
