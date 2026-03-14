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
from impl.models.motion_constants import create_presubmit_artifacts_list_url, create_get_download_url_for_presubmit_artifact

class MotionConstantsTest(unittest.TestCase):

    def test_create_presubmit_artifacts_list_url(self):
        url = create_presubmit_artifacts_list_url("I123", 100)
        self.assertIn("invocationId=I123", url)
        self.assertIn("maxResults=100", url)
        self.assertIn("androidbuildinternal.googleapis.com", url)

    def test_create_get_download_url_for_presubmit_artifact(self):
        url = create_get_download_url_for_presubmit_artifact("res/id", "I123")
        self.assertIn("invocationId=I123", url)
        # Check if resource_id is quoted
        self.assertIn("res%2Fid", url)
        self.assertIn("androidbuildinternal.googleapis.com", url)
