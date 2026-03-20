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

# Sample: SDV Sample Diagnostics communication

## Sample

This test automates the [SDV Diagnostics behaviour under SB update sample (CUJ-APEX-020)](/system/software_defined_vehicle/samples/diagnostics/cuj-apex-020/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```bash
m
sdv-cf create
NOTIFY_AS_NATIVE=0.0.0.0:6520 atest SdvSampleDiagUpdateTest -- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_one_device_local_only_config.yaml
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit sdv-sample-diag-update-cuj020-test --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_local_only_config.yaml
```

## CI/CD execution

-  Name: `sdv/sample/sdv-sample-diag-update-cuj020-test`
