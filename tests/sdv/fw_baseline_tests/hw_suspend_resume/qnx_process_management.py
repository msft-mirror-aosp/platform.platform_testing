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

_RUNNING_PROCESSES_CMD = "pidin -f aA | grep {process}"


def _query_processes_info(
    command_executor, process_identifier: str
) -> list[str]:
    output_lines = command_executor(
        _RUNNING_PROCESSES_CMD.format(process=process_identifier)
    )

    processes_info = [
        process_info
        for process_info in output_lines
        if process_info
        and "pidin" not in process_info
        and "grep" not in process_info
    ]

    logging.debug(f"Processes info {processes_info}")
    return processes_info


def _spawned_processes_pids(
    command_executor, process_identifier: str
) -> list[str]:
    processes_info = _query_processes_info(command_executor, process_identifier)
    pids = []
    for info in processes_info:
        pidin_output = info.split()
        # The pid is expected to be the first element
        # 000000 process_info
        if pidin_output and pidin_output[0].isdigit():
            pids.append(pidin_output[0])

    logging.debug(f"Spawned processes pids: {pids}")
    return pids


def processes_are_running(command_executor, process_identifier: str) -> bool:
    pids = _spawned_processes_pids(command_executor, process_identifier)
    return bool(pids)
