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
import json
from impl.models.cached_golden import CachedGolden

class CachedGoldenTest(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.local_file = os.path.join(self.temp_dir, "test.json")
        self.data = {
            "//metadata": {
                "result": "PASSED",
                "goldenRepoPath": "repo/path",
                "goldenIdentifier": "id",
                "testClassName": "Class",
                "testMethodName": "Method",
                "deviceLocalPath": "/sdcard/path"
            },
            "actual_data": "value"
        }
        with open(self.local_file, "w") as f:
            json.dump(self.data, f)

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def test_metadata_extraction(self):
        cg = CachedGolden("remote/file", self.local_file)
        self.assertEqual(cg.result, "PASSED")
        self.assertEqual(cg.golden_repo_path, "repo/path")
        self.assertEqual(cg.golden_identifier, "id")
        self.assertEqual(cg.test_class_name, "Class")
        self.assertEqual(cg.test_method_name, "Method")
        self.assertEqual(cg.device_local_path, "/sdcard/path")

    def test_metadata_removal_from_file(self):
        CachedGolden("remote/file", self.local_file)
        with open(self.local_file, "r") as f:
            updated_data = json.load(f)
        self.assertNotIn("//metadata", updated_data)
        self.assertEqual(updated_data["actual_data"], "value")
