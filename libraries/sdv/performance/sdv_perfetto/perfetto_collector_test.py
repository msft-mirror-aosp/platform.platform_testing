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

import pathlib
import unittest
from unittest import mock

from mobly.controllers.android_device_lib import adb
from protos.perfetto.metrics import metrics_pb2
from sdv_perfetto import collector_config, perfetto_collector


class PerfettoCollectorTest(unittest.TestCase):

  def setUp(self):
    self.mock_device = mock.MagicMock()
    self.collector = perfetto_collector.PerfettoCollector(
        device=self.mock_device
    )

  def test_trace_running_true(self):
    self.collector._last_perfetto_pid = 123
    self.mock_device.execute_shell_command.return_value = 'perfetto'
    self.assertTrue(self.collector.trace_running())

  def test_trace_running_false(self):
    self.mock_device.execute_shell_command.return_value = 'other_process'
    self.assertFalse(self.collector.trace_running())

  def test_trace_running_adb_error(self):
    self.mock_device.execute_shell_command.side_effect = ''
    self.assertFalse(self.collector.trace_running())

  def test_trace_running_with_read_from_running_process_true(self):
    self.mock_device.execute_shell_command.side_effect = [
        (
            'perfetto           239     1   10832272   4568 0              '
            '     0 S perfetto'
        ),
        'perfetto',
    ]
    self.collector._last_perfetto_pid = None
    self.assertTrue(
        self.collector.trace_running(read_from_running_process=True)
    )

  def test_kill_perfetto(self):
    self.collector._last_perfetto_pid = 123
    self.mock_device.execute_shell_command.return_value = ''
    self.collector._kill_perfetto()
    self.mock_device.execute_shell_command.assert_has_calls(
        [mock.call(['kill', '-INT', '123'])]
    )
    self.assertIsNone(self.collector._last_perfetto_pid)

  def test_kill_perfetto_failed(self):
    self.collector._last_perfetto_pid = 123
    self.mock_device.execute_shell_command.return_value = 'perfetto'
    with self.assertRaises(perfetto_collector.PerfettoCollectorError):
      self.collector._kill_perfetto()

  def _args_based_generate_filename(self, file_type, extension_name):
    return '{}.{}'.format(file_type, extension_name)

  def test_generate_trace_filename(self):
    self.mock_device.generate_filename.side_effect = (
        self._args_based_generate_filename
    )
    self.assertEqual(
        self.collector._generate_trace_filename(), 'perfetto.perfetto-trace'
    )

  def test_generate_trace_filename_with_tag(self):
    self.mock_device.generate_filename.side_effect = (
        self._args_based_generate_filename
    )
    self.assertEqual(
        self.collector._generate_trace_filename('tag'),
        'perfetto_tag.perfetto-trace',
    )

  def _mock_start_trace(self):
    self.mock_device.execute_shell_command.side_effect = [
        'perfetto',
        '1234',
    ]
    self.mock_device.push.return_value = 'push_config'
    self.collector.start_trace()

  def test_start_trace(self):
    self._mock_start_trace()
    self.mock_device.execute_shell_command.assert_has_calls([
        mock.call(['setprop', 'persist.traced.enable', '1']),
    ])

  def test_start_trace_failed(self):
    self.mock_device.execute_shell_command.side_effect = [
        'perfetto',
        adb.Error,
    ]
    with self.assertRaises(perfetto_collector.PerfettoCollectorError):
      self.collector.start_trace()

  @mock.patch('mobly.utils.run_command')
  @mock.patch('mobly.utils.start_standing_subprocess')
  def test_start_trace_with_torq(
      self, mock_start_standing_subprocess, mock_run_command
  ):
    self.mock_device.get_device_serial.return_value = 'serial1'
    self.mock_device.log_path.return_value = 'log_path'
    mock_secondary_device = mock.MagicMock()
    collector = perfetto_collector.PerfettoCollector(
        device=self.mock_device,
        config=collector_config.CollectorConfig(
            multi_vm_tracing=True,
            multi_vm_tracing_vsock=True,
            secondary_devices=[mock_secondary_device],
        ),
    )
    collector.start_trace()
    mock_start_standing_subprocess.assert_called_once()
    mock_start_standing_subprocess.assert_has_calls([
        mock.call([
            collector._torq_binary_path,
            '--serial',
            'serial1',
            '--perfetto-config',
            collector._config.config_path,
            '--no-ui',
            '--out-dir',
            'log_path',
        ])
    ])

  @mock.patch('mobly.utils.run_command')
  def test_set_multi_vm_tracing_vsock(self, mock_run_command):
    self.mock_device.get_device_serial.return_value = 'serial1'
    self.mock_device.get_vsock_local_cid.return_value = '3'
    mock_secondary_device = mock.MagicMock()
    mock_secondary_device.get_device_serial.return_value = 'serial2'
    collector = perfetto_collector.PerfettoCollector(
        device=self.mock_device,
        config=collector_config.CollectorConfig(
            multi_vm_tracing=True,
            multi_vm_tracing_vsock=True,
            secondary_devices=[mock_secondary_device],
        ),
    )
    mock_run_command.assert_called_once()
    self.assertIn('--primary-cid', mock_run_command.call_args[0][0])
    self.assertIn('3', mock_run_command.call_args[0][0])
    self.assertIn('--secondary', mock_run_command.call_args[0][0])
    self.assertIn('serial2', mock_run_command.call_args[0][0])

  @mock.patch('mobly.utils.run_command')
  def test_set_multi_vm_tracing_inet(self, mock_run_command):
    self.mock_device.get_device_serial.return_value = 'serial1'
    self.mock_device.prop.get.return_value = 'x86_64'
    self.mock_device.execute_shell_command.return_value = (
        'inet addr:192.168.98.3'
    )
    mock_secondary_device = mock.MagicMock()
    mock_secondary_device.get_device_serial.return_value = 'serial2'
    collector = perfetto_collector.PerfettoCollector(
        device=self.mock_device,
        config=collector_config.CollectorConfig(
            multi_vm_tracing=True,
            multi_vm_tracing_vsock=False,
            secondary_devices=[mock_secondary_device],
        ),
    )
    mock_run_command.assert_called_once()
    self.assertIn('--primary-ip', mock_run_command.call_args[0][0])
    self.assertIn('192.168.98.3', mock_run_command.call_args[0][0])
    self.assertIn('--secondary', mock_run_command.call_args[0][0])
    self.assertIn('serial2', mock_run_command.call_args[0][0])

  def _mock_stop_trace(self, tag=None):
    self._mock_start_trace()
    self.mock_device.execute_shell_command.side_effect = [
        'perfetto',  # check if last_pid is perfetto
        'check_traceing_command_after_kill',
        'no_trace_running_after_kill',
        'rm_config',
        'rm_trace',
    ]
    self.mock_device.pull.return_value = 'pull_trace'
    self.collector.stop_trace(tag)

  def test_pull_trace_file_error(self):
    with self.assertRaises(perfetto_collector.PerfettoCollectorError):
      # stop_trace is being called without calling start_trace() first
      # therefore, there are no traces to pull and an exception should be raised
      self.collector.stop_trace()

  def test_stop_trace_with_perfetto(self):
    self._mock_stop_trace()
    self.mock_device.execute_shell_command.assert_has_calls(
        [
            mock.call(['setprop', 'persist.traced.enable', '1']),
            mock.call(['kill', '-INT', '1234']),
            mock.call(['rm', '/data/misc/perfetto-configs/trace_config']),
        ],
        any_order=True,
    )
    self.mock_device.pull.assert_called()

  @mock.patch('pathlib.Path.is_file')
  @mock.patch('pathlib.Path.rglob')
  @mock.patch('mobly.utils.run_command')
  @mock.patch('subprocess.Popen')
  def test_stop_trace_with_torq(
      self,
      mock_popen,
      mock_run_command,
      mock_rglob,
      mock_is_file,
  ):
    mock_is_file.return_value = True
    mock_rglob.return_value = [
        pathlib.Path('/data/misc/perfetto-traces/trace.perfetto-trace')
    ]
    mock_secondary_device = mock.MagicMock()
    collector = perfetto_collector.PerfettoCollector(
        device=self.mock_device,
        config=collector_config.CollectorConfig(
            multi_vm_tracing=True,
            multi_vm_tracing_vsock=True,
            secondary_devices=[mock_secondary_device],
        ),
    )
    collector._torq_proc = mock_popen.return_value
    collector.stop_trace()
    collector._torq_proc.terminate.assert_called_once()

  def test_stop_trace_failed(self):
    self._mock_start_trace()
    self.mock_device.execute_shell_command.side_effect = [
        'perfetto',  # check if last_pid is perfetto
        'check_traceing_command_after_kill',
        'no_trace_running_after_kill',
        'rm_config',
        'rm_trace',
    ]
    self.mock_device.pull.side_effect = adb.Error
    with self.assertRaises(perfetto_collector.PerfettoCollectorError):
      self.collector.stop_trace()

  def test_stop_trace_with_tag(self):
    self._mock_stop_trace('tag')
    self.mock_device.execute_shell_command.assert_has_calls(
        [
            mock.call(['setprop', 'persist.traced.enable', '1']),
            mock.call(['kill', '-INT', '1234']),
            mock.call(['rm', '/data/misc/perfetto-configs/trace_config']),
        ],
        any_order=True,
    )
    self.mock_device.pull.assert_called()

  def test_get_trace_file(self):
    self.collector._last_trace_file = 'path/to/trace.perfetto-trace'
    self.assertEqual(
        self.collector.get_trace_file(), 'path/to/trace.perfetto-trace'
    )

  def test_get_trace_file_with_tag(self):
    self.collector._tagged_trace_files['tag'] = (
        'path/to/trace_tag.perfetto-trace'
    )
    self.assertEqual(
        self.collector.get_trace_file('tag'),
        'path/to/trace_tag.perfetto-trace',
    )

  def test_get_pids_from_ps(self):
    self.mock_device.execute_shell_command.return_value = (
        'perfetto           239     1   10832272   4568 0                  '
        ' 0 S perfetto\n'
        + 'perfetto           761     1   10836456   4824 0                '
        '   0 S perfetto'
    )
    self.assertEqual(self.collector.get_pids_from_ps('perfetto'), [239, 761])

  def test_update_last_perfetto_pid_from_running_process(self):
    self.mock_device.execute_shell_command.return_value = (
        'perfetto           239     1   10832272   4568 0                  '
        ' 0 S perfetto\n'
        + 'perfetto           761     1   10836456   4824 0                '
        '   0 S perfetto'
    )
    self.collector.update_last_perfetto_pid_from_running_process()
    self.assertEqual(self.collector._last_perfetto_pid, 761)

  def test_get_inet_address(self):
    self.mock_device.prop.get.return_value = 'x86_64'
    self.mock_device.execute_shell_command.return_value = """
          inet addr:192.168.98.3  Bcast:192.168.98.255  Mask:255.255.255.0
          """
    self.assertEqual(self.collector.get_inet_address(), '192.168.98.3')

  def test_get_vsock_id(self):
    self.mock_device.get_vsock_local_cid.return_value = 3
    self.assertEqual(self.collector.get_vsock_id(), 3)

  def test_get_inet_interface_arm64(self):
    self.mock_device.prop.get.return_value = 'arm64-v8a'
    self.assertEqual(self.collector.get_inet_interface(), 'eth0')

  def test_get_inet_interface_x86_64(self):
    self.mock_device.prop.get.return_value = 'x86_64'
    self.assertEqual(self.collector.get_inet_interface(), 'eth1')

  def test_get_inet_interface_failed(self):
    self.mock_device.prop.get.return_value = 'arm64'
    with self.assertRaises(perfetto_collector.PerfettoCollectorError):
      self.collector.get_inet_interface()


if __name__ == '__main__':
  unittest.main()
