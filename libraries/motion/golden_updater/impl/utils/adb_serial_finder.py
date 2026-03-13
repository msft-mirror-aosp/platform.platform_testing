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

import subprocess

class ADBSerialFinder:

    def __init__(self):
        self.model_serial_map = {} # stores {model_identifier: <adb_serial>}
        self.update_model_serial_map()

    def update_model_serial_map(self):
        self.model_serial_map = {}
        try:
            devices_response = subprocess.run(
                ["adb", "devices", "-l"], check=True, capture_output=True
            ).stdout.decode("utf-8")
            lines = [s for s in devices_response.splitlines() if s.strip()]

            if len(lines) <= 1:
                print("no adb devices found")
                return None
            self.__update_model_serial_map_with_device_info(lines[1:])
        except Exception as e:
            # Failing quietly as not having adb devices is not a blocker for running the server.
            print(f'Exception occurred while updating adb model serial map: {e}')

    def __update_model_serial_map_with_device_info(self, devices):
        '''
        Example value post update:
        {'Pixel_7_2A121FDH200F40': 'localhost:35725'}
        '''

        for device_line in devices:
            parts = [p for p in device_line.split(" ") if p != ""]
            if not parts:
                continue

            adb_identifier = parts[0]
            model = "unknown_model"
            for part in parts:
                if part.startswith("model:"):
                    model = part.split(":", 1)[1]
                    break

            # Use the adb_identifier (e.g., 0.0.0.0:6520) as part of the key
            # to ensure uniqueness when multiple devices of same model are connected.
            model_key = f'{model}_{adb_identifier}'
            self.model_serial_map[model_key] = adb_identifier

    def __find_serial_number(self, socket_info):
        '''
        Find the serial number of the device connected in the socket: <localhost:port>
        TODO: Remove this method once the current version (using adb_identifier in model_key)
        is verified to be bug-free.
        '''
        if not socket_info:
            return ""
        try:
            serial_number = subprocess.run(
                ["adb", "-s", socket_info, "shell", "getprop", "ro.serialno"],
                check=True, capture_output=True
            ).stdout.decode("utf-8")
            return serial_number.strip()
        except Exception as e:
            # Failing quietly as not having serial number is not a blocker for running the server.
            print(f'Exception occurred while fetching adb model serial number: {e}')
            return ""
