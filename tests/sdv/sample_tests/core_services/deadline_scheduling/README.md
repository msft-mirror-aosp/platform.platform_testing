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

# Sample: Deadline scheduling test

## Sample

This test automates
[the Deadline scheduling service bundle sample](/system/software_defined_vehicle/core_services/samples/lifecycle/deadline_scheduling/README.md)
that tests both changing deadline scheduling parameters and CPU affinity.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```bash
atest SdvSampleServiceBundleDeadlineSchedulingTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit \
    sdv-sample-service-bundle-deadline-scheduling-test \
    --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/sample/service_bundle_deadline_scheduling_test`
