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


class HardwareDHI(device_host_interaction.DeviceHostInteraction):
    """Implements device interaction with Hardware."""

    @property
    def implementation_info(self):
        return 'hardware'

    def cleanup_tasks(self):
        # TODO(431696012): Collect slog2info logs from hypervisor.
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
