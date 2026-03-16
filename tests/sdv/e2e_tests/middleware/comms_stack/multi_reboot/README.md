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

# Comm Stack Integration Tests

Set of end-to-end tests that verify Comm Stack functionality.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Orch Config Tests

To run locally, use:
```
cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance1 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_foo_orch_config.textproto"

cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance2 \
  --extra_kernel_cmdline=" \
    androidboot.sdv.orchestrator_config_path=/etc/orch/vm_bar_orch_config.textproto"
```

### Cli Tool Test

To run locally, use:
```
cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance1

cvd create \
  --report_anonymous_usage_stats=y \
  --config=sdv_core_instance2
```

## Comm Stack Multi Reboot Tests

The individual test modules wrap around the base class, providing 3 arguments:
- the number of server (device1) reboots
- the number of client (device2) reboots
- the number of cuncurrent reboots of both devices

The parameters are encoded in the name of the test (in the respective order as above).

### Mobly

```bash
atest SdvCommsStackMultiRebootTestOrchConfig_10_0_0
atest SdvCommsStackMultiRebootTestOrchConfig_0_10_0
atest SdvCommsStackMultiRebootTestOrchConfig_0_0_10
atest SdvCommsStackMultiRebootTestOrchConfig_5_5_0
atest SdvCommsStackMultiRebootTestCliTool
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-comms-multi-reboot-test-orch-config-10-0-0  --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-comms-multi-reboot-test-orch-config-0-10-0  --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-comms-multi-reboot-test-orch-config-0-0-10  --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-comms-multi-reboot-test-orch-config-5-5-0  --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
NOTIFY_AS_NATIVE=0.0.0.0:6520,0.0.0.0:6521 catbox-tradefed run commandAndExit sdv-comms-multi-reboot-test-cli-tool  --serial 0.0.0.0:6520 --serial 0.0.0.0:6521
```

## CI/CD Execution

```
sdv/e2e/sdv_comms_multi_reboot_test_orch_config_10_0_0
sdv/e2e/sdv_comms_multi_reboot_test_orch_config_0_10_0
sdv/e2e/sdv_comms_multi_reboot_test_orch_config_0_0_10
sdv/e2e/sdv_comms_multi_reboot_test_orch_config_5_5_0
sdv/e2e/sdv_comms_multi_reboot_test_cli_tool
```
