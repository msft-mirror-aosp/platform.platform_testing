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
# SDV VHAL Proxy Tests

## About

These tests automate the VHAL proxy publisher-subscriber communication. They verify that a VHAL property publisher (or subscriber) on an SDV Core VM can successfully send (or receive) events to (or from) a subscriber (or publisher) on an IVI VM through the VHAL proxy.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Setup

Using `instance1` as SDV Core VM and `instance2` as IVI VM.

Launching IVI VM:

```bash
source build/envsetup.sh && lunch sdv_ivi_cf-trunk_staging-userdebug
m
cvd create --config=sdv_ivi_instance1
```

Launching Core VM:

```bash
source build/envsetup.sh && lunch sdv_core_cf-trunk_staging-userdebug
m
cvd create --config=sdv_core_instance2
```

### Mobly

```
atest SdvVhalProxySdvPublisherIviSubscriber

atest SdvVhalProxySdvSubscriberIviPublisher
```


### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-e2e-vhal-proxy-sdv-publisher-ivi-subscriber-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521

NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit sdv-e2e-vhal-proxy-sdv-subscriber-ivi-publisher-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `sdv/e2e/sdv_vhal_proxy_sdv_publisher_ivi_subscriber_test`
- Name: `sdv/e2e/sdv_vhal_proxy_sdv_subscriber_ivi_publisher_test`
