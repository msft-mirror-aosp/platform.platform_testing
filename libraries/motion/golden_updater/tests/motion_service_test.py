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
import os
import pathlib
import shutil
from impl.core.motion_service import MotionService
from impl.core.context import MotionWatcherContext

class MotionServiceTest(unittest.TestCase):

    def setUp(self):
        self.context = MotionWatcherContext(
            android_build_top="/path/to/top",
            temp_dir="/tmp/motion_test",
            secret_token="token",
            this_server_address="localhost",
            port=1234,
            client_url="http://client"
        )
        # Patch ADBSerialFinder in __init__
        with patch("impl.core.motion_service.ADBSerialFinder"):
            self.service = MotionService(self.context)

    def test_get_available_modes(self):
        self.service.adb_serial_finder.model_serial_map = {"Pixel_7": "serial123"}
        modes = self.service.get_available_modes()
        self.assertIn("Pixel_7", modes)
        self.assertIn("atest", modes)

    @patch("mimetypes.guess_type")
    @patch("os.path.isfile")
    @patch("os.path.abspath")
    def test_resolve_file_path_success(self, mock_abspath, mock_isfile, mock_guess):
        mock_abspath.side_effect = lambda p: p
        mock_isfile.return_value = True
        mock_guess.return_value = ("application/json", None)

        # Test regular file
        file_path, mime = self.service.resolve_file_path("/root", "test.json")
        self.assertEqual(file_path, "/root/test.json")
        self.assertEqual(mime, "application/json")

    @patch("shutil.copyfile")
    @patch("os.path.exists")
    @patch("pathlib.Path.mkdir")
    def test_update_goldens_success(self, mock_mkdir, mock_exists, mock_copy):
        mock_exists.return_value = True

        mock_golden = MagicMock()
        mock_golden.id = "g1"
        mock_golden.golden_repo_path = "repo/path.json"
        mock_golden.local_file = "/tmp/local.json"

        results, passed, failed = self.service.update_goldens({"g1"}, [mock_golden])

        self.assertEqual(passed, 1)
        self.assertEqual(results[0]["status"], "PASSED_UPDATE")
        mock_copy.assert_called_with("/tmp/local.json", "/path/to/top/repo/path.json")

    @patch("impl.core.motion_service.GoldenWatcherFactory.create_watcher")
    @patch("impl.core.motion_service.FetchPresubmitTestArtifacts")
    def test_service_presubmit_artifact_list(self, mock_fetcher, mock_factory):
        mock_watcher = MagicMock()
        mock_watcher.artifacts_download_dir = "/tmp/dl"
        mock_factory.return_value = mock_watcher

        mock_fetch_client = MagicMock()
        mock_fetch_client.list_presubmit_test_artifacts.return_value = ["test1"]
        mock_fetcher.return_value = mock_fetch_client

        watcher, client, artifacts = self.service.service_presubmit_artifact_list("I123")

        self.assertEqual(artifacts, ["test1"])
        mock_fetcher.assert_called_with("I123", "/tmp/dl")

if __name__ == "__main__":
    unittest.main()
