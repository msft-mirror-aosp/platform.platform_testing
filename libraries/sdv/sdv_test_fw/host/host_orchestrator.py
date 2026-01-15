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

import enum
import json
import logging
import time

import httplib2


class CvdAction(enum.Enum):
  POWERWASH = "powerwash"
  POWERBTN = "powerbtn"
  START = "start"
  STOP = "stop"


class HostOrchestrator:
  """Interacts with the CF VM through Host Orchestrator API"""

  _CVDS_KEY = "cvds"
  _GROUP_KEY = "group"
  _NAME_KEY = "name"

  _WAIT_FOR_OPERATION_TIMEOUT_MS = 5 * 60 * 1000  # 5 minutes
  _WAIT_FOR_OPERATION_MS = 5 * 1000  # 5 sec

  class Status(enum.Enum):
    SUCCESS = 1
    FAILURE = 2

    def is_success(self):
      return self == self.SUCCESS

  def __init__(self, host_orchestrator_url, device_index):
    """ Requires the host orchestrator API URL (in `user_params['ho_base_url']`)
    and the device index. It correlates with the adb device identifier
    (0 - device1, 1 - device2, 2 - device3)."""
    self.host_orchestrator_url = host_orchestrator_url
    self.http = httplib2.Http()
    self.device_cvd = self._get_device_cvd(device_index)

  def _generate_cvds_url(self):
    return f"{self.host_orchestrator_url}/cvds"

  def _generate_operations_url(self, operation_name):
    return f"{self.host_orchestrator_url}/operations/{operation_name}"

  def _generate_cvd_action_url(self, action_name):
    """Generates the URL for a CVD action."""
    cvd = self.device_cvd
    return f"{self.host_orchestrator_url}/cvds/{cvd[self._GROUP_KEY]}/{cvd[self._NAME_KEY]}/:{action_name}"

  def _request_json(self, url, method="GET", body=None):
    """Sends a request and returns the JSON response."""
    response, content = self.http.request(url, method, body=body)
    if not (200 <= response.status < 300):
      raise httplib2.HttpLib2Error(
          f"HTTP Error: {response.status} {content.decode()}"
      )
    return json.loads(content)

  def _get_cvds(self):
    try:
      return self._request_json(self._generate_cvds_url())
    except httplib2.HttpLib2Error as e:
      logging.error("HostOrchestrator#get_cvds: Error occurred. Error: <%s>", e)
      raise Exception(e)

  def _get_device_cvd(self, device_index):
    """Gets the CVD information for the device_index."""
    cvds = self._get_cvds()
    if self._CVDS_KEY not in cvds or not cvds[self._CVDS_KEY]:
      logging.error(
          "HostOrchestrator#_get_device_cvd: No CVDs found. Response: <%s>",
          cvds,
      )
      raise Exception("Failed to get device CVD. No CVDs found.")
    cvds_list = cvds[self._CVDS_KEY]
    if not (0 <= device_index < len(cvds_list)):
      logging.error(
          "HostOrchestrator#_get_device_cvd: device_index %d is out of"
          " bounds. Found %d CVDs.",
          device_index,
          len(cvds_list),
      )
      raise IndexError(f"device_index {device_index} is out of bounds.")
    return cvds_list[device_index]

  def _get_operation(self, operation_name):
    """
    Get the status of an operation using the host orchestrator
    """
    try:
      return self._request_json(self._generate_operations_url(operation_name))
    except httplib2.HttpLib2Error as e:
      logging.error(
          "HostOrchestrator#get_operation: Error occurred. Error: <%s>", e
      )
      raise Exception(e)

  def _wait_for_operation(
      self,
      operation_name,
      timeout_ms=_WAIT_FOR_OPERATION_TIMEOUT_MS
  ):
    """
    Wait for the operation to complete.
    """
    max_end_time = int(time.time() * 1000) + timeout_ms
    while int(time.time() * 1000) < max_end_time:
      operation = self._get_operation(operation_name)
      if operation["done"]:
        return
      time.sleep(self._WAIT_FOR_OPERATION_MS / 1000)
    logging.error(
      "HostOrchestrator#wait_for_operation: Operation timed out. Name: <%s>",
      operation_name
    )
    raise Exception(f"Operation {operation_name} timed out")

  def _cvd_action(self, action, post_json_data=""):
    """Perform an action on the device CVD."""
    action_name = action.value
    try:
      url = self._generate_cvd_action_url(action_name)

      # The action returns an Operation {name:String, done:Boolean}
      operation = self._request_json(url, "POST", body=post_json_data)

      # Wait for operation to complete
      self._wait_for_operation(operation[self._NAME_KEY])
    except httplib2.HttpLib2Error as e:
      logging.error(
          "HostOrchestrator#%s: Error occurred. Error: <%s>", action.name, e
      )
      raise Exception(e)
    except Exception as e:
      logging.error(
          "HostOrchestrator#%s: Failed to %s CVD; Error: <%s>",
          action.name,
          action_name,
          e,
      )
      raise Exception(e)
    return self.Status.SUCCESS

  def powerwash(self):
    """
    Powerwash CVD using the host orchestrator
    """
    return self._cvd_action(CvdAction.POWERWASH)

  def powerbtn(self):
    """
    Press power button on CVD using the host orchestrator
    """
    return self._cvd_action(CvdAction.POWERBTN)

  def start(self):
    # TODO(b/459780275): does not work with multivm
    """
    Start CVD using the host orchestrator
    """
    # empty JSON is required for start action with default options
    return self._cvd_action(CvdAction.START, "{}")

  def stop(self):
    # TODO(b/459780275): does not work with multivm
    """
    Stop CVD using the host orchestrator
    """
    return self._cvd_action(CvdAction.STOP)

  # TODO(b/430509532): add all REST API methods see https://github.com/jmacnak/android-cuttlefish/blob/main/frontend/src/host_orchestrator/orchestrator/controller.go

