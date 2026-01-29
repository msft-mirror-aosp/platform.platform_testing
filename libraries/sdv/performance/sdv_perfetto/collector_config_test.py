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
import unittest
from unittest import mock
from sdv_perfetto import collector_config

TEST_INET_ADDRESS = '192.168.1.1'
TEST_VSOCK_ADDRESS = '12'

class CollectorConfigTest(unittest.TestCase):

    @mock.patch('os.path.isfile')
    @mock.patch('os.path.exists')
    def test_config_file_exists(self, mock_exists, mock_isfile):
        mock_isfile.return_value = True
        mock_exists.return_value = True
        self.assertEqual(
            collector_config.config_file_exists(
                'path/to/perfetto_config.pbtx'
            ),
            'path/to/perfetto_config.pbtx',
        )

    def test_config_file_exists_does_not_exist(self):
        with self.assertRaises(ValueError):
            collector_config.config_file_exists(
                'path/to/perfetto_config.pbtx'
            )

    def test_get_config_path_from_data_default(self):
        self.assertIn(
            'sdv_perfetto/config/default_trace_cfg.pbtx',
            collector_config.get_config_path_from_data(),

        )

    @mock.patch('os.path.isfile')
    @mock.patch('os.path.exists')
    def test_get_config_path_from_data_custom(self, mock_exists, mock_isfile):
        mock_isfile.return_value = True
        mock_exists.return_value = True
        self.assertIn(
            'sdv_perfetto/config/custom_trace_cfg.pbtx',
            collector_config.get_config_path_from_data('custom_trace_cfg.pbtx'),

        )

    @mock.patch('os.path.isfile')
    @mock.patch('os.path.exists')
    def test_build_config_path_custom(self, mock_exists, mock_isfile):
        mock_isfile.return_value = True
        mock_exists.return_value = True
        self.assertEqual(
            collector_config.build_config_path('path/to/custom_config.pbtx'),
            'path/to/custom_config.pbtx',
        )
    @mock.patch('os.path.exists')
    def test_build_config_path_custom_not_exists(self, mock_exists):
        mock_exists.return_value = False
        with self.assertRaises(ValueError):
            collector_config.build_config_path('path/to/custom_config.pbtx')

    def _args_based_exists(self, path):
        return 'config' in path

    @mock.patch('os.path.exists')
    @mock.patch('os.path.isfile')
    def test_build_config_path_from_data_by_name(self, mock_exists, mock_isfile):
        mock_exists.side_effect = self._args_based_exists
        mock_isfile.return_value = True
        config = collector_config.CollectorConfig(
            config_path= 'custom_trace_cfg.pbtx'
        )
        self.assertIn(
            'sdv_perfetto/config/custom_trace_cfg.pbtx',
            config.config_path
        )

    def test_collector_config_default(self):
        config = collector_config.CollectorConfig()
        self.assertIn(
            'sdv_perfetto/config/default_trace_cfg.pbtx',
            config.config_path
        )
        self.assertTrue(config.config_txt)
        self.assertFalse(config.background_wait)

    def test_multi_vm_tracing_config_errors(self):
        with self.assertRaises(ValueError):
            collector_config.CollectorConfig(multi_vm_tracing=True)
        with self.assertRaises(ValueError):
            collector_config.CollectorConfig(
                multi_vm_tracing=True, multi_vm_tracing_vsock=True
            )
        with self.assertRaises(ValueError):
            collector_config.CollectorConfig(
                multi_vm_tracing=True, secondary_devices=[]
            )

    def test_multi_vm_tracing_config_success(self):
        mock_device = mock.MagicMock()
        config = collector_config.CollectorConfig(
            multi_vm_tracing=True,
            multi_vm_tracing_vsock=True,
            secondary_devices=[mock_device],
        )
        self.assertTrue(config.multi_vm_tracing)
        self.assertTrue(config.multi_vm_tracing_vsock)
        self.assertEqual(config.secondary_devices, [mock_device])

    def test_check_vsock_id(self):
        self.assertIsNone(collector_config.check_vsock_id(TEST_VSOCK_ADDRESS))
        with self.assertRaises(ValueError):
            collector_config.check_vsock_id('12a')

    def test_check_inet_address(self):
        self.assertIsNone(
            collector_config.check_inet_address(TEST_INET_ADDRESS)
        )
        with self.assertRaises(ValueError):
            collector_config.check_inet_address('192.168.1.1a')

    def test_check_string_format(self):
        self.assertIsNone(
            collector_config.check_string_format('abc', r'[a-z]+')
        )
        with self.assertRaises(ValueError):
            collector_config.check_string_format('123', r'[a-z]+')

if __name__ == '__main__':
    unittest.main()
