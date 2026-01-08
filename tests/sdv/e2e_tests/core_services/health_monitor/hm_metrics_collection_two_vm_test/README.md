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

# SDV Health monitoring performance metric collection test, 2 VM

Test collects perfetto traces and computes relevant metrics, in a 2 VM scenario.
2 core SDV images are used, as CI does not yet support 2 perf SDV images

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
atest SdvHmMetricsCollectionTwoVMTest
```

### CATBox

```

NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-hm-two-vm-metrics-collection-test  --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/e2e/sdv-hm-two-vm-metrics-collection-test`
