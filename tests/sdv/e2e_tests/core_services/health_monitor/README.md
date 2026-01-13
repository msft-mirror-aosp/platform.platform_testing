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

# SDV Health Monitor device E2E Tests

This directory contains integration tests for the Health Monitor:

* `hm_configuration_test`: Verifies configuration loading, including error handling.
* `hm_integration_test`: Tests health status detection with a single monitored and monitoring service and re-registration of crashed service bundles for monitoring.
* `hm_two_vm_integration_test`: Tests health status detection on two VMs, where each vm contains a single monitored service and one VM contains a Vehicle Health Monitor. The test ensures that when each VM becomes unhealthy, the Vehicle Health Monitor gets a message notifying it of this.
* `hm_two_vm_someip_integration_test`: Tests health status detection on two VMs using SOME/IP, where each VM contains a single monitored service and one VM contains a Vehicle Health Monitor.  The test ensures that when each VM becomes unhealthy, the Vehicle Health Monitor gets a message notifying it of this.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

* **Hm Configuration Test:**

```bash
atest SdvHmConfigurationIntegrationTest
```

* **Hm Integration Test:**

```bash
atest SdvHmDeviceIntegrationTest
```

* **Hm Suspend resume from RAM Test:**

```bash
atest SdvHmRamSuspendResumeTest
```

### CATBox

* **Hm Configuration Test:**

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit  sdv-hm-config-integration-test --serial 0.0.0.0:6520
```

* **Hm Integration Test:**

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit sdv-hm-device-integration-test --{device1}serial 0.0.0.0:6520
```

* **Hm Mult-Vm Test :**

```bash

sdv-cf create --instance_name=instance1
sdv-cf create --instance_name=instance2

NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-hm-two-vm-integration-test  --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
```

* **Hm Two VM SOME/IP Integration Test:**

```bash
sdv-cf create --instance_name=instance1
sdv-cf create --instance_name=instance2

NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-hm-two-vm-someip-integration-test --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
```

* **Hm Suspend resume from RAM Test:**

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit sdv-hm-ram-suspend-resume-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

* Name: `sdv/e2e/sdv_hm_device_integration_test`
* Name: `sdv/e2e/sdv_hm_config_integration_test`
* Name: `sdv/e2e/sdv_hm_dumpsys_output_test`
* Name: `sdv/e2e/sdv_hm_two_vm_integration_test`
* Name: `sdv/e2e/sdv_hm_two_vm_someip_integration_test`
