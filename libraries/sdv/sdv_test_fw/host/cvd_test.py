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

import unittest
from unittest import mock

from mobly import utils
from sdv_test_fw.host import cvd


class CvdProxyTest(unittest.TestCase):
    """Tests for CvdProxy."""

    def setUp(self):
        self.proxy = cvd.CvdProxy()

    @mock.patch.object(utils, 'run_command')
    def test_start_success(self, mock_run_command):
        """Tests successful execution for start command."""
        mock_run_command.return_value = (0, 'output', 'stderr')

        self.proxy.start('1')
        mock_run_command.assert_called_once_with(
            ['cvd', '--instance_name=1', 'start'], universal_newlines=True
        )

    @mock.patch.object(utils, 'run_command')
    def test_start_failure(self, mock_run_command):
        """Tests execution failure handling for start command."""
        mock_run_command.return_value = (1, 'output', 'error')

        with self.assertRaises(cvd.CvdError) as cm:
            self.proxy.start('2')

        mock_run_command.assert_called_once_with(
            ['cvd', '--instance_name=2', 'start'], universal_newlines=True
        )
        self.assertIn(
            "Command ['cvd', '--instance_name=2', 'start'] failed",
            str(cm.exception),
        )
        self.assertIn('return code 1', str(cm.exception))

    @mock.patch.object(utils, 'run_command')
    def test_stop_success(self, mock_run_command):
        """Tests successful execution of stop command."""
        mock_run_command.return_value = (0, 'output', 'stderr')

        self.proxy.stop('1')
        mock_run_command.assert_called_once_with(
            ['cvd', '--instance_name=1', 'stop'], universal_newlines=True
        )

    @mock.patch.object(utils, 'run_command')
    def test_restart_success(self, mock_run_command):
        """Tests successful execution of restart command."""
        mock_run_command.return_value = (0, 'output', 'stderr')

        self.proxy.restart('1')
        mock_run_command.assert_called_once_with(
            ['cvd', '--instance_name=1', 'restart'], universal_newlines=True
        )

    @mock.patch.object(utils, 'run_command')
    def test_powerwash_success(self, mock_run_command):
        """Tests successful execution of powerwash command."""
        mock_run_command.return_value = (0, 'output', 'stderr')

        self.proxy.powerwash('1')
        mock_run_command.assert_called_once_with(
            ['cvd', '--instance_name=1', 'powerwash'], universal_newlines=True
        )

    @mock.patch.object(utils, 'run_command')
    def test_powerbutton_returns_output(self, mock_run_command):
        """Tests successful execution of restart command."""
        mock_run_command.return_value = (0, 'output', 'stderr')

        self.proxy.powerbutton('1')
        mock_run_command.assert_called_once_with(
            ['cvd', '--instance_name=1', 'powerbtn'], universal_newlines=True
        )

    @mock.patch.object(utils, 'run_command')
    def test_status_returns_output(self, mock_run_command):
        """Tests that status returns the command output."""
        expected_output = 'status output'
        mock_run_command.return_value = (0, expected_output, '')

        actual_output = self.proxy.status('1')
        mock_run_command.assert_called_once_with(
            ['cvd', '--instance_name=1', 'status', '--print'],
            universal_newlines=True,
        )
        self.assertEqual(actual_output, expected_output)


if __name__ == '__main__':
    unittest.main()
