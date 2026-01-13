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

# SDV Bundle Management Metrics Collection Test

Collects metrics centered around bundle management. Provides a test pattern which allows for putting device
in an arbitrary state using adb commands, and then collecting metric based on distinct log entries. For each metric collected
measurements are measured NR_MEAS times, and distribution metrics (avg, max, p90) are computed.

Tests following service bundle transitions:

- bundle creation post cold boot
- bundle destroy as result of custom mode state change
- bundle started->created as result of pre-suspend to ram state
- bundle created->started as result of post-suspend to ram state

Arbitrary scenarios/ duration metrics can be a further added, as long as duration can be defined by 2 log entries.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

NOTE: metrics should be collected on sdv_core_perf* image.
Depends on CustomModeDispatcherServiceBundle, available only on perf image.
To build and run perf image on CF:

```
lunch sdv_core_perf_cf-trunk_staging-userdebug
m
cvd create
```

### Mobly

```
atest BundleManagementMetricsCollectionTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit sdv-e2e-bundle-management-metrics-collection-test --{device1}serial 0.0.0.0:6520
```

## Test Execution on ARM Hardware
(tested on 8255 02, RPi)
Steps to execute before running CATBox (edit OSPREY_IP accordingly):
```
export OSPREY_IP=10.42.82.102  # set to the IP of the booked osprey
ssh sdv-devlab-001.muc.corp.google.com -L 12222:$OSPREY_IP:22 -L 15555:$OSPREY_IP:5555 -L 15556:$OSPREY_IP:5556
(keep this alive)

Flash and connect the device:
export HW_IP=10.42.82.102
./collect-boot-logs -i $HW_IP -c sdv_8255/sdv-1.json -f -x
./collect-boot-logs -i $HW_IP -c sdv_8255/sdv-1.json -x
SERIAL1=127.0.0.1:15555
adb connect $SERIAL1
```

Then run the test with the CATBox command:
```
NOTIFY_AS_NATIVE=127.0.0.1:15555 catbox-tradefed run commandAndExit sdv-e2e-bundle-management-metrics-collection-test --{device1}serial 127.0.0.1:15555
```

## CI/CD execution

For now only supporting Cuttlefish

- Name: `sdv/performance/sdv_orchestration_performance_test`
