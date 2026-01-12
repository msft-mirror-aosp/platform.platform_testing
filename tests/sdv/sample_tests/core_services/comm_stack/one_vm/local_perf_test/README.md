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

# Sample: CommStack - Local Performance Test

## Sample

This test automates the [CommStack local_perf_test sample](/system/software_defined_vehicle/core_services/samples/sdv_comms/rs/README.md),
with various quantities for the number of messages, which are defined in the [helper](/vendor/google_testing/software_defined_vehicle/tests/sample_tests/comm_stack/common/helper.py) module:

- 1000, 10000, 100000, and 1000000 messages
- 1 KB, 3 KB, 10 KB, and 32 KB message sizes

For a total of 4 * 4 = 16 test runs. The result, which contains latency statistics such as the average read/write speed in MB/s, is parsed and exported to CrystalBall.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```shell
atest SdvSampleCommStackOneVMLocalPerfTest
```

### CATBox

```shell
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-sample-comm-stack-one-vm-local-perf-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/sample/comm_stack_local_perf_test`
