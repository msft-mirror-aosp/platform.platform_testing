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

import enum

from sdv_test_fw.device import sdv_property


class SdvTarget(enum.Enum):
    CORE = 'core'
    IVI = 'ivi'
    MEDIA = 'media'

    @classmethod
    def from_flavor(cls, device_flavor):
        # The flavor property contains information about the target.
        # SdvTarget values must match with the targets in the property.
        # e.g. core: sdv_core_cf-userdebug
        # e.g. ivi: sdv_ivi_cf-userdebug
        # e.g. media: sdv_media_cf-userdebug
        for target in cls:
            if target.value in device_flavor:
                return target

        raise NotImplementedError(f'Device target not found in {device_flavor}')


class SdvVm(enum.Enum):
    CF = 'CF VM'
    HW = 'HW VM'

    @classmethod
    def from_flavor(cls, device_flavor):
        # The flavor property for CF VMs contains _cf while for hardware there is
        # no relevant information. We assume that if it is not cuttlefish, it is
        # hardware
        # e.g. CF: sdv_core_cf-userdebug
        # e.g. HW: sdv_core_arm64-userdebug
        if '_cf' in device_flavor:
            return cls.CF
        return cls.HW


class SdvInfo:
    """Stores relevant Instance information for SDV testing.

    The information that does not change throughout the test life cycle is kept
    for easy access, as it is accurate and efficient.

    This information is available even if the device is offline, which is
    critical when accessing the system properties via adb is not possible.
    """

    def __init__(self, adb_device):
        """Initialize with all relevant device information for SDV.

        Args:
            adb_device: SdvAdb for interacting with the device.
        """
        # TODO(467096713): Extend with more relevant information (e.g. device serial)

        self.instance_name = adb_device.prop.get(
            sdv_property.SdvDeviceProperty.INSTANCE_NAME
        )

        # The target and VM architecture can be inferred from the flavor of the device.
        device_flavor = adb_device.prop.get(
            sdv_property.SdvDeviceProperty.BUILD_FLAVOR
        )
        self._target = SdvTarget.from_flavor(device_flavor)
        self._vm = SdvVm.from_flavor(device_flavor)

    @property
    def is_cuttlefish(self):
        """Checks if the current device is a Cuttlefish (CF) virtual device.

        Returns:
          bool: True if the device is identified as CF, False otherwise.
        """
        return self._vm is SdvVm.CF

    @property
    def is_hardware(self):
        """Checks if the current device is a Hardware VM.

        Returns:
          bool: True if the device is identified as HW, False otherwise.
        """
        return self._vm is SdvVm.HW

    @property
    def is_core(self):
        """Checks if the target of the device is SDV Core.

        Returns:
          bool: True if the device is identified as SDV Core target, False
          otherwise.
        """
        return self._target is SdvTarget.CORE

    @property
    def is_ivi(self):
        """Checks if the target of the device is IVI.

        Returns:
          bool: True if the device is identified as IVI, False otherwise.
        """
        return self._target is SdvTarget.IVI

    @property
    def is_media(self):
        """Checks if the target of the device is Media.

        Returns:
          bool: True if the device is identified as media, False otherwise.
        """
        return self._target is SdvTarget.MEDIA

    @property
    def is_sdv(self):
        """Checks if the target is SDV.

        Both core and media targets are considered SDV.

        Returns:
          bool: True if the device is identified as SDV, False otherwise.
        """
        return self.is_core or self.is_media
