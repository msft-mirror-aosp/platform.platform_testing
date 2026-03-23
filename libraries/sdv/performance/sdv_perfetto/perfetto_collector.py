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

"""Library for Perfetto trace recording.
We support two types of traces:
1. On demand trace:
  - start_trace()
  - stop_trace()
2. On boot trace:
  - set_start_trace_on_boot()
  - stop_trace(read_from_running_process=True)

Example usage:    # pylint: disable=line-too-long

from sdv_perfetto import collector_config
from sdv_perfetto import perfetto_collector

self.perfetto_collector = (
            perfetto_collector.PerfettoCollector(
                device=self.sdv_device,
                [config=collector_config.CollectorConfig(
                    config_path=<config_path|config_name_in_data>]
                ),
            )
        )

# Start trace
self.perfetto_collector.start_trace()
or for on boot trace:
self.perfetto_collector.set_start_trace_on_boot()
self.device.reboot_device()


# Test scenarios

trace_file_path = self.perfetto_collector.stop_trace([tag='tag'])
or for on boot trace:
trace_file_path = self.perfetto_collector.stop_trace([tag='tag'], read_from_running_process=True)

Notes:
  1. Steps of rebooting device will interrupt perfetto process. Make sure to
  call start_trace() after reboot device.
  2. Test assertion Error will interrupt code execution. Make sure to call
  stop_trace() in teardown_test(), teardown_class() or try-finally block.

For multi-vm tracing, use the following API to start/stop trace.
# Create collector for center VM and client VM:

self.center_vm_collector = perfetto_collector.PerfettoCollector(
    device=self.sdv_device1,
    config=collector_config.CollectorConfig(
        multi_vm_tracing=True,
        multi_vm_tracing_vsock=True,
        secondarry_devices=[self.device2, self.device3],
    ),
)


# Start trace
self.center_vm_collector.start_trace()

# Test scenarios

# Stop trace
self.center_vm_collector.stop_trace()

"""

import logging
import os
import pathlib
import re
import time
from typing import List, Optional

from mobly import utils
from mobly.controllers.android_device_lib import adb
from mobly.controllers.android_device_lib import errors
from sdv_perfetto import collector_config
from sdv_test_fw.device import sdv_adb
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


PERFETTO_CMD = 'perfetto'

TRACED_ENABLE_PROP = 'persist.traced.enable'
TRACE_ON_BOOT_PROP = 'persist.debug.perfetto.boottrace'
SDV_TRACE_ON_BOOT_PROP = 'persist.debug.sdv.boottrace'

KILL_PERFETTO_WAIT_COUNT = 5
KILL_PERFETTO_WAIT_TIME = 5
DEFAULT_START_TRACE_DELAY = 0
DEFAULT_STOP_TRACE_DELAY = 5
PUSH_CONFIG_WAIT_TIME = 5

HOST_TRACE_DIR = '/tmp/perfetto-traces/'
DEVICE_TRACE_DIR = '/data/misc/perfetto-traces/'
DEVICE_TRACE_CONFIG_DIR = '/data/misc/perfetto-configs/'
DEFAULT_TRACE_CONFIG_NAME = 'default_trace_cfg.pbtx'
DEVICE_TRACE_CONFIG_FILENAME = 'trace_config'
# On boot trace config name and output file name are hardcoded.
# https://perfetto.dev/docs/case-studies/android-boot-tracing
DEVICE_ON_BOOT_TRACE_CONFIG_FILENAME = 'boottrace.pbtxt'
DEVICE_ON_BOOT_TRACE_OUTPUT_FILENAME = 'boottrace.perfetto-trace'

ARCH_TO_INET_INTERFACE = {
    'x86_64': 'eth1',  # Cuttlefish
    'arm64-v8a': 'eth0',  # Raspberry Pi
}


class PerfettoCollectorError(errors.Error):
    """Class for errors encountered by the Perfetto trace collector."""


class PerfettoCollector:
    """Class for managing Perfetto trace collection.

    Attributes:
      device_trace_output: Full path to a temporary trace file on a device.
    """

    device_trace_output: Optional[str] = None

    def __init__(
            self,
            device: sdv_adb.SdvAdb,
            config: collector_config.CollectorConfig = collector_config.CollectorConfig()):
        """Create a PerfettoCollector.

        Args:
          device: sdv_test_fw.test_execution.sdv_adb.SdvAdb
          config: CollectorConfig
        """
        self._config = config
        self._device = device

        self._last_perfetto_pid = None
        self._last_trace_file = None
        self._tagged_trace_files = {}
        self._device_trace_config_path = None
        if self._config.multi_vm_tracing:
            self._set_multi_vm_tracing(self._config)
            self._torq_proc = None

    def _set_multi_vm_tracing(self, config: collector_config.CollectorConfig):
        """Sets multi vm tracing.
            This is required to collect traces from multiple VMs.
            go/sdv-tracing#unified-tracing-with-torq
        """
        self._torq_binary_path = os.path.join(
            os.path.dirname(__file__), 'torq')
        os.chmod(self._torq_binary_path, 0o777)
        host_mode = '--primary-cid' if config.multi_vm_tracing_vsock else '--primary-ip'
        host_id = self.get_vsock_id() if config.multi_vm_tracing_vsock else self.get_inet_address()
        secondary_devices = []
        for device in config.secondary_devices:
            secondary_devices.append('--secondary')
            secondary_devices.append(device.get_device_serial())
        # Using VSOCK
        # torq vm configure --primary <android-serial-1> --primary-cid <HOST_VM_CID> \
        #          --secondary <android-serial-2> \
        #          --secondary <android-serial-3>
        # Using TCP
        #torq vm configure --primary <serial-1> --primary-ip <PRIMARY_VM_IP> \
        #          --secondary <serial-2>
        cmd = [
            self._torq_binary_path,
            'vm',
            'configure',
            '--primary',
            self._device.get_device_serial(),
            host_mode,
            host_id
        ]
        cmd.extend(secondary_devices)
        logging.info(f'Config vm for multi-vm tracing: {cmd}')
        utils.run_command(cmd)

    def get_trace_file(self, tag: Optional[str] = None) -> str:
        """Get the path to the last collected trace, or trace specified by tag.

        Args:
          tag: str, tag to uniquely identify desired trace file. If None,
            defaults to last collected trace.

        Returns:
          str, absolute path to the trace file
        """
        return self._tagged_trace_files[tag] if tag else self._last_trace_file

    def _adb_shell(self, *args: str) -> str:
        """Runs adb shell command.

        Args:
          *args: Command to run as separate string parts.

        Returns:
          Command output as string.
        """
        return self._device.execute_shell_command(list(args))

    def _get_process_by_pid(self, pid: int) -> str:
        """Get the process name by pid.
        Args:
          pid: int, pid of the process
        Returns:
          str, process name or empty string if not found
        """
        return self._adb_shell('ps', '-p', str(pid), '-o', 'name=')

    def update_last_perfetto_pid_from_running_process(self):
        """Update the lastperfetto pid from running process."""
        perfetto_process_ids = self.get_pids_from_ps(PERFETTO_CMD)
        if perfetto_process_ids:
            self._device.log().info(
                f'Find running {PERFETTO_CMD} process ids:\n'
                + f'{perfetto_process_ids}')
            self._last_perfetto_pid = perfetto_process_ids[-1]
        else:
            self._last_perfetto_pid = None

    # TODO: move this function to test_framework
    def get_pids_from_ps(self, process_name: str) -> Optional[List[int]]:
        """Returns the given process' PIDs.
        Args:
          process_name: str, name of the process

        Returns:
          List[int], list of PIDs or None if not found
        """
        try:
            result = self._adb_shell('ps', '|', 'grep', process_name)
            self._device.log().info(
                f'Find running {process_name} process:\n' + result
            )
            flattened_result = result.split()
            return [
                int(flattened_result[i])
                for i in range(1, len(flattened_result), 9)
            ]
        except Exception:
            pass
        return None

    def get_vsock_id(self) -> str:
        """Returns the vsock id of the device.
        """
        return self._device.get_vsock_local_cid()

    # TODO: move this function to test_framework
    def get_inet_address(self) -> str:
        """Returns the inet address of the device.
        Parses the inet address of eth1 from ifconfig output.
        '''
          inet addr:192.168.98.3  Bcast:192.168.98.255  Mask:255.255.255.0
        '''
        """
        inet_interface = self.get_inet_interface()
        eth = self._device.execute_shell_command(
            f'ifconfig {inet_interface} | grep "inet addr"')
        regex = r'(?<=inet addr:)\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}'
        match = re.search(regex, eth)
        if match:
            ip_address = match.group(0)
            logging.info(f'Found IP address: {ip_address}')
            return ip_address
        raise PerfettoCollectorError(
            self._device, 'Failed to get inet address.'
        )

    def get_inet_interface(self) -> str:
        """Returns the inet interface of the device.
        """
        target_arch = self._device.prop.get(SdvDeviceProperty.CPU_ARCH)
        logging.info(f'target arch: {target_arch}')
        if target_arch in ARCH_TO_INET_INTERFACE:
            return ARCH_TO_INET_INTERFACE[target_arch]
        raise PerfettoCollectorError(
            self._device, f'{target_arch} is not supported target arch.'
        )

    def _start_torq(
            self,
            config: collector_config.CollectorConfig,
            start_trace_delay: int = DEFAULT_START_TRACE_DELAY):
        """
          Starts multi vm tracing via torq. go/sdv-tracing#unified-tracing-with-torq
        """
        self._torq_binary_path = os.path.join(
            os.path.dirname(__file__), 'torq')
        cmd = [
            self._torq_binary_path,
            '--serial',
            self._device.get_device_serial(),
            '--perfetto-config',
            config.config_path,
            '--no-ui',
            '--out-dir',
            self._device.log_path(),
        ]
        logging.info(f'starting torq for multi-vm tracing: {cmd}')
        self._torq_proc = utils.start_standing_subprocess(cmd)
        time.sleep(start_trace_delay)

    def trace_running(self, read_from_running_process: bool = False) -> bool:
        """Checks if the trace is currently running.
        Args:
          read_from_running_process: bool, if True, read the running process to
            get the last perfetto pid.

        Returns:
          bool, True if a Perfetto trace is currently running.
        """
        if read_from_running_process:
            self.update_last_perfetto_pid_from_running_process()
        if self._last_perfetto_pid is None:
            return False

        try:
            process_name = self._get_process_by_pid(self._last_perfetto_pid)
            if process_name.strip().lower() == PERFETTO_CMD:
                return True
        except (adb.AdbError, Exception):
            pass
        return False

    def _kill_perfetto(self):
        """Kill an active Perfetto trace started by this collector.

        Raises:
          PerfettoCollectorError: failed to kill Perfetto.
        """
        self._device.log().debug(
            'Terminating currently active Perfetto trace process with PID: %d.',
            self._last_perfetto_pid,
        )
        self._adb_shell('kill', '-INT', str(self._last_perfetto_pid))
        for _ in range(KILL_PERFETTO_WAIT_COUNT):
            if not self.trace_running():
                self._last_perfetto_pid = None
                return
            time.sleep(KILL_PERFETTO_WAIT_TIME)
        raise PerfettoCollectorError(
            self._device, 'Failed to kill active Perfetto trace.'
        )

    def _generate_trace_filename(self, tag: Optional[str] = None) -> str:
        """Generates a trace filename to be used on both host and device.

        Args:
          tag: Optional tag that can be used to uniquely identify this trace.

        Returns:
          str, a new trace filename.
        """
        file_type = 'perfetto'
        if tag:
            file_type += f'_{tag}'
        return self._device.generate_filename(
            file_type, extension_name='perfetto-trace'
        )

    def _start_trace(self, start_trace_delay: int = DEFAULT_START_TRACE_DELAY):
        """Starts the Perfetto trace via the modern (Android 12+) approach.

        Since Android 12 /data/misc/perfetto-configs can be used for storing
        configs (https://perfetto.dev/docs/quickstart/android-tracing - ctrl-F
        "caveats").
        the recording command is something like:
        ```
        perfetto -D -c /data/misc/perfetto-configs/trace_config -o /data/misc/perfetto-traces/trace.perfetto-trace
        ```
        Raises:
          PerfettoCollectorError: failed to start Perfetto process
        """
        try:
            # Push the trace config, then run Perfetto.
            self._device_trace_config_path = os.path.join(
                DEVICE_TRACE_CONFIG_DIR, DEVICE_TRACE_CONFIG_FILENAME
            )
            self._device.push(
                [self._config.config_path, self._device_trace_config_path]
            )
            self._adb_shell('sync')

            cmd = [
                PERFETTO_CMD,
                '-D' if self._config.background_wait else '-d',
                '-c',
                self._device_trace_config_path,
                '-o',
                self.device_trace_output,
            ]
            if self._config.config_txt:
                cmd.append('--txt')
            self._device.log().debug(
                'Running Perfetto command via pushed config: %s.', cmd
            )

            # Try to start Perfetto via the modern (Android 12+) approach.
            perfetto_output = self._adb_shell(*cmd)
            time.sleep(start_trace_delay)
            # Parse Perfetto PID as the last number in perfetto command output.
            perfetto_pids = re.findall(r'^(\d+)$', perfetto_output, re.M)
            self._last_perfetto_pid = (
                int(perfetto_pids[-1]) if perfetto_pids else None)
            if self._last_perfetto_pid is None:
                raise PerfettoCollectorError(
                    self._device, 'Failed to start Perfetto trace.')
            self._device.log().debug(
                'Successfully started Perfetto trace process with PID: %d.',
                self._last_perfetto_pid,
            )

        except adb.Error as e:
            raise PerfettoCollectorError(
                self._device, 'Failed to start Perfetto trace.'
            ) from e

    def start_trace(self, start_trace_delay: int = DEFAULT_START_TRACE_DELAY):
        """Starts the Perfetto trace through adb.

        Args:
          start_trace_delay: int, seconds to wait before starting Perfetto
            trace.

        Raises:
          PerfettoCollectorError: failed to start Perfetto process
        """
        # Kill the previous Perfetto process started by this collector.
        if self.trace_running():
            self._kill_perfetto()
        try:
            if self._config.multi_vm_tracing:
                logging.info('start multi-vms tracing via torq')
                self._start_torq(self._config)
                return None
            else:
                # Use an unique trace filename on a device, so that different collector
                # instances won't interfere.
                self.device_trace_output = os.path.join(
                    DEVICE_TRACE_DIR, self._generate_trace_filename()
                )
                self._device.log().info('Starting Perfetto trace.')
                self._adb_shell('setprop', TRACED_ENABLE_PROP, '1')
                logging.info('start tracing via perfetto directly')
                self._start_trace(start_trace_delay)
        except PerfettoCollectorError as modern_exc:
            raise modern_exc  # pylint: disable=bad-exception-cause

    def set_start_trace_on_boot(self):
        """Sets the start trace on boot."""
        # Push the trace config, and enable the perfetto_trace_on_boot service.
        # https://perfetto.dev/docs/case-studies/android-boot-tracing
        self._device_trace_config_path = os.path.join(
            DEVICE_TRACE_CONFIG_DIR, DEVICE_ON_BOOT_TRACE_CONFIG_FILENAME
        )
        self._device.push(
            [self._config.config_path, self._device_trace_config_path]
        )
        self._device.execute_shell_command(['setprop', TRACE_ON_BOOT_PROP, '1'])
        self._device.execute_shell_command(['setprop', SDV_TRACE_ON_BOOT_PROP, '1'])
        self.device_trace_output = os.path.join(
            DEVICE_TRACE_DIR, DEVICE_ON_BOOT_TRACE_OUTPUT_FILENAME
        )
        # Ensure that the config file is written before the device is rebooted.
        self._adb_shell('sync')

    def stop_trace(
        self,
        host_output_dir: Optional[str] = None,
        tag: Optional[str] = None,
        stop_trace_delay: int = DEFAULT_STOP_TRACE_DELAY,
        read_from_running_process: bool = False,
    ) -> Optional[str]:
        """Kills the Perfetto process and pulls the collected trace from device.

        Args:
          host_output_dir: str, dir on host to store Perfetto output. Defaults
            to device's log path.
          tag: str, optional tag that can be used to uniquely identify this
            trace collection, and is added to the trace filename.
          stop_trace_delay: int, seconds to wait before stopping Perfetto trace.
            This gives Perfetto time to dump its final collected data. Set this
            value in accordance with the write interval in your trace config.

        Returns:
          str, path to the collected trace

        Raises:
          PerfettoCollectorError: if the trace process failed to terminate, or
          if
            the trace file is missing from device.
        """
        if self._config.multi_vm_tracing:
            return self._stop_torq(stop_trace_delay)

        self._stop_perfetto(stop_trace_delay, read_from_running_process)
        # pull perfetto trace file
        host_output_path = self._pull_trace_file(host_output_dir, tag)
        # clean up the trace config and trace file on the device
        self._cleanup_device_output()
        return host_output_path

    def _stop_perfetto(self, stop_trace_delay: int, read_from_running_process: bool = False):
        """Stops the Perfetto process.

        Args:
          stop_trace_delay: int, seconds to wait before stopping Perfetto trace.
            This gives Perfetto time to dump its final collected data. Set this
            value in accordance with the write interval in your trace config.

        Raises:
          PerfettoCollectorError: if the trace process failed to terminate.
        """
        self._device.log().debug(
            'Wait for %d seconds before stopping Perfetto. '
            'This allows Perfetto to dump its final data.',
            stop_trace_delay,
        )
        time.sleep(stop_trace_delay)
        self._device.log().info('Stopping Perfetto trace.')
        try:
            if self.trace_running(read_from_running_process):
                self._kill_perfetto()
                self._adb_shell('sync')
        except adb.Error as e:
            # reboot device if failed to kill perfetto to assure perfetto process is to be interrupted
            self._device.reboot_device()
            raise PerfettoCollectorError(
                self._device, 'Failed to kill Perfetto trace.'
            ) from e

    def _stop_torq(self, stop_trace_delay: int = DEFAULT_STOP_TRACE_DELAY):
        """stop an active Torq process started by this collector."""
        if self._torq_proc:
            self._device.log().debug(
                'Terminating currently active Torq process.'
            )
            self._torq_proc.terminate()
            time.sleep(stop_trace_delay)
            pattern = '*.perfetto-trace'
            found_files = []
            dir_path = pathlib.Path(self._device.log_path())
            for file_path in dir_path.rglob(pattern):
                if file_path.is_file():
                    found_files.append(file_path)
            return str(found_files[0])
        else:
            raise PerfettoCollectorError(
                self._device, 'No active Torq process to stop.'
            )

    def _pull_trace_file(self, host_output_dir: Optional[str] = None, tag: Optional[str] = None) -> str:
        """Pulls the collected trace from device.

        Args:
          host_output_dir: str, dir on host to store Perfetto output. Defaults
            to device's log path.
          tag: str, optional tag that can be used to uniquely identify this
            trace collection, and is added to the trace filename.
        Returns:
          str, path to the collected trace

        """
        host_output_dir = host_output_dir or self._device.log_path()
        trace_filename = self._generate_trace_filename(tag)
        host_output_path = os.path.join(host_output_dir, trace_filename)
        try:
            if not self.device_trace_output:
                raise PerfettoCollectorError(
                    self._device, '_pull_trace_file: device_trace_output is None.'
                )
            self._device.pull([self.device_trace_output, host_output_path])
        except adb.Error as e:
            raise PerfettoCollectorError(
                self._device, 'Failed to pull trace file.'
            ) from e
        self._last_trace_file = host_output_path
        if tag:
            self._tagged_trace_files[tag] = host_output_path
        return host_output_path

    def _cleanup_device_output(self):
        """Clean up the pushded config and trace file on the device."""
        # Clean up trace config.
        try:
            self._adb_shell('rm', self._device_trace_config_path)
        except adb.Error:
            # Ignore - we may not have ever pushed the trace config, if we are running
            # on a pre-Android 12 device.
            pass

        try:
            self._adb_shell('rm', self.device_trace_output)
        except adb.Error as e:
            raise PerfettoCollectorError(
                self._device,
                f'Failed to clean up trace file {self.device_trace_output}.',
            ) from e
        finally:
            self.device_trace_output = None

