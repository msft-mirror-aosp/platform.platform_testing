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
import logging

from mobly import signals
from sdv_test_fw.device import sdv_adb
from sdv_test_fw.device import sdv_property

DEVICE_TAG_PREFIX = 'device'


def build_device_tag(device_id: int) -> str:
    return f'{DEVICE_TAG_PREFIX}{device_id}'


class SdvDeviceInfoError(signals.ControllerError):
    """Raised when there is an issue reading or parsing SDV device information."""

    pass


class SdvTarget(enum.Enum):
    CORE = 'core'
    IVI = 'ivi'
    MEDIA = 'media'

    @classmethod
    def from_flavor(cls, device_flavor: str) -> 'SdvTarget':
        # The flavor property contains information about the target.
        # SdvTarget values must match with the targets in the property.
        # e.g. core: sdv_core_cf-userdebug
        # e.g. ivi: sdv_ivi_cf-userdebug
        # e.g. media: sdv_media_cf-userdebug
        for target in cls:
            if target.value in device_flavor:
                return target

        raise SdvDeviceInfoError(f'Device target not found in {device_flavor}')


class SdvVm(enum.Enum):
    CF = 'CF VM'
    HW = 'HW VM'

    @classmethod
    def from_flavor(cls, device_flavor: str) -> 'SdvVm':
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

    INSTANCE_NAME_PREFIX = 'instance'

    def __init__(self, adb_device: sdv_adb.SdvAdb):
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
    def device_tag(self) -> str:
        # In HW, only 2VMs is supported for multiVM tests. There is a temporary
        # workaround in the setup that requires the instance name for the second
        # VM to be instance3. TODO(458268779): Remove conditional when bug is
        # fixed and setup is aligned between CF and HW.
        if self.is_hardware:
            if self.instance_number > 1:
                return build_device_tag(device_id=2)

        return build_device_tag(self.instance_number)

    @property
    def instance_number(self) -> int:
        """Returns the instance number derived from the instance name.

        Returns:
          int: Instance number (i.e. 1, 2, 3)
        """
        if not self.instance_name.startswith(self.INSTANCE_NAME_PREFIX):
            logging.error(
                'Not possible to parse instance number. Unexpected instance'
                f' name {self.instance_name}. Expected format:'
                f' {self.INSTANCE_NAME_PREFIX}<number>'
            )
            raise SdvDeviceInfoError(
                f'Invalid instance name format: {self.instance_name}. Expected'
                f' format: {self.INSTANCE_NAME_PREFIX}<number>'
            )
        return int(self.instance_name[len(self.INSTANCE_NAME_PREFIX) :])

    @property
    def is_cuttlefish(self) -> bool:
        """Checks if the current device is a Cuttlefish (CF) virtual device.

        Returns:
          bool: True if the device is identified as CF, False otherwise.
        """
        return self._vm is SdvVm.CF

    @property
    def is_hardware(self) -> bool:
        """Checks if the current device is a Hardware VM.

        Returns:
          bool: True if the device is identified as HW, False otherwise.
        """
        return self._vm is SdvVm.HW

    @property
    def is_core(self) -> bool:
        """Checks if the target of the device is SDV Core.

        Returns:
          bool: True if the device is identified as SDV Core target, False
          otherwise.
        """
        return self._target is SdvTarget.CORE

    @property
    def is_ivi(self) -> bool:
        """Checks if the target of the device is IVI.

        Returns:
          bool: True if the device is identified as IVI, False otherwise.
        """
        return self._target is SdvTarget.IVI

    @property
    def is_media(self) -> bool:
        """Checks if the target of the device is Media.

        Returns:
          bool: True if the device is identified as media, False otherwise.
        """
        return self._target is SdvTarget.MEDIA

    @property
    def is_sdv(self) -> bool:
        """Checks if the target is SDV.

        Both core and media targets are considered SDV.

        Returns:
          bool: True if the device is identified as SDV, False otherwise.
        """
        return self.is_core or self.is_media
