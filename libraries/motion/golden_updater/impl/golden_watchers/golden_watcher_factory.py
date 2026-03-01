# Copyright 2025, The Android Open Source Project
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
#

import os
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.golden_watchers.atest_golden_watcher import AtestGoldenWatcher
from impl.golden_watchers.presubmit_golden_watcher import PresubmitGoldenWatcher
from impl.core.context import MotionWatcherContext
from impl.golden_watchers.golden_file_watcher import GoldenFileWatcher

class GoldenWatcherFactory:

    @staticmethod
    def create_watcher(type: GoldenWatcherTypes, context: MotionWatcherContext, adb_client = None):

        match type:
            case GoldenWatcherTypes.ATEST:
                user = os.environ.get("USER")
                return AtestGoldenWatcher(
                    os.path.join(context.temp_dir, type.value), f"/tmp/atest_result_{user}/LATEST/"
                )

            case GoldenWatcherTypes.PRESUBMIT:
                tmpdir = os.path.join(context.temp_dir, type.value)
                return PresubmitGoldenWatcher(
                    tmpdir, os.path.join(tmpdir,"artifacts_download_dir")
                )

            case GoldenWatcherTypes.ADB:
                if not adb_client:
                    raise ValueError("adb client not found")

                return GoldenFileWatcher(os.path.join(context.temp_dir, type.value), adb_client)


            case _:
                print("No such Golden Watcher exists.")
                raise ValueError("Imporper Golden Watcher Type.")



