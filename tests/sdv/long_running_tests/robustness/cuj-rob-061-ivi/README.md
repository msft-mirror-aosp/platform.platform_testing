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

# Sample: CUJ-ROB-061-IVI

## Sample

This test automates the [CUJ-ROB-061](/system/software_defined_vehicle/samples/cujs/cuj-rob-061/README.md) for an IVI device.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

#### Build SDV Core and IVI

First, build the SDV Core VM:
```bash
export OUT_DIR=out_sdv
source build/envsetup.sh
lunch sdv_core_cf-trunk_staging-userdebug
m
```

Then, in a separate terminal, build the IVI VM:
```bash
export OUT_DIR=out_ivi
source build/envsetup.sh
lunch sdv_ivi_cf-trunk_staging-userdebug
m
```

#### Create VMs

After building, create the two VMs.

First, clean up any existing VMs:
```bash
cvd reset -y ; (yes | cvd rm) ; cvd clear
```

Create the SDV Core VM (device1):
```bash
cvd create -base_instance_num=1 --config=sdv_core_instance1
```

Create the IVI VM (device2):
```bash
cvd create -base_instance_num=2 --config=sdv_ivi_instance2
```

#### User build flavor

Not supported: Root access required to issue LCM CLI commands.

### Mobly test execution

```bash
atest SdvLongRunningCujRob061IviTest
```

### CATBox test execution

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit \
    sdv-long-running-cuj-rob-061-ivi-test \
    --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/long_running/cuj_rob_061_ivi`
