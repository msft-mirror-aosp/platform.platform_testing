# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import dataclasses
import json
import unittest
from unittest import mock
import urllib.error
import urllib.request

from sdv_test_fw.host import api_client


class ApiRequestTest(unittest.TestCase):
    """Tests for the ApiRequest data class."""

    def test_initialization_defaults(self):
        """Verifies correct initialization with default values."""
        request = api_client.ApiRequest(path="test")

        self.assertEqual(request.path, "test")
        self.assertEqual(request.method, api_client.HttpMethod.GET)
        self.assertIsNone(request.payload)
        self.assertIsNone(request.query_params)
        self.assertIsNone(request.headers)

    def test_path_sanitization_removes_slash(self):
        """Verifies that a leading slash is automatically removed."""
        request = api_client.ApiRequest(path="/test/path")
        self.assertEqual(request.path, "test/path")

    def test_path_sanitization_leaves_clean_path(self):
        """Verifies that an existing clean path is preserved."""
        request = api_client.ApiRequest(path="test/path")
        self.assertEqual(request.path, "test/path")

    def test_full_initialization(self):
        """Verifies initialization with all fields populated."""
        payload = {"key": "value"}
        headers = {"Content-Type": "application/json"}
        params = {"q": "search"}

        request = api_client.ApiRequest(
            path="test/path",
            method=api_client.HttpMethod.POST,
            payload=payload,
            query_params=params,
            headers=headers,
        )

        self.assertEqual(request.path, "test/path")
        self.assertEqual(request.method, api_client.HttpMethod.POST)
        self.assertEqual(request.payload, payload)
        self.assertEqual(request.query_params, params)
        self.assertEqual(request.headers, headers)

    def test_immutability(self):
        """Verifies that attributes cannot be modified after creation."""
        request = api_client.ApiRequest(path="test")

        with self.assertRaises(dataclasses.FrozenInstanceError):
            request.path = "new/path"


class ApiClientTest(unittest.TestCase):
    """Tests for the ApiClient execution logic."""

    _BASE_URL = "http://localhost:8080"

    def setUp(self):
        super().setUp()
        self.client = api_client.ApiClient(self._BASE_URL)

    def _setup_success_response(self, mock_urlopen, content_str):
        """Configures the mock to return a successful 200 OK response."""
        mock_resp = mock.MagicMock()
        mock_resp.read.return_value = content_str.encode("utf-8")
        mock_urlopen.return_value.__enter__.return_value = mock_resp

    def _setup_error_response(self, mock_urlopen, status_code, content_str):
        """Configures the mock to raise an HTTPError."""
        error = urllib.error.HTTPError(
            url="http://mock",
            code=status_code,
            msg="Mock Error",
            hdrs={},
            fp=mock.MagicMock(),
        )
        error.read = mock.MagicMock(return_value=content_str.encode("utf-8"))
        mock_urlopen.side_effect = error

    @mock.patch("urllib.request.urlopen")
    def test_execute_get_request(self, mock_urlopen):
        """Verifies a simple GET request is formed and parsed correctly."""
        request = api_client.ApiRequest(path="list")

        expected_resp = {"status": "ok"}
        self._setup_success_response(mock_urlopen, json.dumps(expected_resp))

        result = self.client.execute(request)

        self.assertEqual(result, expected_resp)

        # Verify URL construction
        sent_request = mock_urlopen.call_args[0][0]
        self.assertEqual(sent_request.full_url, f"{self._BASE_URL}/list")

    @mock.patch("urllib.request.urlopen")
    def test_execute_post_with_payload(self, mock_urlopen):
        """Verifies a POST request with JSON payload is sent correctly."""
        payload = {"foo": "bar"}
        request = api_client.ApiRequest(
            path="create", method=api_client.HttpMethod.POST, payload=payload
        )

        expected_resp = {"status": "ok"}
        self._setup_success_response(mock_urlopen, json.dumps(expected_resp))

        result = self.client.execute(request)

        self.assertEqual(result, expected_resp)

        # Verify payload serialization
        sent_request = mock_urlopen.call_args[0][0]
        self.assertEqual(sent_request.data, json.dumps(payload).encode("utf-8"))

    @mock.patch("logging.error")
    @mock.patch("urllib.request.urlopen")
    def test_execute_handles_http_error(self, mock_urlopen, mock_error_logger):
        """Verifies that server errors (e.g. 404) raise HttpError."""
        request = api_client.ApiRequest(path="test")

        self._setup_error_response(
            mock_urlopen, status_code=404, content_str="Not Found"
        )

        with self.assertRaises(api_client.HttpError) as cm:
            self.client.execute(request)

        self.assertEqual(cm.exception.status_code, 404)
        mock_error_logger.assert_called_once()

    @mock.patch("logging.error")
    @mock.patch("urllib.request.urlopen")
    def test_execute_handles_invalid_json(
        self, mock_urlopen, mock_error_logger
    ):
        """Verifies that malformed JSON responses raise ApiClientError."""
        request = api_client.ApiRequest(path="test")

        self._setup_success_response(
            mock_urlopen, content_str="<html>Not JSON</html>"
        )

        with self.assertRaises(api_client.ApiClientError):
            self.client.execute(request)

        mock_error_logger.assert_called_once()

    @mock.patch("logging.error")
    @mock.patch("urllib.request.urlopen")
    def test_execute_handles_network_error(
        self, mock_urlopen, mock_error_logger
    ):
        """Verifies that connection failures raise NetworkError."""
        request = api_client.ApiRequest(path="test")

        mock_urlopen.side_effect = urllib.error.URLError(reason="Refused")

        with self.assertRaises(api_client.NetworkError):
            self.client.execute(request)

        mock_error_logger.assert_called_once()


if __name__ == "__main__":
    unittest.main()
