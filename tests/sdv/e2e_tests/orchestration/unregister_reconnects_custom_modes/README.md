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

# SDV E2E Orchestration Unregisters Reconnects Custom Modes Test

## E2E

This test automates the scenario in which two VMs are sending custom modes through the [Custom Mode Sample](/system/software_defined_vehicle/samples/orchestration/custom_state/README.md), and one of the VM shutdowns and starts again (we test this by running `adb reboot`). There are different scenarios in which this could happen, for example if a VM crashes or after running updates, and the orchestrator needs to continue working properly after this.
In the test, the VM that has not died, should get a notification that the custom mode publisher has unregistered and, once the VM is back on, it should register again and custom modes should be sent/received by both of them.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
atest SdvE2EOrchestrationUnregistersReconnectsCustomModeTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-e2e-orchestration-unregisters-reconnects-custom-modes-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/e2e/orch_unregisters_reconnects_test`
