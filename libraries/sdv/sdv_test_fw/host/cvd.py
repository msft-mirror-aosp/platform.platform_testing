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

import logging
from mobly import utils
from sdv_test_fw.host import cvd_common


class CvdError(Exception):
    """Exception raised for errors in CVD operations."""


class CvdProxy:
    """Proxy for executing local cvd commands."""

    def execute_command(
        self,
        action: cvd_common.CvdAction,
        instance_id: str,
        extra_args: list[str] = None,
    ) -> str:
        """Executes a cvd command for a specific instance.

        Args:
            action: The cvd action to run.
            instance_id: The target instance identifier (e.g., '1').
            extra_args: Additional arguments to append to the command.

        Returns:
            The standard output of the command.

        Raises:
            CvdError: If the command fails.
        """
        cmd = ['cvd', f'--instance_name={instance_id}', action.value]
        if extra_args:
            cmd.extend(extra_args)

        logging.debug('Executing CVD command: %s', ' '.join(cmd))

        ret, out, err = utils.run_command(cmd, universal_newlines=True)

        if ret != 0:
            raise CvdError(
                f'Command {cmd} failed with return code {ret}. Output: {out},'
                f' Error: {err}'
            )
        return out

    def start(self, instance_id: str) -> None:
        """Starts a stopped CVD.

        Args:
            instance_id: The target instance identifier.
        """
        self.execute_command(cvd_common.CvdAction.START, instance_id)

    def stop(self, instance_id: str) -> None:
        """Stops a running CVD.

        Args:
            instance_id: The target instance identifier.
        """
        self.execute_command(cvd_common.CvdAction.STOP, instance_id)

    def restart(self, instance_id: str) -> None:
        """Restarts a running CVD.

        Args:
            instance_id: The target instance identifier.
        """
        self.execute_command(cvd_common.CvdAction.RESTART, instance_id)

    def powerwash(self, instance_id: str) -> None:
        """Powerwashes a CVD.

        Args:
            instance_id: The target instance identifier.
        """
        self.execute_command(cvd_common.CvdAction.POWERWASH, instance_id)

    def powerbutton(self, instance_id: str) -> None:
        """Presses the power button on a CVD.

        Args:
            instance_id: The target instance identifier.
        """
        self.execute_command(cvd_common.CvdAction.POWER_BUTTON, instance_id)

    def status(self, instance_id: str) -> str:
        """Checks status of a CVD.

        Args:
            instance_id: The target instance identifier.

        Returns:
            The status output.
            Example output for a running instance:
            [
                    {
                            "adb_port" : 6521,
                            "adb_serial" : "0.0.0.0:6521",
                            "assembly_dir" : "assembly_dir",
                            "instance_dir" : "instance_dir",
                            "instance_name" : "1",
                            "status" : "Running",
                            "web_access" : "web_access",
                            "webrtc_device_id" : "cvd_1"
                    }
            ]
        """
        return self.execute_command(
            cvd_common.CvdAction.STATUS, instance_id, ['--print']
        )
