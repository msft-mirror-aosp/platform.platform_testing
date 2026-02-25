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

# HW Suspend Resume Baseline tests

Created for verifying the hardware suspend/resume testing flow.

This test ensures the reliability and stability of the suspend/resume testing
infrastructure on hardware.

The test verifies:

  1. Connection to the hypervisor.
  2. Responsiveness of the test environment.
  3. Correct hypervisor setup for suspend/resume operations.
  4. Basic suspend and resume functionality.
  5. Suspend and resume with a short idle period to ensure CI/CD support of
     complex scenarios.

These tests only works on HW. They can be run locally and in CI/CD without
modifications.

### One VM

#### Atest

```
NOTIFY_AS_NATIVE=127.0.0.1:15555 atest SdvBaselineHwSuspendResumeOneVMTest
```

#### CATBox

```
NOTIFY_AS_NATIVE=127.0.0.1:15555 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-fw-baseline-suspend-resume-hw-one-vm-test.xml --{device1}serial 127.0.0.1:15555 --mobly-config-file-name sdv_one_device_config_local.yaml
```

### Two VMs

Because of known limitations in the infrastructure that arbitrarily assigns the
devices to device1 and device2, this test should be run in CATBox or CI/CD.

#### CATBox

```
NOTIFY_AS_NATIVE=127.0.0.1:15555,127.0.0.1:15556 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-fw-baseline-suspend-resume-hw-two-vm-test.xml --{device1}serial 127.0.0.1:15555 --{device2}serial 127.0.0.1:15556 --mobly-config-file-name sdv_two_devices_config_local.yaml
```

## Test name in CI/CD

- `v2/aaos-bi-engprod/qnx_core_core/sdv/tfw_baseline/suspend_resume_test`
- `v2/aaos-bi-engprod/qnx_core/sdv/tfw_baseline/suspend_resume_test`