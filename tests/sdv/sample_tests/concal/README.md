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

# Sample: SDV ConCal and OverrideLibrary Implementation Test

## Sample

This test automates the the [SDV ConCal and OverrideLibrary Implementation Sample](/system/software_defined_vehicle/samples/concal/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
atest SdvSampleConCalServerAndClientTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-sample-concal-sever-and-client-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/sample/sdv_sample_concal_sever_and_client_test`