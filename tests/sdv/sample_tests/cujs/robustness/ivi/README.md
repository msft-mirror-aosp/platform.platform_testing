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

# Robustness tests for SDV-IVI+SDV-Core

Note: Due to limitations on CI enveronment (b/393555389) for calling `cvd`
commands (for power management in the test cases), these tests cannot be
executed on CI.

## Test Execution

Read
[SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md)
for detailed information.

### Test Setup

Clear existing running VMs:

```shell
cvd reset
```

`Device 1`: Start IVI VM in the first shell:

```shell
cd (path-to-code)
source source build/envsetup.sh && lunch sdv_ivi_cf-trunk_staging-userdebug
m
cvd --instance_name=instance1 create --config=sdv_ivi_instance1 -report_anonymous_usage_stats=n
```

`Device 2`: Start core VM in the second shell:

```shell
cd (path-to-code)
export OUT_DIR=out_sdv
source source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug
m
cvd --instance_name=instance2 create --config=sdv_core_instance2 -report_anonymous_usage_stats=n
```

Note: `--instance_name=instance1` and `--instance_name=instance2` are necessary
for reboot and resume commands used by the tests working correctly.

### Running the test

Each CUJ has two variants, one for IVI publisher (server) and one for IVI
subscriber (client).

-   Publisher running on core, subscriber running on IVI
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_001_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_002_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_003_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_004_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_010_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_011_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_012_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_013_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_020_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_021_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_022_core_pub_ivi_sub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_023_core_pub_ivi_sub`
-   Subscriber running on core, publisher running on IVI
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_001_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_002_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_003_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_004_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_010_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_011_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_012_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_013_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_020_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_021_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_022_core_sub_ivi_pub`
    -   `SdvIviRobCujTest:SdvIviRobCujTest#test_cuj_023_core_sub_ivi_pub`

#### atest

```shell
atest <test name> --iteration=100
```

Change `<test name>` to the actual name of the test listed above, and using
`--iteration=100` for multiple iterations.

Note: When running multiple iterations or tests, be mindful of available space
in `/tmp`. To prevent disk space issues, clear the following files and
directories:

-   `/tmp/logcat_*.txt`
-   `/tmp/serial-util*.ser`
-   `/tmp/cvd` (after running `cvd reset`)
-   `/tmp/atest_result_(your user name)`

#### CATBox

Tested with the following command:

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-sample-gateway-robustness-cuj-test.xml --{device1}serial 0.0.0.0:6520 --{device1}serial 0.0.0.0:6521
```

Note: `NOTIFY_AS_NATIVE` only contains the address for core VM (`0.0.0.0:6521`
in this case), as the IVI VM is a full Android.
