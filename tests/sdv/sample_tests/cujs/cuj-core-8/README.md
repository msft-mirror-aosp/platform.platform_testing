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

# Sample: CUJ-CORE-08

## Sample

This test automates the [CUJ CORE 8](/system/software_defined_vehicle/samples/cujs/cuj-core-8/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

```bash
# Userdebug/User/Eng buld flavors, select one or the other
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug # Userdebug
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-eng # Eng
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-user # User

# Clean up
cvd reset -y ; (yes | cvd rm) ; cvd clear

# Build up
m installclean ; m ; m catbox

# Create 2 VMs

cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance1 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_foo_baz_orch_config.textproto"

# !!! WARNING !!!
# User build only: Lock the VM bootloader on first VM
sdv-avb-lock 0.0.0.0:6520

cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance2 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_bar_orch_config.textproto"

# User build only: Lock the VM bootloader on second VM
sdv-avb-lock 0.0.0.0:6521
```

### Mobly test execution

```bash
atest SdvSampleCujCore8Test
```

### CATBox test execution

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit \
    sdv-sample-cuj-core-8-test \
    --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/sample/cuj_core_008`
