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
import sys
from unittest.mock import MagicMock

# Mock 'requests' module before it's imported by any implementation file.
# This avoids the need for the real library during unit testing.
mock_requests = MagicMock()
sys.modules["requests"] = mock_requests

# Import all tests for discovery
from tests.adb_client_test import AdbClientTest
from tests.adb_serial_finder_test import ADBSerialFinderTest
from tests.argument_parser_test import ArgumentParserTest
from tests.atest_golden_watcher_test import AtestGoldenWatcherTest
from tests.cached_golden_test import CachedGoldenTest
from tests.fetch_presubmit_test_artifact_test import FetchPresubmitTestArtifactsTest
from tests.gerrit_downloader_test import GerritDownloaderTest
from tests.golden_file_watcher_test import GoldenFileWatcherTest
from tests.golden_watcher_factory_test import GoldenWatcherFactoryTest
from tests.motion_constants_test import MotionConstantsTest
from tests.motion_service_test import MotionServiceTest
from tests.port_finder_test import PortFinderTest
from tests.presubmit_golden_watcher_test import PresubmitGoldenWatcherTest
from tests.token_generator_test import TokenGeneratorTest
from tests.watch_web_app_request_handler_test import WatchWebAppRequestHandlerTest

if __name__ == "__main__":
    unittest.main()
