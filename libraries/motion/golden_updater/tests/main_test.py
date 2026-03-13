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

# Import all tests for discovery
from tests.adb_client_test import AdbClientTest
from tests.adb_serial_finder_test import ADBSerialFinderTest
from tests.argument_parser_test import ArgumentParserTest
from tests.cached_golden_test import CachedGoldenTest
from tests.golden_watcher_factory_test import GoldenWatcherFactoryTest
from tests.motion_constants_test import MotionConstantsTest
from tests.port_finder_test import PortFinderTest
from tests.token_generator_test import TokenGeneratorTest

if __name__ == "__main__":
    unittest.main()
