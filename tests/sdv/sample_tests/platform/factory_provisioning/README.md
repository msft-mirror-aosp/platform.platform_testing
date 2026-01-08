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

# Sample: Native SDV Gateway sample

## Sample

This test automates the [factory provisioning](system/software_defined_vehicle/platform/tools/sdv-cf/README.md).

## Test Execution

Create 3 VMs in an unlocked mode without a preprovisioned vvmtruststore image:

```
sdv-cf create --instance_name=instance1 --boot_mode=unlocked --blank
sdv-cf create --instance_name=instance2 --boot_mode=unlocked --blank
sdv-cf create --instance_name=instance3 --boot_mode=unlocked --blank
```

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```
atest SdvFactoryProvisioningTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521,0.0.0.0:6522 ./tools/catbox-tradefed run commandAndExit sdv-factory-provisioning-test
 --{device1}serial 0.0.0.0:6520 --{device2}serial 0.0.0.0:6521 --{device3}serial 0.0.0.0:6522
```


## CI/CD execution

- Name: `sdv/sample/factory_provisioning_test`