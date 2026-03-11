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

# SDV Recovery E2E Tests

Set of end-to-end tests that verify VM recovery scenarios.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

## Recovery Critical Services E2E Test

Service Discovery, Data Tunnel, and Health Monitor are critical SDV agents. If any of them fails, no VM health reports should be sent. This serves as a signal to OEM to restart a VM to get it in a working state again. The test kills those agents, checks that health reports are not sent anymore, restarts VM, and controls that the VM is again in a healthy state.

### Mobly

```bash
atest SdvE2ERecoveryCriticalServicesTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-e2e-recovery-critical-services-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

Name: `sdv/e2e/recovery_critical_services_test`
