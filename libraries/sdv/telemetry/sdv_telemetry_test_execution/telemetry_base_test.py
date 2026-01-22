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

from mobly import asserts
import contextlib
from functools import lru_cache
import os
from pathlib import Path
import re
import tempfile
from typing import Callable, ContextManager, Iterator, List, Optional, TypeVar

from google.protobuf import text_format
from google.protobuf.descriptor_pool import DescriptorPool
from google.protobuf.message_factory import GetMessageClass
from mobly.controllers.android_device import AndroidDevice
from sdv_telemetry_test_execution.telemetry_utils import get_report_file_pattern
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test
from system.software_defined_vehicle.telemetry.proto.metrics_configuration.metrics_configuration_pb2 import MetricsConfig
from system.software_defined_vehicle.telemetry.proto.metrics_configuration.metrics_configuration_pb2 import MetricsReport


T = TypeVar('T')


class SdvTelemetryBaseTestClass(sdv_base_test.SdvBaseTestClass):
    TELEMETRY_SERVICE_LOG_TAG = 'SdvTelemetryService'
    IVI_TELEMETRY_SERVICE_COMMAND = 'sdv_ivi_telemetry_service_agent'

    CORE_SIMULATOR_BINARY = 'sdv_telemetry_simulator'
    IVI_SIMULATOR_BINARY = 'sdv_ivi_telemetry_simulator'

    _class_resource_stack: Optional[contextlib.ExitStack] = None
    _test_resource_stack: Optional[contextlib.ExitStack] = None

    def setup_class(self) -> None:
        super().setup_class()

        self._class_resource_stack = contextlib.ExitStack()

    def setup_test(self) -> None:
        super().setup_test()

        self._test_resource_stack = contextlib.ExitStack()

    def teardown_test(self) -> None:
        # `self._test_resource_stack` may be `None` if `setup_test` fails.
        if self._test_resource_stack is not None:
            self._test_resource_stack.close()
            self._test_resource_stack = None

        super().teardown_test()

    def teardown_class(self) -> None:
        # `self._class_resource_stack` may be `None` if `setup_class` fails.
        if self._class_resource_stack is not None:
            self._class_resource_stack.close()
            self._class_resource_stack = None

        super().teardown_class()

    def enter_class_context(self, cm: ContextManager[T]) -> T:
        """Enter the provided ContextManager and automatically close it during class teardown."""
        return self._class_resource_stack.enter_context(cm)

    def enter_context(self, cm: ContextManager[T]) -> T:
        """Enter the provided ContextManager and automatically close it during test teardown."""
        return self._test_resource_stack.enter_context(cm)

    def add_class_cleanup(self, callback: Callable[[], None]):
        """Run the provided callback during class teardown."""
        return self._class_resource_stack.callback(callback)

    def add_cleanup(self, callback: Callable[[], None]):
        """Run the provided callback during test teardown."""
        return self._test_resource_stack.callback(callback)

    @contextlib.contextmanager
    def create_temp_dir(self, device: sdv_device.SdvDevice) -> Iterator[Path]:
        """Creates a temporary directory.

        The directory is automatically deleted when it goes out of scope.

        Returns:
          A context manager that creates a directory and returns its path.
        """
        temp_dir = None
        try:
            temp_dir = Path(device.adb().execute_shell_command('mktemp -d'))
            yield temp_dir
        finally:
            if temp_dir is not None:
                device.adb().execute_shell_command(
                    shlex_join(['rm', '-rf', str(temp_dir)])
                )

    @contextlib.contextmanager
    def create_host_temp_file(self) -> Iterator[Path]:
        """Creates a temporary file on the host.

        The file is automatically deleted when it goes out of scope.

        Returns:
          A context manager that creates a file and returns its path.
        """
        temp_file = None
        try:
            (fd, temp_file) = tempfile.mkstemp()
            os.close(fd)
            yield Path(temp_file)
        finally:
            if temp_file is not None:
                os.remove(temp_file)

    # TODO(b/395067075): Update all tests to work with authz enabled
    @contextlib.contextmanager
    def disable_authz(self, device: sdv_device.SdvDevice) -> Iterator[None]:
        """Disables authz.

        Authz status is automatically restored when the returned context manager
        exits.

        Returns:
          A context manager that disables authz while it is active.
        """

        original_value = None
        try:
            original_value = device.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
            device.adb().log().info(
                f'Saving original {SdvDeviceProperty.AUTHZ_ENABLE.value} value: {original_value}'
            )

            device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'false')

            yield
        finally:
            if original_value is not None:
                device.adb().log().info(f'Restoring {SdvDeviceProperty.AUTHZ_ENABLE.value} to {original_value}')
                device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, original_value)

    def get_simulator_binary(self, device: sdv_device.SdvDevice) -> str:
        """Returns the simulator binary for the given device."""
        if self.is_ivi(device):
            return self.IVI_SIMULATOR_BINARY
        else:
            return self.CORE_SIMULATOR_BINARY

    def start_telemetry_service_if_ivi(
        self, device: sdv_device.SdvDevice
    ) -> None:
        """Starts the Telemetry Service if the device is IVI."""
        if self.is_ivi(device):
            device.adb().execute_shell_command_in_subprocess(
                'IVI SDV Telemetry Service',
                self.IVI_TELEMETRY_SERVICE_COMMAND,
            )

    @lru_cache(maxsize=None)
    def is_ivi(self, device: sdv_device.SdvDevice):
        """Checks whether the given device is IVI and caches the result."""

        flavor = device.adb().prop.get(SdvDeviceProperty.BUILD_FLAVOR)
        if not ('_ivi_' in flavor or '_core_' in flavor):
            raise Exception(f'Unexpected {SdvDeviceProperty.BUILD_FLAVOR.value}: {flavor}')

        return '_ivi_' in flavor

    def _android_device(self, device: sdv_device.SdvDevice) -> AndroidDevice:
        """Access mobly's AndroidDevice controller directly.

        For methods not yet exposed by the SDV test framework, we need to access
        mobly's underlying AndroidDevice controller.
        """
        return device.adb()._SdvDeviceAdb__android_device

    def find_report_paths(
        self,
        device: sdv_device.SdvDevice,
        simulator_out_dir: Path,
        config_uuid: Optional[str] = None,
        report_name: Optional[str] = None,
        report_number: Optional[int] = None,
    ) -> List[Path]:
        """Gets the paths of report files matching the given criteria."""

        reports_dir = simulator_out_dir / 'reports'

        all_files = (
            device.adb()
            .execute_shell_command(shlex_join(['ls', '-1q', str(reports_dir)]))
            .strip()
            .splitlines()
        )

        pattern = get_report_file_pattern(
            config_uuid, report_name, report_number
        )
        regex = re.compile(pattern)
        return [reports_dir / f for f in all_files if regex.match(f)]

    def pull_file(
        self,
        device: sdv_device.SdvDevice,
        device_path: Path,
    ) -> Path:
        """Pulls file from the device to the test host machine.

        Returns:
          Path to the pulled temporary file on the host machine.
        """

        host_path = self.enter_class_context(self.create_host_temp_file())
        device.adb().pull([str(device_path), str(host_path)])
        return host_path

    def pull_report(
        self,
        device: sdv_device.SdvDevice,
        simulator_out_dir: Path,
        config_uuid: str,
        report_name: str,
        report_number: int,
    ) -> Path:
        """Pulls report from the device to the test machine assuming it's unique,

        i.e. there are no other reports having the same tuple of
        (config_uuid, report_name, report_number)

        Returns:
          Path to the pulled file on the host machine.
        """

        pattern = get_report_file_pattern(
            config_uuid, report_name, report_number
        )
        reports = self.find_report_paths(
            device, simulator_out_dir, config_uuid, report_name, report_number
        )
        asserts.assert_equal(
            len(reports),
            1,
            f'Expected to find exactly one report for pattern {pattern}',
        )

        return self.pull_file(device, reports[0])

    def parse_binary_report(self, file_path):
        """Parses a binary protobuf message from a file."""
        try:
            with open(file_path, 'rb') as f:
                binary_data = f.read()
                message = MetricsReport()
                message.ParseFromString(binary_data)
                return message
        except FileNotFoundError:
            print(f'Error: File not found: {file_path}')
            return None
        except Exception as e:
            print(f'Error parsing protobuf: {e}')
            return None

    def parse_textproto_metrics_config(self, file_path: Path) -> MetricsConfig:
        """Parses a textproto `MetricsConfig` from a file."""
        return text_format.Parse(file_path.read_text(), MetricsConfig())

    def decode_report_payload(self, descriptor_protos, report):
        # Create a descriptor pool and add the file descriptors from the metrics config
        descriptor_pool = DescriptorPool()
        for descriptor_proto in descriptor_protos:
            descriptor_pool.Add(descriptor_proto)

        # Get the message type from the Any message
        message_type_url = report.report_data.type_url
        message_type_name = message_type_url.split('/')[-1]

        # Get the message descriptor from the pool
        message_descriptor = descriptor_pool.FindMessageTypeByName(
            message_type_name
        )

        # Create a dynamic message using the descriptor
        report_payload = GetMessageClass(
            message_descriptor
        )()

        # Unpack the Any message into the dynamic message
        report.report_data.Unpack(report_payload)
        return report_payload