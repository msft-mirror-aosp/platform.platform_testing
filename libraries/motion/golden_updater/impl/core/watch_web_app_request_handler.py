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

import http.server
import urllib.parse
import json
import mimetypes
import shutil
from os import path
import pathlib
import os
import requests
from impl.models.motion_constants import GOLDEN_ACCESS_TOKEN_HEADER
from impl.utils.zip_to_video_converter import ZipToVideoConverter
from impl.downloaders.gerrit_downloader import GerritDownloader
from impl.downloaders.fetch_presubmit_test_artifact import FetchPresubmitTestArtifacts
from impl.golden_watchers.golden_watcher_factory import GoldenWatcherFactory
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.utils.adb_client import AdbClient
from impl.models.test_entity import TestEntity

class WatchWebAppRequestHandler(http.server.BaseHTTPRequestHandler):
    context = None
    service = None
    test_entity: TestEntity = None
    test_entity_cache = {}

    def __init__(self, *args, **kwargs):
        self.root_directory = path.abspath(path.dirname(__file__))
        super().__init__(*args, **kwargs)

    def verify_access_token(self):
        token = self.headers.get(GOLDEN_ACCESS_TOKEN_HEADER)
        if not token or token != WatchWebAppRequestHandler.context.secret_token:
            self.send_response(403, "Bad authorization token!")
            return False

        return True

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Allow", "GET,POST,PUT")
        self.add_standard_headers()
        self.end_headers()
        self.wfile.write(b"GET,POST,PUT")

    def do_GET(self):

        parsed = urllib.parse.urlparse(self.path)

        if parsed.path == "/service/config/modes":
            self.get_available_modes()
            return
        elif parsed.path.startswith("/golden/"):
            requested_file_start_index = parsed.path.find("/", len("/golden/") + 1)
            requested_file = parsed.path[requested_file_start_index + 1 :]
            self.serve_file(
                WatchWebAppRequestHandler.test_entity.golden_watcher.temp_dir,
                requested_file
            )
            return
        elif parsed.path.startswith("/expected/"):
            golden_id = parsed.path[len("/expected/") :]

            goldens = WatchWebAppRequestHandler.test_entity.golden_watcher.cached_goldens.values()
            for golden in goldens:
                if golden.id != golden_id:
                    continue

                self.serve_file(
                    WatchWebAppRequestHandler.context.android_build_top,
                    golden.golden_repo_path, "application/json"
                )
                return

        self.send_error_with_message(404, message=f"Invalid GET API: {parsed.path}")

    def do_POST(self):
        if not self.verify_access_token():
            return

        content_type = self.headers.get("Content-Type")

        # Refuse to receive non-json content
        if content_type != "application/json":
            self.send_response(400)
            return

        length = int(self.headers.get("Content-Length"))
        message = json.loads(self.rfile.read(length))

        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/service/goldens/refresh":
            self.service_refresh_goldens(message["clear"])
        elif parsed.path == '/service/presubmit/tests':
            self.service_presubmit_artifact_list(message["invocation_id"])
        elif parsed.path == '/service/presubmit/artifact':
            self.service_fetch_artifacts(message["resource_id"])
        elif parsed.path == "/service/config/mode":
            self.switch_mode(message["mode"])
        elif parsed.path == "/service/gerrit/goldens":
            self.fetch_gerrit_artifacts(message["linkPairs"])
        else:
            self.send_error_with_message(404, message=f"Invalid POST API: {parsed.path}")

    def do_PUT(self):
        if not self.verify_access_token():
            return

        parsed = urllib.parse.urlparse(self.path)

        if parsed.path == "/service/goldens/update":
            query_params = urllib.parse.parse_qs(parsed.query)
            golden_id = query_params["id"][0]
            results, _, _ = self.service_update_golden({golden_id})
            if results:
                self.send_json(results[0])
            else:
                self.send_error_with_message(404, "Golden not found")
        elif parsed.path == '/service/goldens/batch-update':
            length = int(self.headers.get("Content-Length"))
            message = json.loads(self.rfile.read(length))
            results, passed, failed = self.service_update_golden(set(message["selectedGoldenIds"]))
            response = {
                "results": results,
                "passedCount": passed,
                "failedCount": failed
            }
            self.send_json(response)
        else:
            self.send_error_with_message(404, message=f"Invalid PUT API: {parsed.path}")

    def serve_file(self, root_directory, file_relative_to_root, mime_type=None):
        resolved_path, resolved_mime_type = (
            WatchWebAppRequestHandler.service.resolve_file_path(root_directory, file_relative_to_root)
        )

        if resolved_path:
            self.send_response(200)
            self.send_header("Content-type", mime_type or resolved_mime_type)
            self.add_standard_headers()
            self.end_headers()
            with open(resolved_path, "rb") as f:
                self.wfile.write(f.read())
        else:
            self.send_error_with_message(404, message=resolved_mime_type)

    def send_error_with_message(self, code, message):
        try:
            self.send_response(code)
            self.send_header("Content-Type", "application/json")
            self.add_standard_headers()
            self.end_headers()
            payload = {"success": False, "error": message}
            self.wfile.write(json.dumps(payload).encode("utf-8"))
        except BrokenPipeError:
            print("Client disconnected before error response could be sent.")

    def fetch_gerrit_artifacts(self, linkPairs):
        golden_list = WatchWebAppRequestHandler.service.fetch_gerrit_artifacts(linkPairs)

        # Sort by goldenName first (ascending), then by testTime (descending)
        # to ensure newest runs are on top with consistent alphabetical order within.
        golden_list.sort(key=lambda x: x.get("goldenName") or str(x))
        golden_list.sort(key=lambda x: x.get("testTime") or "", reverse=True)

        testEntity = TestEntity(goldens_list=golden_list)
        WatchWebAppRequestHandler.test_entity_cache[GoldenWatcherTypes.GERRIT.value] = testEntity
        self.send_json(golden_list)

    def service_list_goldens(self):
        if not self.verify_access_token():
            return

        goldens_list = []

        for golden in WatchWebAppRequestHandler.test_entity.golden_watcher.cached_goldens.values():
            goldens_list.append(self.create_golden_data(golden))

        # Sort by goldenName first (ascending), then by testTime (descending)
        # to ensure newest runs are on top with consistent alphabetical order within.
        goldens_list.sort(key=lambda x: x.get("goldenName") or "")
        goldens_list.sort(key=lambda x: x.get("testTime") or "", reverse=True)

        # Update the goldens list
        WatchWebAppRequestHandler.test_entity.goldens_list = goldens_list
        self.send_json(goldens_list)

    def create_golden_data(self, golden):
        golden_data = {}
        golden_data["id"] = golden.id
        golden_data["result"] = golden.result
        golden_data["label"] = golden.golden_identifier
        golden_data["goldenRepoPath"] = golden.golden_repo_path
        golden_data["updated"] = golden.updated
        golden_data["testClassName"] = golden.test_class_name
        golden_data["testMethodName"] = golden.test_method_name
        golden_data["testTime"] = golden.test_time
        golden_data["goldenName"] = golden.golden_name

        golden_data["actualUrl"] = (
            f"{WatchWebAppRequestHandler.context.this_server_address}/golden/"
            f"{golden.checksum}/"
            f"{golden.local_file[len(WatchWebAppRequestHandler.test_entity
                                     .golden_watcher.temp_dir) + 1 :]}"
        )
        expected_file = path.join(
                            WatchWebAppRequestHandler.context.android_build_top,
                            golden.golden_repo_path
                        )

        if os.path.exists(expected_file):
            golden_data["expectedUrl"] = (
                f"{WatchWebAppRequestHandler.context.this_server_address}/expected/{golden.id}"
            )

        if golden.video_location:
            golden_data["videoUrl"] = (
                f"{WatchWebAppRequestHandler.context.this_server_address}/golden/"
                f"{golden.checksum}/{golden.video_location}"
            )

        return golden_data

    def service_presubmit_artifact_list(self, invocation_id):
        try:
            golden_watcher, presubmit_fetch_client, test_artifact_list = (
                WatchWebAppRequestHandler.service.service_presubmit_artifact_list(invocation_id)
            )
            WatchWebAppRequestHandler.test_entity = TestEntity(
                golden_watcher=golden_watcher,
                download_client=presubmit_fetch_client
            )
            WatchWebAppRequestHandler.test_entity_cache[
                GoldenWatcherTypes.PRESUBMIT.value] = WatchWebAppRequestHandler.test_entity

            self.serve_presubmit_data(test_artifact_list)
        except requests.exceptions.RequestException as exception:
            if exception.response.status_code == 500:
                self.send_error_with_message(503, str(exception))
            else:
                self.send_error_with_message(exception.response.status_code, str(exception))
        except Exception as exception:
            self.send_error_with_message(500, str(exception))

    def serve_presubmit_data(self, test_list):
        presubmit_data = []
        for test in test_list:
            presubmit_data_json = {}
            presubmit_data_json["testname"] = test
            presubmit_data.append(presubmit_data_json)

        # Sort presubmit data by testname
        presubmit_data.sort(key=lambda x: x.get("testname") or "")

        # Update the goldens list
        WatchWebAppRequestHandler.test_entity.goldens_list = presubmit_data
        self.send_json(presubmit_data)

    def service_fetch_artifacts(self, test_name):
        try:
            golden, error = WatchWebAppRequestHandler.service.service_fetch_artifacts(
                WatchWebAppRequestHandler.test_entity, test_name
            )
            if golden:
                self.send_json(self.create_golden_data(golden))
            else:
                self.send_error_with_message(404, error)
        except requests.exceptions.RequestException as exception:
            if exception.response.status_code == 500:
                self.send_error_with_message(503, str(exception))
            else:
                self.send_error_with_message(exception.response.status_code, str(exception))
        except Exception as exception:
            self.send_error_with_message(500, str(exception))

    def get_available_modes(self):
        '''
        Collects all adb devices available and send them along with modes like
        atest as available test mode options.
        '''

        available_modes = WatchWebAppRequestHandler.service.get_available_modes()
        self.send_json(available_modes)

    def switch_mode(self, mode: GoldenWatcherTypes):
        print(f'Switched to: {mode}')

        if mode in WatchWebAppRequestHandler.test_entity_cache:
            (WatchWebAppRequestHandler
            .test_entity) = WatchWebAppRequestHandler.test_entity_cache[mode]
            self.send_json(WatchWebAppRequestHandler.test_entity.goldens_list)

        else:
            try:
                golden_watcher = None
                match(mode):
                    case GoldenWatcherTypes.ATEST.value:
                        golden_watcher = GoldenWatcherFactory.create_watcher(
                                            GoldenWatcherTypes.ATEST,
                                            WatchWebAppRequestHandler.context
                                        )

                    case _:
                        if mode not in WatchWebAppRequestHandler.service.adb_serial_finder.model_serial_map:
                             WatchWebAppRequestHandler.service.get_available_modes()

                        serial = (WatchWebAppRequestHandler.service.adb_serial_finder
                                  .model_serial_map.get(mode))

                        if not serial:
                            raise ValueError(f"Mode or device '{mode}' not supported or found.")

                        adb_client = AdbClient(serial)
                        if not adb_client.run_as_root():
                            raise Exception("Cannot run ADB as root.")

                        golden_watcher = GoldenWatcherFactory.create_watcher(
                                            GoldenWatcherTypes.ADB,
                                            WatchWebAppRequestHandler.context,
                                            adb_client
                                        )
                (WatchWebAppRequestHandler
                .test_entity) = TestEntity(golden_watcher=golden_watcher)
                (WatchWebAppRequestHandler
                .test_entity_cache[mode]) = WatchWebAppRequestHandler.test_entity
                self.service_list_goldens()
            except Exception as ex:
                print(f"Failure occurred: {ex}")
                self.send_error_with_message(503, f"Failure occurred: {ex}")
                return

    def service_refresh_goldens(self, clear):
        success, error = WatchWebAppRequestHandler.service.refresh_goldens(
            WatchWebAppRequestHandler.test_entity, clear
        )
        if not success:
            self.send_error_with_message(400, error)
            return

        self.service_list_goldens()

    def service_update_golden(self, update_golden_id_set):
        '''
        Find goldens with IDs in update_golden_id_set and updates expected values.
        '''
        goldens = WatchWebAppRequestHandler.test_entity.golden_watcher.cached_goldens.values()
        results, passed_count, failed_count = WatchWebAppRequestHandler.service.update_goldens(
            update_golden_id_set, goldens
        )

        return results, passed_count, failed_count

    def send_json(self, data, status_code=200):
        try:
            response = {"success": True, "data": data}
            data_encoded = json.dumps(response).encode("utf-8")
            self.send_response(status_code)
            self.send_header("Content-type", "application/json")
            self.add_standard_headers()
            self.end_headers()
            self.wfile.write(data_encoded)
        except BrokenPipeError:
            print("Client disconnected before JSON response could be sent.")

    def add_standard_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS")
        self.send_header(
            "Access-Control-Allow-Headers",
            GOLDEN_ACCESS_TOKEN_HEADER
            + ", Content-Type, Content-Length, Range, Accept-ranges",
        )
        # Accept-ranges: bytes is needed for chrome to allow seeking the
        # video. At this time, won't handle ranges on subsequent gets,
        # but that is likely OK given the size of these videos and that
        # its local only.
        self.send_header("Accept-ranges", "bytes")
