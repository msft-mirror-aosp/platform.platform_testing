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

# SDV Media MultiDisplay test

A simple test that captures a screenshot from a test binary that renders a static solid color images on three displays. As a result there are three screenshots which are checked to be a solid image of a different colors each.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Build

```bash
  cd path/to/android-repo
  source build/envsetup.sh
  lunch sdv_media_cf-trunk_staging-eng
  m
```

### Starting cuttlefish

One cuttlefish VM is required. It should be started with three displays by using the following command:

```
cvd create --display{0,1,2}=width=800,height=600
```

### Mobly

```
atest SdvMediaMultiDisplayTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-media-multi-display-test --{device1}serial 0.0.0.0:6520
```