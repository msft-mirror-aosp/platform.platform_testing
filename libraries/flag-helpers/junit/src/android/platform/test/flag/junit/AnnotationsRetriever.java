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

package android.platform.test.flag.junit;

import android.platform.test.annotations.RequiresFlagsOff;
import android.platform.test.annotations.RequiresFlagsOn;

import org.junit.runner.Description;

import java.lang.annotation.Annotation;

import javax.annotation.Nullable;

/**
 * Retrieves feature flag related annotations from a given {@code Description}.
 *
 * <p>For each annotation, it trys to get it from the method first, then trys to get it from the
 * test class if the method has no such annotation.
 */
public class AnnotationsRetriever {
    /** Contains all feature flag related annotations. */
    public static class FlagAnnotations {
        /** Annotation for the flags that requires to be on. */
        @Nullable public final RequiresFlagsOn mRequiresFlagsOn;

        /** Annotation for the flags that requires to be off. */
        @Nullable public final RequiresFlagsOff mRequiresFlagsOff;

        FlagAnnotations(RequiresFlagsOn requiresFlagsOn, RequiresFlagsOff requiresFlagsOff) {
            mRequiresFlagsOn = requiresFlagsOn;
            mRequiresFlagsOff = requiresFlagsOff;
        }
    }

    /** Gets all feature flag related annotations. */
    public static FlagAnnotations getFlagAnnotations(Description description) {
        return new FlagAnnotations(
                getAnnotation(RequiresFlagsOn.class, description),
                getAnnotation(RequiresFlagsOff.class, description));
    }

    @Nullable
    private static <T extends Annotation> T getAnnotation(
            Class<T> annotationType, Description description) {
        T annotation = description.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        Class<?> testClass = description.getTestClass();
        if (testClass != null) {
            annotation = testClass.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    private AnnotationsRetriever() {}
}