# Copyright 2025 Google LLC

import multiprocessing
import time
from typing import List

from mobly.controllers.android_device_lib.adb import AdbError
from mobly.controllers.android_device_lib.services import logcat

from spectatio_host_tf.utils import error_handler


class TestDeviceAdb:
  """
    A controller for device actions, primarily interacting with ADB.
  """

  _LOG_TAG = 'TestDeviceAdb'
  _DEFAULT_WAIT_TIMEOUT_SECONDS = 60
  _DEFAULT_TIMEOUT_BOOT_COMPLETION_SECOND = 5 * 60
  _DEFAULT_TIMEOUT_LOGCAT_SECONDS = 10
  _LOGCAT_NON_EMPTY_LINES_GREP_TEXT = '.'

  def __init__(self, android_device):
    self._android_device: android_device.AndroidDevice = android_device
    self._subprocess_map: dict[str, multiprocessing.Process] = {}
    self._temp_files: List[str] = []

  def verify_logcat_is_running(
      self, timeout: int =_DEFAULT_TIMEOUT_LOGCAT_SECONDS
  ) -> None:
    """
      Checks that logcat is running by verifying logs exist.

      Args:
          timeout: How long to wait for logcat (default 10 seconds).

      Raises:
          DeviceError: If logcat is not running within the time limit.
    """
    end_time: float = time.time() + timeout
    while time.time() < end_time:
      if self.grep_from_logcat(
          grep=self._LOGCAT_NON_EMPTY_LINES_GREP_TEXT, logcat_args='-t 1'
      ):
        return
    raise error_handler.DeviceError('Logcat is not running.')

  @property
  def serial(self) -> str:
    """
      The serial number of the device.
    """
    return self._android_device.serial

  def generate_filename(
      self, file_type: str, time_identifier=None, extension_name=None
  ) -> str:
    """
      Generates a name for an output file related to this device.

      The name follows the pattern:
      {file type},{debug_tag},{serial},{model},{time identifier}.{ext}

      Args:
        file_type: Type of this file, like "logcat".
        time_identifier: A string or RuntimeTestInfo. If a `RuntimeTestInfo` is
          passed, its `signature` is used. If a string is passed, it is used
          directly. Otherwise, the current timestamp is used.
        extension_name: The extension name of the file.

      Returns:
          The generated filename.
    """
    return self._android_device.generate_filename(
        file_type, time_identifier, extension_name
    )

  @property
  def log(self):
    """
      The logger for this device.
    """
    return self._android_device.log

  @property
  def log_path(self) -> str:
    """
      The path where logs for this device are stored.
    """
    return self._android_device.log_path

  def push(self, host_remote_path: List[str]) -> None:
    """
      Pushes a file from the host to the device.

      Args:
        host_remote_path: A list containing [host_path, remote_path].
    """
    return self._android_device.adb.push(host_remote_path)

  def pull(self, remote_host_path: List[str]) -> None:
    """
      Pulls a file from the device to the host.

      Args:
        remote_host_path:  A list containing [remote_path, host_path].
    """
    return self._android_device.adb.pull(remote_host_path)

  def dumpsys(self, service_name: str) -> str:
    """
      Gets dumpsys information for a service.

      Args:
        service_name: Name of the service.

      Returns:
        The dumpsys information as a string.
    """
    return self.execute_shell_command(f'dumpsys {service_name}')

  def wait_for_device_online(self, timeout=_DEFAULT_WAIT_TIMEOUT_SECONDS) -> None:
    """
      Waits for the device to be online.

      Args:
          timeout: How long to wait.

      Raises:
        adb.AdbTimeoutError: If the device is not online within the timeout.
    """
    return self._android_device.adb.wait_for_device(timeout=timeout)

  def wait_for_device_offline(self, timeout=_DEFAULT_WAIT_TIMEOUT_SECONDS) -> None:
    """
      Waits for the device to go offline.

      Args:
          timeout: How long to wait.

      Raises:
          DeviceError: If the device is not offline within the timeout.
    """
    end_time: float = time.time() + timeout
    self.log.info(
        '%s: Waiting for %s secs for device to go offline',
        self._LOG_TAG,
        timeout,
    )
    while time.time() < end_time:
      try:
        self.execute_shell_command('true', raise_exception=True)
        self.log.info(
            '%s: Device is still responsive. Continuing to wait.', self._LOG_TAG
        )
      except error_handler.DeviceError as e:
        self.log.info(
            '%s: Device not responsive (Error: %s). Considering it offline.',
            self._LOG_TAG,
            e,
        )
        return
      time.sleep(0.1)
    raise error_handler.DeviceError('Device did not go offline within the timeout.')

  def wait_for_boot_complete(self) -> None:
    """
      Waits for the device's boot flag to be set.

      Raises:
        adb.AdbTimeoutError: If the boot flag is not set within the timeout.
    """
    return self._android_device.wait_for_boot_completion(
        timeout=self._DEFAULT_TIMEOUT_BOOT_COMPLETION_SECOND
    )

  def reboot(self) -> None:
    """
      Reboots the device.
    """
    return self._android_device.reboot()

  def reboot_and_verify_logcat(self) -> None:
    """
      Reboots the device and verifies that logcat is running.
    """
    self.reboot()
    self.verify_logcat_is_running()

  def root(self) -> None:
    """
      Restarts adbd with root permissions
    """
    return self._android_device.adb.root()

  def forward(self, host_port, device_port) -> None:
    """
      Forwards a host port to a device port.

      Args:
        host_port: The host port.
        device_port: The device port.
    """
    self.log.info(
        f'{self._LOG_TAG}: Executing `adb forward {host_port} {device_port}`'
    )
    args: List[str] = [host_port, device_port]
    return self._android_device.adb.forward(args)

  def _create_temp_file(self) -> str:
    return self.execute_shell_command('mktemp')

  def read_file(self, file_path) -> str:
    """
      Reads the contents of a file on the device.
    """
    return self.execute_shell_command(f'cat {file_path}').strip()

  def remove_file(self, file_path) -> None:
    """
      Removes a file from the device.
    """
    self.execute_shell_command(f'rm {file_path}')

  def remove_all_temp_files(self) -> None:
    """
      Removes all temporary files created during the test.
    """
    for f in self._temp_files:
      self.remove_file(f)
    self._temp_files.clear()

  def execute_shell_command_with_log(
      self, shell_command, raise_exception=True) -> str:
    """
      Executes a shell command and logs the output to a temporary file.

      Args:
        shell_command: The shell command to execute.
        raise_exception: If True, raises an exception on failure.

      Returns:
        The path to the log file on the device.
    """
    temp_file: str = self._create_temp_file()
    self._temp_files.append(temp_file)
    self.execute_shell_command(
        f'{shell_command} 2>&1 | tee {temp_file}', raise_exception
    )
    return temp_file

  def execute_shell_command_in_subprocess_with_log(self, shell_command) -> str:
    """
      Executes a shell command in a subprocess, logging to a temporary file.

      Args:
        shell_command: The shell command to execute.

      Returns:
        The path to the log file on the device.
    """
    temp_file: str = self._create_temp_file()
    self._temp_files.append(temp_file)
    self.execute_shell_command_in_subprocess(
        temp_file, f'{shell_command} 2>&1 | tee {temp_file}'
    )
    return temp_file

  def execute_shell_command(self, shell_command, raise_exception=True) -> str:
    """
      Executes a shell command on the device.

      Args:
        shell_command: The command to execute.
        raise_exception: If True, raises DeviceError on failure.

      Returns:
        The output of the shell command.

      Raises:
        DeviceError: If the command fails and raise_exception is True.
    """
    self.log.info('%s:Executing: %s', self._LOG_TAG, shell_command)
    try:
      return (
          self._android_device.adb.shell(shell_command)
          .decode('utf-8')
          .strip()
      )
    except AdbError as e:
      self.log.error(
          '%s: Command failed: %s. stdout: %s, stderr: %s',
          self._LOG_TAG,
          shell_command,
          e.stdout,
          e.stderr,
      )
      if raise_exception:
        raise error_handler.DeviceError(
            f'Failed to execute "{shell_command}".'
            f' stdout: {e.stdout}, stderr: {e.stderr}'
        ) from e
    return ''

  def execute_shell_command_in_subprocess(
      self, subprocess_name, shell_command
  ) -> None:
    """
      Executes a shell command in a background process.

      Args:
        subprocess_name: A unique name for this process.
        shell_command: The command to execute.
    """
    self.log.info(
        '%s: Starting subprocess "%s": %s',
        self._LOG_TAG,
        shell_command,
        subprocess_name,
    )

    def execute_command():
      self._android_device.adb.shell(shell_command)

    process: multiprocessing.Process = multiprocessing.Process(
        target=execute_command)
    process.start()
    time.sleep(1)  # Allow time for the process to start.
    self._subprocess_map[subprocess_name] = process

  def is_subprocess_running(self, subprocess_name) -> bool:
    """
      Checks if a named subprocess is still running.

      Args:
        subprocess_name: The name of the subprocess.

      Returns:
        True if the subprocess is running, False otherwise.
    """
    if subprocess_name not in self._subprocess_map:
      self.log.warning(
          '%s: Subprocess "%s" does not exist.', self._LOG_TAG, subprocess_name
      )
      return False
    return self._subprocess_map[subprocess_name].is_alive()

  def terminate_subprocess(self, subprocess_name) -> None:
    """
      Terminates a named subprocess.

      Args:
        subprocess_name: The name of the subprocess.

      Raises:
        DeviceError: If the subprocess fails to terminate.
    """
    self.log.info('%s: Terminating subprocess "%s"', self._LOG_TAG, subprocess_name)
    if subprocess_name not in self._subprocess_map:
      self.log.warning(
          '%s: Subprocess "%s" does not exist.', self._LOG_TAG, subprocess_name
      )
      return

    process: multiprocessing.Process = self._subprocess_map[subprocess_name]
    if process.is_alive():
      process.kill()
      time.sleep(1)

    if process.is_alive():
      process.terminate()
      time.sleep(1)

    process.join(timeout=5)

    if process.is_alive():
      raise error_handler.DeviceError(f'Failed to stop subprocess "{subprocess_name}".')

    del self._subprocess_map[subprocess_name]

  def terminate_all_subprocesses(self) -> None:
    """Terminates all running subprocesses."""
    self.log.info('%s: Terminating all subprocesses.', self._LOG_TAG)
    for name in list(self._subprocess_map.keys()):
      self.terminate_subprocess(name)
    self._subprocess_map.clear()

  def grep_from_logcat(self, grep, logcat_args=None, grep_args=None) -> str:
    """
      Returns filtered logcat output.

      Args:
        grep: Grep string to filter the logcat output.
        logcat_args: Additional arguments for logcat.
        grep_args: Additional arguments for grep.
    """
    grep_command = 'grep'
    if grep_args:
      grep_command += f' {grep_args}'

    grep_pipe = f' | {grep_command} "{grep}"' if grep else ''
    logcat_args = f' {logcat_args}' if logcat_args else ''

    # "|| true" ignores non-zero exit codes from grep if no matches are found.
    logcat_command = f'logcat -d{logcat_args}{grep_pipe} || true'
    return self.execute_shell_command(logcat_command, raise_exception=False)

  def grep_from_logcat_with_log_in_subprocess(
      self, grep_text, logcat_args=None
  ) -> str:
    """
      Starts a subprocess to filter logcat output and logs it to a log file.

      Args:
        grep_text: Grep string to filter the logcat output.
        logcat_args: Additional arguments for logcat.
    """
    logcat_args = f' {logcat_args}' if logcat_args else ''

    logcat_command = f'logcat{logcat_args} -e {grep_text}'
    return self.execute_shell_command_in_subprocess_with_log(
        logcat_command
    )

  def poll_logcat_with_grep_text_and_check_for_expected_result(
      self, grep_text, expected_result, timeout=5, poll_interval=0.1
  ):
    """
      Polls the logcat output for a specific text until found or timeout.

      Args:
          grep_text: The text to search for in the logcat output.
          expected_result: The expected result to find.
          timeout: The maximum time (in seconds) to wait.
          poll_interval: The time (in seconds) between polls.

      Returns:
          True if the expected result is found within the timeout,
          False otherwise.
    """
    deadline = time.perf_counter() + timeout
    while time.perf_counter() < deadline:
      logcat_result = self.grep_from_logcat(grep_text)
      if expected_result in logcat_result:
        return True
      time.sleep(poll_interval)

    return False

  def poll_logs_from_file_and_check_for_expected_result(
      self, file_path, expected_result, timeout=5, poll_interval=0.1
  ):
    """
      Polls the file contents for a specific text until found or timeout.

      Args:
          file_path: The path to the file to poll.
          expected_result: The expected result to find in the file.
          timeout: The maximum time (in seconds) to wait.
          poll_interval: The time (in seconds) between polls.

      Returns:
          True if the expected result is found within the timeout,
          False otherwise.
    """
    deadline = time.perf_counter() + timeout
    while time.perf_counter() < deadline:
      logcat_result = self.read_file(file_path)
      if expected_result in logcat_result:
        return True
      time.sleep(poll_interval)

    return False

  def clear_logcat(self) -> None:
    """
      Clears the logcat buffer.
    """
    return self.execute_shell_command('logcat -c')

  def getprop(self, property_name) -> str:
    """
      Reads a device property.
    """
    return self._android_device.adb.getprop(property_name)

  def get_current_device_timestamp(self) -> str:
    """
      Returns the current device time in "MM-DD HH:MM:SS.mmm" format.

      Returns:
        The formatted timestamp string.

      Raises:
        DeviceError: If the date format from the device is unexpected.
    """
    ts_string: str = self.execute_shell_command('date +"%m-%d %H:%M:%S.%N"')
    parts: List[str] = ts_string.split('.')
    if len(parts) != 2:
      raise error_handler.DeviceError(
          f'Unexpected date format from device: "{ts_string}"')

    milliseconds: str = parts[1].ljust(3, '0')[:3]
    return f'{parts[0]}.{milliseconds}'


class TestDevice:
  """
    A facade for interacting with a test device, providing access to ADB
    commands and device services.
  """

  def __init__(self, android_device):
    self._adb = TestDeviceAdb(android_device)
    self._services = android_device.services

  @property
  def adb(self) -> TestDeviceAdb:
    """
      Provides access to ADB-related device actions
    """
    return self._adb

  @property
  def services(self):
    """
      Provides access to Mobly device services (e.g., Logcat).
    """
    return self._services

  def update_logcat_config_to_persist_for_given_log_level(
      self, log_level: str = 'V'
  ) -> None:
    """
      Enables verbose logs and restarts the logcat service, preserving the
      buffer.
    """
    self.adb.execute_shell_command(f'setprop persist.log.tag {log_level}')
    self.services.logcat.stop()
    self.services.logcat.update_config(logcat.Config(clear_log=False))
    self.services.logcat.start()
    self.adb.verify_logcat_is_running()
