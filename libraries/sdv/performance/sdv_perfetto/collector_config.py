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

import os
import re
from typing import Optional

from sdv_test_fw.device import sdv_adb


DEFAULT_TRACE_CONFIG_NAME = 'default_trace_cfg.pbtx'

INET_ADDRESS_REGEX = r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}'
VSOOCK_ADDRESS_REGEX = r'\d{1,3}'

def build_config_path(config_path=None) -> str:
    """Builds the config path.

      Args:
        config_path: str, path to the Perfetto config file or the name of the config file in the data.

      Returns:
        str, absolute path to the config file
    """
    if config_path is None:
        return get_config_path_from_data()
    if os.path.exists(config_path) and os.path.isfile(config_path):
      return config_path
    # if the config_path is the name of the config file in the data.
    return get_config_path_from_data(name=config_path)

def config_file_exists(config_path: str) -> str:
        """Verify if the config file exists.

        Args:
          config_path: str, path to the Perfetto config file

        Returns:
          str, absolute path to the config file

        Raises:
          ValueError: if the config file does not exist.
        """
        if not os.path.exists(config_path) or not os.path.isfile(config_path):
            err_msg = f'Config file {config_path} does not exist.'
            raise ValueError(err_msg)
        return config_path

def get_config_path_from_data(name=None) -> str:
        """Get the path to the Perfetto config file from the library bundle.

        Args:
          name: str, name of the config file

        Returns:
          str, absolute path to the config file, if name is not provided, it
          will
          return the default config file.
        """
        config_dir = os.path.join(os.path.dirname(__file__), 'config')
        if name is None:
            config_path = os.path.join(config_dir, DEFAULT_TRACE_CONFIG_NAME)
        else:
            config_path = os.path.join(config_dir, name)
        return config_file_exists(config_path)

class CollectorConfig:
    """Config for the PerfettoCollector. """

    def __init__(
        self,
        config_path=None,
        config_txt: bool = True,
        background_wait: bool = False,
        multi_vm_tracing: bool = False,
        multi_vm_tracing_vsock: Optional[bool] = None,
        secondary_devices: Optional[list[sdv_adb.SdvAdb]] = None):
        """Create a CollectorConfig.

         Args:
           config_path: str, path to the Perfetto config file
           config_txt: bool, indicates whether the config file is a text proto
           background_wait: bool, uses `--background-wait` in short '-D' instead
                of`--background` in short '-d' when launching the Perfetto command.
                This makes the command wait until all data sources are started
                before returning.
           multi_vm_tracing: bool, enables multi-vm tracing.
           multi_vm_tracing_vsock: Optional[bool], enables multi-vm tracing over vsock, otherwise over inet. Valid only if multi_vm_tracing is True.
           secondary_devices: Optional[list[sdv_adb.SdvAdb]], list of secondary devices to trace. Valid only if multi_vm_tracing is True.
        """
        self.config_path = build_config_path(config_path)
        self.config_txt = config_txt
        self.background_wait = background_wait
        if multi_vm_tracing:
            if multi_vm_tracing_vsock is None:
                raise ValueError(
                    'multi_vm_tracing_vsock must be specified if multi_vm_tracing is True.'
                )
            if secondary_devices is None:
                raise ValueError(
                    'secondary_devices must be specified if multi_vm_tracing is True.'
                )
        self.multi_vm_tracing = multi_vm_tracing
        self.multi_vm_tracing_vsock = multi_vm_tracing_vsock
        self.secondary_devices = secondary_devices

def check_vsock_id(vsock_id: str):
  """Check if the id is valid for vsock."""
  check_string_format(vsock_id, VSOOCK_ADDRESS_REGEX, 'Invalid vsock id: {vsock_id}')

def check_inet_address(inet_address: str):
  """Check if the address is valid for inet."""
  check_string_format(inet_address, INET_ADDRESS_REGEX, 'Invalid inet address: {inet_address}')

def check_string_format(input_string, expected_format_regex, error_message="String does not match the expected format"):
    """
    Checks if the input string matches the specified regular expression format.

    Args:
        input_string (str): The string to check.
        expected_format_regex (str): The regular expression pattern for the expected format.
        error_message (str, optional): The message to include in the FormatError if the string doesn't match.
                                        Defaults to "String does not match the expected format".

    Raises:
        FormatError: If the input string does not match the expected format.

    Returns:
        str: The input string if it matches the format.
    """
    if not re.fullmatch(expected_format_regex, input_string):
        raise ValueError(error_message)
