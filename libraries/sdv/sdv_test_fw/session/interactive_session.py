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

"""Create detachable sessions to interact using pexpect"""

import logging
from mobly import asserts
import pexpect


class Session:
    """Represents an interactive session."""

    def __init__(self, adb_serial=None, session_label=None):
        """Initializes and starts the session.

        Args:
            adb_serial (str, optional): The serial number of the device to
              connect to. If not provided, the session will be started in a
              shell.
            session_label (str, optional): Identifier of the session to be used
              for logs with format [Session|adb_serial|session_label] <log>
        """
        self.adb_serial = adb_serial
        self._label = "|".join(
            [s for s in ["Session", adb_serial, session_label] if s]
        )

        self.session = self._create_session()

    def __del__(self):
        """Destructor to ensure session is closed."""
        self.close(force=True)

    def _create_session(self):
        """Creates the interactive session.

         Returns:
            pexpect.spawn: The pexpect session object.

        Raises:
            Exception: If there is an error creating the session.
        """
        logging.info(f"[{self._label}] Connect to session.")
        try:
            if self.adb_serial:
                return pexpect.spawn(f"adb -s {self.adb_serial} shell")
            else:
                return pexpect.spawn("/bin/bash")
        except pexpect.ExceptionPexpect as e:
            raise Exception(f"Error creating session: {e}")

    def reconnect(self):
        """Restarts the interactive session

        Pexpect sessions are tied to the lifecycle of a specific child process.
        To reconnect, the current process must be terminated and a new one
        spawned. This method closes the existing session and replaces it with
        a new instance.
        """
        logging.info(f"[{self._label}] Reconnect to session.")
        self.session.close(force=True)
        self.session = self._create_session()

    def send_command(self, command):
        """Sends a command to the session.

        Args:
            command (str): The command to be sent to the session.

        Returns:
            None: This method does not return any value. It sends the command
            and handles the interaction with the session.
        """
        logging.info(f"[{self._label}] Send command: {command}")
        self.session.sendline(command)

    def get_output(self):
        """Retrieve the output of the session.

        Returns:
            str: This method decodes the output from the session's buffer
            and returns it as a string.
        """
        return self.session.before.decode("utf-8")

    def close(self, force=False):
        """Closes the session

        Args:
            force (bool, optional): Force to close the session. False by
              default.
        """
        logging.info(f"[{self._label}] Close session.")
        self.session.close(force=force)

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
        If any pattern is not found within the specified timeout, the test
        fails.

        Args:
            outputs (list of str): A list of patterns to search for in the
              session output. Each pattern can be a regular expression or a
              simple string.
            timeout (int, optional): The timeout in seconds for each pattern
              search. Defaults to 30.

        Raises:
            AssertionError: If any pattern is not found in the output within the
            specified timeout.
        """
        for text in outputs:
            logging.info(f"[{self._label}] Waiting for output: {text}")
            try:
                self.session.expect(text, timeout=timeout)
            except pexpect.TIMEOUT:
                session_content = self.session.before.decode("utf-8")
                asserts.fail(
                    f"Expected output '{text}' was not found within {timeout}"
                    f" seconds.\nSession content:\n{session_content}"
                )
