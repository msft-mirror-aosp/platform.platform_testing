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
