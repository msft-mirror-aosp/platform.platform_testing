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

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.platform.test.flag.util.FlagReadException;
import android.platform.test.flag.util.FlagSetException;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Sets;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A {@link TestRule} that helps to set flag values in unit test. */
public final class SetFlagsRule implements TestRule {
    private static final String FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME = "FakeFeatureFlagsImpl";
    private static final String FEATURE_FLAGS_CLASS_NAME = "FeatureFlags";
    private static final String FEATURE_FLAGS_FIELD_NAME = "FEATURE_FLAGS";
    private static final String FLAGS_CLASS_NAME = "Flags";
    private static final String FLAG_CONSTANT_PREFIX = "FLAG_";
    private static final String SET_FLAG_METHOD_NAME = "setFlag";
    private static final String RESET_ALL_METHOD_NAME = "resetAll";
    private static final String IS_FLAG_READ_ONLY_OPTIMIZED_METHOD_NAME = "isFlagReadOnlyOptimized";

    // Store instances for entire life of a SetFlagsRule instance
    private final Map<Class<?>, Object> mFlagsClassToFakeFlagsImpl = new HashMap<>();
    private final Map<Class<?>, Object> mFlagsClassToRealFlagsImpl = new HashMap<>();

    // Store classes that are currently mutated by this rule
    private final Set<Class<?>> mMutatedFlagsClasses = new HashSet<>();

    // Any flags added to this list cannot be set imperatively (i.e. with enableFlags/disableFlags)
    private final Set<String> mLockedFlagNames = new HashSet<>();

    // TODO(322377082): remove repackage prefix list
    private static final String[] REPACKAGE_PREFIX_LIST =
            new String[] {
                "", "com.android.internal.hidden_from_bootclasspath.",
            };
    private final Map<String, Set<String>> mPackageToRepackage = new HashMap<>();

    private final boolean mIsInitWithDefault;
    private FlagsParameterization mFlagsParameterization;
    private boolean mIsRuleEvaluating = false;

    private boolean mIsAtLeastV = isAtLeastV();

    public enum DefaultInitValueType {
        /**
         * Initialize flag value as null
         *
         * <p>Flag value need to be set before using
         */
        NULL_DEFAULT,

        /**
         * Initialize flag value with the default value from the device
         *
         * <p>If flag value is not overridden by adb, then the default value is from the release
         * configuration when the test is built.
         */
        DEVICE_DEFAULT,
    }

    public SetFlagsRule() {
        this(DefaultInitValueType.DEVICE_DEFAULT);
    }

    public SetFlagsRule(DefaultInitValueType defaultType) {
        this(defaultType, null);
    }

    public SetFlagsRule(
            DefaultInitValueType defaultType,
            @Nullable FlagsParameterization flagsParameterization) {
        mIsInitWithDefault = defaultType == DefaultInitValueType.DEVICE_DEFAULT;
        mFlagsParameterization = flagsParameterization;
        if (flagsParameterization != null) {
            mLockedFlagNames.addAll(flagsParameterization.mOverrides.keySet());
        }
    }

    /**
     * Set the FlagsParameterization to be used during this test. This cannot be used to override a
     * previous call, and cannot be called once the rule has been evaluated.
     */
    public void setFlagsParameterization(@Nonnull FlagsParameterization flagsParameterization) {
        Objects.requireNonNull(flagsParameterization, "FlagsParameterization cannot be cleared");
        if (mFlagsParameterization != null) {
            throw new AssertionError("FlagsParameterization cannot be overridden");
        }
        if (mIsRuleEvaluating) {
            throw new AssertionError("Cannot set FlagsParameterization once the rule is running");
        }
        ensureFlagsAreUnset();
        mFlagsParameterization = flagsParameterization;
        mLockedFlagNames.addAll(flagsParameterization.mOverrides.keySet());
    }

    /**
     * Enables the given flags.
     *
     * @param fullFlagNames The name of the flags in the flag class with the format
     *     {packageName}.{flagName}
     */
    public void enableFlags(String... fullFlagNames) {
        if (!mIsRuleEvaluating) {
            throw new IllegalStateException("Not allowed to set flags outside test and setup code");
        }
        for (String fullFlagName : fullFlagNames) {
            if (mLockedFlagNames.contains(fullFlagName)) {
                throw new FlagSetException(fullFlagName, "Not allowed to change locked flags");
            }
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
        if (!mIsRuleEvaluating) {
            throw new IllegalStateException("Not allowed to set flags outside test and setup code");
        }
        for (String fullFlagName : fullFlagNames) {
            if (mLockedFlagNames.contains(fullFlagName)) {
                throw new FlagSetException(fullFlagName, "Not allowed to change locked flags");
            }
            setFlagValue(fullFlagName, false);
        }
    }

    private void ensureFlagsAreUnset() {
        if (!mFlagsClassToFakeFlagsImpl.isEmpty()) {
            throw new IllegalStateException("Some flags were set before the rule was initialized");
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable throwable = null;
                try {
                    AnnotationsRetriever.FlagAnnotations flagAnnotations =
                            AnnotationsRetriever.getFlagAnnotations(description);
                    assertAnnotationsMatchParameterization(flagAnnotations, mFlagsParameterization);
                    flagAnnotations.assumeAllSetFlagsMatchParameterization(mFlagsParameterization);
                    if (mFlagsParameterization != null) {
                        ensureFlagsAreUnset();
                        for (Map.Entry<String, Boolean> pair :
                                mFlagsParameterization.mOverrides.entrySet()) {
                            setFlagValue(pair.getKey(), pair.getValue());
                        }
                    }
                    for (Map.Entry<String, Boolean> pair :
                            flagAnnotations.mSetFlagValues.entrySet()) {
                        setFlagValue(pair.getKey(), pair.getValue());
                    }
                    mLockedFlagNames.addAll(flagAnnotations.mRequiredFlagValues.keySet());
                    mLockedFlagNames.addAll(flagAnnotations.mSetFlagValues.keySet());
                    mIsRuleEvaluating = true;
                    base.evaluate();
                } catch (Throwable t) {
                    throwable = t;
                } finally {
                    mIsRuleEvaluating = false;
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

    private static void assertAnnotationsMatchParameterization(
            AnnotationsRetriever.FlagAnnotations flagAnnotations,
            FlagsParameterization parameterization) {
        if (parameterization == null) return;
        Set<String> parameterizedFlags = parameterization.mOverrides.keySet();
        Set<String> requiredFlags = flagAnnotations.mRequiredFlagValues.keySet();
        // Assert that NO Annotation-Required flag is in the parameterization
        Set<String> parameterizedAndRequiredFlags =
                Sets.intersection(parameterizedFlags, requiredFlags);
        if (!parameterizedAndRequiredFlags.isEmpty()) {
            throw new AssertionError(
                    "The following flags have required values (per @RequiresFlagsEnabled or"
                            + " @RequiresFlagsDisabled) but they are part of the"
                            + " FlagParameterization: "
                            + parameterizedAndRequiredFlags);
        }
    }

    private void setFlagValue(String fullFlagName, boolean value) {
        assumeTrue(
                "SetFlagsRule: Test related to aconfig flags can only be executed when SdkLevel >="
                        + " V",
                mIsAtLeastV);

        if (!fullFlagName.contains(".")) {
            throw new FlagSetException(
                    fullFlagName, "Flag name is not the expected format {packgeName}.{flagName}.");
        }
        // Get all packages containing Flags referencing the same fullFlagName.
        Set<String> packageSet = getPackagesContainsFlag(fullFlagName);

        for (String packageName : packageSet) {
            setFlagValue(Flag.createFlag(fullFlagName, packageName), value);
        }
    }

    private Set<String> getPackagesContainsFlag(String fullFlagName) {
        Flag flag = Flag.createFlag(fullFlagName);
        String packageName = flag.packageName();
        Set<String> packageSet = mPackageToRepackage.getOrDefault(packageName, new HashSet<>());

        if (!packageSet.isEmpty()) {
            return packageSet;
        }

        for (String prefix : REPACKAGE_PREFIX_LIST) {
            String repackagedName = String.format("%s%s", prefix, packageName);
            String flagClassName = String.format("%s.%s", repackagedName, FLAGS_CLASS_NAME);
            try {
                Class.forName(flagClassName, false, this.getClass().getClassLoader());
                packageSet.add(repackagedName);
            } catch (ClassNotFoundException e) {
                // Skip if the class is not found
                // An error will be thrown if no package containing flags referencing
                // the passed in flag
            }
        }
        mPackageToRepackage.put(packageName, packageSet);
        if (packageSet.isEmpty()) {
            throw new FlagSetException(
                    fullFlagName,
                    "Cannot find package containing Flags class referencing to this flag.");
        }
        return packageSet;
    }

    private void setFlagValue(Flag flag, boolean value) {

        Object fakeFlagsImplInstance = null;

        Class<?> flagsClass = getFlagClassFromFlag(flag);
        fakeFlagsImplInstance = getOrCreateFakeFlagsImp(flagsClass);

        if (!mMutatedFlagsClasses.contains(flagsClass)) {
            // Replace FeatureFlags in Flags class with FakeFeatureFlagsImpl
            replaceFlagsImpl(flagsClass, fakeFlagsImplInstance);
            mMutatedFlagsClasses.add(flagsClass);
        }

        // If the test is trying to set the flag value on a read_only flag in an optimized build
        // skip this test, since it is not a valid testing case
        // The reason for skipping instead of throwning error here is all read_write flag will be
        // change to read_only in the final release configuration. Thus the test could be executed
        // in other release configuration cases
        boolean isOptimized = verifyFlagReadOnlyAndOptimized(fakeFlagsImplInstance, flag);
        assumeFalse(
                String.format(
                        "Flag %s is read_only, and the code is optimized. "
                                + " The flag value should not be modified on this build"
                                + " Skip this test.",
                        flag.fullFlagName()),
                isOptimized);

        // Set desired flag value in the FakeFeatureFlagsImpl
        setFlagValueInFakeFeatureFlagsImpl(fakeFlagsImplInstance, flag, value);
    }

    private Class<?> getFlagClassFromFlag(Flag flag) {
        String className = flag.flagsClassName();
        Class<?> flagsClass = null;
        try {
            flagsClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new FlagSetException(
                    flag.fullFlagName(),
                    String.format(
                            "Can not load the Flags class %s to set its values. Please check the "
                                    + "flag name and ensure that the aconfig auto generated "
                                    + "library is in the dependency.",
                            className),
                    e);
        }
        return flagsClass;
    }

    private boolean getFlagValue(Object featureFlagsImpl, Flag flag) {
        // Must be consistent with method name in aconfig auto generated code.
        String methodName = getFlagMethodName(flag);
        String fullFlagName = flag.fullFlagName();

        try {
            Object result =
                    featureFlagsImpl.getClass().getMethod(methodName).invoke(featureFlagsImpl);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "Flag type is %s, not boolean", result.getClass().getSimpleName()));
        } catch (NoSuchMethodException e) {
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "No method %s in the Flags class %s to read the flag value. Please"
                                    + " check the flag name.",
                            methodName, featureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "Fail to get value of flag %s from instance %s",
                            fullFlagName, featureFlagsImpl.getClass().getName()),
                    e);
        }
    }

    private String getFlagMethodName(Flag flag) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, flag.simpleFlagName());
    }

    private void setFlagValueInFakeFeatureFlagsImpl(
            Object fakeFeatureFlagsImpl, Flag flag, boolean value) {
        String fullFlagName = flag.fullFlagName();
        try {
            fakeFeatureFlagsImpl
                    .getClass()
                    .getMethod(SET_FLAG_METHOD_NAME, String.class, boolean.class)
                    .invoke(fakeFeatureFlagsImpl, fullFlagName, value);
        } catch (NoSuchMethodException e) {
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Flag implementation %s is not fake implementation",
                            fakeFeatureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagSetException(fullFlagName, e);
        }
    }

    private boolean verifyFlagReadOnlyAndOptimized(Object fakeFeatureFlagsImpl, Flag flag) {
        String fullFlagName = flag.fullFlagName();
        try {
            boolean result =
                    (Boolean)
                            fakeFeatureFlagsImpl
                                    .getClass()
                                    .getMethod(
                                            IS_FLAG_READ_ONLY_OPTIMIZED_METHOD_NAME, String.class)
                                    .invoke(fakeFeatureFlagsImpl, fullFlagName);
            return result;
        } catch (NoSuchMethodException e) {
            // If the flag is generated under exported mode, then it doesn't have this method
            if (fakeFeatureFlagsImpl
                    .getClass()
                    .getSimpleName()
                    .equals(FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME)) {
                return false;
            }
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Cannot check whether flag is optimized. "
                                    + "Flag implementation %s is not fake implementation",
                            fakeFeatureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagSetException(fullFlagName, e);
        }
    }

    @Nonnull
    private Object getOrCreateFakeFlagsImp(Class<?> flagsClass) {
        Object fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);
        if (fakeFlagsImplInstance != null) {
            return fakeFlagsImplInstance;
        }

        String packageName = flagsClass.getPackageName();
        String fakeClassName =
                String.format("%s.%s", packageName, FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME);
        String interfaceName = String.format("%s.%s", packageName, FEATURE_FLAGS_CLASS_NAME);

        Field featureFlagsField = getFeatureFlagsField(flagsClass);
        featureFlagsField.setAccessible(true);
        Object realFlagsImplInstance = null;
        try {
            realFlagsImplInstance = featureFlagsField.get(null);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot get FeatureFlagsImpl from Flags class %s.",
                            flagsClass.getName()),
                    e);
        }
        mFlagsClassToRealFlagsImpl.put(flagsClass, realFlagsImplInstance);

        try {
            Class<?> flagImplClass = Class.forName(fakeClassName);
            Class<?> flagInterface = Class.forName(interfaceName);
            fakeFlagsImplInstance = flagImplClass
                .getConstructor(flagInterface)
                .newInstance(mIsInitWithDefault ? realFlagsImplInstance : null);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot create FakeFeatureFlagsImpl in Flags class %s.",
                            flagsClass.getName()),
                    e);
        }

        mFlagsClassToFakeFlagsImpl.put(flagsClass, fakeFlagsImplInstance);

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
                            flagsImplInstance.getClass().getName()),
                    e);
        }
    }

    private Field getFeatureFlagsField(Class<?> flagsClass) {
        Field featureFlagsField = null;
        try {
            featureFlagsField = flagsClass.getDeclaredField(FEATURE_FLAGS_FIELD_NAME);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot store FeatureFlagsImpl in Flag %s.", flagsClass.getName()),
                    e);
        }
        return featureFlagsField;
    }

    private void resetFlags() {
        String flagsClassName = null;
        try {
            for (Class<?> flagsClass : mMutatedFlagsClasses) {
                flagsClassName = flagsClass.getName();
                Object fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);
                Object flagsImplInstance = mFlagsClassToRealFlagsImpl.get(flagsClass);
                // Replace FeatureFlags in Flags class with real FeatureFlagsImpl
                replaceFlagsImpl(flagsClass, flagsImplInstance);
                fakeFlagsImplInstance
                        .getClass()
                        .getMethod(RESET_ALL_METHOD_NAME)
                        .invoke(fakeFlagsImplInstance);
            }
            mMutatedFlagsClasses.clear();
        } catch (Exception e) {
            throw new FlagSetException(flagsClassName, e);
        }
    }

    // TODO(340230814): do not use reflection to check the sdk version
    private static boolean isAtLeastV() {
        String androidOsVersionClass = "android.os.Build$VERSION";
        String versionField = "SDK_INT";
        int v_sdk_level = 35;
        try {
            Class<?> versionClass = Class.forName(androidOsVersionClass);
            int version = (Integer) versionClass.getDeclaredField(versionField).get(null);
            return version >= v_sdk_level;
        } catch (ClassNotFoundException e) {
            // this tool is running on host side code or ravenwood tests
            // skip the version check
            return true;
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException("Cannot found field SDK_INT", e);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Fail to get sdk level", e);
        }
    }

    private static class Flag {
        private static final String PACKAGE_NAME_SIMPLE_NAME_SEPARATOR = ".";
        private final String mFullFlagName;
        private final String mPackageName;
        private final String mSimpleFlagName;

        public static Flag createFlag(String fullFlagName) {
            int index = fullFlagName.lastIndexOf(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR);
            String packageName = fullFlagName.substring(0, index);
            return createFlag(fullFlagName, packageName);
        }

        public static Flag createFlag(String fullFlagName, String packageName) {
            if (!fullFlagName.contains(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR)
                    || !packageName.contains(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Flag %s is invalid. The format should be {packageName}"
                                        + ".{simpleFlagName}",
                                fullFlagName));
            }
            int index = fullFlagName.lastIndexOf(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR);
            String simpleFlagName = fullFlagName.substring(index + 1);

            return new Flag(fullFlagName, packageName, simpleFlagName);
        }

        private Flag(String fullFlagName, String packageName, String simpleFlagName) {
            this.mFullFlagName = fullFlagName;
            this.mPackageName = packageName;
            this.mSimpleFlagName = simpleFlagName;
        }

        public String fullFlagName() {
            return mFullFlagName;
        }

        public String packageName() {
            return mPackageName;
        }

        public String simpleFlagName() {
            return mSimpleFlagName;
        }

        public String flagsClassName() {
            return String.format("%s.%s", mPackageName, FLAGS_CLASS_NAME);
        }
    }
}
