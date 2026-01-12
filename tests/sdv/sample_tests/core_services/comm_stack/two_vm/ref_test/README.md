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

# Sample: CommStack - Two VM - Reference Test

## Sample

This test automates the [CommStack sample](/system/software_defined_vehicle/core_services/samples/sdv_comms/rs/README.md) in the form of a reference test (basic publish & subscribe),
with various parameters to track the performance of the CommStack through the recording of a Perfetto trace.
The values of the parameters are defined in the [helper](/vendor/google_testing/software_defined_vehicle/tests/sample_tests/comm_stack/common/helper.py) module:

- Payload sizes for message: 1 KB, 3 KB, 10 KB, 32 KB
- Waiting times until a new message is published (not waiting for response/notification etc.): 10 ms, 50 ms, 100 ms, 250 ms, 500 ms
- Number of messages: 1000, 200, 100, 40, 20

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Create VMs
#### With example json file to create 2 VMs under the same group
Replace `<REPLACE_THIS_BY_REPO_ROOT>` with your repo root in file `config.json`, then run following command

```bash
cvd create --config_file=vendor/google_testing/software_defined_vehicle/tests/sample_tests/comm_stack/two_vm/config.json
```

### Mobly

```shell
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 atest SdvSampleCommStackTwoVMTest
```

### CATBox

```shell
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-sample-comm-stack-two-vm-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/sample/comm_stack_ref_test`
