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
            collector_config.CollectorConfig(multi_vm_tracing=True, multi_vm_tracing_vsock=True)
            collector_config.CollectorConfig(multi_vm_tracing=True, multi_vm_tracing_center=True)
            collector_config.CollectorConfig(multi_vm_tracing=True, multi_vm_tracing_center=False)
            collector_config.CollectorConfig(multi_vm_tracing=True, multi_vm_tracing_center=False, multi_vm_tracing_vsock=True, multi_vm_tracing_center_address=TEST_INET_ADDRESS)
            collector_config.CollectorConfig(multi_vm_tracing=True, multi_vm_tracing_center=False, multi_vm_tracing_vsock=False, multi_vm_tracing_center_address=TEST_VSOCK_ADDRESS)

    def test_multi_vm_tracing_config_center_vm_success(self):
        config = collector_config.CollectorConfig(multi_vm_tracing=True, multi_vm_tracing_vsock=True, multi_vm_tracing_center=True)
        self.assertTrue(config.multi_vm_tracing)
        self.assertTrue(config.multi_vm_tracing_vsock)
        self.assertTrue(config.multi_vm_tracing_center)

    def test_multi_vm_tracing_config_client_vm_vsock_success(self):
        config = collector_config.CollectorConfig(
                multi_vm_tracing=True,
                multi_vm_tracing_center=False,
                multi_vm_tracing_vsock=True,
                multi_vm_tracing_center_address=TEST_VSOCK_ADDRESS)

        self.assertEqual(config.multi_vm_tracing_center_address, TEST_VSOCK_ADDRESS)

    def test_multi_vm_tracing_config_client_vm_inet_success(self):
        config = collector_config.CollectorConfig(
                multi_vm_tracing=True,
                multi_vm_tracing_center=False,
                multi_vm_tracing_vsock=False,
                multi_vm_tracing_center_address=TEST_INET_ADDRESS)

        self.assertEqual(config.multi_vm_tracing_center_address, TEST_INET_ADDRESS)

if __name__ == '__main__':
    unittest.main()
