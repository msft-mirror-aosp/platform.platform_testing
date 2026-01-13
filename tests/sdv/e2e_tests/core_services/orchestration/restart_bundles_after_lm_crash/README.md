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

# SDV E2E Orchestration Restart Bundles After LM Crash Test

Verifies that service bundles are restarted to the required state after LM crashed.

If LM crashes, it does not know which state was requested for each bundle as that information is maintained by orchestrator.
In case LM crashes, the bundle processes are killed and orchestrator needs to move every bundle to the requested state.

NOTE: In SDV1.0 all agents are oneshot and crashes lead to VM restart. Therefore, scenario under test
(VM alive post killing LM) does not occur in production. However, as logic to handle the scenario exists in the code, scenario is still tested.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
atest SdvE2EOrchestrationBundlesRestartedAfterLMCrashTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-e2e-orchestration-bundles-restarted-after-lm-crashed-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/e2e/orchestration_bundles_restarted_after_lm_crashed_test`
