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
from impl.downloaders.fetch_presubmit_test_artifact import FetchPresubmitTestArtifacts

class FetchPresubmitTestArtifactsTest(unittest.TestCase):

    def setUp(self):
        self.invocation_id = "I123"
        self.download_dir = "/tmp/artifacts"

        # Patch __refresh_user_token and __create_download_directory during init
        with patch.object(FetchPresubmitTestArtifacts, "_FetchPresubmitTestArtifacts__refresh_user_token"), \
             patch.object(FetchPresubmitTestArtifacts, "_FetchPresubmitTestArtifacts__create_download_directory"):
            self.fetcher = FetchPresubmitTestArtifacts(self.invocation_id, self.download_dir)
            self.fetcher._token = "fake_token"

    @patch("subprocess.run")
    def test_get_oauth_token_success(self, mock_run):
        mock_run.return_value.stdout = 'oauth2_token: "token123"'
        token = self.fetcher._FetchPresubmitTestArtifacts__get_oauth_token("user@google.com", "scope", subprocess_run=mock_run)
        self.assertEqual(token, "token123")

    def test_fetch_artifacts_list_success(self):
        mock_api = MagicMock()
        mock_response = MagicMock()
        mock_response.json.return_value = {
            "test_artifacts": [
                {"name": "test1.actual.json"},
                {"name": "test2.other"}
            ],
            "nextPageToken": None
        }
        mock_api.get.return_value = mock_response

        artifacts = self.fetcher._FetchPresubmitTestArtifacts__fetch_artifacts_list(api_client=mock_api)
        self.assertEqual(artifacts, ["test1.actual.json"])

    def test_get_signed_download_url_success(self):
        mock_api = MagicMock()
        mock_response = MagicMock()
        mock_response.json.return_value = {"signedUrl": "http://signed.url"}
        mock_api.get.return_value = mock_response

        url = self.fetcher._FetchPresubmitTestArtifacts__get_signed_download_url("res_id", api_client=mock_api)
        self.assertEqual(url, "http://signed.url")

    def test_list_presubmit_test_artifacts(self):
        with patch.object(self.fetcher, "_FetchPresubmitTestArtifacts__fetch_artifacts_list") as mock_fetch:
            mock_fetch.return_value = ["path/to/test1.actual.json", "path/to/test2.actual.json"]

            test_names = self.fetcher.list_presubmit_test_artifacts()
            # "path/to/test1.actual" becomes "path_to_test1"
            self.assertIn("path_to_test1", test_names)
            self.assertIn("path_to_test2", test_names)

if __name__ == "__main__":
    unittest.main()
