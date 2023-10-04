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

import android.platform.test.flag.util.Flag;
import android.platform.test.flag.util.FlagSetException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** A {@link TestRule} that helps to set flag values in unit test. */
public final class SetFlagsRule implements TestRule {
    private static final String FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME = "FakeFeatureFlagsImpl";
    private static final String FEATURE_FLAGS_FIELD_NAME = "FEATURE_FLAGS";
    private static final String SET_FLAG_METHOD_NAME = "setFlag";
    private static final String RESET_ALL_METHOD_NAME = "resetAll";

    private final Map<Class<?>, Object> mFlagsClassToFakeFlagsImpl = new HashMap<>();
    private final Map<Class<?>, Object> mFlagsClassToRealFlagsImpl = new HashMap<>();

    /**
     * Enables the given flags.
     *
     * @param fullFlagNames The name of the flags in the flag class with the format
     *     {packageName}.{flagName}
     */
    public void enableFlags(String... fullFlagNames) {
        for (String fullFlagName : fullFlagNames) {
            setFlagValue(fullFlagName, true);
        }
    }

    /**
     * Disables the given flags.
     *
     * @param fullFlagNames The name of the flags in the flag class with the format
     *     {packageName}.{flagName}
     */
    public void disableFlags(String... fullFlagNames) {
        for (String fullFlagName : fullFlagNames) {
            setFlagValue(fullFlagName, false);
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable throwable = null;
                try {
                    base.evaluate();
                } catch (Throwable t) {
                    throwable = t;
                } finally {
                    try {
                        resetFlags();
                    } catch (Throwable t) {
                        if (throwable != null) {
                            t.addSuppressed(throwable);
                        }
                        throwable = t;
                    }
                }
                if (throwable != null) throw throwable;
            }
        };
    }

    private void setFlagValue(String fullFlagName, boolean value) {
        if (!fullFlagName.contains(".")) {
            throw new FlagSetException(
                    fullFlagName, "Flag name is not the expected format {packgeName}.{flagName}.");
        }
        Flag flag = Flag.createFlag(fullFlagName);
        String className = flag.flagsClassName();
        Object fakeFlagsImplInstance = null;
        try {
            Class<?> flagsClass = Class.forName(className);
            fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);

            if (fakeFlagsImplInstance == null) {
                // Create a new FakeFeatureFlagsImpl instance
                fakeFlagsImplInstance = createFakeFlagsImplInstance(flagsClass);
                mFlagsClassToFakeFlagsImpl.put(flagsClass, fakeFlagsImplInstance);

                // Store the real FeatureFlagsImpl instance
                Field featureFlagsField = getFeatureFlagsField(flagsClass);
                featureFlagsField.setAccessible(true);
                mFlagsClassToRealFlagsImpl.put(flagsClass, featureFlagsField.get(null));
            }

            // Set desired flag value in the FakeFeatureFlagsImpl
            fakeFlagsImplInstance
                    .getClass()
                    .getMethod(SET_FLAG_METHOD_NAME, String.class, boolean.class)
                    .invoke(fakeFlagsImplInstance, fullFlagName, value);

            // Replace FeatureFlags in Flags class with FakeFeatureFlagsImpl
            replaceFlagsImpl(flagsClass, fakeFlagsImplInstance);
        } catch (ClassNotFoundException e) {
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Can not load the Flags class %s to set its values. Please check the "
                                    + "flag name and ensure that the aconfig auto generated "
                                    + "library is in the dependency.",
                            className));
        } catch (NoSuchMethodException e) {
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Flag implementation %s are not generated by test mode",
                            fakeFlagsImplInstance.getClass().getName()));
        } catch (ReflectiveOperationException e) {
            throw new FlagSetException(fullFlagName, e);
        } catch (IllegalArgumentException e) {
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Flags class %s doesn't have flag %s"
                                    + "Flag name is expected in format {packgeName}.{flagName}.",
                            className, fullFlagName));
        }
    }

    private Object createFakeFlagsImplInstance(Class<?> flagsClass) {
        String packageName = flagsClass.getPackageName();
        String className = String.format("%s.%s", packageName, FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME);
        Object fakeFlagsImplInstance = null;

        try {
            Class<?> flagImplClass = Class.forName(className);
            fakeFlagsImplInstance = flagImplClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot create FakeFeatureFlagsImpl in Flag %s.",
                            flagsClass.getName()));
        }

        return fakeFlagsImplInstance;
    }

    private void replaceFlagsImpl(Class<?> flagsClass, Object flagsImplInstance) {
        Field featureFlagsField = getFeatureFlagsField(flagsClass);
        featureFlagsField.setAccessible(true);
        try {
            featureFlagsField.set(null, flagsImplInstance);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot replace FeatureFlagsImpl to %s.",
                            flagsImplInstance.getClass().getName()));
        }
    }

    private Field getFeatureFlagsField(Class<?> flagsClass) {
        Field featureFlagsField = null;
        try {
            featureFlagsField = flagsClass.getDeclaredField(FEATURE_FLAGS_FIELD_NAME);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot store FeatureFlagsImpl in Flag %s.", flagsClass.getName()));
        }
        return featureFlagsField;
    }

    private void resetFlags() {
        Class<?> flagsClass = null;
        try {
            for (Class<?> key : mFlagsClassToFakeFlagsImpl.keySet()) {
                flagsClass = key;
                // Reset all flags in FakeFeatureFlagsImpl to null
                Object fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);
                fakeFlagsImplInstance
                        .getClass()
                        .getMethod(RESET_ALL_METHOD_NAME)
                        .invoke(fakeFlagsImplInstance);

                // Replace FeatureFlags in Flags class with FeatureFlagsImpl
                Object flagsImplInstance = mFlagsClassToRealFlagsImpl.get(flagsClass);
                replaceFlagsImpl(flagsClass, flagsImplInstance);
            }
        } catch (Exception e) {
            throw new FlagSetException(flagsClass.getName(), e);
        }
    }
}
