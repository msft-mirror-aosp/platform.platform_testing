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

# HostOrchestratorUtil

`HostOrchestratorUtil` is a Python utility class that provides methods to interact with the host orchestrator for managing Cuttlefish Virtual Devices (CVDs).

## Available CVD Actions

The following actions can be performed on a CVD instance:

| Action | Description |
|---|---|
| `powerwash` | Reinitialize and restart the Cuttlefish device. |
| `powerbtn` | To wake up the VM from suspension. |
| `start` | Start the VM with default parameters. |
| `stop` | Stop the VM. |

## Usage

To use the utility, create an instance of the `HostOrchestratorUtil` class, providing the host orchestrator URL (stored in `self.user_params['ho_base_url']`) and the device index. The device index correlates with the adb device identifier (0 - device1, 1 - device2, 2 - device3).

## Note

This library should not be used directly in tests!

```python
from host_orchestrator_util import HostOrchestratorUtil

self.sdv_device = self.get_device('device1').adb()
ho_url = self.user_params['ho_base_url']
ho = HostOrchestratorUtil(ho_url, device_index=0)

# Example: Powerwash the device
status = ho.powerwash()
if status.is_success():
    print("Powerwash successful!")
else:
    print("Powerwash failed.")
```
