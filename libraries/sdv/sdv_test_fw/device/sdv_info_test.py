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

import unittest
from unittest import mock

from sdv_test_fw.device import sdv_info


class SdvDeviceTagTests(unittest.TestCase):

    def test_build_device_tag(self):
        self.assertEqual(sdv_info.build_device_tag(1), "device1")
        self.assertEqual(sdv_info.build_device_tag(2), "device2")
        self.assertEqual(sdv_info.build_device_tag(3), "device3")


class SdvTargetTests(unittest.TestCase):

    def test_core_from_flavor(self):
        device_flavor = "sdv_core_cf-userdebug"

        result = sdv_info.SdvTarget.from_flavor(device_flavor)
        self.assertEqual(result, sdv_info.SdvTarget.CORE)

    def test_ivi_from_flavor(self):
        device_flavor = "sdv_ivi_cf-userdebug"

        result = sdv_info.SdvTarget.from_flavor(device_flavor)
        self.assertEqual(result, sdv_info.SdvTarget.IVI)

    def test_media_from_flavor(self):
        device_flavor = "sdv_media_cf-userdebug"

        result = sdv_info.SdvTarget.from_flavor(device_flavor)
        self.assertEqual(result, sdv_info.SdvTarget.MEDIA)


class SdvVmTests(unittest.TestCase):

    def test_cf_from_flavor(self):
        device_flavor = "sdv_core_cf-userdebug"

        result = sdv_info.SdvVm.from_flavor(device_flavor)
        self.assertEqual(result, sdv_info.SdvVm.CF)

    def test_hw_from_flavor(self):
        device_flavor = "sdv_core_arm64-userdebug"

        result = sdv_info.SdvVm.from_flavor(device_flavor)
        self.assertEqual(result, sdv_info.SdvVm.HW)


class SdvInfoProperties(unittest.TestCase):
    DEFAULT_TARGET = sdv_info.SdvTarget.CORE
    DEFAULT_VM = sdv_info.SdvVm.CF

    def setUp(self):
        super().setUp()
        mock_adb_device = mock.MagicMock()
        # Provide a valid internal call to
        # adb_device.prop.get(SdvDeviceProperty.BUILD_FLAVOR) as it allows to
        # complete the initialization without errors. Instance is also set with
        # prop.get but it is set directly. We are overriding the members in each
        # test with default valid values so the prop result does not matter.
        mock_adb_device.prop.get.return_value = "sdv_core_cf-userdebug"
        self.info = sdv_info.SdvInfo(mock_adb_device)
        self.set_info_members()

    def set_info_members(self, target=DEFAULT_TARGET, vm=DEFAULT_VM):
        self.info.instance_name = "instance1"
        self.info._target = target
        self.info._vm = vm

    def test_instance_number(self):
        self.assertEqual(self.info.instance_number, 1)

    def test_device_tag_when_cf(self):
        self.set_info_members(vm=sdv_info.SdvVm.CF)
        self.info.instance_name = "instance1"
        self.assertEqual(self.info.device_tag, "device1")

        self.info.instance_name = "instance2"
        self.assertEqual(self.info.device_tag, "device2")

        self.info.instance_name = "instance3"
        self.assertEqual(self.info.device_tag, "device3")

    def test_device_tag_when_hw(self):
        self.set_info_members(vm=sdv_info.SdvVm.HW)
        self.info.instance_name = "instance1"
        self.assertEqual(self.info.device_tag, "device1")

        self.info.instance_name = "instance3"
        self.assertEqual(self.info.device_tag, "device2")

    @mock.patch("logging.error")
    def test_instance_number_invalid(self, mock_logging_error):
        self.info.instance_name = "invalid"
        with self.assertRaises(sdv_info.SdvDeviceInfoError):
            _ = self.info.instance_number

        mock_logging_error.assert_called()

    def test_when_vm_is_cf(self):
        self.set_info_members(vm=sdv_info.SdvVm.CF)

        self.assertTrue(self.info.is_cuttlefish, "Should be True for a CF VM.")
        self.assertFalse(self.info.is_hardware, "Should be False for a CF VM.")

    def test_when_vm_is_hw(self):
        self.set_info_members(vm=sdv_info.SdvVm.HW)

        self.assertTrue(
            self.info.is_hardware, "Should be True for a Hardware VM."
        )
        self.assertFalse(
            self.info.is_cuttlefish, "Should be False for a Hardware VM."
        )

    def test_when_target_is_core(self):
        self.set_info_members(target=sdv_info.SdvTarget.CORE)

        self.assertTrue(self.info.is_core, "Should be True for a core target.")
        self.assertTrue(self.info.is_sdv, "Should be True for a core target.")
        self.assertFalse(self.info.is_ivi, "Should be False for a core target.")
        self.assertFalse(
            self.info.is_media, "Should be False for a core target."
        )

    def test_when_target_is_ivi(self):
        self.set_info_members(target=sdv_info.SdvTarget.IVI)

        self.assertTrue(self.info.is_ivi, "Should be True for an IVI target.")
        self.assertFalse(self.info.is_sdv, "Should be False for an IVI target.")
        self.assertFalse(
            self.info.is_core, "Should be False for an IVI target."
        )
        self.assertFalse(
            self.info.is_media, "Should be False for an IVI target."
        )

    def test_when_target_is_media(self):
        self.set_info_members(target=sdv_info.SdvTarget.MEDIA)

        self.assertTrue(
            self.info.is_media, "Should be True for a media target."
        )
        self.assertTrue(self.info.is_sdv, "Should be True for a media target.")
        self.assertFalse(
            self.info.is_ivi, "Should be False for a media target."
        )
        self.assertFalse(
            self.info.is_core, "Should be False for a media target."
        )


if __name__ == "__main__":
    unittest.main()
