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

# SDV Health monitoring performance metric collection test

This test performs trace collection from several hm performance relevant components.
It reduces the collected traces to metrics, exports them to crystalball for visualization in
SDV Dashboards.
Additionally, if test ran locally, it exports output to file, for ease of manual investigation.
Should be ran on sdv_core_perf* image.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

Set environment with:

```
source build/envsetup.sh
lunch sdv_core_perf_cf-trunk_staging-userdebug
cvd create --config=sdv_core_instance1 --extra_kernel_cmdline="androidboot.sdv.someip.enable=true"
```

### Mobly

```
atest SdvHmMetricsCollectionTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-hm-metrics-collection-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/e2e/hm_metrics_collection_test`
