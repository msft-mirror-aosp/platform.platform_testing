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

# SDV Recovery Service Bundles Hm and Orch E2E Tests

Set of end-to-end tests that verify service bundles recovery scenarios between the Orchestrator and Health Monitor.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```bash
atest SdvE2ERecoveryServiceBundlesHmOrchTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-e2e-recovery-service-bundles-hm-orch-test --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_config_local.yaml
```

## CI/CD execution

Name: `sdv/e2e/sdv_recovery_service_bundles_hm_orch_test`
