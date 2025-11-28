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

import android.aconfig.Aconfig;
import android.app.UiAutomation;
import android.os.flagging.AconfigPackage;
import android.os.flagging.AconfigStorageReadException;
import android.platform.test.flag.util.Flag;
import android.platform.test.flag.util.FlagReadException;
import android.provider.DeviceConfig;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.modules.utils.build.SdkLevel;

import com.google.common.base.CaseFormat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** A {@code IFlagsValueProvider} which provides flag values from device side. */
public class DeviceFlagsValueProvider implements IFlagsValueProvider {
    private static final String READ_DEVICE_CONFIG_PERMISSION =
            "android.permission.READ_DEVICE_CONFIG";
    private static final String TAG = "DeviceFlagsValue";

    private static final Set<String> VALID_BOOLEAN_VALUE = Set.of("true", "false");

    private static final Map<String, AconfigPackage> ACONFIG_PACKAGE = new ConcurrentHashMap<>();

    private static final Map<String, Aconfig.parsed_flag> ACONFIG_PB_FLAGS = new HashMap<>();

    private static final boolean IS_STATIC_ACONFIG_PB_ENABLED = loadAconfigPbFromTestResource();

    private static boolean loadAconfigPbFromTestResource() {
        try (InputStream stream =
                DeviceFlagsValueProvider.class.getClassLoader().getResourceAsStream("aconfig.pb")) {
            if (stream == null) {
                return false;
            }
            Aconfig.parsed_flags flags = Aconfig.parsed_flags.parseFrom(stream);
            for (Aconfig.parsed_flag flag : flags.getParsedFlagList()) {
                String fullFlagName =
                        String.format(
                                Flag.ACONFIG_FULL_FLAG_FORMAT, flag.getPackage(), flag.getName());
                ACONFIG_PB_FLAGS.put(fullFlagName, flag);
            }
            return true;
        } catch (IOException exception) {
            throw new FlagReadException(
                    "ALL_FLAGS", "Failed to read static flags from aconfig.pb", exception);
        }
    }

    private final UiAutomation mUiAutomation;

    public static CheckFlagsRule createCheckFlagsRule() {
        return new CheckFlagsRule(new DeviceFlagsValueProvider());
    }

    /**
     * Creates a {@link CheckFlagsRule} with an existing {@link UiAutomation} instance.
     *
     * <p>This is necessary if the test modifies {@link UiAutomation} flags.
     *
     * @param uiAutomation The {@link UiAutomation} used by the test.
     */
    public static CheckFlagsRule createCheckFlagsRule(UiAutomation uiAutomation) {
        return new CheckFlagsRule(new DeviceFlagsValueProvider(uiAutomation));
    }

    public DeviceFlagsValueProvider() {
        this(InstrumentationRegistry.getInstrumentation().getUiAutomation());
    }

    private DeviceFlagsValueProvider(UiAutomation uiAutomation) {
        mUiAutomation = uiAutomation;
    }

    @Override
    public boolean getBoolean(String flag) throws FlagReadException {
        Flag parsedFlag = Flag.createFlag(flag);
        if (parsedFlag.namespace() != null) {
            if (parsedFlag.packageName() != null) {
                throw new FlagReadException(
                        flag, "You can not specify the namespace for aconfig flags.");
            }
            return getLegacyFlagBoolean(parsedFlag);
        }
        if (parsedFlag.packageName() == null) {
            throw new FlagReadException(
                    flag, "Flag name is not the expected format {packageName}.{flagName}.");
        }

        if (IS_STATIC_ACONFIG_PB_ENABLED) {
            return getFlagBooleanViaAconfigPbOrPublicApi(parsedFlag);
        }

        // Fallback to the reflection
        String className = parsedFlag.flagsClassName();
        String simpleFlagName = parsedFlag.simpleFlagName();

        // Must be consistent with method name in aconfig auto generated code.
        String methodName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, simpleFlagName);

        try {
            Class<?> flagsClass = Class.forName(className);
            Method method = flagsClass.getMethod(methodName);
            Object result = method.invoke(null);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            throw new FlagReadException(
                    flag,
                    String.format(
                            "Flag type is %s, not boolean", result.getClass().getSimpleName()));
        } catch (ClassNotFoundException e) {
            throw new FlagReadException(
                    flag,
                    String.format(
                            "Can not load the Flags class %s to get its values. Please check the "
                                    + "flag name and ensure that the aconfig auto generated "
                                    + "library is in the dependency.",
                            className),
                    e);
        } catch (NoSuchMethodException e) {
            throw new FlagReadException(
                    flag,
                    String.format(
                            "No method %s in the Flags class to read the flag value. Please check"
                                    + " the flag name.",
                            methodName),
                    e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new FlagReadException(flag, e);
        }
    }

    private boolean getLegacyFlagBoolean(Flag flag) throws FlagReadException {
        mUiAutomation.adoptShellPermissionIdentity(READ_DEVICE_CONFIG_PERMISSION);
        String property = DeviceConfig.getProperty(flag.namespace(), flag.fullFlagName());
        mUiAutomation.dropShellPermissionIdentity();
        if (property == null) {
            throw new FlagReadException(flag.fullFlagName(), "Flag does not exist on the device.");
        }
        if (VALID_BOOLEAN_VALUE.contains(property)) {
            return Boolean.valueOf(property);
        }
        throw new FlagReadException(
                flag.fullFlagName(), String.format("Value %s is not a valid boolean.", property));
    }

    /**
     * Gets the boolean value for the given flag via aconfig.pb or public API.
     *
     * <p>This method first checks the flag's definition in the packaged aconfig.pb in test
     * resources. If the flag is not found, return false. Otherwise:
     *
     * <ul>
     *   <li>If the flag is read-only, the static value from aconfig.pb is returned.
     *   <li>If the flag is read-write, the value is fetched using the public aconfig API to ensure
     *       the most up-to-date state.
     * </ul>
     *
     * @param flag The {@link Flag} to get.
     * @return The boolean flag value.
     */
    private boolean getFlagBooleanViaAconfigPbOrPublicApi(Flag flag) {
        if (!ACONFIG_PB_FLAGS.containsKey(flag.fullFlagName())) {
            return false;
        }
        Aconfig.parsed_flag staticFlag = ACONFIG_PB_FLAGS.get(flag.fullFlagName());
        // If the flag is READ_ONLY, read the flag value from the aconfig.pb in test resources
        if (staticFlag.getPermission().equals(Aconfig.flag_permission.READ_ONLY)) {
            Log.d(
                    TAG,
                    String.format(
                            "Read flag value from test resources: %s - %s | %s",
                            flag.fullFlagName(),
                            staticFlag.getPermission(),
                            staticFlag.getState()));
            return staticFlag.getState().equals(Aconfig.flag_state.ENABLED);
        }
        // If the flag is READ_WRITE, read the flag value through public API
        return getFlagBooleanViaPublicApi(flag);
    }

    /**
     * Gets the boolean value for the given flag via public aconfig API.
     *
     * <p>Returns false if the public API is not supported or the flag is missing.
     *
     * @param flag The {@link Flag} to get.
     * @return The boolean flag value.
     */
    private boolean getFlagBooleanViaPublicApi(Flag flag) {
        if (!SdkLevel.isAtLeastB()) {
            return false;
        }

        AconfigPackage aconfigPackage =
                ACONFIG_PACKAGE.computeIfAbsent(
                        flag.packageName(),
                        p -> {
                            try {
                                return AconfigPackage.load(p);
                            } catch (AconfigStorageReadException e) {
                                Log.w(TAG, e);
                                return null;
                            }
                        });
        // If the flag package or the flag is missing, consider the flag as false
        // The test to test the flag off behavior should be executed
        if (aconfigPackage != null) {
            boolean value =
                    aconfigPackage.getBooleanFlagValue(flag.simpleFlagName(), false /* default */);
            Log.d(
                    TAG,
                    String.format(
                            "Read flag value from public API: %s: %s", flag.fullFlagName(), value));
            return value;
        }
        Log.d(
                TAG,
                String.format(
                        "Unable to find %s on device, treating as false", flag.fullFlagName()));
        return false;
    }
}
