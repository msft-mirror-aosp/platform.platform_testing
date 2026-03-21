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

# Sample: CommStack - Two VM - Ping-Pong Notifications Latency Test

## Sample

This test automates the [CommStack ping-pong notifications latency test sample](/system/software_defined_vehicle/samples/middleware/sdv_comms/rs/README.md),
with various values for the quantity of exchanged messages: 100, 1000, and 10000 messages,
and message sizes: 1 KB, 3KB, 10KB, and 32KB, which are defined in the [helper](/platform_testing/tests/sdv/sample_tests/core_services/comm_stack/common/helper.py) module.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```shell
atest SdvSampleCommStackTwoVMPingPongTest
```

### CATBox

```shell
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-sample-comm-stack-two-vm-ping-pong-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/sample/comm_stack_ping_pong_test`
