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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;

import com.google.auto.value.AutoAnnotation;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Annotation;

@RunWith(JUnit4.class)
public class AnnotationsRetrieverTest {
    static class TestClassHasNoAnnotation {}

    @RequiresFlagsEnabled({"flag1", "flag2"})
    static class TestClassHasRequiresFlagsEnabled {
    }

    @RequiresFlagsDisabled({"flag3", "flag4"})
    static class TestClassHasRequiresFlagsDisabled {
    }

    @RequiresFlagsEnabled({"flag1", "flag2"})
    @RequiresFlagsDisabled({"flag3", "flag4"})
    static class TestClassHasAllAnnotations {}

    private final RequiresFlagsEnabled mRequiresFlagsEnabled =
            createRequiresFlagsEnabled(new String[]{"flag5"});

    private final RequiresFlagsDisabled mRequiresFlagsDisabled =
            createRequiresFlagsDisabled(new String[]{"flag6"});

    @Test
    public void getFlagAnnotations_noAnnotation() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(TestClassHasNoAnnotation.class);

        assertNull(flagAnnotations.mRequiresFlagsEnabled);
        assertNull(flagAnnotations.mRequiresFlagsDisabled);
    }

    @Test
    public void getFlagAnnotations_oneAnnotationFromMethod() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations1 =
                getFlagAnnotations(TestClassHasRequiresFlagsEnabled.class, mRequiresFlagsEnabled);
        AnnotationsRetriever.FlagAnnotations flagAnnotations2 =
                getFlagAnnotations(TestClassHasRequiresFlagsDisabled.class, mRequiresFlagsDisabled);

        assertNull(flagAnnotations1.mRequiresFlagsDisabled);
        assertEquals(
                flagAnnotations1.mRequiresFlagsEnabled,
                createRequiresFlagsEnabled(new String[]{"flag5"}));
        assertNull(flagAnnotations2.mRequiresFlagsEnabled);
        assertEquals(
                flagAnnotations2.mRequiresFlagsDisabled,
                createRequiresFlagsDisabled(new String[]{"flag6"}));
    }

    @Test
    public void getFlagAnnotations_oneAnnotationFromClass() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations1 =
                getFlagAnnotations(TestClassHasRequiresFlagsEnabled.class);
        AnnotationsRetriever.FlagAnnotations flagAnnotations2 =
                getFlagAnnotations(TestClassHasRequiresFlagsDisabled.class);

        assertNull(flagAnnotations1.mRequiresFlagsDisabled);
        assertEquals(
                flagAnnotations1.mRequiresFlagsEnabled,
                createRequiresFlagsEnabled(new String[]{"flag1", "flag2"}));
        assertNull(flagAnnotations2.mRequiresFlagsEnabled);
        assertEquals(
                flagAnnotations2.mRequiresFlagsDisabled,
                createRequiresFlagsDisabled(new String[]{"flag3", "flag4"}));
    }

    @Test
    public void getFlagAnnotations_twoAnnotationsFromMethod() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(
                        TestClassHasAllAnnotations.class,
                        mRequiresFlagsEnabled,
                        mRequiresFlagsDisabled);

        assertEquals(
                flagAnnotations.mRequiresFlagsEnabled,
                createRequiresFlagsEnabled(new String[]{"flag5"}));
        assertEquals(
                flagAnnotations.mRequiresFlagsDisabled,
                createRequiresFlagsDisabled(new String[]{"flag6"}));
    }

    @Test
    public void getFlagAnnotations_twoAnnotationsFromClass() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(TestClassHasAllAnnotations.class);

        assertEquals(
                flagAnnotations.mRequiresFlagsEnabled,
                createRequiresFlagsEnabled(new String[]{"flag1", "flag2"}));
        assertEquals(
                flagAnnotations.mRequiresFlagsDisabled,
                createRequiresFlagsDisabled(new String[]{"flag3", "flag4"}));
    }

    @Test
    public void getFlagAnnotations_twoAnnotationsFromMethodAndClass() {
        AnnotationsRetriever.FlagAnnotations flagAnnotations =
                getFlagAnnotations(TestClassHasAllAnnotations.class, mRequiresFlagsEnabled);

        assertEquals(
                flagAnnotations.mRequiresFlagsEnabled,
                createRequiresFlagsEnabled(new String[]{"flag5"}));
        assertEquals(
                flagAnnotations.mRequiresFlagsDisabled,
                createRequiresFlagsDisabled(new String[]{"flag3", "flag4"}));
    }

    private AnnotationsRetriever.FlagAnnotations getFlagAnnotations(
            Class<?> testClass, Annotation... annotations) {
        Description description =
                Description.createTestDescription(testClass, "testMethod", annotations);
        return AnnotationsRetriever.getFlagAnnotations(description);
    }

    @AutoAnnotation
    private static RequiresFlagsEnabled createRequiresFlagsEnabled(String[] value) {
        return new AutoAnnotation_AnnotationsRetrieverTest_createRequiresFlagsEnabled(value);
    }

    @AutoAnnotation
    private static RequiresFlagsDisabled createRequiresFlagsDisabled(String[] value) {
        return new AutoAnnotation_AnnotationsRetrieverTest_createRequiresFlagsDisabled(value);
    }
}
