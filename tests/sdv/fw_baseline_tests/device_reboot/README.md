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
# Baseline: Device Reboot Tests

## Overview

These tests reboot the devices and verifies its availability and we run them iteratively for 100 times.

> **Note:** this test is created to verify robustness setups where a test needs to run many times and rebooting in each one of them.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

**WARNING:** these tests is meant to run for 100 iterations.

> **Note:** to run the `atest` command iteratively use the `--iterations` flag, (e.g. `atest <MODULE> --iterations 100`)

> **Note:** it is not possible to run the CATBox command iteratively locally, you can only do that on CI/CD.

### One VM

#### Atest

```
atest SdvFWBaselineOneDeviceRebootTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-fw-baseline-device-reboot-one-vm-test --{device1}serial 0.0.0.0:6520
```

### Two VMs

#### Atest

```
atest SdvFWBaselineTwoDevicesRebootTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-fw-baseline-device-reboot-two-vms-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

### Three VMs

#### Atest

```
atest SdvFWBaselineThreeDevicesRebootTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521,0.0.0.0:6522 ./tools/catbox-tradefed run commandAndExit sdv-fw-baseline-device-reboot-three-vms-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --{device3}serial 0.0.0.0:6522
```

## Tests name in CI/CD

- Name: `sdv/long_running/tfw_baseline/reboot_device_test`
