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

# uds_certs_provisioner_sample test

A test to verify the uds_certs_provisioner_sample. See
[uds_certs_provisioner/README.md] for more details.

## How to run locally?

This test requires a single VM and works both with the SDV Core and IVI
Cuttlefish targets.

The environment and the device must be started before running the tests. Launch
the SDV environment. For `sdv_core_cf-trunk_staging-userdebug`:

```bash
. build/envsetup.sh
lunch sdv_core_cf-trunk_staging-userdebug
```

The test requires the VM to be booted in unlocked SDV mesh mode so that the
`/vvmtruststore` partition is writable.

```bash
sdv-cf create --instance_name=instance1 --boot_mode=unlocked --blank
```

See [uds_certs_provisioner/README.md] for more details.

### Atest

```bash
atest SdvE2EUdsCertsProvisionerTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-e2e-uds-certs-provisioner-test.xml --{device1}serial 0.0.0.0:6520
```

## Test name in CI/CD

Name: `sdv/sample/uds_certs_provisioner_test`

[uds_certs_provisioner/README.md]: /system/software_defined_vehicle/platform/samples/uds_certs_provisioner/README.md
