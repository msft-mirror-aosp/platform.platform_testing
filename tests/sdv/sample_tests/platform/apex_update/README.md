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

# Samples: APEX update tests

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

## Sample 1

This test automates the [APEX Update sample](/system/software_defined_vehicle/samples/apex/apex_update_with_service_bundles/README.md).

### atest

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 atest SdvSampleApexUpdateTest -- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_one_device_local_only_config.yaml
```

### CATBox

```
m catbox && NOTIFY_AS_NATIVE=0.0.0.0:6520 $ANDROID_BUILD_TOP/out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-sample-apex-update-test --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_local_only_config.yaml
```

### CI/CD

Name: `sdv/sample/apex_update_test`

## Sample 2

This test automates the [SDV/IVI APEX Update sample](/system/software_defined_vehicle/samples/apex/apex_update_ivi/README.md).

For local testing, ensure the IVI and SDV VMs have ports 6520 and 6521 respectively, mirroring the CI environment setup.

IVI VM:

```bash
export OUT_DIR=out_ivi
source build/envsetup.sh
lunch sdv_ivi_cf-trunk_staging-userdebug
m
sdv-cf create --instance_name=instance1
```

SDV VM:

```bash
source build/envsetup.sh
lunch sdv_core_cf-trunk_staging-userdebug
m
sdv-cf create --instance_name=instance2
```

### atest

Run the test command in the SDV VM Terminal:

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 atest SdvSampleIviApexUpdateTest -- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_two_devices_local_only_config.yaml
```

### CATBox

Run the test command in the SDV VM Terminal:

```
m catbox && NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 $ANDROID_BUILD_TOP/out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-sample-ivi-apex-update-test --mobly-config-file-name sdv_two_devices_local_only_config.yaml
```

### CI/CD

Name: `sdv/sample/ivi_apex_update_test`

## Sample 3

This test automates the [Brand-new APEX sample](/system/software_defined_vehicle/samples/apex/brand_new_apex/README.md).

### atest

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 atest SdvSampleBrandNewApexTest -- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_one_device_local_only_config.yaml
```

### CATBox

```
m catbox && NOTIFY_AS_NATIVE=0.0.0.0:6520 $ANDROID_BUILD_TOP/out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-sample-brand-new-apex-test --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_local_only_config.yaml
```

### CI/CD

Name: `sdv/sample/brand_new_apex_test`
