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
# Long Running: Example

## Overview

These tests are the baseline tests that run for 10 hours. These tests ensure that the configuration for long-term tests work as expected and can serve as reference to create SDV long-running tests

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

**WARNING:** these are long running tests that takes at least 10 hours to finish so keep that in mind before running locally or remotely.

### One VM

#### Mobly

```
atest SdvOneDeviceLongRunningExampleTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-long-running-example-test-1vm --{device1}serial 0.0.0.0:6520
```

### Two VMs

#### Mobly

```
atest SdvTwoDevicesLongRunningExampleTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run sdv-long-running-example-test-2vm --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

### Three VMs

#### Mobly

```
atest SdvThreeDevicesLongRunningExampleTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521,0.0.0.0:6522 ./tools/catbox-tradefed run sdv-long-running-example-test-3vm --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --{device3}serial 0.0.0.0:6522
```

## Tests name in CI/CD

- Name: `sdv/long_running_tests/long_running_example_test`
