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

# Sample: SDV VSIDL Compatibility

## Sample

This test automates the [VSIDL Compatibility Sample](/system/software_defined_vehicle/samples/vsidl/compatibility/README.md).

## Test Execution

### Setup

```bash
# Clean up
cvd reset -y ; (yes | cvd rm) ; cvd clear
# Build up
m installclean ; m ; m catbox
# Create 1 VM
sdv-cf create --instance_name=instance1
```

### Mobly test execution

```bash
atest SdvSampleVsidlCompatibilityTest
```

### CATBox test execution

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 $ANDROID_BUILD_TOP/out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-sample-vsidl-compatibility-test \
  --{device1}serial 0.0.0.0:6520 \
  --mobly-config-file-name sdv_one_device_config_local.yaml
```

## CI/CD execution

- Name: `sdv/sample/vsidl_compatibility_test`
