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

# SDV End to End (E2E) Update Manager Agent Test Suite

Each test is contained within its own test class. See `Android.bp` for a complete list of tets.

These tests run through several update scenarios for both service bundle and system updates.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

NOTE: Tests should be run from the Android project root because they require access to build artifacts in the `out` folder

### Mobly

To run an individual test case, execute it with `atest`. Insert the proper test name from `Android.bp`:

```
m dist
atest <name> -- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_one_device_local_only_config.yaml
```

To run all tests at once, execute the helper script `run_all.sh`

```run_all.sh atest```

### CATBox

To run an individual test case, execute it with `catbox`. Insert the proper test name from the list below:

```
m dist
m catbox
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit <name> --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_local_only_config.yaml
```

CATBox Test name list:

sdv-e2e-um-dropped-subscriber-test
sdv-e2e-um-service-bundle-activate-post-reboot-rollback-test
sdv-e2e-um-service-bundle-activate-pre-reboot-rollback-test
sdv-e2e-um-service-bundle-commit-and-uninstall-test
sdv-e2e-um-service-bundle-prepare-rollback-test
sdv-e2e-um-service-bundle-rollback-triggered-from-too-many-reboots-test
sdv-e2e-um-system-activate-post-reboot-rollback-test
sdv-e2e-um-system-activate-pre-reboot-rollback-test
sdv-e2e-um-system-commit-prepare-arguments-supplied-test
sdv-e2e-um-system-commit-test
sdv-e2e-um-system-prepare-cancel-test
sdv-e2e-um-system-prepare-rollback-test
sdv-e2e-um-system-prepare-suspend-reboot-resume-test
sdv-e2e-um-system-prepare-suspend-resume-test
sdv-e2e-um-system-prepare-suspend-rollback-test
sdv-e2e-um-system-prepare-suspend-vpm-test
sdv-e2e-um-system-rollback-triggered-from-too-many-reboots-test

To run all tests at once, execute the helper script `run_all.sh`

```run_all.sh catbox```

## CI/CD execution

- Name: `sdv/e2e/update_manager_agent_test`

