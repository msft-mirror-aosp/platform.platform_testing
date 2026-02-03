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

# End-to-end test: APEX untrusted install test

The test verifies

- a new APEX signed with untrusted private key fails to install

- an updated version of a pre-installed APEX signed with untrusted private key fails to install

## Test Execution

### atest

```
atest SdvApexUntrustedInstallTest -- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_one_device_local_only_config.yaml
```

### CATBox

```
m catbox && NOTIFY_AS_NATIVE=0.0.0.0:6520 $ANDROID_BUILD_TOP/out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-e2e-apex-untrusted-install-test --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_local_only_config.yaml
```

### CI/CD

Name: `sdv/e2e/apex_untrusted_install_test`
