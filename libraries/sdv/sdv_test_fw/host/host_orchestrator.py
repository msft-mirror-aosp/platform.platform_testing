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

"""Client for interacting with the Android Host Orchestrator service."""

import logging
from typing import Any, Dict, List, Optional

from sdv_test_fw.host import api_client
from sdv_test_fw.host import ho_requests
from sdv_test_fw.verification import polling


class HostOrchestrator:
    """Interacts with the CF VM through Host Orchestrator API.

    This client manages the lifecycle and actions of Cuttlefish Virtual Devices
    (CVDs)
    running on a host. It handles retrieving device lists, executing actions
    like
    start/stop, and polling for operation completion.
    """

    def __init__(self, host_orchestrator_url: str):
        """Initializes the Host Orchestrator client.

        Args:
            host_orchestrator_url: The root URL of the Host Orchestrator
              service.
        """
        self._api_client = api_client.ApiClient(host_orchestrator_url)
        # Lazy initialization: _cvds is None until the first request needs it.
        self._cvds = None

    # ==========================================================================
    # Public Interface
    # ==========================================================================

    def powerwash(self, device_index: int):
        """Powerwashes (factory resets) a specific CVD.

        Args:
            device_index: The zero-based index of the device in the host's
              device list.

        Raises:
            IndexError: If the device_index is out of bounds of the available
            CVDs.
            api_client.ApiClientError: If the underlying API request fails.
            TimeoutError: If the operation does not complete within the timeout.
        """
        self._perform_action(device_index, ho_requests.CvdAction.POWERWASH)

    def powerbtn(self, device_index: int):
        """Simulates a power button press on a specific CVD.

        Args:
            device_index: The zero-based index of the device in the host's
              device list.

        Raises:
            IndexError: If the device_index is out of bounds of the available
            CVDs.
            api_client.ApiClientError: If the underlying API request fails.
            TimeoutError: If the operation does not complete within the timeout.
        """
        self._perform_action(device_index, ho_requests.CvdAction.POWERBTN)

    def start(self, device_index: int):
        """Starts a stopped CVD.

        Args:
            device_index: The zero-based index of the device in the host's
              device list.

        Raises:
            IndexError: If the device_index is out of bounds of the available
            CVDs.
            api_client.ApiClientError: If the underlying API request fails.
            TimeoutError: If the operation does not complete within the timeout.
        """
        # The original code sent empty JSON "{}" for start
        self._perform_action(
            device_index, ho_requests.CvdAction.START, payload={}
        )

    def stop(self, device_index: int):
        """Stops a running CVD.

        Args:
            device_index: The zero-based index of the device in the host's
              device list.

        Raises:
            IndexError: If the device_index is out of bounds of the available
            CVDs.
            api_client.ApiClientError: If the underlying API request fails.
            TimeoutError: If the operation does not complete within the timeout.
        """
        self._perform_action(device_index, ho_requests.CvdAction.STOP)

    # ==========================================================================
    # Internal Logic
    # ==========================================================================

    def _load_cvds(self):
        """Fetches the list of CVDs from the host and caches them locally.

        This method performs a lazy load; if `self._cvds` is already populated,
        it returns immediately. If the server returns an empty list or invalid
        response, this method raises an exception.

        Raises:
            api_client.ApiClientError: If the network request fails.
            Exception: If the response format is invalid or the host reports
            zero CVDs.
        """
        if self._cvds is not None:
            return

        try:
            response = self._api_client.execute(ho_requests.list_cvds())
        except api_client.ApiClientError as e:
            logging.error("HostOrchestrator: Error listing CVDs: %s", e)
            raise  # Re-raise the explicit API error

        if not response or "cvds" not in response:
            logging.error(
                "HostOrchestrator: Invalid response structure: %s", response
            )
            raise Exception("Failed to get device CVD. Invalid response.")

        cvds = [ho_requests.Cvd.from_json(item) for item in response["cvds"]]

        # Explicit verification: Fail fast if the list is empty
        if not cvds:
            logging.error("HostOrchestrator: Server returned 0 CVDs.")
            raise Exception("Failed to get device CVD. No CVDs found.")

        # Cache the result only if valid
        self._cvds = cvds

    def _get_device_cvd(self, device_index: int) -> ho_requests.Cvd:
        """Retrieves the cached CVD object for the given index.

        Ensures the local cache is populated before access.

        Args:
            device_index: The zero-based index of the device.

        Returns:
            The Cvd object corresponding to the index.

        Raises:
            IndexError: If the device_index is outside the range of available
            CVDs.
            api_client.ApiClientError: If _load_cvds fails to retrieve the
            device list.
        """
        self._load_cvds()

        # Access internal variable directly, guarded by _load_cvds
        if not (0 <= device_index < len(self._cvds)):
            logging.error(
                "HostOrchestrator: device_index %d is out of bounds. Found %d"
                " CVDs.",
                device_index,
                len(self._cvds),
            )
            raise IndexError(f"device_index {device_index} is out of bounds.")

        return self._cvds[device_index]

    def _perform_action(
        self,
        device_index: int,
        action: ho_requests.CvdAction,
        payload: Optional[Dict[str, Any]] = None,
    ):
        """Executes a generic action on a CVD and waits for completion.

        This method resolves the target device, sends the action request, and
        polls the resulting operation until it completes or times out.

        Args:
            device_index: The zero-based index of the target device.
            action: The CvdAction enum member indicating the action to perform.
            payload: Optional dictionary payload to include in the request body.

        Raises:
            IndexError: If the device_index is invalid.
            api_client.ApiClientError: If the action request fails.
            TimeoutError: If the operation times out.
        """
        try:
            # 1. Resolve target CVD
            cvd = self._get_device_cvd(device_index)

            # 2. Execute Request
            response = self._api_client.execute(
                ho_requests.cvd_action(
                    group=cvd.group,
                    name=cvd.name,
                    action=action,
                    payload=payload,
                )
            )
            operation = ho_requests.Operation.from_json(response)

            # 3. Wait for Completion
            # We use wait_and_return_result so we can raise a TimeoutError
            # instead of the default assert raised if we use wait_for_true.
            result = polling.wait_and_return_result(
                self._poll_operation_completion, operation.name
            )

            if result is None:
                raise TimeoutError(f"Operation {operation.name} timed out")

        except api_client.ApiClientError as e:
            logging.error(
                "HostOrchestrator#%s: Failed to execute action on device %d."
                " Error: <%s>",
                action.name,
                device_index,
                e,
            )
            raise  # Re-raise the explicit API error

    def _poll_operation_completion(self, operation_name: str) -> Optional[bool]:
        """Checks the status of a long-running operation.

        Args:
            operation_name: The unique identifier of the operation to check.

        Returns:
            True if the operation is marked as 'done'.
            None if the operation is still in progress (to continue polling).

        Raises:
            api_client.ApiClientError: If the network request to fetch the
            operation status fails.
        """
        response = self._api_client.execute(
            ho_requests.get_operation(operation_name)
        )
        operation = ho_requests.Operation.from_json(response)

        # wait_and_return_result continues polling while the return value is None.
        if operation.done:
            return True
        return None
