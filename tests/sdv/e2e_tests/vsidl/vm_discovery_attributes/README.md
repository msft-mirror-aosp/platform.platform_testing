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

# SDV E2E VM Discovery Attributes

Tests that VMs connect to the VM specified by `vm_name` in `SubscribeOptions` (for PubSub) or `create_rpc_client`'s `server` parameter (for RPC).
This tests validates that messages and RPC answers from the requested VM are received and messages and RPC answers from a different VM are not received.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```bash
atest SdvVmDiscoveryAttributeTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 ./tools/catbox-tradefed run commandAndExit \
  sdv-e2e-vm-discovery-attributes-test \
  --{device1}serial 0.0.0.0:6520 \
  --{device2}serial 0.0.0.0:6521
```

## CI/CD execution

- Name: `vm_core_core/sdv/e2e/vm_discovery_attributes_test`
