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
import tempfile
import shutil
from unittest.mock import patch
from impl.utils.token_generator import TokenGenerator

class TokenGeneratorTest(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.token_file = os.path.join(self.temp_dir, "token")

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def test_token_is_string(self):
        with patch("impl.utils.token_generator.GOLDEN_ACCESS_TOKEN_LOCATION", self.token_file):
            token = TokenGenerator.get_token()
            self.assertIsInstance(token, str)
            self.assertGreater(len(token), 0)

    def test_token_is_random_across_cleans(self):
        # Run 1: generate first token
        with patch("impl.utils.token_generator.GOLDEN_ACCESS_TOKEN_LOCATION", self.token_file):
            token1 = TokenGenerator.get_token()

        # Delete file to force generation
        os.remove(self.token_file)

        # Run 2: generate second token
        with patch("impl.utils.token_generator.GOLDEN_ACCESS_TOKEN_LOCATION", self.token_file):
            token2 = TokenGenerator.get_token()

        self.assertNotEqual(token1, token2)

    def test_token_is_persistent(self):
        with patch("impl.utils.token_generator.GOLDEN_ACCESS_TOKEN_LOCATION", self.token_file):
            token1 = TokenGenerator.get_token()
            token2 = TokenGenerator.get_token()
            self.assertEqual(token1, token2)
