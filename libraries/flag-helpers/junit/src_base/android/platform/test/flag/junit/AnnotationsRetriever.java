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

import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;

import org.junit.Test;
import org.junit.runner.Description;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Retrieves feature flag related annotations from a given {@code Description}.
 *
 * <p>For each annotation, it trys to get it from the method first, then trys to get it from the
 * test class if the method has no such annotation.
 */
public class AnnotationsRetriever {
    private static final Set<Class<?>> KNOWN_UNRELATED_ANNOTATIONS =
            Set.of(Test.class, Retention.class, Target.class, Documented.class);

    private AnnotationsRetriever() {}

    /** Gets all feature flag related annotations. */
    public static FlagAnnotations getFlagAnnotations(Description description) {
        return new FlagAnnotations(
                getAnnotation(RequiresFlagsEnabled.class, description),
                getAnnotation(RequiresFlagsDisabled.class, description));
    }

    @Nullable
    private static <T extends Annotation> T getAnnotation(
            Class<T> annotationType, Description description) {
        T annotation = getAnnotation(annotationType, description.getAnnotations());
        if (annotation != null) {
            return annotation;
        }
        Class<?> testClass = description.getTestClass();
        return testClass == null
                ? null
                : getAnnotation(annotationType, List.of(testClass.getAnnotations()));
    }

    @Nullable
    private static <T extends Annotation> T getAnnotation(
            Class<T> annotationType, Collection<Annotation> annotations) {
        List<T> results = new ArrayList<>();
        Queue<Annotation> annotationQueue = new ArrayDeque<>();
        Set<Class<? extends Annotation>> visitedAnnotations = new HashSet<>();
        annotationQueue.addAll(annotations);
        while (!annotationQueue.isEmpty()) {
            Annotation annotation = annotationQueue.poll();
            Class<? extends Annotation> currentAnnotationType = annotation.annotationType();
            if (currentAnnotationType.equals(annotationType)) {
                results.add((T) annotation);
            } else if (!KNOWN_UNRELATED_ANNOTATIONS.contains(currentAnnotationType)
                    && !visitedAnnotations.contains(currentAnnotationType)) {
                annotationQueue.addAll(List.of(annotation.annotationType().getAnnotations()));
                visitedAnnotations.add(currentAnnotationType);
            }
        }

        if (results.size() > 1) {
            throw new RuntimeException(
                    String.format(
                            "Annotation %s has been specified multiple time: %s",
                            annotationType, results));
        }
        return results.isEmpty() ? null : results.get(0);
    }

    /** Contains all feature flag related annotations. */
    public static class FlagAnnotations {
        /** Annotation for the flags that requires to be enabled. */
        @Nullable public final RequiresFlagsEnabled mRequiresFlagsEnabled;

        /** Annotation for the flags that requires to be disabled. */
        @Nullable public final RequiresFlagsDisabled mRequiresFlagsDisabled;

        FlagAnnotations(
                RequiresFlagsEnabled requiresFlagsEnabled,
                RequiresFlagsDisabled requiresFlagsDisabled) {
            mRequiresFlagsEnabled = requiresFlagsEnabled;
            mRequiresFlagsDisabled = requiresFlagsDisabled;
        }
    }
}
