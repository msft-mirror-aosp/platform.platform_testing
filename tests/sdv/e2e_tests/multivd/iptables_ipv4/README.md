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

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
atest SdvMultivdIpTest
```

### CATBox

## Build

```bash
cd path/to/android-repo
source build/envsetup.sh
lunch sdv_core_cf-trunk_staging-userdebug
m
```

## Run test

1.  Launch 1st SDV VM and set the androidboot.virt.address property using the extra_bootconfig_args flag. This property will be used to set IP address by the mobly test. Expect tons of logs finishing by "Virtual device booted successfully".

    ```bash
    cvd start --extra_bootconfig_args='androidboot.sdv.instance_name=instance1 androidboot.virt.address=3 androidboot.virt.vm2=4' -guest-enforce-security false
    ```

2.  Launch 2nd SDV VM.

    ```bash
    cvd start --extra_bootconfig_args='androidboot.sdv.instance_name=instance2 androidboot.virt.address=4 androidboot.virt.vm2=3' -guest-enforce-security false
    ```

3. Run the test in Catbox.

    ```
    NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-multivd-iptables-ip-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
    ```

## CI/CD execution

- Name: `sdv/e2e/multivd_iptables_ip_test`
