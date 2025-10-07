# Copyright 2025 Google LLC


class SpectatioFrameworkError(Exception):
  """
    Base exception for the Spectatio framework.
  """

  pass


class DeviceNotFoundError(SpectatioFrameworkError):
  """
    Raised when a requested device is not found.
  """

  pass


class TestArgumentNotFoundError(SpectatioFrameworkError):
  """
    Raised when a required test argument is not found.
  """

  pass


class DeviceError(Exception):
  """
    Base exception for device-related errors.
  """

  pass
