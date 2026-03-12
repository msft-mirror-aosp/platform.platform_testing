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
from impl.golden_watchers.golden_file_watcher import GoldenFileWatcher

class GoldenFileWatcherTest(unittest.TestCase):

    def setUp(self):
        self.temp_dir = "/tmp/motion_test"
        self.mock_adb = MagicMock()

    @patch("os.path.isdir")
    @patch("os.makedirs")
    @patch("impl.golden_watchers.golden_file_watcher.GoldenFileWatcher.refresh_golden_files")
    def test_init(self, mock_refresh, mock_makedirs, mock_isdir):
        mock_isdir.return_value = False
        GoldenFileWatcher(self.temp_dir, self.mock_adb)
        mock_makedirs.assert_called_with(self.temp_dir, exist_ok=True)
        mock_refresh.assert_called_once()

    @patch("impl.golden_watchers.golden_file_watcher.GoldenFileWatcher.adb_pull")
    @patch("impl.golden_watchers.golden_file_watcher.GoldenFileWatcher.adb_pull_image")
    @patch("impl.golden_watchers.golden_file_watcher.GoldenFileWatcher.run_adb_command")
    @patch("datetime.datetime")
    def test_refresh_golden_files(self, mock_datetime, mock_run, mock_pull_image, mock_pull):
        mock_run.side_effect = ["1000", "100.0 /path/to/test.actual.json"]
        mock_datetime.now().timestamp.return_value = 2000
        mock_pull.return_value = "/tmp/local_test.json"

        # Mock CachedGolden
        mock_golden = MagicMock()
        mock_golden.video_location = "test.mp4"
        mock_golden.device_local_path = "/sdcard/test"
        mock_cached_service = MagicMock(return_value=mock_golden)

        with patch("impl.golden_watchers.golden_file_watcher.GoldenFileWatcher.refresh_golden_files"):
            watcher = GoldenFileWatcher(self.temp_dir, self.mock_adb, mock_cached_service)

        watcher.refresh_golden_files()

        mock_pull.assert_called_with("/path/to/test.actual.json")
        mock_pull_image.assert_called_with("/sdcard/test", "test.mp4")
        self.assertIn("/path/to/test.actual.json", watcher.cached_goldens)

    @patch("impl.golden_watchers.golden_file_watcher.GoldenFileWatcher.run_adb_command")
    @patch("hashlib.md5")
    def test_adb_pull(self, mock_md5, mock_run):
        mock_md5.return_value.hexdigest.return_value = "hash123"

        with patch("impl.golden_watchers.golden_file_watcher.GoldenFileWatcher.refresh_golden_files"):
            watcher = GoldenFileWatcher(self.temp_dir, self.mock_adb)

        local_file = watcher.adb_pull("/remote/path/test.json")

        self.assertEqual(local_file, "/tmp/motion_test/test_hash123.json")
        mock_run.assert_any_call(["pull", "/remote/path/test.json", local_file])
        mock_run.assert_any_call(["shell", "rm", "/remote/path/test.json"])

if __name__ == "__main__":
    unittest.main()
