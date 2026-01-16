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

import unittest
from unittest import mock

from sdv_test_fw.host import api_client
from sdv_test_fw.host import ho_requests
from sdv_test_fw.host import host_orchestrator


class BaseHostOrchestratorTest(unittest.TestCase):
    """Base test class handling common setup for HostOrchestrator tests."""

    def setUp(self):
        self.mock_api_client = mock.MagicMock(spec=api_client.ApiClient)

        # Patch ApiClient only during init to inject the mock.
        with mock.patch(
            "sdv_test_fw.host.api_client.ApiClient",
            return_value=self.mock_api_client,
        ):
            self.ho = host_orchestrator.HostOrchestrator(
                "http://localhost:8080"
            )

        # Reusable Data
        self.cvd_list_response = {
            "cvds": [
                {"group": "g1", "name": "cvd-1"},
                {"group": "g2", "name": "cvd-2"},
            ]
        }
        self.op_running = {"name": "ops/1", "done": False}
        self.op_done = {"name": "ops/1", "done": True}


class HostOrchestratorLogicTest(BaseHostOrchestratorTest):
    """Tests the internal logic, state management, and error handling."""

    def test_lazy_loading_success(self):
        """Verifies that the CVD list is fetched on the first access."""
        self.mock_api_client.execute.side_effect = [
            self.cvd_list_response,
            self.op_running,
            self.op_done,
        ]

        self.ho.start(0)

        # Assert: Check that the ListCVDs request was executed at least once
        self.mock_api_client.execute.assert_any_call(ho_requests.list_cvds())

    def test_caching_behavior(self):
        """Verifies that subsequent requests reuse the cached CVD list."""
        # Sequence:
        # 1. List Request (fetched once)
        # 2. Action 1 (Start) -> running, done
        # 3. Action 2 (Stop) -> running, done (List NOT fetched again)
        self.mock_api_client.execute.side_effect = [
            self.cvd_list_response,
            self.op_running,
            self.op_done,
            self.op_running,
            self.op_done,
        ]

        self.ho.start(0)
        self.ho.stop(0)

        # Assert: Verify list_cvds was called EXACTLY once.
        # We use .count() with the exact request object for a clean, strict check.
        self.assertEqual(
            self.mock_api_client.execute.call_args_list.count(
                mock.call(ho_requests.list_cvds())
            ),
            1,
        )

    @mock.patch("logging.error")
    def test_lazy_loading_handles_empty_list(self, mock_logging_error):
        """Verifies that an empty CVD list raises an exception immediately."""
        self.mock_api_client.execute.return_value = {"cvds": []}

        with self.assertRaisesRegex(Exception, "No CVDs found"):
            self.ho.start(0)

        mock_logging_error.assert_called()

    @mock.patch("logging.error")
    def test_index_out_of_bounds(self, mock_logging_error):
        """Verifies correct exception when accessing an invalid device index."""
        self.mock_api_client.execute.return_value = self.cvd_list_response

        with self.assertRaises(IndexError):
            self.ho.start(99)

        mock_logging_error.assert_called()

    @mock.patch("sdv_test_fw.host.host_orchestrator.polling")
    def test_polling_timeout_raises_timeout_error(self, mock_polling):
        """Verifies that a timeout in polling raises a specific TimeoutError."""
        mock_polling.wait_and_return_result.return_value = None

        self.mock_api_client.execute.side_effect = [
            self.cvd_list_response,
            self.op_running,
        ]

        with self.assertRaisesRegex(TimeoutError, "Operation ops/1 timed out"):
            self.ho.start(0)

    @mock.patch("logging.error")
    def test_api_client_error_propagation(self, mock_logging_error):
        """Verifies that exceptions from the ApiClient are re-raised correctly."""
        self.mock_api_client.execute.side_effect = api_client.NetworkError(
            "Connection refused"
        )

        with self.assertRaises(api_client.NetworkError):
            self.ho.start(0)

        mock_logging_error.assert_called()

    @mock.patch("logging.error")
    def test_action_api_failure_propagation(self, mock_logging_error):
        """Verifies that exceptions during the action request are re-raised."""
        self.mock_api_client.execute.side_effect = [
            self.cvd_list_response,
            api_client.HttpError(500, "Internal Server Error"),
        ]

        with self.assertRaises(api_client.HttpError):
            self.ho.powerbtn(0)

        mock_logging_error.assert_called()


class HostOrchestratorInterfaceTest(BaseHostOrchestratorTest):
    """Tests for the public API contract of HostOrchestrator.

    Verifies that client methods construct and send the exact expected
    request objects using the ho_requests factory.
    """

    def _setup_successful_action_sequence(self):
        """Configures the mock to simulate a full successful action flow.

        Sequence:
        1. List CVDs (Lazy load)
        2. Action Request (Returns 'Running')
        3. Poll Request (Returns 'Done')
        """
        self.mock_api_client.execute.side_effect = [
            self.cvd_list_response,
            self.op_running,
            self.op_done,
        ]

    def test_powerwash(self):
        self._setup_successful_action_sequence()

        self.ho.powerwash(0)

        expected_request = ho_requests.cvd_action(
            "g1", "cvd-1", ho_requests.CvdAction.POWERWASH, payload=None
        )
        self.mock_api_client.execute.assert_any_call(expected_request)

    def test_powerbtn(self):
        self._setup_successful_action_sequence()

        self.ho.powerbtn(0)

        expected_request = ho_requests.cvd_action(
            "g1", "cvd-1", ho_requests.CvdAction.POWERBTN, payload=None
        )
        self.mock_api_client.execute.assert_any_call(expected_request)

    def test_start(self):
        self._setup_successful_action_sequence()

        self.ho.start(0)

        expected_request = ho_requests.cvd_action(
            "g1", "cvd-1", ho_requests.CvdAction.START, payload={}
        )
        self.mock_api_client.execute.assert_any_call(expected_request)

    def test_stop(self):
        self._setup_successful_action_sequence()

        self.ho.stop(0)

        expected_request = ho_requests.cvd_action(
            "g1", "cvd-1", ho_requests.CvdAction.STOP, payload=None
        )
        self.mock_api_client.execute.assert_any_call(expected_request)


if __name__ == "__main__":
    unittest.main()
