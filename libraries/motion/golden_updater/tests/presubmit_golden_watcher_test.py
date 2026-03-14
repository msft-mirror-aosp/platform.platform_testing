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
from impl.golden_watchers.presubmit_golden_watcher import PresubmitGoldenWatcher

class PresubmitGoldenWatcherTest(unittest.TestCase):

    def setUp(self):
        self.temp_dir = "/tmp/motion_test"
        self.download_dir = "/tmp/artifacts"
        self.watcher = PresubmitGoldenWatcher(self.temp_dir, self.download_dir)

    @patch("os.path.exists")
    @patch("shutil.rmtree")
    @patch("os.makedirs")
    def test_clean(self, mock_makedirs, mock_rmtree, mock_exists):
        mock_exists.return_value = True
        self.watcher.cached_goldens = {"file": "golden"}

        self.watcher.clean()

        self.assertEqual(self.watcher.cached_goldens, {})
        mock_rmtree.assert_called_with(self.temp_dir)
        mock_makedirs.assert_called_with(self.temp_dir, exist_ok=True)

    @patch("impl.golden_watchers.presubmit_golden_watcher.PresubmitGoldenWatcher._PresubmitGoldenWatcher__copy_file")
    @patch("glob.iglob")
    def test_refresh_golden_files_success(self, mock_iglob, mock_copy):
        filenames = ["test.actual_123.json"]
        test_name = "MyTest"

        # Mock CachedGolden
        mock_golden = MagicMock()
        mock_golden.video_location = None
        mock_cached_service = MagicMock(return_value=mock_golden)
        self.watcher.cached_golden_service = mock_cached_service

        self.watcher.refresh_golden_files(filenames, test_name)

        expected_filename = f"{self.download_dir}/test.actual_123.json"
        mock_copy.assert_called_with(
            expected_filename,
            f"{self.temp_dir}/MyTest_123.actual.json",
            False
        )
        self.assertIn(expected_filename, self.watcher.cached_goldens)
        self.assertEqual(mock_golden.golden_name, test_name)

    @patch("impl.golden_watchers.presubmit_golden_watcher.PresubmitGoldenWatcher._PresubmitGoldenWatcher__copy_file")
    @patch("glob.iglob")
    def test_refresh_golden_files_with_video(self, mock_iglob, mock_copy):
        filenames = ["test.actual_123.json"]
        test_name = "MyTest"
        video_file = f"{self.download_dir}/test.actual_123.mp4"
        mock_iglob.side_effect = [[video_file], []]

        # Mock CachedGolden with video
        mock_golden = MagicMock()
        mock_golden.video_location = "videos/test.mp4"
        mock_cached_service = MagicMock(return_value=mock_golden)
        self.watcher.cached_golden_service = mock_cached_service

        self.watcher.refresh_golden_files(filenames, test_name)

        expected_json = f"{self.download_dir}/test.actual_123.json"
        mock_copy.assert_any_call(expected_json, f"{self.temp_dir}/MyTest_123.actual.json", False)
        mock_copy.assert_any_call(video_file, f"{self.temp_dir}/videos/test.mp4", False)

if __name__ == "__main__":
    unittest.main()
