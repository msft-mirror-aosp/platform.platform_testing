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

# Sample: CUJ-CORE-027

## Sample

This test automates the [CUJ 27](/system/software_defined_vehicle/samples/cujs/cuj-core-27/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```bash
# Run test
atest SdvSampleCujCore27Test
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521,0.0.0.0:6522 \
    catbox-tradefed run commandAndExit sdv-sample-cuj-core-27-test \
    --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --{device3}serial 0.0.0.0:6522
```
