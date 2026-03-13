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
import base64
import json
from impl.downloaders.gerrit_downloader import GerritDownloader

class GerritDownloaderTest(unittest.TestCase):

    def setUp(self):
        self.mock_run = MagicMock()
        self.downloader = GerritDownloader(subprocess_run_func=self.mock_run)

    def test_download_success(self):
        # Mock 'gob-curl' output: base64 encoded JSON
        test_json = {"key": "value"}
        encoded_json = base64.b64encode(json.dumps(test_json).encode("utf-8"))

        self.mock_run.return_value.stdout = encoded_json

        url = "https://gerrit.com/files/MyTest/download"
        # Test private method via name mangling or just test public 'download'
        results = self.downloader.download(url, url)

        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["actualData"], test_json)
        self.assertEqual(results[0]["expectedData"], test_json)
        self.assertEqual(results[0]["testClassName"], "MyTest")

    def test_download_failure(self):
        self.mock_run.side_effect = Exception("gob-curl failed")

        results = self.downloader.download("url1", "url2")
        # Should return golden with empty data if download fails
        self.assertEqual(results[0]["actualData"], {})
        self.assertEqual(results[0]["expectedData"], {})

    def test_download_multiple_jsons(self):
        test_json = {"k": "v"}
        encoded = base64.b64encode(json.dumps(test_json).encode("utf-8"))
        self.mock_run.return_value.stdout = encoded

        linkPairs = [
            {"linkLeft": "url1/files/T1/download", "linkRight": "url2/files/T1/download"},
            {"linkLeft": "url3/files/T2/download", "linkRight": "url4/files/T2/download"}
        ]

        results = self.downloader.downloadMultipleJsons(linkPairs)
        self.assertEqual(len(results), 2)

    def test_parse_data_to_json_valid(self):
        left_data = b'{"a": 1}'
        right_data = b'{"b": 2}'
        l, r = self.downloader.parseDataToJson(left_data, {}, right_data, {})
        self.assertEqual(l, {"a": 1})
        self.assertEqual(r, {"b": 2})

    def test_parse_data_to_json_invalid(self):
        left_data = b'not json'
        l, r = self.downloader.parseDataToJson(left_data, {"default": 1}, None, {})
        # Should return default if parsing fails
        self.assertEqual(l, {"default": 1})

if __name__ == "__main__":
    unittest.main()
