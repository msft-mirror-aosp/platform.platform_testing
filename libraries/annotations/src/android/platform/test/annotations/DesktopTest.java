/*
 * Copyright (C) 2024 The Android Open Source Project
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
 * Marks the type of test with purpose of asserting Desktop requirements and cujs.
 *
 * <p>This annotation is used to categorize tests that are relevant to Desktop requirements and
 * Customer Usage Journeys (CUJs).
 *
 * <p>The {@code requirements} member is the list of Desktop requirements.
 *
 * <p>The {@code cujs} member is the list of Desktop CUJs.
 *
 * <p>The {@code manual} member is the list of manual tests that this test case provides full or
 * partial coverage for. This is used to track the automation progress of manual tests.
 *
 * <p>The {@code alpeid} (context: b/447658460) member is the list of alpe ids that this test case
 * provides coverage for. To override the test count, an entry of the form "test_count:N" can be
 * added, where N is the number of tests. This is intended for generator functions that generate
 * multiple tests but are counted as one.
 *
 * <p>Example with alpeid and test_count override:
 *
 * <pre>
 *   &#64;DesktopTest(alpeid = {"gpu-1", "gpu-2", "test_count:5"})
 *   public void testSomething() { ... }
 * </pre>
 *
 * <p>Example with alpeid without test_count override:
 *
 * <pre>
 *   &#64;DesktopTest(alpeid = {"gpu-1", "gpu-2"})
 *   public void testAnotherThing() { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DesktopTest {
    String[] requirements() default {""};

    String[] cujs() default {""};

    String[] manual() default {""};

    String[] alpeid() default {""};
}
