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
# SdvVsidlProviderAgentTest

## About

This end-to-end test involves two virtual machines (VMs): an In-Vehicle Infotainment (IVI) VM and a Software-Defined Vehicle (SDV) VM.

Similar to the `vdsidl_provider_test`, the SDV VM utilizes the `query_tester` binary. The test verifies that the SDV VM can successfully create clients for a VSIDL provider server that is running on the VSIDL provider agent of the IVI VM.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

Using `instance1` as SDV Core VM and `instance2` as IVI VM.

Launching IVI VM:

```bash
source build/envsetup.sh && lunch sdv_ivi_cf-trunk_staging-userdebug
m
cvd create --config=sdv_ivi_instance1
```

Launching Core VM:

```bash
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug
m
cvd create --config=sdv_core_instance2
```

### Mobly

```
atest SdvVsidlProviderAgentTest
```


### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-e2e-vsidl-provider-agent-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/e2e/sdv_vsidl_provider_agent_test`
