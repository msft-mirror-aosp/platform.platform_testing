# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from sdv_test_fw.device.dhi import device_host_interaction
from sdv_test_fw.host import host_orchestrator


class RemoteCuttlefishDHI(device_host_interaction.DeviceHostInteraction):
    """Implements device interaction with CF remotely."""

    def __init__(self, adb_device, device_info, user_params):
        super().__init__(adb_device, device_info)

        # Requires a Host Orchestrator URL that should be provided by user_params
        ho_api_url = user_params.get('ho_base_url', None)
        if ho_api_url is None:
            raise device_host_interaction.DeviceHostInteractionError(
                'RemoteCuttlefishDHI requires Host Orchestrator API URL. Please'
                ' ensure the test is being run in the right environment and'
                ' setup.'
            )

        self._host_orchestrator = host_orchestrator.HostOrchestrator(ho_api_url)

    @property
    def _device_id(self) -> int:
        """Returns the identifier of the device for Host Orchestrator."""
        # Host orchestrator receives requests for devices in a 0 to N_DEVICES-1
        # format. It is assumed the devices are in order so the instances
        # correlate with the index in the list (e.g., 0 - instance1,
        # 1 - instance2, 2 - instance3, etc.).
        return self._device_info.instance_number - 1

    @property
    def implementation_info(self):
        return 'remote CF VM'

    def cleanup_tasks(self):
        # No cleanup tasks required
        pass

    # Not implemented methods must raise an error to ensure they are not used.
    # Move methods up once supported.
    def status(self):
        self._not_implemented_error('status')

    def stop(self):
        self._not_implemented_error('stop')

    def start(self):
        self._not_implemented_error('start')

    def restart(self):
        self._not_implemented_error('restart')

    def powerwash(self):
        self._not_implemented_error('powerwash')

    def powerbtn(self):
        self._not_implemented_error('powerbtn')
