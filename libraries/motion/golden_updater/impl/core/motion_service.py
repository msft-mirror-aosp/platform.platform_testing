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

import json
import os
from os import path
import mimetypes
import pathlib
import shutil
import requests
from impl.core.context import MotionWatcherContext
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.utils.adb_serial_finder import ADBSerialFinder
from impl.utils.zip_to_video_converter import ZipToVideoConverter
from impl.downloaders.gerrit_downloader import GerritDownloader
from impl.golden_watchers.golden_watcher_factory import GoldenWatcherFactory
from impl.downloaders.fetch_presubmit_test_artifact import FetchPresubmitTestArtifacts
from impl.models.test_entity import TestEntity

class MotionService:
    def __init__(self, context: MotionWatcherContext):
        self.context = context
        self.adb_serial_finder = ADBSerialFinder()

    def get_available_modes(self):
        '''
        Collects all adb devices available and send them along with modes like
        robolectric and atest as available test mode options.
        '''
        available_modes = []
        self.adb_serial_finder.update_model_serial_map()
        if self.adb_serial_finder.model_serial_map:
            available_modes = list(self.adb_serial_finder.model_serial_map.keys())
        available_modes.append(GoldenWatcherTypes.ATEST.value)
        available_modes.append(GoldenWatcherTypes.ROBOLECTRIC.value)

        print(f"available modes: {available_modes}")
        return available_modes

    def resolve_file_path(self, root_directory, file_relative_to_root):
        resolved_path = path.abspath(path.join(root_directory, file_relative_to_root))
        if path.commonprefix([resolved_path, root_directory]) == root_directory and path.isfile(resolved_path):
            if resolved_path.endswith("screenshots.zip"):
                if ZipToVideoConverter.process_single_zip(pathlib.Path(resolved_path)):
                    video_path = resolved_path.replace(".zip", ".mp4")
                    if path.isfile(video_path):
                        return video_path, "video/mp4"
                return None, "Zip to Video converter failed"
            
            mime_type = mimetypes.guess_type(resolved_path)[0]
            return resolved_path, mime_type
        
        return None, f"File not found: {resolved_path}"

    def update_goldens(self, update_golden_id_set, cached_goldens):
        '''
        Find goldens with IDs in update_golden_id_set and updates expected values.
        '''
        results = []
        passed_count = 0
        failed_count = 0

        if len(update_golden_id_set) == 0:
            return results, passed_count, failed_count

        for golden in cached_goldens:
            if golden.id not in update_golden_id_set:
                continue

            result_item = {"id": golden.id}
            try:
                dst = path.join(self.context.android_build_top,
                                golden.golden_repo_path)
                if not path.exists(path.dirname(dst)):
                    pathlib.Path(path.dirname(dst)).mkdir(parents=True, exist_ok=True)

                shutil.copyfile(golden.local_file, dst)

                golden.updated = True
                result_item["status"] = "PASSED_UPDATE"
                result_item["message"] = "Updated"
                passed_count += 1
            except Exception as e:
                result_item["status"] = "FAILED_UPDATE"
                result_item["message"] = f"Failed with exception: {e}"
                failed_count += 1

            results.append(result_item)

        return results, passed_count, failed_count

    def fetch_gerrit_artifacts(self, linkPairs):
        gerrit_downloader = GerritDownloader()
        golden_list = gerrit_downloader.downloadMultipleJsons(linkPairs)
        return golden_list

    def service_presubmit_artifact_list(self, invocation_id):
        golden_watcher = GoldenWatcherFactory.create_watcher(
            GoldenWatcherTypes.PRESUBMIT, self.context
        )
        presubmit_fetch_client = FetchPresubmitTestArtifacts(
            invocation_id, golden_watcher.artifacts_download_dir
        )
        test_artifact_list = presubmit_fetch_client.list_presubmit_test_artifacts()
        return golden_watcher, presubmit_fetch_client, test_artifact_list

    def service_fetch_artifacts(self, test_entity, test_name):
        artifacts = test_entity.download_client.download_presubmit_test_artifact_for_test_name(test_name)
        if artifacts:
            test_entity.golden_watcher.refresh_golden_files(artifacts, test_name)
        
        for golden in test_entity.golden_watcher.cached_goldens.values():
            if golden.golden_name == test_name:
                return golden, None
        
        return None, "Golden not found for test name: " + test_name

    def refresh_goldens(self, test_entity, clear):
        if not test_entity.golden_watcher:
            return None, "No test mode selected !"

        if clear:
            test_entity.golden_watcher.clean()
        test_entity.golden_watcher.refresh_golden_files()
        return True, None
