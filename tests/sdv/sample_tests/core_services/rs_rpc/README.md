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

# Sample and Perforance Rust RPC

## Sample

This test automates the [RsRPC sample](system/software_defined_vehicle/core_services/samples/sdv_comms/rs/README.md).


### Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

#### Mobly

```
atest SdvSampleRsRpcTest
```

#### CATBox

NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-sample-rs-rpc-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521

### CI/CD execution

- Name: `sdv/sample/rs_rpc_test`

## Performance

This test automates the [performance measurment](system/software_defined_vehicle/core_services/samples/sdv_comms/rs/README.md).


### Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

#### Mobly

```
atest SdvRsRpcPerformanceTest
```

#### CATBox

NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-rs-rpc-performance-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521

### CI/CD execution

- Name: `sdv/sample/sdv_rs_rpc_performance_test`