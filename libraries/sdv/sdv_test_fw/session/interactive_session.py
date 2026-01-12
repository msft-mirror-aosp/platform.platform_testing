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

"""
Create detachable sessions to interact using pexpect
"""
import pexpect
import logging
from mobly import asserts

class Session:
    """Represents an interactive session."""

    def __init__(self, adb_serial=None):
        """Initializes and starts the session.

        Args:
            adb_serial (str, optional): The serial number of the device to connect to.
                If not provided, the session will be started in a shell.
        """
        self.session = self._create_session(adb_serial)

    def __del__(self):
        """Destructor to ensure session is closed."""
        self.close()

    def _create_session(self, adb_serial=None):
        """Creates the interactive session.

        Args:
            adb_serial (str, optional): The serial number of the device to connect to.
            If this parameter is not provided, the ADB connection defaults to the shell.

         Returns:
            pexpect.spawn: The pexpect session object.

        Raises:
            Exception: If there is an error creating the session.
        """
        try:
            if adb_serial:
                return pexpect.spawn(f'adb -s {adb_serial} shell')
            else:
                return pexpect.spawn('/bin/bash')
        except pexpect.ExceptionPexpect as e:
            raise Exception(f"Error creating session: {e}")

    def send_command(self, command):
        """Sends a command to the session.

        Args:
            command (str): The command to be sent to the session.

        Returns:
            None: This method does not return any value. It sends the command and
            handles the interaction with the session.
        """
        self.session.sendline(command)

    def get_output(self):
        """Retrieve the output of the session.

        Returns:
            str: This method decodes the output from the session's buffer
            and returns it as a string.
        """
        return self.session.before.decode('utf-8')

    def close(self):
        """Closes the session."""
        self.session.close()

    def send_command_and_wait_for_outputs(self, command, outputs, timeout=30):
        """Sends a command and waits for expected output.

        Args:
            command (str): The command to send.
            outputs (list): A list of strings to wait for in the output.
            timeout (int): The timeout in seconds to wait for the output.
        """
        self.send_command(command)
        self.expect_outputs(outputs, timeout)

    def expect_outputs(self, outputs, timeout=30):
        """Waits for all expected outputs in the session.

        This method searches for a list of patterns in the session's output.
        If any pattern is not found within the specified timeout, the test fails.

        Args:
            outputs (list of str): A list of patterns to search for in the session output.
                Each pattern can be a regular expression or a simple string.
            timeout (int, optional): The timeout in seconds for each pattern search. Defaults to 30.

        Raises:
            AssertionError: If any pattern is not found in the output within the specified timeout.
        """
        for text in outputs:
            try:
                self.session.expect(text, timeout=timeout)
            except pexpect.TIMEOUT:
                session_content = self.session.before.decode('utf-8')
                asserts.fail(f'Text "{text}" was not found in the output within {timeout} seconds.\nSession content:\n{session_content}')

