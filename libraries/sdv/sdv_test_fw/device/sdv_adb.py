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

"""SDV Adb Class

This class is not intended to be imported directly outside of the SDV Test
Framework library. Use SdvDevice instead.
"""

import multiprocessing
import time
from typing import List

from mobly.controllers.android_device_lib.adb import AdbError
from sdv_test_fw.device import sdv_property
from sdv_test_fw.logcat import log_processor
from sdv_test_fw.session.interactive_session import Session


class SdvAdb:
    DEFAULT_WAIT_TIMEOUT_SECONDS = 60
    DEFAULT_TIMEOUT_BOOT_COMPLETION_SECOND = 5 * 60
    DEFAULT_TIMEOUT_LOGCAT_SECONDS = 10
    LOGCAT_NON_EMPTY_LINES_GREP_TEXT = '.'

    def __init__(self, android_device):
        self.__android_device = android_device
        self.__subprocess_map = {}
        self.__temp_files = []
        self.prop = sdv_property.SdvProperty(android_device)

    def interactive_session(self):
        """Returns an interactive session to the device.

        This method creates and returns a `Session` object, which provides an
        interactive interface to interact with the device.

        Returns:
            Session: An interactive session object for the device.
        """
        session = Session(self.get_device_serial())
        session.expect_outputs([r'[#\$]'])
        return session

    def advance_logcat(self):
        """Use advance logcat functionality for the device logs.

        Returns:
            LogcatProcessor: A log processor object for detail logcat
            processing.
        """
        log = self.execute_shell_command('logcat -d')
        return log_processor.LogcatProcessor(log)

    def verify_logcat_is_running(self, timeout=DEFAULT_TIMEOUT_LOGCAT_SECONDS):
        """Checks that logcat is running by verifying logs exist.

        Args:
            timeout: how long to wait for logcat (default 10 seconds).

        Raises:
            Exception: if logcat is not running within the time limit.
        """
        end_time = time.time() + timeout
        while time.time() < end_time:
            if self.grep_from_logcat(
                self.LOGCAT_NON_EMPTY_LINES_GREP_TEXT,
                logcat_args='-v printable',
            ):
                return
        raise Exception('Logcat is not running')

    # Get Device Serial
    def get_device_serial(self):
        return self.__android_device.serial

    def generate_filename(
        self, file_type: str, time_identifier=None, extension_name=None
    ):
        """Expose the android_device.generate_filename method.

        for generates a name for an output file related to this device.

        The name follows the pattern:

        {file type},{debug_tag},{serial},{model},{time identifier}.{ext}

        "debug_tag" is only added if it's different from the serial. "ext" is
        added if specified by user.

        Args:
          file_type: string, type of this file, like "logcat" etc.
          time_identifier: string or RuntimeTestInfo. If a `RuntimeTestInfo` is
            passed in, the `signature` of the test case will be used. If a
            string is passed in, the string itself will be used. Otherwise the
            current timestamp will be used.
          extension_name: string, the extension name of the file.

        Returns:
            String, the filename generated.
        """
        return self.__android_device.generate_filename(
            file_type, time_identifier, extension_name
        )

    def log(self):
        """Use device log

        Provides information relevant to a specific device object.
        """
        return self.__android_device.log

    def log_path(self):
        """Expose the android_device.log_path property to get the path where

        logs will be uploaded from this device.
        """
        return self.__android_device.log_path

    def push(self, host_remote_path: List[str]):
        """Expose the android_device.adb.push method for pushing file from host

        to remote.

        Args:
          host_remote_path: List of strings with host path and remote path
            [host_path, remote_path].
        """
        return self.__android_device.adb.push(host_remote_path)

    def pull(self, remote_host_path: List[str]):
        """Expose the android_device.adb.pull method for pulling file from

        remote to host.

        Args:
          remote_host_path:  List of strings with remote path and host path
            [remote_path, host_path].
        """
        return self.__android_device.adb.pull(remote_host_path)

    def dumpsys(self, service_name: str):
        """Expose the android_device.adb.dumpsys method for getting dumpsys

        information from the device.

        Args:
          service_name: Name of the service to get dumpsys information.

        Returns:
          String, the dumpsys information from the device.
        """
        dumpsys_command = 'dumpsys '
        return self.execute_shell_command(dumpsys_command + service_name)

    def wait_for_device_online(self, timeout=DEFAULT_WAIT_TIMEOUT_SECONDS):
        """Waits for the devices to be online.

        Args:
            timeout: How long to wait for devices to be online (default 60
              seconds). Avoid increasing this unless absolutely necessary.

        Raises:
          adb.AdbTimeoutError: If the device is not online within the timeout.
        """
        return self.__android_device.adb.wait_for_device(timeout=timeout)

    def wait_for_device_offline(self, timeout=DEFAULT_WAIT_TIMEOUT_SECONDS):
        """Waits for the devices to be offline.

        Args:
            timeout: How long to wait for devices to be offline (default 60
              seconds).

        Raises:
            Exception: If devices is not offline within the timeout.
        """
        end_time = time.time() + timeout

        self.log().info(
            f'Waiting for device to go offline for up to {timeout}s...'
        )
        while time.time() < end_time:
            try:
                # Attempt a simple shell command. If it succeeds, the device is still online.
                # If it raises Exception, the device is considered offline or unreachable.
                # Using "true" as a minimal, standard shell command.
                self.execute_shell_command('true', raise_exception=True)
                # If the command succeeded, the device is still responsive.
                self.log().info(
                    'Device is still responsive to ADB shell. Continuing to'
                    ' wait...'
                )
            except Exception as e:
                # Exception implies the device is not reachable via shell.
                self.log().info(
                    f'Device not responsive to ADB shell (Exception: {e}).'
                    ' Considering it offline.'
                )
                return
            time.sleep(0.1)

        raise Exception('Devices is not offline')

    def wait_for_boot_complete(self):
        """Waits for the boot flag to be set.

        Raises:
          adb.AdbTimeoutError: If the boot flag is not set within the timeout.
        """
        return self.__android_device.wait_for_boot_completion(
            timeout=self.DEFAULT_TIMEOUT_BOOT_COMPLETION_SECOND
        )

    # Reboot Device
    def reboot_device(self):
        return self.__android_device.reboot()

    # Reboot Device and verifies logcat afterwards
    def reboot_device_and_verify_logcat(self):
        self.__android_device.reboot()
        self.verify_logcat_is_running()

    # Root Device
    def root_device(self):
        return self.__android_device.adb.root()

    # Execute Forward Command for port forwarding
    def execute_forward_command(self, host_port, device_port):
        self.log().info(
            'Executing command (adb forward <%s> <%s>)',
            host_port,
            device_port,
        )
        args = [host_port, device_port]
        return self.__android_device.adb.forward(args)

    def execute_shell_command_in_subprocess_log(self, shell_command):
        temp_file = self._create_temp_file()
        self.__temp_files.append(temp_file)
        self.execute_shell_command_in_subprocess(
            temp_file, f'{shell_command} 2>&1 | tee {temp_file}'
        )
        return temp_file

    def _create_temp_file(self):
        return self.execute_shell_command('mktemp')

    def read_file(self, file_name):
        return self.execute_shell_command(f'cat {file_name}').strip()

    def remove_file(self, file_name):
        self.execute_shell_command(f'rm {file_name}')

    def remove_all_temp_files(self):
        for f in self.__temp_files:
            self.remove_file(f)
        self.__temp_files.clear()

    def execute_shell_command_log(self, shell_command, raise_exception=True):
        temp_file = self._create_temp_file()
        self.__temp_files.append(temp_file)
        self.execute_shell_command(
            f'{shell_command} 2>&1 | tee {temp_file}', raise_exception
        )
        return temp_file

    # Execute Shell Command
    def execute_shell_command(self, shell_command, raise_exception=True):
        self.log().info(
            'Executing shell command <%s>',
            shell_command,
        )
        result = ''
        try:
            result = (
                self.__android_device.adb.shell(shell_command)
                .decode('utf-8')
                .strip()
            )
        except AdbError as e:
            self.log().error(
                'Error executing shell command <%s>. Error: stdout=[%s]'
                ' stderr=[%s]',
                shell_command,
                e.stdout,
                e.stderr,
            )
            if raise_exception:
                raise Exception(
                    f'Failed to execute shell command [{shell_command}]. Error:'
                    f' stdout=[{e.stdout}] stderr=[{e.stderr}]'
                )
        return result

    # If Shell Command is Blocking, Execute it as Subprocess
    def execute_shell_command_in_subprocess(
        self, subprocess_name, shell_command
    ):
        self.log().info(
            'Executing shell command <%s> in subprocess <%s>',
            shell_command,
            subprocess_name,
        )

        def execute_command():
            self.__android_device.adb.shell(shell_command)

        # Execute Shell Command In Subprocess
        subprocess_shell_command = multiprocessing.Process(
            target=execute_command
        )
        subprocess_shell_command.start()

        # Wait for 1 second to start the process
        time.sleep(1)

        #  Add subprocess to map so that it can be terminated later
        self.__subprocess_map[subprocess_name] = subprocess_shell_command

    # Check if sub-process is running
    def is_subprocess_running(self, subprocess_name):
        self.log().info(
            'Checking subprocess <%s>',
            subprocess_name,
        )

        if not subprocess_name in self.__subprocess_map:
            self.log().warn(
                'Subprocess <%s> does not exist',
                subprocess_name,
            )
            return False

        # Get Subprocess
        subprocess_shell_command = self.__subprocess_map[subprocess_name]

        return subprocess_shell_command.is_alive()

    # Terminate Subprocess
    def terminate_subprocess(self, subprocess_name):
        self.log().info(
            'Terminating subprocess <%s>',
            subprocess_name,
        )

        if not subprocess_name in self.__subprocess_map:
            self.log().warn(
                'Subprocess <%s> does not exist',
                subprocess_name,
            )
            return

        #  Get Subprocess
        subprocess_shell_command = self.__subprocess_map[subprocess_name]

        # Terminate Subprocess
        if subprocess_shell_command.is_alive():
            subprocess_shell_command.kill()
            # Wait for 1 second to terminate the process
            time.sleep(1)

        if subprocess_shell_command.is_alive():
            subprocess_shell_command.terminate()  # Force Kill
            # Wait for 1 second to terminate the process
            time.sleep(1)

        subprocess_shell_command.join()

        if subprocess_shell_command.is_alive():
            raise Exception(f'Failed to stop subprocess {subprocess_name}')

        #  Remove subprocess from map
        del self.__subprocess_map[subprocess_name]

    # Terminate Subprocess
    def terminate_all_subprocesses(self):
        self.log().info(
            'Terminating all subprocesses ',
        )

        for subprocess in self.__subprocess_map:
            self.log().info(
                'Terminating subprocess <%s>',
                subprocess,
            )

            #  Get Subprocess
            subprocess_shell_command = self.__subprocess_map[subprocess]

            # Terminate Subprocess
            if subprocess_shell_command.is_alive():
                subprocess_shell_command.kill()

            # Wait for 1 second to terminate the process
            time.sleep(1)

            if subprocess_shell_command.is_alive():
                subprocess_shell_command.terminate()  # Force Kill
                subprocess_shell_command.join()
                # Wait for 1 second to terminate the process
                time.sleep(1)

            if subprocess_shell_command.is_alive():
                raise Exception(f'Failed to stop subprocess {subprocess}')

        #  Clear Subprocess Map
        self.__subprocess_map.clear()

    # Grep given text from logcat
    def grep_from_logcat(self, grep, logcat_args=None, grep_args=None):
        """Returns filtered logcat output in string format.

        Args:

        grep: grep string to filter the logcat output.
        logcat_args: args for logcat
        grep_args: args for grep
        """
        grep_command = 'grep'
        if grep_args:
            grep_command += f' {grep_args}'

        if grep:
            grep = f' | {grep_command} "{grep}"'
        args = ' -d'
        if logcat_args:
            args += f' {logcat_args}'
        # Grep has a non-zero return code if no matches are found.
        # "|| true" ignores these return codes to avoid logging errors.
        ignore_errors = ' || true'
        logcat_command = (
            'logcat' + args + grep + ignore_errors
        )  # should exit logcat
        return self.execute_shell_command(logcat_command, raise_exception=False)

    def clear_logcat(self):
        return self.execute_shell_command('logcat -c')

    def getprop(self, property_name):
        """Read device property"""
        return self.__android_device.adb.getprop(property_name)

    def get_vsock_local_cid(self):
        """Returns the vsock local cid of the device."""
        return int(
            self.execute_shell_command('./system_ext/bin/vsock_local_cid')
        )

    def get_current_device_timestamp(self):
        """Returns the current device time in a format compatible with logcat timestamps.

        The format is "MM-DD HH:MM:SS.mmm".

        Returns:
            str: The formatted timestamp string.
        """
        timestamp_string = self.execute_shell_command(
            'date +"%m-%d %H:%M:%S.%N"'
        )
        # Ensure the timestamp matches the format used in logcat: mm-dd HH:MM:SS.NNN
        parts = timestamp_string.split('.')
        if len(parts) != 2:
            raise Exception(f'Unexpected date format: {timestamp_string}')
        nanoseconds = parts[1]
        # ljust to handle cases where nanoseconds are less than 9 digits.
        # then truncate to milliseconds (3 digits)
        milliseconds = nanoseconds.ljust(3, '0')[:3]
        return parts[0] + '.' + milliseconds
