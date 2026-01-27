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

import logging
from typing import Any

import grpc

import vehicle_tire_pb2
import vehicledata_grpc_service_pb2
import vehicledata_grpc_service_pb2_grpc
import vehicledata_pb2


class DisplaySafetyClientError(Exception):
  """
    Base exception for Display Safety client errors.
  """

  pass


class DisplaySafetyClient:
  """
    A gRPC client for the Display Safety test service.
  """

  _LOG_TAG = 'DisplaySafetyClient'
  _DEFAULT_HOST_PORT = '7002'
  _DEFAULT_DEVICE_PORT = '7002'

  _singleton_instance: 'DisplaySafetyClient' = None
  _is_singleton_initialized: bool = False

  def __new__(cls, *args, **kwargs):
    if cls._singleton_instance is None:
      cls._singleton_instance = super().__new__(cls)
    return cls._singleton_instance

  def __init__(
      self,
      device,
      host_port: str = _DEFAULT_HOST_PORT,
      device_port: str = _DEFAULT_DEVICE_PORT,
  ):
    if not self._is_singleton_initialized:
      # Forward host port to device port has to be done before using the client.
      device.adb.forward(f'tcp:{host_port}', f'tcp:{device_port}')

      self._device_serial = device.adb.serial
      self._server_address = f'{self._device_serial.split(':', 1)[0]}:{host_port}'
      self._channel = None
      self._stub = None
      self._is_singleton_initialized = True

  def __enter__(self):
    """
      Establishes the gRPC channel and creates the stub.
    """
    logging.info(
        f'{self._LOG_TAG}: Connecting to server at {self._server_address}')
    try:
      self._channel = grpc.insecure_channel(self._server_address)
      self._stub = vehicledata_grpc_service_pb2_grpc.SdvVehicleDataGrpcStub(
          self._channel
      )
    except grpc.RpcError as e:
      raise DisplaySafetyClientError(
          f'Failed to connect to gRPC server: {e}') from e
    return self

  def __exit__(self, exc_type, exc_val, exc_tb):
    """
      Closes the gRPC channel.
    """
    if self._channel:
      self._channel.close()
    logging.info(f'{self._LOG_TAG}: Disconnected from server.')

  def _make_grpc_call(self, method_name: str, request: Any) -> Any:
    """
      Makes a gRPC call to the specified method with the given request.

      Args:
          method_name: The name of the gRPC method to call.
          request: The request object for the gRPC call.

      Returns:
          The status from the gRPC response.

      Raises:
          DisplaySafetyClientError: If the gRPC call fails.
    """
    if not self._stub:
      raise DisplaySafetyClientError(
          'Client is not connected. Use within a `with` statement.'
      )
    try:
      logging.debug(
          f'{self._LOG_TAG}: Calling "{method_name}" with request: {request}')
      method = getattr(self._stub, method_name)
      response = method(request)
      logging.debug(
          f'{self._LOG_TAG}: Received response status: {response.status}')
      return response.status
    except grpc.RpcError as e:
      raise DisplaySafetyClientError(
          f'gRPC call "{method_name}" failed: {e}'
      ) from e

  def post_vehicle_speed(self, topic: str, value: float) -> Any:
    speed = vehicledata_pb2.VehicleSpeed(precise_speed=value)
    request = vehicledata_grpc_service_pb2.PublishVehicleSpeedRequest(
        topic=topic, speed=speed
    )
    return self._make_grpc_call('PublishVehicleSpeed', request)

  def post_telltale_status(self, topic: str, is_on: bool) -> Any:
    status = vehicledata_pb2.TellTaleStatus(alert=is_on)
    request = vehicledata_grpc_service_pb2.PublishTelltaleStatusRequest(
        topic=topic, status=status
    )
    return self._make_grpc_call('PublishTelltaleStatus', request)

  def post_current_gear(self, topic: str, gear_value: str) -> Any:
    gear = vehicledata_pb2.CurrentGear(gear=gear_value)
    request = vehicledata_grpc_service_pb2.PublishCurrentGearRequest(
        topic=topic, gear=gear
    )
    return self._make_grpc_call('PublishCurrentGear', request)

  def post_tire_pressure(self, topic: str, value: int) -> Any:
    pressure = vehicle_tire_pb2.TirePressure(pressure=value)
    request = vehicledata_grpc_service_pb2.PublishTirePressureRequest(
        topic=topic, pressure=pressure
    )
    return self._make_grpc_call('PublishTirePressure', request)

  def post_engine_rpm(self, topic: str, value: int) -> Any:
    rpm = vehicledata_pb2.EngineRpm(rpm=value)
    request = vehicledata_grpc_service_pb2.PublishEngineRpmRequest(
        topic=topic, rpm=rpm
    )
    return self._make_grpc_call('PublishEngineRpm', request)
