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

import abc

from mobly import signals


class DeviceHostInteraction(abc.ABC):
    """Device Host Interaction Interface

    Allows for a common interface to communicate and interact with the device
    host
    """

    def __init__(self, adb_device, device_info):
        self.__adb_device = adb_device
        self.__device_info = device_info

    def _not_implemented_error(self, name):
        """Raises controller error with implementation information"""
        raise signals.ControllerError(
            f'{name} has not been implemented for {self.implementation_info}'
        )

    @property
    @abc.abstractmethod
    def implementation_info(self):
        """String with information about the implementation class."""

    @abc.abstractmethod
    def cleanup_tasks(self):
        """Run tasks required in the host at the end of a test"""

    @abc.abstractmethod
    def status(self):
        """Retrieves status of the VM"""

    @abc.abstractmethod
    def stop(self):
        """Stops a running VM"""

    @abc.abstractmethod
    def start(self):
        """Starts a stopped VM"""

    @abc.abstractmethod
    def restart(self):
        """Re-starts a running VM"""

    @abc.abstractmethod
    def powerwash(self):
        """Re-initializes and restarts the VM"""

    @abc.abstractmethod
    def power_button(self):
        """Trigger power button event on the VM"""
