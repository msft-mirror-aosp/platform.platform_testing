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

"""SDV Device Class"""

from sdv_test_fw.device import sdv_info
from sdv_test_fw.device.dhi import hardware_dhi
from sdv_test_fw.device.dhi import remote_cuttlefish_dhi
from sdv_test_fw.device.sdv_adb import SdvAdb


class SdvDevice:

    def _device_controller(self):
        # The controller instantiation depends on the architecture of the device.
        # We consider two scenarios:
        # 1) It is a CF VM
        # 2) It is a HW VM
        if self.info.is_cuttlefish:
            # TODO(467096715): add distinction beetwen local and remote controllers
            # for CF once we have a way of identifying tests execution environment.
            self.__adb.log().info('Remote Cuttlefish DHI controller')
            return remote_cuttlefish_dhi.RemoteCuttlefishDHI(
                self.__adb, self.info
            )

        self.__adb.log().info('Hardware DHI controller')
        return hardware_dhi.HardwareDHI(self.__adb, self.info)

    def __init__(self, android_device):
        self.__adb = SdvAdb(android_device)
        self.__services = android_device.services

        # Device information to have available even when the device is offline.
        self.info = sdv_info.SdvInfo(self.__adb)

        # Device Host Controller (DHI) set as VM member for familiarity.
        self.vm = self._device_controller()

    # Get ADB
    def adb(self):
        return self.__adb

    # Get Logcat
    def services(self):
        return self.__services
