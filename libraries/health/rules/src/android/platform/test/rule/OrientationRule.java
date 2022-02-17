/*
 * Copyright (C) 2022 The Android Open Source Project
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
package android.platform.test.rule;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This rule will lock orientation before running a test class and unlock after. The orientation is
 * portrait by default, and landscape if the test or one of its superclasses is marked with
 * the @Landscape annotation.
 */
public class OrientationRule implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        final boolean landscape = hasLandscapeAnnotation(description.getTestClass());
        final TestRule orientationRule =
                landscape ? new LandscapeOrientationRule() : new NaturalOrientationRule();
        return orientationRule.apply(base, description);
    }

    private boolean hasLandscapeAnnotation(Class<?> testClass) {
        if (testClass == null) return false;
        if (testClass.isAnnotationPresent(Landscape.class)) return true;
        return hasLandscapeAnnotation(testClass.getSuperclass());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Landscape {}
}
