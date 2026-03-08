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

import socketserver
import os
import sys
import webbrowser
from impl.core.watch_web_app_request_handler import WatchWebAppRequestHandler
from impl.utils.argument_parser import ArgumentParser
from impl.utils.token_generator import TokenGenerator
from impl.core.context import MotionWatcherContext
from impl.core.motion_service import MotionService
from impl.models.motion_constants import MOTION_CACHE_DIR

def main():

    args = ArgumentParser.get_args()

    if args.android_build_top is None or not os.path.exists(args.android_build_top):
        print("ANDROID_BUILD_TOP not set. Have you sourced envsetup.sh?")
        sys.exit(1)

    android_build_top = args.android_build_top

    this_server_address = f"http://localhost:{args.port}"
    tmpdir = MOTION_CACHE_DIR
    secret_token = TokenGenerator.get_token()

    context = MotionWatcherContext(
        android_build_top=android_build_top,
        temp_dir=tmpdir,
        secret_token=secret_token,
        this_server_address=this_server_address,
        port=args.port,
        client_url=args.client_url
    )

    service = MotionService(context)

    WatchWebAppRequestHandler.context = context
    WatchWebAppRequestHandler.service = service

    with socketserver.TCPServer(
        ("localhost", args.port), WatchWebAppRequestHandler
    ) as httpd:
        uiAddress = f"{args.client_url}?token={secret_token}&port={args.port}"
        print(f"Open UI at {uiAddress}")
        webbrowser.open(uiAddress)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            httpd.shutdown()
            print("Shutting down")
