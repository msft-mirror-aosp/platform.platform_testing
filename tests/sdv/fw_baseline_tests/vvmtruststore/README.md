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

Note: Vvmtrustore partition is not yet supported in CI/CD and this test has been
created to verify infrastrcuture works once supported. It will be moved to E2E
once it has been completed and enabled.

# VVM Trust Store Partition Test

This test verifies the
`--default_vvmtruststore_file_name=<default_vvmtruststore_img_file_name>`

## Steps

1.  Build and launch environment

2.  Run Cuttlefish **with the `/vvmtruststore` partition configured**

    VM1: `cvd create -base_instance_num=1 \
    --default_vvmtruststore_file_name=vvmtruststore_preprovisioned_instance1.img \
    --extra_bootconfig_args="androidboot.sdv.instance_name=instance1 \
    androidboot.virt.address=3 androidboot.sdv.boot_mode=locked \
    androidboot.sdv.preprovisioned_vvmtruststore=img \
    androidboot.sdv.vvmfactorytrust=00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff \
    androidboot.sdv.ignore_avb_state=true"`

    VM2: `cvd create -base_instance_num=2 \
    --default_vvmtruststore_file_name=vvmtruststore_preprovisioned_instance2.img \
    --extra_bootconfig_args="androidboot.sdv.instance_name=instance2 \
    androidboot.virt.address=4 androidboot.sdv.boot_mode=locked \
    androidboot.sdv.preprovisioned_vvmtruststore=img \
    androidboot.sdv.vvmfactorytrust=00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff \
    androidboot.sdv.ignore_avb_state=true"`

### Run with atest

`atest SdvE2EVVMTrustStorePartitionTest`

### Run with CATBox

`NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-e2e-vvmtruststore-partition-test --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521`

### Test in CI/CD

TODO. This test does not run in CI/CD yet, it will be used to verify the vvmtrustore setup once supported. (See b/411021381)
