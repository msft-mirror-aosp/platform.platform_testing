/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * Marks the type of test with purpose of evaluating security vulnerabilities.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SecurityTest {

    // Denotes the patch level when the test was introduced
    /** @deprecated @see android.platform.test.annotations.AsbSecurityTest */
    @Deprecated
    String minPatchLevel() default "";

    // Denotes the CVE ID(s), comma-separated, to which this test applies.
    /** @deprecated @see android.platform.test.annotations.AsbSecurityTest */
    @Deprecated
    String cve() default "";

    // Denotes the scope (platform/kernel/vendor) to which this test applies.
    /** @deprecated @see android.platform.test.annotations.AsbSecurityTest */
    @Deprecated
    String scope() default "";
}
