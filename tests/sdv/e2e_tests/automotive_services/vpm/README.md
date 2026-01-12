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

# VPM current power state persistency test

## About

This test that VPM agent correctly persists power state to disk when its internal state changes,
and furthermore start up in the persisted power state, in case that it has crashed.

NOTE: System state under test never happens in production: on first VPM agent crash,
HM reports Unhealthy VM and OEM restarts VM. Scenario however still tested, as feature already
exists in VPM. Agent crash recovery strategies might be rescoped post-Concorde

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
atest SdvVpmPowerStatePersistenceTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-vpm-power-state-persistence-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/e2e/sdv-vpm-power-state-persistence-test`
// IMPORTANT: This exact name is used to run the test in our CI/CD pipeline (ATP) and is defined in our g3 config.
