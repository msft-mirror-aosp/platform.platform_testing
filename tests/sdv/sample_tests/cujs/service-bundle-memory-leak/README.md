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

# Sample: SDV Service Bundle memory leak detection test

## Sample

This test verifies SDV service bundle memory leaks.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Local setup

```bash
# Clean up
cvd reset -y ; (yes | cvd rm) ; cvd clear
# Build
m installclean ; m ; m catbox
# Create 2 VMs
sdv-cf create --instance_name=instance1
sdv-cf create --instance_name=instance2
```

### Local Mobly test execution

```bash
atest SdvSampleCujMemoryLeakDetection
```

### Local CATBox test execution

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit \
    sdv-sample-cuj-memory-leak-detection-test \
    --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

### Remote CI/CD execution

Warning: The test can takes more than 10 minutes.

- Name: `sdv/sample/cuj_memory_leak_detection`
