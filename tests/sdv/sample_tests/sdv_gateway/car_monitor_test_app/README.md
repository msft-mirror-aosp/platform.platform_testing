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

# Sample: SDV Car Monitor Test App sample

## Sample

This test automates the [SdvCarMonitorTestApp sample](/system/software_defined_vehicle/samples/sdv_gateway/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

Using `instance1` as IVI VM and using `instance2` as SDV Core VM.

Launching IVI VM:

```bash
export OUT_DIR=out
source build/envsetup.sh && lunch sdv_ivi_cf-trunk_staging-userdebug
m
cvd --instance_name=instance1 create --config=sdv_ivi_instance1
```

Launching Core VM:

```bash
export OUT_DIR=out_sdv
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug
m
cvd --instance_name=instance2 create --config=sdv_core_instance2
```

### Mobly

```
atest SdvSampleCarMonitorAppTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-sample-car-monitor-app-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/sample/car_monitor_app_test`
