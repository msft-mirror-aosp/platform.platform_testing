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
import os
from unittest.mock import patch, MagicMock
from impl.golden_watchers.golden_watcher_factory import GoldenWatcherFactory
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.core.context import MotionWatcherContext

class GoldenWatcherFactoryTest(unittest.TestCase):

    def setUp(self):
        self.context = MotionWatcherContext(
            android_build_top="/path/to/top",
            temp_dir="/tmp/motion_test",
            secret_token="token",
            this_server_address="localhost",
            port=1234,
            client_url="http://client"
        )

    @patch("impl.golden_watchers.golden_watcher_factory.AtestGoldenWatcher")
    def test_create_atest_watcher(self, mock_atest):
        with patch.dict(os.environ, {"USER": "testuser"}):
            GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.ATEST, self.context)
            mock_atest.assert_called_once_with(
                "/tmp/motion_test/atest", "/tmp/atest_result_testuser/LATEST/"
            )

    @patch("impl.golden_watchers.golden_watcher_factory.PresubmitGoldenWatcher")
    def test_create_presubmit_watcher(self, mock_presubmit):
        GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.PRESUBMIT, self.context)
        mock_presubmit.assert_called_once_with(
            "/tmp/motion_test/presubmit", "/tmp/motion_test/presubmit/artifacts_download_dir"
        )

    @patch("impl.golden_watchers.golden_watcher_factory.GoldenFileWatcher")
    def test_create_adb_watcher(self, mock_adb_watcher):
        mock_adb_client = MagicMock()
        GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.ADB, self.context, mock_adb_client)
        mock_adb_watcher.assert_called_once_with(
            "/tmp/motion_test/adb", mock_adb_client
        )

    def test_create_adb_watcher_no_client(self):
        with self.assertRaises(ValueError) as cm:
            GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.ADB, self.context)
        self.assertEqual(str(cm.exception), "adb client not found")

    def test_create_improper_type(self):
        with self.assertRaises(ValueError) as cm:
            # GERRIT is defined in Types but not handled in match
            GoldenWatcherFactory.create_watcher(GoldenWatcherTypes.GERRIT, self.context)
        self.assertEqual(str(cm.exception), "Imporper Golden Watcher Type.")

if __name__ == "__main__":
    unittest.main()
