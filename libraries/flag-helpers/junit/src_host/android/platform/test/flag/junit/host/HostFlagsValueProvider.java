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

package android.platform.test.flag.junit.host;

import android.aconfig.Aconfig;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.IFlagsValueProvider;
import android.platform.test.flag.util.Flag;
import android.platform.test.flag.util.FlagReadException;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.flag.AFlagsFeatureFlag;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** A {@code IFlagsValueProvider} which provides flag values from host side. */
public class HostFlagsValueProvider implements IFlagsValueProvider {
    /** The key is the device serial number. */
    private static final LoadingCache<String, DeviceFlags> CACHED_DEVICE_FLAGS;

    /** The key is the device serial number. */
    private static final Map<String, ITestDevice> TEST_DEVICES = new HashMap<>();

    /** The key is the test class code source location. */
    private static final LoadingCache<String, TestResourceFlags> CACHED_FLAGS_BY_TEST_RESOURCE;

    static {
        CacheLoader<String, DeviceFlags> deviceCacheLoader =
                new CacheLoader<>() {
                    @Override
                    public DeviceFlags load(String deviceSerial) throws FlagReadException {
                        if (!TEST_DEVICES.containsKey(deviceSerial)) {
                            throw new IllegalStateException(
                                    String.format(
                                            "No ITestDevice found for serial %s.", deviceSerial));
                        }
                        return DeviceFlags.createDeviceFlags(TEST_DEVICES.get(deviceSerial));
                    }
                };
        CACHED_DEVICE_FLAGS = CacheBuilder.newBuilder().build(deviceCacheLoader);

        CacheLoader<String, TestResourceFlags> staticFlagsCacheLoader =
                new CacheLoader<>() {
                    @Override
                    public TestResourceFlags load(String testClassLocation) {
                        return TestResourceFlags.createTestResourceFlags(testClassLocation);
                    }
                };
        CACHED_FLAGS_BY_TEST_RESOURCE = CacheBuilder.newBuilder().build(staticFlagsCacheLoader);
    }

    private final String mTestClassLocation;

    private final Supplier<ITestDevice> mTestDeviceSupplier;

    private DeviceFlags mDeviceFlags = null;

    private TestResourceFlags mTestResourceFlags = null;

    HostFlagsValueProvider(Supplier<ITestDevice> testDeviceSupplier, Class<?> testClass) {
        mTestDeviceSupplier = testDeviceSupplier;
        mTestClassLocation =
                testClass == null
                        ? ""
                        : testClass.getProtectionDomain().getCodeSource().getLocation().toString();
    }

    /** Creates CheckFlagsRule with flags from test device. */
    public static CheckFlagsRule createCheckFlagsRule(Supplier<ITestDevice> testDeviceSupplier) {
        return new CheckFlagsRule(new HostFlagsValueProvider(testDeviceSupplier, null));
    }

    /** Creates CheckFlagsRule with flags from test device and test class. */
    public static CheckFlagsRule createCheckFlagsRule(
            Supplier<ITestDevice> testDeviceSupplier, Class<?> testClass) {
        return new CheckFlagsRule(new HostFlagsValueProvider(testDeviceSupplier, testClass));
    }

    /**
     * Refreshes all flag values of a given device. Must be called when the device flags have been
     * changed.
     */
    public static void refreshFlagsCache(String serial) throws FlagReadException {
        Map<String, DeviceFlags> cachedDeviceFlagsMap = CACHED_DEVICE_FLAGS.asMap();
        if (cachedDeviceFlagsMap.containsKey(serial)) {
            cachedDeviceFlagsMap.get(serial).init(TEST_DEVICES.get(serial));
        }
    }

    @Override
    public void setUp() throws FlagReadException {
        try {
            if (mTestClassLocation.isEmpty()) {
                ITestDevice testDevice = mTestDeviceSupplier.get();
                TEST_DEVICES.put(testDevice.getSerialNumber(), testDevice);
                mDeviceFlags = CACHED_DEVICE_FLAGS.get(testDevice.getSerialNumber());
            } else {
                mTestResourceFlags = CACHED_FLAGS_BY_TEST_RESOURCE.get(mTestClassLocation);
            }
        } catch (ExecutionException e) {
            throw new FlagReadException("ALL_FLAGS", e);
        }
    }

    @Override
    public boolean getBoolean(String flag) throws FlagReadException {
        Flag parsedFlag = Flag.createFlag(flag);
        if (parsedFlag.packageName() != null
                && mTestResourceFlags != null
                && mTestResourceFlags.exists()) {
            Aconfig.parsed_flag staticFlag = mTestResourceFlags.getFlag(flag);
            if (staticFlag == null) {
                return false;
            }
            // If the flag is READ_ONLY, read the flag value from the static aconfig.pb
            if (staticFlag.getPermission().equals(Aconfig.flag_permission.READ_ONLY)) {
                LogUtil.CLog.i(
                        String.format(
                                "Read flag value from test resources: %s - %s | %s",
                                flag, staticFlag.getPermission(), staticFlag.getState()));
                return staticFlag.getState().equals(Aconfig.flag_state.ENABLED);
            }
            // If the flag is READ_WRITE, read the flag value from device through adb command
            return getFlagValueWithAdbCommand(parsedFlag.fullFlagName());
        }
        return getDeviceBoolean(flag);
    }

    private boolean getDeviceBoolean(String flag) throws FlagReadException {
        String value = mDeviceFlags.getFlagValue(flag);

        if (value == null) {
            LogUtil.CLog.i("Flag %s is not found, treating as false.", flag);
            return false;
        }

        if (!IFlagsValueProvider.isBooleanValue(value)) {
            throw new FlagReadException(
                    flag, String.format("Flag value %s is not a boolean", value));
        }
        return Boolean.parseBoolean(value);
    }

    private boolean getFlagValueWithAdbCommand(String flag) {
        try {
            CommandResult commandResult =
                    mTestDeviceSupplier
                            .get()
                            .executeAdbV2Command(
                                    String.format("su root aflags list | grep %s", flag));
            if (!commandResult.getStatus().equals(CommandStatus.SUCCESS)
                    || commandResult.getStdout() == null
                    || commandResult.getStdout().trim().isEmpty()) {
                LogUtil.CLog.i("Flag %s is not found on device, treating as false.", flag);
                return false;
            }
            AFlagsFeatureFlag targetFlag = new AFlagsFeatureFlag(commandResult.getStdout().trim());
            LogUtil.CLog.i("Read flag value from device: %s: %s", flag, targetFlag);
            return targetFlag.getCurrentState().equals(AFlagsFeatureFlag.State.ENABLED);
        } catch (DeviceNotAvailableException | IllegalArgumentException e) {
            throw new FlagReadException(flag, e);
        }
    }
}
