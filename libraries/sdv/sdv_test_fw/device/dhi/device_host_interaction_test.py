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

from sdv_test_fw.device.dhi import hardware_dhi
from sdv_test_fw.device.dhi import local_cuttlefish_dhi
from sdv_test_fw.device.dhi import remote_cuttlefish_dhi


class DhiTestCase(unittest.TestCase):
    """Base class for DHI tests with common utilities."""

    def assert_not_implemented_methods(self, methods):
        for method_name in methods:
            with self.subTest(method=method_name):
                with self.assertRaises(NotImplementedError):
                    getattr(self.dhi, method_name)()


class HardwareTest(DhiTestCase):

    def setUp(self):
        super().setUp()

        mock_adb_device = mock.MagicMock()
        mock_device_info = mock.MagicMock()
        self.dhi = hardware_dhi.HardwareDHI(mock_adb_device, mock_device_info)

    def test_not_implemented_methods_raise_error(self):
        # Verify that calling methods that are not implemented raises an error.
        self.assert_not_implemented_methods(
            ['status', 'stop', 'start', 'restart', 'powerwash', 'powerbtn']
        )

    def test_implementation(self):
        # Verify the implementation is instantiated correctly by retrieving its info.
        self.assertEqual(self.dhi.implementation_info, 'hardware')


class LocalCuttlefishTest(DhiTestCase):

    def setUp(self):
        super().setUp()

        mock_adb_device = mock.MagicMock()
        mock_device_info = mock.MagicMock()
        self.dhi = local_cuttlefish_dhi.LocalCuttlefishDHI(
            mock_adb_device, mock_device_info
        )

    def test_not_implemented_methods_raise_error(self):
        # Verify that calling methods that are not implemented raises an error.
        self.assert_not_implemented_methods(
            ['status', 'stop', 'start', 'restart', 'powerwash', 'powerbtn']
        )

    def test_implementation(self):
        # Verify the implementation is instantiated correctly by retrieving its info.
        self.assertEqual(self.dhi.implementation_info, 'local CF VM')


class RemoteCuttlefishTest(DhiTestCase):

    def setUp(self):
        super().setUp()

        mock_adb_device = mock.MagicMock()
        mock_device_info = mock.MagicMock()
        self.dhi = remote_cuttlefish_dhi.RemoteCuttlefishDHI(
            mock_adb_device, mock_device_info
        )

    def test_not_implemented_methods_raise_error(self):
        # Verify that calling methods that are not implemented raises an error.
        self.assert_not_implemented_methods(
            ['status', 'stop', 'start', 'restart', 'powerwash', 'powerbtn']
        )

    def test_implementation(self):
        # Verify the implementation is instantiated correctly by retrieving its info.
        self.assertEqual(self.dhi.implementation_info, 'remote CF VM')


if __name__ == '__main__':
    unittest.main()
