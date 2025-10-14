# Copyright 2025 Google LLC

from dataclasses import dataclass
import json
import logging
import time
from typing import Any

import httplib2


class HostOrchestratorError(Exception):
  """
    Error class for Host Orchestrator.
  """
  pass


class HostOrchestratorResourceNotFoundError(HostOrchestratorError):
  """
    Error class for Host Orchestrator not found.
  """
  pass


class HostOrchestratorUtil:
  """
    Util class which provides methods to interact with host orchestrator

    Reference:
    https://github.com/google/android-cuttlefish/blob/main/frontend/src/host_orchestrator/orchestrator/controller.go#L64
  """

  _LOG_TAG = 'HostOrchestratorUtil'

  _CVDS_KEY: str = 'cvds'
  _GROUP_KEY: str = 'group'
  _NAME_KEY: str = 'name'
  _DONE_KEY: str = 'done'

  _WAIT_FOR_OPERATION_TIMEOUT_MS: int = 5 * 60 * 1000  # 5 minutes
  _WAIT_FOR_OPERATION_MS: int = 5 * 1000  # 5 sec

  class HostOrchestratorAction:
    """
      Defines the supported CVD actions on Host Orchestrator.
    """

    @dataclass(frozen=True)
    class ActionInfo:
      """
        Defines an action data class on the Host Orchestrator.
      """
      action_name: str
      url: str
      request_type: str
      request_body: str = ''

    GET_CVDS = ActionInfo(
        action_name='get_cvds',
        url='{host_orchestrator_url}/cvds',
        request_type='GET',
    )
    GET_OPERATIONS = ActionInfo(
        action_name='get_operations',
        url='{host_orchestrator_url}/operations/{operation_name}',
        request_type='GET',
    )
    GET_OPERATIONS_RESULT = ActionInfo(
        action_name='get_operations',
        url='{host_orchestrator_url}/operations/{operation_name}/result',
        request_type='GET',
    )
    POWERWASH = ActionInfo(
        action_name='powerwash',
        url='{host_orchestrator_url}/cvds/{group}/{name}/:powerwash',
        request_type='POST',
    )
    POWERBTN = ActionInfo(
        action_name='powerbtn',
        url='{host_orchestrator_url}/cvds/{group}/{name}/:powerbtn',
        request_type='POST',
    )
    SCREENSHOT = ActionInfo(
        action_name='screenshot',
        url=(
            '{host_orchestrator_url}/cvds/{group}/{name}/displays/'
            '{displayNumber}/:screenshot'
        ),
        request_type='GET',
    )

  @dataclass(frozen=True)
  class Status():
    """
      Defines the status of data for a Host Orchestrator Request.
    """
    success: bool = False
    result: dict[str, Any] = None

    def is_success(self):
      return self.success

  def __init__(self, host_orchestrator_url: str) -> None:
    self.host_orchestrator_url: str = host_orchestrator_url
    logging.info(
        f'{self._LOG_TAG}: Initializing Host Orchestrator Util with URL:'
        f' {self.host_orchestrator_url}'
    )

    # Create the HTTP client.
    self.http: httplib2.Http = httplib2.Http()

    # Get the CVDs from the host orchestrator.
    self.cvds: list[dict[str, Any]] = self._get_cvds()

  def _send_http_request_and_get_json_response(
      self, url: str, method: str = 'GET', body: str = None
  ) -> dict[str, Any]:
    """
      Sends a HTTP request and returns the JSON response.

      Args:
        url: The URL to request.
        method: The HTTP method to use.
        body: The body of the request.

      Returns:
        The JSON response.
    """
    logging.info(f'{self._LOG_TAG}: Requesting URL : {url}')
    response, content = self.http.request(url, method, body=body)
    logging.debug(f'{self._LOG_TAG}: URL : {url}, Response: {response}, Content: {content}')
    if not (200 <= response.status < 300):
      raise httplib2.HttpLib2Error(
          f'HTTP Error: {response.status} {content.decode()}'
      )
    return json.loads(content)

  def _get_cvds(self) -> list[dict[str, Any]]:
    """
      Gets the CVD information for all devices.

      Returns:
        All the available CVDs.
    """
    try:
      url = self.HostOrchestratorAction.GET_CVDS.url.format(
          host_orchestrator_url=self.host_orchestrator_url
      )
      response = self._send_http_request_and_get_json_response(url)

      # Check if CVDs are found.
      if not response.get(self._CVDS_KEY):
        logging.error(f'{self._LOG_TAG}: No CVDs found. Response: {response}')
        raise HostOrchestratorResourceNotFoundError(
            f'No CVDs found in the host orchestrator response: {response}'
        )

      return response.get(self._CVDS_KEY)
    except (httplib2.HttpLib2Error, Exception) as e:
      logging.error(f'{self._LOG_TAG}: Failed to get CVDs. Error: {e}')
      raise HostOrchestratorError(f'Failed to get CVDs: {e}') from e

  def _get_cvd_with_index(self, cvd_index: int) -> dict[str, Any]:
    """
      Gets the CVD information for the cvd_index.

      Args:
        cvd_index: The index of the device to get the CVD information for.

      Returns:
        The CVD information for the cvd_index.
    """
    if not (0 <= cvd_index < len(self.cvds)):
      logging.error(
          f'{self._LOG_TAG}: Index {cvd_index} is out of bounds.'
          f' Found {len(self.cvds)} CVDs.'
      )
      raise IndexError(f'cvd_index {cvd_index} is out of bounds.')
    return self.cvds[cvd_index]

  def _get_operation(self, operation_name: str) -> dict[str, Any]:
    """
      Get the status of an operation using the host orchestrator

      Args:
        operation_name: The name of the operation.

      Returns:
        The status of the operation.
    """
    try:
      url = self.HostOrchestratorAction.GET_OPERATIONS.url.format(
          host_orchestrator_url=self.host_orchestrator_url,
          operation_name=operation_name,
      )
      return self._send_http_request_and_get_json_response(url)
    except (httplib2.HttpLib2Error, Exception) as e:
      logging.error(
          f'{self._LOG_TAG}: Error occurred when getting operation {operation_name}.'
          f' Error: {e}'
      )
      raise HostOrchestratorError(
          f'Failed to get operation {operation_name}: {e}'
      ) from e

  def _wait_for_operation(
      self,
      operation_name: str,
      timeout_ms: int = _WAIT_FOR_OPERATION_TIMEOUT_MS
  ) -> None:
    """
      Wait for the operation to complete.
    """
    max_end_time = int(time.time() * 1000) + timeout_ms
    while int(time.time() * 1000) < max_end_time:
      operation = self._get_operation(operation_name)
      if operation[self._DONE_KEY]:
        logging.info(
            f'{self._LOG_TAG}: Operation {operation_name} completed.'
        )
        return
      time.sleep(self._WAIT_FOR_OPERATION_MS / 1000)
    logging.error(
        f'{self._LOG_TAG}: Operation {operation_name} timed out.'
    )
    raise HostOrchestratorError(f'Operation {operation_name} timed out.')

  def _get_operation_result(self, operation_name: str) -> dict[str, Any]:
    """
      Get the result of an operation using the host orchestrator

      Args:
        operation_name: The name of the operation.

      Returns:
        The status of the operation.
    """
    try:
      url = self.HostOrchestratorAction.GET_OPERATIONS_RESULT.url.format(
          host_orchestrator_url=self.host_orchestrator_url,
          operation_name=operation_name,
      )
      return self._send_http_request_and_get_json_response(url)
    except (httplib2.HttpLib2Error, Exception) as e:
      logging.error(
          f'{self._LOG_TAG}: Error occurred when getting operation {operation_name}.'
          f' Error: {e}'
      )
      raise HostOrchestratorError(
          f'Failed to get operation {operation_name}: {e}'
      ) from e

  def _perform_cvd_action(
      self,
      action: HostOrchestratorAction.ActionInfo,
      cvd_index: int = 0,
      **kwargs: Any,
  ) -> Status:
    """
      Perform a CVD action on the device using the host orchestrator.

      Args:
        action: The action to perform on the device.
        cvd_index: The index of the device to perform the action on.
        **kwargs: Additional keyword arguments for the action's URL formatting.

      Returns:
        The status of the action.
    """
    try:
      cvd = self._get_cvd_with_index(cvd_index)
      logging.debug(
          f'{self._LOG_TAG}: Performing action {action.action_name} on Host'
          f' Orchestrator {cvd}'
      )

      url_params = {
          'host_orchestrator_url': self.host_orchestrator_url,
          'group': cvd[self._GROUP_KEY],
          'name': cvd[self._NAME_KEY],
      }
      if 'display_index' in kwargs:
        url_params['displayNumber'] = kwargs['display_index']

      url = action.url.format(**url_params)

      operation = self._send_http_request_and_get_json_response(
          url,
          action.request_type,
          action.request_body,
      )

      # Wait for operation to complete
      self._wait_for_operation(operation[self._NAME_KEY])

      return self.Status(success=True, result=operation)
    except (httplib2.HttpLib2Error, IndexError, Exception) as e:
      logging.error(
          f'{self._LOG_TAG}: Error occurred when performing action'
          f' <{action.action_name}>. Error: {e}'
      )
      raise HostOrchestratorError(
          f'Failed to perform action {action.action_name}: {e}'
      ) from e

  def powerwash(self, cvd_index: int = 0) -> Status:
    """
      Powerwash given CVD using the host orchestrator

      Args:
        cvd_index: The index of the device to powerwash. In case of multiple
        devices, the powerwash will be performed on the device with the given
        index. By default, it will press the power button on the first device
        i.e. with index 0

      Returns:
        The status of the powerwash action.
    """
    return self._perform_cvd_action(
        self.HostOrchestratorAction.POWERWASH, cvd_index
    )

  def powerbtn(self, cvd_index: int = 0) -> Status:
    """
      Press power button on the given CVD using the host orchestrator

      Args:
        cvd_index: The index of the device to press the power button on.
        In case of multiple devices, the power button will be pressed
        on the device with the given index. By default, it will press the
        power button on the first device i.e. with index 0

      Returns:
        The status of the power button action.
    """
    return self._perform_cvd_action(
        self.HostOrchestratorAction.POWERBTN, cvd_index
    )

  def take_screenshot(
      self, cvd_index: int = 0, display_index: int = 0
  ) -> Status:
    """
      Take cvd screenshot using the host orchestrator

      Args:

        cvd_index: The index of the cvd to take the screenshot of.
        In case of multiple devices, the screenshot will be taken
        of the device with the given index. By default, it will take
        the screenshot of the first device i.e. with index 0

        display_index: The index of the display to take the screenshot of.
        By default, it will take the screenshot of the first display i.e.
        with index 0

      Returns:
        The status of the screenshot action.
    """
    status: self.Status = self._perform_cvd_action(
        self.HostOrchestratorAction.SCREENSHOT,
        cvd_index=cvd_index,
        display_index=display_index,
    )

    if not status.is_success():
      raise HostOrchestratorError(
          f'Failed to take screenshot: {status.result}'
      )

    # Get the screenshot result
    operation_name = status.result.get(self._NAME_KEY)
    screenshot_result = self._get_operation_result(operation_name)
    logging.debug(
        f'{self._LOG_TAG}: Screenshot result: {screenshot_result}'
    )

    # Check if screenshot result is valid
    if not screenshot_result.get('screenshot_mime_type'):
      raise HostOrchestratorError(
          'Error occurred while taking screenshot. Screenshot mime type is'
          f' missing: {screenshot_result}'
      )

    if not screenshot_result.get('screenshot_bytes'):
      raise HostOrchestratorError(
          'Error occurred while taking screenshot. Screenshot bytes is'
          f' missing: {screenshot_result}'
      )

    # Return the screenshot result
    return self.Status(
        success=True,
        result={
            'screenshot_mime_type': screenshot_result['screenshot_mime_type'],
            'screenshot_bytes': screenshot_result['screenshot_bytes'],
        }
    )
