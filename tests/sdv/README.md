<!-- Copyright (C) 2025 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. -->

# SDV Testing

This folder contains SDV System tests implemented using the [SDV Test Framework](libraries/sdv).

See [Android partner documentation](https://docs.partner.android.com/automotive/sdv/overview)
for general information about SDV.

## Tests execution

### Setup

Ensure you are sync and have built most recent version.

1. Set environment and lunch SDV target

  ```bash
  . build/envsetup.sh
  lunch sdv_core_cf-trunk_staging-userdebug
  ```

1. Start up to three CF virtual devices with the default configuration for SDV:

  ```bash
  // on sdv_core_cf
  cvd create --config=sdv_core_instance1
  cvd create --config=sdv_core_instance2
  cvd create --config=sdv_core_instance3
  ```

  ```bash
  // on sdv_ivi_cf
  cvd create --config=sdv_ivi_instance1
  cvd create --config=sdv_ivi_instance2
  cvd create --config=sdv_ivi_instance3
  ```

  ```bash
  // on sdv_media_cf
  cvd create --config=sdv_media_instance1
  cvd create --config=sdv_media_instance2
  cvd create --config=sdv_media_instance3
  ```

### Mobly tests

1. Execute test

  ```bash
  atest <testName>
  ```

### CATBox tests

1. Build catbox: `m catbox`
1. The following commands assume you are at the root of the repo
1. Check available test plans for SDV

    ```bash
    ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed l p | grep sdv
    ```

1. Execute test

   - One device:

    ```bash
    NOTIFY_AS_NATIVE=0.0.0.0:6520 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit <test_name> --{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_config_local.yaml
    ```

    - Two devices:

    ```bash
    NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit <test_name> --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --mobly-config-file-name sdv_two_devices_config_local.yaml
    ```

    - Three devices:

    ```bash
    NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521,0.0.0.0:6522 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit <test_name> --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --{device3}serial 0.0.0.0:6522 --mobly-config-file-name sdv_three_devices_config_local.yaml
    ```
