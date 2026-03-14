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

        mock_run.return_value = mock_devices_output

        finder = ADBSerialFinder()

        # New format: {model}_{adb_identifier}
        expected_map = {'Pixel_7_localhost:35725': 'localhost:35725'}
        self.assertEqual(finder.model_serial_map, expected_map)

    @patch("subprocess.run")
    def test_update_model_serial_map_multiple_devices(self, mock_run):
        # Mock 'adb devices -l' with the specific values provided by the user
        mock_devices_output = MagicMock()
        mock_devices_output.stdout = (
            b"List of devices attached\n"
            b"0.0.0.0:6520           device product:cf_x86_64_phone model:Cuttlefish_GMS_x86_64 device:vsoc_x86_64 transport_id:28\n"
            b"0.0.0.0:6521           device product:cf_x86_64_phone model:Cuttlefish_GMS_x86_64 device:vsoc_x86_64 transport_id:32\n"
            b"0.0.0.0:6522           device product:cf_x86_64_phone model:Cuttlefish_GMS_x86_64 device:vsoc_x86_64 transport_id:31\n"
            b"0.0.0.0:6523           device product:cf_x86_64_phone model:Cuttlefish_GMS_x86_64 device:vsoc_x86_64 transport_id:29\n"
            b"0.0.0.0:6524           device product:cf_x86_64_phone model:Cuttlefish_GMS_x86_64 device:vsoc_x86_64 transport_id:30\n"
            b"localhost:34917        device product:panther model:Pixel_7 device:panther transport_id:34\n"
            b"localhost:38669        device product:husky model:Pixel_8_Pro device:husky transport_id:33\n"
        )

        mock_run.return_value = mock_devices_output

        finder = ADBSerialFinder()

        self.assertEqual(len(finder.model_serial_map), 7)
        self.assertIn('Cuttlefish_GMS_x86_64_0.0.0.0:6520', finder.model_serial_map)
        self.assertIn('Cuttlefish_GMS_x86_64_0.0.0.0:6521', finder.model_serial_map)
        self.assertIn('Cuttlefish_GMS_x86_64_0.0.0.0:6522', finder.model_serial_map)
        self.assertIn('Cuttlefish_GMS_x86_64_0.0.0.0:6523', finder.model_serial_map)
        self.assertIn('Cuttlefish_GMS_x86_64_0.0.0.0:6524', finder.model_serial_map)
        self.assertIn('Pixel_7_localhost:34917', finder.model_serial_map)
        self.assertIn('Pixel_8_Pro_localhost:38669', finder.model_serial_map)

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
