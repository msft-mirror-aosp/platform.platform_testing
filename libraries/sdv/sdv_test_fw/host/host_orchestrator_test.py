# Copyright (C) 2026 The Android Open Source Project
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

import unittest
from unittest import mock

from sdv_test_fw.host import host_orchestrator


@mock.patch("httplib2.Http")
class HostOrchestratorTest(unittest.TestCase):

  def test_powerwash_success(self, mock_http_class):
    mock_http = mock.Mock()
    mock_http_class.return_value = mock_http
    mock_http.request.side_effect = [
        (
            mock.Mock(status=200),
            b'{"cvds": [{"group": "cvd-group", "name": "cvd-1"}]}',
        ),
        (mock.Mock(status=200), b'{"name": "op", "done": false}'),
        (mock.Mock(status=200), b'{"name": "op", "done": true}'),
    ]
    ho = host_orchestrator.HostOrchestrator(
        "http://localhost:8080", 0
    )
    result = ho.powerwash()
    self.assertEqual(
        result, host_orchestrator.HostOrchestrator.Status.SUCCESS
    )
    self.assertEqual(mock_http.request.call_count, 3)

  def test_powerbtn_success(self, mock_http_class):
    mock_http = mock.Mock()
    mock_http_class.return_value = mock_http
    mock_http.request.side_effect = [
        (
            mock.Mock(status=200),
            b'{"cvds": [{"group": "cvd-group", "name": "cvd-1"}]}',
        ),
        (mock.Mock(status=200), b'{"name": "op", "done": false}'),
        (mock.Mock(status=200), b'{"name": "op", "done": true}'),
    ]
    ho = host_orchestrator.HostOrchestrator(
        "http://localhost:8080", 0
    )
    result = ho.powerbtn()
    self.assertEqual(
        result, host_orchestrator.HostOrchestrator.Status.SUCCESS
    )
    self.assertEqual(mock_http.request.call_count, 3)

  @mock.patch("logging.error")
  def test_init_no_cvds(self, mock_logging_error, mock_http_class):
    mock_http = mock.Mock()
    mock_http_class.return_value = mock_http
    mock_http.request.return_value = (mock.Mock(status=200), b'{"cvds": []}')
    with self.assertRaises(Exception) as context:
      host_orchestrator.HostOrchestrator("http://localhost:8080", 0)
    self.assertIn("No CVDs found", str(context.exception))
    mock_logging_error.assert_called()

  @mock.patch("logging.error")
  def test_init_http_error(self, mock_logging_error, mock_http_class):
    mock_http = mock.Mock()
    mock_http_class.return_value = mock_http
    mock_http.request.return_value = (
        mock.Mock(status=500),
        b"Internal Server Error",
    )
    with self.assertRaises(Exception) as context:
      host_orchestrator.HostOrchestrator("http://localhost:8080", 0)
    self.assertIn("HTTP Error: 500", str(context.exception))
    mock_logging_error.assert_called()

  @mock.patch("time.sleep")
  @mock.patch("time.time")
  @mock.patch("logging.error")
  def test_powerwash_timeout(
      self, mock_logging_error, mock_time, mock_sleep, mock_http_class
  ):
    mock_http = mock.Mock()
    mock_http_class.return_value = mock_http
    responses = [
        (
            mock.Mock(status=200),
            b'{"cvds": [{"group": "cvd-group", "name": "cvd-1"}]}',
        ),
        (mock.Mock(status=200), b'{"name": "op"}'),
    ]
    status_response = (mock.Mock(status=200), b'{"name": "op", "done": false}')

    def mock_request_logic(*args, **kwargs):
      if responses:
        return responses.pop(0)
      return status_response

    mock_http.request.side_effect = mock_request_logic

    start_time = 1726052400.0
    timeout_ms = 100
    mock_time.return_value = start_time

    def advance_time(sleep_duration_s):
      mock_time.return_value += sleep_duration_s

    mock_sleep.side_effect = advance_time

    ho = host_orchestrator.HostOrchestrator(
        "http://localhost:8080", 0
    )
    original_timeout = (
        host_orchestrator.HostOrchestrator._WAIT_FOR_OPERATION_TIMEOUT_MS
    )
    host_orchestrator.HostOrchestrator._WAIT_FOR_OPERATION_TIMEOUT_MS = (
        timeout_ms
    )

    with self.assertRaisesRegex(Exception, "Operation op timed out"):
      ho.powerwash()

    host_orchestrator.HostOrchestrator._WAIT_FOR_OPERATION_TIMEOUT_MS = (
        original_timeout
    )

    mock_logging_error.assert_any_call(
        "HostOrchestrator#wait_for_operation: Operation timed out. Name: <%s>",
        "op",
    )


if __name__ == "__main__":
  unittest.main()
