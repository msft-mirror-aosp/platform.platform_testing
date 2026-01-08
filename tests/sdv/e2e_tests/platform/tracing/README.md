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

# SDV Perfetto tracing test

Test captures perfetto tracing report and checks whether it contains expected processes.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 atest SdvPerfettoTracingTest -- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_one_device_local_only_config.yaml
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-e2e-perfetto-tracing-test --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_local_only_config.yaml
```
