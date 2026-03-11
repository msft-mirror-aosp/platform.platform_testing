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

import glob
import re
import os
import gzip
import shutil
import datetime
from impl.models.cached_golden import CachedGolden
from impl.golden_watchers.golden_watcher import GoldenWatcher
import itertools


class AtestGoldenWatcher(GoldenWatcher):

    def __init__(self, temp_dir, atest_latest_dir, cached_golden_service=CachedGolden):
        self.temp_dir = temp_dir
        self.atest_latest_dir = atest_latest_dir
        self.cached_golden_service = cached_golden_service
        # name -> CachedGolden
        self.cached_goldens = {}
        # Ensure temp_dir exists
        os.makedirs(self.temp_dir, exist_ok=True)
        self.refresh_golden_files()

    def clean(self):
        # Deletion is disabled for Atest mode to prevent unintended data loss
        # and because results persist in /tmp.
        pass

    def refresh_golden_files(self):

        pattern_type = (
            r".*/(?P<name>.*)\.actual((\.(?P<ext1>[a-zA-Z0-9]+)_(?P<hash1>\d+)\.txt)|(_(?P<hash2>\d+)\.(?P<ext2>[a-zA-Z0-9]+)))(?P<compressed>\.gz)?"
        )

        # Output from on-device runs
        for filename in glob.iglob(
            f"{self.atest_latest_dir}//**/*.actual*json*", recursive=True
        ):

            match = re.search(pattern_type, filename)

            if not match:
                continue

            golden_name = match.group("name")
            ext = match.group("ext1") or match.group("ext2")
            hash = match.group("hash1") or match.group("hash2")
            is_compressed = match.group("compressed") == ".gz"

            file_time = datetime.datetime.fromtimestamp(os.path.getmtime(filename)).isoformat()

            local_file = os.path.join(self.temp_dir, f"{golden_name}_{hash}.actual.json")
            self.copy_file(filename, local_file, is_compressed)
            golden = self.cached_golden_service(filename, local_file, test_time=file_time)

            if golden.video_location:
                mp4Pattern = f"{self.atest_latest_dir}/**/{golden_name}.actual*.mp4*"
                zipPattern = f"{self.atest_latest_dir}/**/{golden_name}.actual*.zip*"

                mp4_iterator = glob.iglob(mp4Pattern, recursive=True)
                zip_iterator = glob.iglob(zipPattern, recursive=True)

                combined_iter = itertools.chain(mp4_iterator, zip_iterator)
                for video_filename in combined_iter:

                    local_video_file = os.path.join(
                        self.temp_dir, golden.video_location
                    )
                    video_is_compressed = video_filename.endswith(".gz")
                    self.copy_file(
                        video_filename, local_video_file, video_is_compressed
                    )

                    break

            self.cached_goldens[filename] = golden

    def copy_file(self, source, target, is_compressed):
        os.makedirs(os.path.dirname(target), exist_ok=True)

        if is_compressed:
            with gzip.open(source, "rb") as f_in:
                with open(target, "wb") as f_out:
                    shutil.copyfileobj(f_in, f_out)
        else:
            shutil.copyfile(source, target)