# Copyright 2025 Google LLC

import multiprocessing
import time
from typing import List

from mobly.controllers.android_device_lib.adb import AdbError
from mobly.controllers.android_device_lib.services import logcat


class TestDeviceAdb:

  __LOG_TAG = 'TestDeviceAdb'
  __DEFAULT_WAIT_TIMEOUT_SECONDS = 60
  __DEFAULT_TIMEOUT_BOOT_COMPLETION_SECOND = 5 * 60
  __DEFAULT_TIMEOUT_LOGCAT_SECONDS = 10
  __LOGCAT_NON_EMPTY_LINES_GREP_TEXT = '.'

  def __init__(self, android_device):
    self.__android_device = android_device
    self.__subprocess_map = {}
    self.__temp_files = []

  def verify_logcat_is_running(
      self, timeout = __DEFAULT_TIMEOUT_LOGCAT_SECONDS
  ):
    """
    Checks that logcat is running by verifying logs exist.

    Args:
        timeout: how long to wait for logcat (default 10 seconds).

    Raises:
        Exception: if logcat is not running within the time limit.
    """
    end_time = time.time() + timeout
    while time.time() < end_time:
      if self.grep_from_logcat(self.__LOGCAT_NON_EMPTY_LINES_GREP_TEXT):
        return
    raise Exception('Logcat is not running')

  def get_device_serial(self):
    """
      Returns the serial of the device.

      Returns:
          str: The serial of the device.
    """
    return self.__android_device.serial

  def generate_filename(
      self, file_type: str, time_identifier=None, extension_name=None
  ):
    """
      Expose the android_device.generate_filename method.

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
    """
      Use device log.
      Provides information relevant to a specific device object.
    """
    return self.__android_device.log

  def log_path(self):
    """
      Expose the android_device.log_path property to get the path where
      logs will be uploaded from this device.
    """
    return self.__android_device.log_path

  def push(self, host_remote_path: List[str]):
    """
      Expose the android_device.adb.push method for pushing file from host
      to remote.

      Args:
        host_remote_path: List of strings with host path and remote path
          [host_path, remote_path].
    """
    return self.__android_device.adb.push(host_remote_path)

  def pull(self, remote_host_path: List[str]):
    """
      Expose the android_device.adb.pull method for pulling file from
      remote to host.

      Args:
        remote_host_path:  List of strings with remote path and host path
          [remote_path, host_path].
    """
    return self.__android_device.adb.pull(remote_host_path)

  def dumpsys(self, service_name: str):
    """
      Expose the android_device.adb.dumpsys method for getting dumpsys
      information from the device.

      Args:
        service_name: Name of the service to get dumpsys information.

      Returns:
        String, the dumpsys information from the device.
    """
    return self.execute_shell_command(f'dumpsys {service_name}')

  def wait_for_device_online(self, timeout=__DEFAULT_WAIT_TIMEOUT_SECONDS):
    """
      Waits for the devices to be online.

      Args:
          timeout: How long to wait for devices to be online (default 60 secs).

      Raises:
        adb.AdbTimeoutError: If the device is not online within the timeout.
    """
    return self.__android_device.adb.wait_for_device(timeout=timeout)

  def wait_for_device_offline(self, timeout=__DEFAULT_WAIT_TIMEOUT_SECONDS):
    """
      Waits for the devices to be offline.

      Args:
          timeout: How long to wait for devices to be offline (default 60 secs).

      Raises:
          Exception: If devices is not offline within the timeout.
    """
    end_time = time.time() + timeout
    self.log().info(
        f'{self.__LOG_TAG}: Waiting for {timeout} secs for device to go offline'
    )
    while time.time() < end_time:
      try:
        # Attempt a simple shell command. If it succeeds, the device is still online.
        # If it raises Exception, the device is considered offline or unreachable.
        # Using "true" as a minimal, standard shell command.
        self.execute_shell_command('true', raise_exception=True)
        # If the command succeeded, the device is still responsive.
        self.log().info(
            f'{self.__LOG_TAG}: Device is still responsive to ADB shell. Continuing to wait.')
      except Exception as e:
        # Exception implies the device is not reachable via shell.
        self.log().info(
            f'{self.__LOG_TAG}: Device not responsive to ADB shell (Exception: {e}). Considering it offline.')
        return
      time.sleep(0.1)
    raise Exception('Devices is not offline')

  def wait_for_boot_complete(self):
    """
      Waits for the boot flag to be set.

      Raises:
        adb.AdbTimeoutError: If the boot flag is not set within the timeout.
    """
    return self.__android_device.wait_for_boot_completion(
        timeout=self.__DEFAULT_TIMEOUT_BOOT_COMPLETION_SECOND
    )

  def reboot_device(self):
    """
      Reboots the device.
    """
    return self.__android_device.reboot()

  def reboot_device_and_verify_logcat(self):
    """
      Reboots the device and verifies that logcat is running.
    """
    self.__android_device.reboot()
    self.verify_logcat_is_running()

  def root_device(self):
    """
      Root the device.
    """
    return self.__android_device.adb.root()

  def execute_forward_command(self, host_port, device_port):
    """
      Executes the adb forward command.

      Args:
        host_port: The host port to forward.
        device_port: The device port to forward to.
    """
    self.log().info(
        '%s: Executing command (adb forward <%s> <%s>)',
        self.__LOG_TAG,
        host_port,
        device_port,
    )
    args = [host_port, device_port]
    return self.__android_device.adb.forward(args)

  def _create_temp_file(self):
    return self.execute_shell_command('mktemp')

  def read_file(self, file_name):
    """
      Read the contents of a file.
    """
    return self.execute_shell_command(f'cat {file_name}').strip()

  def remove_file(self, file_name):
    """
      Remove the file from the device.
    """
    self.execute_shell_command(f'rm {file_name}')

  def remove_all_temp_files(self):
    """
      Remove all temp files from the device.
    """
    for f in self.__temp_files:
      self.remove_file(f)
    self.__temp_files.clear()

  def execute_shell_command_with_log(self, shell_command, raise_exception=True):
    """
      Execute the shell command and log the output to a file.

      Args:
        shell_command: The shell command to execute.
        raise_exception: Whether to raise an exception if the shell command fails.

      Returns:
        The path to the log file.
    """
    temp_file = self._create_temp_file()
    self.__temp_files.append(temp_file)
    self.execute_shell_command(
        f'{shell_command} 2>&1 | tee {temp_file}', raise_exception
    )
    return temp_file

  def execute_shell_command_in_subprocess_with_log(self, shell_command):
    """
      Execute the shell command in a subprocess and log the output to a file.

      Args:
        shell_command: The shell command to execute.

      Returns:
        The path to the log file.
    """
    temp_file = self._create_temp_file()
    self.__temp_files.append(temp_file)
    self.execute_shell_command_in_subprocess(
        temp_file, f'{shell_command} 2>&1 | tee {temp_file}'
    )
    return temp_file

  def execute_shell_command(self, shell_command, raise_exception=True):
    """
      Execute the shell command on the device.

      Args:
        shell_command: The shell command to execute.

      Returns:
        The output of the shell command.
    """
    self.log().info(
        '%s:Executing shell command <%s>',
        self.__LOG_TAG,
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
          '%s: Error executing shell command <%s>. Error: stdout=[%s]'
          ' stderr=[%s]',
          self.__LOG_TAG,
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

  def execute_shell_command_in_subprocess(
      self, subprocess_name, shell_command
  ):
    """
      Execute the shell command in a subprocess.

      Args:
        subprocess_name: The name of the subprocess.
        shell_command: The shell command to execute.

      Raises:
        Exception: If the shell command fails.
    """
    self.log().info(
        '%s: Executing shell command <%s> in subprocess <%s>',
        self.__LOG_TAG,
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

  def is_subprocess_running(self, subprocess_name):
    """
      Check if the subprocess is running.

      Args:
        subprocess_name: The name of the subprocess.

      Returns:
        True if the subprocess is running, False otherwise.
    """
    self.log().info(
        '%s: Checking subprocess <%s>',
        self.__LOG_TAG,
        subprocess_name,
    )

    if not subprocess_name in self.__subprocess_map:
      self.log().warn(
          '%s: Subprocess <%s> does not exist',
          self.__LOG_TAG,
          subprocess_name,
      )
      return False

    # Get Subprocess
    subprocess_shell_command = self.__subprocess_map[subprocess_name]

    return subprocess_shell_command.is_alive()

  # Terminate Subprocess
  def terminate_subprocess(self, subprocess_name):
    """
      Terminate the subprocess.

      Args:
        subprocess_name: The name of the subprocess.

      Raises:
        Exception: If the subprocess fails to terminate.
    """
    self.log().info(
        '%s: Terminating subprocess <%s>',
        self.__LOG_TAG,
        subprocess_name,
    )

    if not subprocess_name in self.__subprocess_map:
      self.log().warn(
          '%s: Subprocess <%s> does not exist',
          self.__LOG_TAG,
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

  def terminate_all_subprocesses(self):
    """
      Terminate all subprocesses.
    """
    self.log().info(
        '%s: Terminating all subprocesses ', self.__LOG_TAG
    )

    for subprocess in self.__subprocess_map:
      self.log().info(
          '%s: Terminating subprocess <%s>',
          self.__LOG_TAG,
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

  def grep_from_logcat(self, grep, logcat_args=None, grep_args=None):
    """
      Returns filtered logcat output in string format.

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
    logcat_command = 'logcat' + args + grep + ignore_errors  # should exit logcat
    return self.execute_shell_command(logcat_command, raise_exception=False)

  def clear_logcat(self):
    """
      Clear the logcat buffer.
    """
    return self.execute_shell_command('logcat -c')

  def getprop(self, property_name):
    """
      Read device property
    """
    return self.__android_device.adb.getprop(property_name)

  def get_current_device_timestamp(self):
    """
      Returns the current device time in a format compatible with logcat timestamps.

      The format is "MM-DD HH:MM:SS.mmm".

      Returns:
        str: The formatted timestamp string.
    """
    timestamp_string = self.execute_shell_command('date +"%m-%d %H:%M:%S.%N"')
    # Ensure the timestamp matches the format used in logcat: mm-dd HH:MM:SS.NNN
    parts = timestamp_string.split('.')
    if len(parts) != 2:
      raise Exception(f'Unexpected date format: {timestamp_string}')
    nanoseconds = parts[1]
    # ljust to handle cases where nanoseconds are less than 9 digits.
    # then truncate to milliseconds (3 digits)
    milliseconds = nanoseconds.ljust(3, '0')[:3]
    return parts[0] + '.' + milliseconds


class TestDevice:

  def __init__(self, android_device):
    self.__adb = TestDeviceAdb(android_device)
    self.__services = android_device.services

  def adb(self):
    """
      Returns the object to access device adb.'
    """
    return self.__adb

  def services(self):
    """
      Returns the object to access device services.
    """
    return self.__services

  def update_logcat_config_to_verbose_and_persist(self):
    """
      Enable verbose logs and restart logcat service
      with 'clear_log' set to False
    """
    self.adb().execute_shell_command('setprop persist.log.tag V')
    self.services().logcat.stop()
    self.services().logcat.update_config(logcat.Config(clear_log=False))
    self.services().logcat.start()
    self.adb().verify_logcat_is_running()
