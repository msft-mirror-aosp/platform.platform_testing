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

## Robustness tests for SDV-CORE are based on the setup for CUJ-22

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

#### Userdebug build flavor

```bash
# Userdebug buld flavor, select one or the other
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug # Userdebug

# Clean up
cvd reset -y ; (yes | cvd rm) ; cvd clear

# Build up
m installclean ; m
# Create 2 VMs

cvd --instance_name=instance1 create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance1 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_foo_orch_config.textproto"

cvd --instance_name=instance2 create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance2 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_bar_orch_config.textproto"

```
