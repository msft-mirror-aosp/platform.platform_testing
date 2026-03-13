# Copyright 2026, The Android Open Source Project
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
#

import unittest
from unittest.mock import patch, MagicMock
from impl.utils.adb_serial_finder import ADBSerialFinder

class ADBSerialFinderTest(unittest.TestCase):

    @patch("subprocess.run")
    def test_update_model_serial_map_success(self, mock_run):
        # Mock 'adb devices -l'
        mock_devices_output = MagicMock()
        mock_devices_output.stdout = (
            b"List of devices attached\n"
            b"localhost:35725        device product:panther model:Pixel_7 device:panther transport_id:4\n"
        )

        # Mock 'adb -s localhost:35725 shell getprop ro.serialno'
        mock_serial_output = MagicMock()
        mock_serial_output.stdout = b"2A121FDH200F40\n"

        mock_run.side_effect = [mock_devices_output, mock_serial_output]

        finder = ADBSerialFinder()

        expected_map = {'Pixel_7_2A121FDH200F40': 'localhost:35725'}
        self.assertEqual(finder.model_serial_map, expected_map)

    @patch("subprocess.run")
    def test_update_model_serial_map_no_devices(self, mock_run):
        mock_output = MagicMock()
        mock_output.stdout = b"List of devices attached\n"
        mock_run.return_value = mock_output

        finder = ADBSerialFinder()
        self.assertEqual(finder.model_serial_map, {})

    @patch("subprocess.run")
    def test_update_model_serial_map_exception(self, mock_run):
        mock_run.side_effect = Exception("ADB error")

        # Should not raise exception
        finder = ADBSerialFinder()
        self.assertEqual(finder.model_serial_map, {})

if __name__ == "__main__":
    unittest.main()
