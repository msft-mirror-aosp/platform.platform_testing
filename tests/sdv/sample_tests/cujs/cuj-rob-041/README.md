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

# Sample: CUJ-ROB-041

## Sample

This test automates the [CUJ-ROB-041](/system/software_defined_vehicle/samples/cujs/cuj-rob-041/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

#### Userdebug/Eng build flavor

```bash
# Userdebug/Eng buld flavor, select one or the other
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug # Userdebug
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-eng # Eng

# Clean up
cvd reset -y ; (yes | cvd rm) ; cvd clear

# Build up
m installclean ; m ; m catbox

# Create 2 VMs

cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance1

cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance2
```

#### User build flavor

Not supported: Root access required to issue LCM CLI commands.

### Mobly test execution

```bash
atest SdvSampleCujRob041Test
```

### CATBox test execution

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit \
    sdv-sample-cuj-rob-041-test \
    --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/sample/cuj_rob_041`
