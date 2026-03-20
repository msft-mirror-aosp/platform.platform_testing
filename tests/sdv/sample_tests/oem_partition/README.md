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

# Sample: OEM Partition sample

## Sample

This test automates the
[OEM partition manual test](https://testtracker.googleplex.com/tc/98e9b476-6276-4591-ada7-608cab2e73b5).
The additional partition is defined under
[device/google/sdv/sdv_core_base/oem](https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:device/google/sdv/sdv_core_base/oem/).

## Test Execution

The `oem_ab.img` and `oem_metadata.img` files can be pulled either from ab/ or
built locally with `m`.

Create 1 VM and mount the additional OEM partition:

```sh
cvd create --custom_partition_path=$OUT/oem_ab.img;$OUT/oem_metadata.img --config=sdv_core_instance1
```

### atest

Test execution using `atest`:

```sh
atest SdvSampleOemPartitionTest
```

### CATBox

Test execution using `catbox-tradefed`:

```sh
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-sample-oem-partition-test
```
