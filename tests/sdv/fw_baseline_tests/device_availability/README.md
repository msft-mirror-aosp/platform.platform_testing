<!--
  Copyright (C) 2026 The Android Open Source Project
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
      http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Device Availability tests

Created by EngProd team to verify the infrastructure is stable.

Used to verify the SDV Test Framework infrastructure is working as expected:

1. Ensure devices are online and have booted successfullt.
2. Ensure devices have been initialized with the right boot arguments.
3. Verify logcat is available after rebooting.

**Important:** If these tests fail, there is something wrong in the infrastructure or setup
that need to be addressed.

## How to run locally?

Environment and devices must be started before running the tests. Launch SDV
environment. For `sdv_core_cf-trunk_staging-userdebug`:

```bash
. build/envsetup.sh
lunch sdv_core_cf-trunk_staging-userdebug
```

The test also verifies that the VMs are running in an auth mesh, so ensure to
create them in the expected way:

### One VM

```
sdv-cf create --preprovisioned=etc --instance_name=instance1
```

#### Atest

```
atest SdvOneDeviceAvailabilityTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-one-device-availability-test.xml --{device1}serial 0.0.0.0:6520
```

### Two VMs

```
sdv-cf create --preprovisioned=etc --instance_name=instance1

sdv-cf create --preprovisioned=etc --instance_name=instance2
```

#### Atest

```
atest SdvTwoDevicesAvailabilityTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-two-devices-availability-test.xml --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

### Three VMs

**Note:** This test has been created to extend the SDV infrastructure but it is
not running in CI/CD yet.

```
sdv-cf create --preprovisioned=etc --instance_name=instance1

sdv-cf create --preprovisioned=etc --instance_name=instance2

sdv-cf create --preprovisioned=etc --instance_name=instance3
```

#### Atest

```
atest SdvThreeDevicesAvailabilityTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521,0.0.0.0:6522 out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-three-devices-availability-test.xml --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --{device3}serial 0.0.0.0:6522
```

## Test name in CI/CD

- `sdv/tfw_baseline/one_device_availability_test`
- `sdv/tfw_baseline/two_devices_availability_test`