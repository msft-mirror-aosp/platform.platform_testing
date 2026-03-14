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
import sys
from unittest.mock import patch
from impl.utils.argument_parser import ArgumentParser

class ArgumentParserTest(unittest.TestCase):

    def test_get_args_defaults(self):
        with patch.object(sys, 'argv', ['main.py']):
            with patch.dict(os.environ, {'ANDROID_BUILD_TOP': '/path/to/top'}):
                args = ArgumentParser.get_args()
                self.assertEqual(args.android_build_top, '/path/to/top')
                self.assertEqual(args.client_url, "http://motion.teams.x20web.corp.google.com/")
                self.assertIsInstance(args.port, int)

    def test_get_args_custom_values(self):
        custom_args = [
            'main.py',
            '--port', '1234',
            '--android_build_top', '/custom/top',
            '--client_url', 'http://custom.url'
        ]
        with patch.object(sys, 'argv', custom_args):
            args = ArgumentParser.get_args()
            self.assertEqual(args.port, 1234)
            self.assertEqual(args.android_build_top, '/custom/top')
            self.assertEqual(args.client_url, 'http://custom.url')
