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

import re
import shlex
from typing import List, Optional


def shlex_join(args: List[str]) -> str:
    """Replicates shlex.join from Python, which is only available from Python 3.8."""
    return ' '.join([shlex.quote(arg) for arg in args])


def get_report_file_pattern(
    config_uuid: Optional[str] = None,
    report_name: Optional[str] = None,
    report_number: Optional[int] = None,
):
    """Returns a regex pattern for matching report files."""
    uuid = '.*' if config_uuid is None else re.escape(config_uuid)
    name = '.*' if report_name is None else re.escape(report_name)
    num = '.*' if report_number is None else re.escape(str(report_number))

    return f'^{uuid}_{name}_{num}_.*\\.pb$'
