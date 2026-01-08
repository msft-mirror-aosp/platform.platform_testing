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
<!-- TODO(b/402089309): Deprecate test when b/383592250 and b/403259181 are ready. -->

# End-to-End: CUJ-CORE-022-User-build

## Sample

This is the user build test automates for the
[CUJ CORE 22](/system/software_defined_vehicle/samples/cujs/cuj-core-22/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

This test is explicitly for testing User build and has own custom local setup.

### Cleanup

```bash
cvd reset -y ; (yes | cvd rm) ; cvd clear
```

#### UserDebug or Eng build VM / "Proxy VM"

```bash
# lunch userdebug build
source $ANDROID_BUILD_TOP/build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug
# Build
m installclean ; m ; m catbox
# Create the 1st VM with the UserDebug build ("Proxy VM")
#TODO(b/383592250) Use SDV boot mode locked.
cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance1 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.boot_mode=unlocked \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_bar_orch_config.textproto"
```

#### User build VM / "VM under test"

```bash
# lunch user build
source $ANDROID_BUILD_TOP/build/envsetup.sh && lunch sdv_core_cf-trunk_staging-user
# Build
m installclean ; m ; m catbox
# Create the 2nd VM with the User build ("VM under test")
#TODO(b/383592250) Use SDV boot mode locked.
cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance2 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.boot_mode=unlocked \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_foo_orch_config.textproto"
```

### Mobly test execution

```bash
atest SdvE2ECujCore22TestUserBuildTest
```

### CATBox test execution

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit \
    sdv-e2e-cuj-core-22-user-build-test \
    --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `vm_core_core/sdv/e2e/cuj_core_022_user_build`
