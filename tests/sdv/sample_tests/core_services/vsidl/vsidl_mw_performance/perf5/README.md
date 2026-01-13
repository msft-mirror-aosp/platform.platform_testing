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

# Sample: SDV VSIDL Middleware Performance Scenario 5

## Sample

This test automates the [VSIDL Middleware Performance 5 Scenario](/system/software_defined_vehicle/core_services/samples/vsidl/perf5/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

```bash
# Clean up
cvd reset -y ; (yes | cvd rm) ; cvd clear
# Build up
m installclean ; m ; m catbox
# Create 1 VM
sdv-cf create --instance_name=instance1
```

### Mobly test execution

```bash
atest SdvSampleVsidlMwPerf5Test
```

### CATBox test execution

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit \
    sdv-sample-vsidl-mw-perf5-test \
    --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/sample/vsidl_mw_perf5_test`
