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
from unittest.mock import MagicMock
from impl.utils.adb_client import AdbClient

class AdbClientTest(unittest.TestCase):

    def test_run_adb_command(self):
        mock_run = MagicMock()
        mock_run.return_value.stdout = b"some output\n"

        client = AdbClient("serial123", subprocess_run_func=mock_run)
        result = client.run_adb_command(["shell", "ls"])

        self.assertEqual(result, "some output\n")
        mock_run.assert_called_with(
            ["adb", "-s", "serial123", "shell", "ls"],
            check=True,
            capture_output=True
        )

    def test_run_as_root_restarting(self):
        mock_run = MagicMock()
        # First call for 'root', second for 'wait-for-device'
        mock_run.return_value.stdout = b"restarting adbd as root\n"

        client = AdbClient("serial123", subprocess_run_func=mock_run)
        self.assertTrue(client.run_as_root())

        self.assertEqual(mock_run.call_count, 2)
        mock_run.assert_any_call(
            ["adb", "-s", "serial123", "root"],
            check=True,
            capture_output=True
        )
        mock_run.assert_any_call(
            ["adb", "-s", "serial123", "wait-for-device"],
            check=True,
            capture_output=True
        )

    def test_run_as_root_already_root(self):
        mock_run = MagicMock()
        mock_run.return_value.stdout = b"adbd is already running as root\n"

        client = AdbClient("serial123", subprocess_run_func=mock_run)
        self.assertTrue(client.run_as_root())

        self.assertEqual(mock_run.call_count, 1)

    def test_run_as_root_failure(self):
        mock_run = MagicMock()
        mock_run.return_value.stdout = b"error: device not found\n"

        client = AdbClient("serial123", subprocess_run_func=mock_run)
        self.assertFalse(client.run_as_root())

if __name__ == "__main__":
    unittest.main()
