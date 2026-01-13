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

### Catbox

## Build

  ```
  sudo apt install -y clang pkg-config meson libfmt-dev libgflags-dev libjsoncpp-dev protobuf-compiler libgtest-dev libcurl4-openssl-dev libprotobuf-c-dev libgoogle-glog-dev libssl-dev libxml2-dev openssl uuid-dev
  ```

  ```
  git clone https://github.com/google/android-cuttlefish
  ```

  ```
  cd android-cuttlefish/base/cvd
  ```

  ```
  meson setup build
  cd build
  meson compile
  ```

## Run Test

1. Launch three SDV instances using cvd load and below json config.

    ```bash
    {
        "common": {
            "host_package": "/path/to/local/host/tools/package"
        },
        "instances": [
            {
                "disk": {
                    "default_build": "/path/to/local/sdv/image"
                },
                 "connectivity": {
                    "vsock": {
                        "guest_group": "g1+g2"
                    }
                }
            },
            {
                "disk": {
                    "default_build": "/path/to/local/sdv/image"
                },
                 "connectivity": {
                    "vsock": {
                        "guest_group": "g1+g6"
                    }
                }
            },
            {
                "disk": {
                    "default_build": "/path/to/local/sdv/image"
                },
                 "connectivity": {
                    "vsock": {
                        "guest_group": "g3+g5"
                    }
                }
            }
        ]
    }
    ```

    ``` bash
    cvd load sdv.json
    ```

3. Run the test in Catbox.
    ```
    NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521,0.0.0.0:6522 ./tools/catbox-tradefed run commandAndExit sdv-multivd-vsock-isolation-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --{device3}serial 0.0.0.0:6522
    ```