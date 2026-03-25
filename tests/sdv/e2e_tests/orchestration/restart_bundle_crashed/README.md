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

# SDV E2E Orchestration Restart Bundle Crashed Test

This test suite verifies the behavior of the platform after a bundle has crashed.
It includes different verifications, such as:

* Orchestrator gets the crash notification from LM after a bundle has crashed.

* A service bundle is restarted when setting any mode after it has crashed.
After setting any mode, the evaluation of each bundle state is triggered again and each bundle should be moved to the required state. This checks that the orchestrator always retrieves the state from LM as it's the source of truth of what the actual state of the bundles are.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Set Up

Test must be set up with default retries configuration by launching the VM with:

```bash
cvd create --extra_kernel_cmdline="androidboot.ro.boot.sdv.orchestrator.recovery.max_retries=2"
```

### Mobly

```
atest SdvE2EOrchestrationRestartBundleCrashedTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-e2e-orchestration-restart-bundle-crashed-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

* Name: `sdv/e2e/orchestration_restart_bundle_crashed_test`
