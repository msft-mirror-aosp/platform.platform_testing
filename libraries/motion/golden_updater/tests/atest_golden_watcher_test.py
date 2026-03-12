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
from unittest.mock import patch, MagicMock, call
from impl.golden_watchers.atest_golden_watcher import AtestGoldenWatcher

class AtestGoldenWatcherTest(unittest.TestCase):

    def setUp(self):
        self.temp_dir = "/tmp/motion_test"
        self.atest_dir = "/tmp/atest_latest"

    @patch("os.makedirs")
    @patch("impl.golden_watchers.atest_golden_watcher.AtestGoldenWatcher.refresh_golden_files")
    def test_init(self, mock_refresh, mock_makedirs):
        AtestGoldenWatcher(self.temp_dir, self.atest_dir)
        mock_makedirs.assert_called_with(self.temp_dir, exist_ok=True)
        mock_refresh.assert_called_once()

    @patch("glob.iglob")
    @patch("os.path.getmtime")
    @patch("impl.golden_watchers.atest_golden_watcher.AtestGoldenWatcher.copy_file")
    @patch("os.makedirs")
    def test_refresh_golden_files_success(self, mock_makedirs, mock_copy, mock_mtime, mock_iglob):
        # Realistic filename matching the pattern:
        # r".*/(?P<name>.*)\.actual((\.(?P<ext1>[a-zA-Z0-9]+)_(?P<hash1>\d+)\.txt)|(_(?P<hash2>\d+)\.(?P<ext2>[a-zA-Z0-9]+)))(?P<compressed>\.gz)?"
        json_file = "/path/to/test.actual_123.json"
        mock_iglob.return_value = [json_file]
        mock_mtime.return_value = 123456789.0

        # Mock CachedGolden
        mock_golden = MagicMock()
        mock_golden.video_location = None
        mock_cached_service = MagicMock(return_value=mock_golden)

        with patch("impl.golden_watchers.atest_golden_watcher.AtestGoldenWatcher.refresh_golden_files"):
            watcher = AtestGoldenWatcher(self.temp_dir, self.atest_dir, mock_cached_service)

        watcher.refresh_golden_files()

        mock_copy.assert_called_with(
            json_file,
            "/tmp/motion_test/test_123.actual.json",
            False
        )
        self.assertIn(json_file, watcher.cached_goldens)

    @patch("glob.iglob")
    @patch("os.path.getmtime")
    @patch("impl.golden_watchers.atest_golden_watcher.AtestGoldenWatcher.copy_file")
    @patch("os.makedirs")
    def test_refresh_golden_files_with_video(self, mock_makedirs, mock_copy, mock_mtime, mock_iglob):
        json_file = "/path/to/test.actual_123.json"
        video_file = "/path/to/test.actual_123.mp4"

        mock_iglob.side_effect = [
            [json_file], # First call for json files
            [video_file], # Second call for mp4 files
            [] # Third call for zip files
        ]
        mock_mtime.return_value = 123456789.0

        # Mock CachedGolden with video_location
        mock_golden = MagicMock()
        mock_golden.video_location = "videos/test.mp4"
        mock_cached_service = MagicMock(return_value=mock_golden)

        with patch("impl.golden_watchers.atest_golden_watcher.AtestGoldenWatcher.refresh_golden_files"):
            watcher = AtestGoldenWatcher(self.temp_dir, self.atest_dir, mock_cached_service)

        watcher.refresh_golden_files()

        mock_copy.assert_any_call(json_file, "/tmp/motion_test/test_123.actual.json", False)
        mock_copy.assert_any_call(video_file, "/tmp/motion_test/videos/test.mp4", False)

if __name__ == "__main__":
    unittest.main()
