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
import io
import json
from impl.core.watch_web_app_request_handler import WatchWebAppRequestHandler
from impl.core.context import MotionWatcherContext

class MockRequest:
    def makefile(self, *args, **kwargs):
        return io.BytesIO(b"")

    def sendall(self, *args, **kwargs):
        pass

class WatchWebAppRequestHandlerTest(unittest.TestCase):

    def setUp(self):
        self.mock_context = MotionWatcherContext(
            android_build_top="/path/to/top",
            temp_dir="/tmp/motion_test",
            secret_token="fake_token",
            this_server_address="localhost",
            port=1234,
            client_url="http://client"
        )
        self.mock_service = MagicMock()
        WatchWebAppRequestHandler.context = self.mock_context
        WatchWebAppRequestHandler.service = self.mock_service

        self.request = MockRequest()
        self.client_address = ("127.0.0.1", 8080)
        self.server = MagicMock()

    def create_handler(self, method, path, headers=None, body=None):
        with patch('http.server.BaseHTTPRequestHandler.handle'):
            handler = WatchWebAppRequestHandler(self.request, self.client_address, self.server)
            handler.command = method
            handler.path = path
            handler.headers = headers or {}
            if body:
                handler.rfile = io.BytesIO(body)
            handler.wfile = io.BytesIO()
            handler.send_response = MagicMock()
            handler.send_header = MagicMock()
            handler.end_headers = MagicMock()
            return handler

    def test_verify_access_token_success(self):
        headers = {"Golden-Access-Token": "fake_token"}
        handler = self.create_handler("GET", "/", headers=headers)
        self.assertTrue(handler.verify_access_token())

    def test_verify_access_token_failure(self):
        headers = {"Golden-Access-Token": "wrong_token"}
        handler = self.create_handler("GET", "/", headers=headers)
        self.assertFalse(handler.verify_access_token())
        handler.send_response.assert_called_with(403, "Bad authorization token!")

    def test_do_OPTIONS(self):
        handler = self.create_handler("OPTIONS", "/")
        handler.do_OPTIONS()
        handler.send_response.assert_called_with(200)

    def test_do_GET_modes(self):
        self.mock_service.get_available_modes.return_value = ["atest"]
        handler = self.create_handler("GET", "/service/config/modes")
        handler.do_GET()

        response = json.loads(handler.wfile.getvalue())
        self.assertTrue(response["success"])
        self.assertEqual(response["data"], ["atest"])

    def test_do_POST_refresh_success(self):
        headers = {
            "Golden-Access-Token": "fake_token",
            "Content-Type": "application/json",
            "Content-Length": str(len(b'{"clear": true}'))
        }
        handler = self.create_handler("POST", "/service/goldens/refresh", headers=headers, body=b'{"clear": true}')

        # Mock dependencies for service_refresh_goldens
        self.mock_service.refresh_goldens.return_value = (True, None)
        WatchWebAppRequestHandler.test_entity = MagicMock()
        WatchWebAppRequestHandler.test_entity.golden_watcher.cached_goldens.values.return_value = []

        handler.do_POST()

        self.mock_service.refresh_goldens.assert_called()
        handler.send_response.assert_any_call(200)

if __name__ == "__main__":
    unittest.main()
