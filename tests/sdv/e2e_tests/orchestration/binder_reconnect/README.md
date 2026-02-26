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

# SDV E2E Orchestration Binder Reconnect Test

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

NOTE: In SDV1.0 all agents are oneshot and crashes lead to VM restart. Therefore, scenario under test
does not occur in production. However, as logic to handle the scenario exists in the code, scenario
is still tested.

### Mobly

```
atest SdvE2EOrchestrationBinderReconnectTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-e2e-orchestration-binder-reconnect-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/e2e/orchestration_binder_reconnect_test`
