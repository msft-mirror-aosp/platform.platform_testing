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
from pexpect import spawn, TIMEOUT, ExceptionPexpect
from sdv_test_fw.session.interactive_session import Session

class TestSession(unittest.TestCase):

    def setUp(self):
        super().setUp()

    @mock.patch('pexpect.spawn', autospec=True)
    def test_create_session_with_adb_serial_success(self, mock_spawn):
        test_serial = "my_serial"
        mock_session = mock.MagicMock()
        mock_spawn.return_value = mock_session
        session = Session(adb_serial=test_serial)
        mock_spawn.assert_called_once_with(f"adb -s {test_serial} shell")
        self.assertEqual(session.session, mock_session)

    @mock.patch('pexpect.spawn', autospec=True)
    def test_create_session_without_adb_serial_success(self, mock_spawn):
          mock_spawn.return_value = mock.MagicMock()
          session = Session()
          mock_spawn.assert_called_once_with("/bin/bash")
          self.assertEqual(session.session, mock_spawn.return_value)

    @mock.patch('pexpect.spawn', autospec=True)
    def test_create_session_with_error_raises_exception(self, mock_spawn):
        mock_spawn.side_effect = ExceptionPexpect("Test Error")
        with self.assertRaisesRegex(Exception, "Error creating session: Test Error"):
            Session()

    @mock.patch('pexpect.spawn', autospec=True)
    def test_send_command_sends_command_to_session(self, mock_spawn):
        mock_session = mock.MagicMock()
        mock_spawn.return_value = mock_session
        session = Session()
        test_command = "ls -l"
        session.send_command(test_command)
        mock_session.sendline.assert_called_once_with(test_command)

    @mock.patch('pexpect.spawn', autospec=True)
    def test_get_output_returns_session_output(self, mock_spawn):
        mock_session = mock.MagicMock()
        mock_spawn.return_value = mock_session
        session = Session()
        mock_session.before = b"test_output"
        output = session.get_output()
        self.assertEqual(output, "test_output")

    @mock.patch('pexpect.spawn', autospec=True)
    def test_close_session_closes_session(self, mock_spawn):
        mock_session = mock.MagicMock()
        mock_spawn.return_value = mock_session
        session = Session()
        session.close()
        mock_session.close.assert_called_once()

    @mock.patch('pexpect.spawn', autospec=True)
    def test_send_command_and_wait_for_outputs_sends_command_and_waits(self, mock_spawn):
        mock_session = mock.MagicMock()
        mock_spawn.return_value = mock_session
        session = Session()
        test_command = "echo test"
        test_outputs = ["test"]
        timeout = 10
        session.send_command_and_wait_for_outputs(test_command, test_outputs, timeout)
        mock_session.sendline.assert_called_once_with(test_command)
        mock_session.expect.assert_called_once_with(test_outputs[0], timeout=timeout)

    @mock.patch('pexpect.spawn', autospec=True)
    def test_expect_outputs_success_when_all_outputs_found(self, mock_spawn):
        mock_session = mock.MagicMock()
        mock_spawn.return_value = mock_session
        session = Session()
        test_outputs = ["output1", "output2"]
        session.expect_outputs(test_outputs)
        mock_session.expect.assert_has_calls([
            mock.call("output1", timeout=30),
            mock.call("output2", timeout=30)
        ])

    @mock.patch('pexpect.spawn', autospec=True)
    def test_expect_outputs_empty_outputs_list_does_not_raise_error(self, mock_spawn):
          mock_session = mock.MagicMock()
          mock_spawn.return_value = mock_session
          session = Session()
          session.expect_outputs([])
          mock_session.expect.assert_not_called()

if __name__ == "__main__":
    unittest.main()