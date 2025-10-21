#  Copyright (C) 2025 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

from mobly.controllers import android_device
from mobly.controllers.android_device_lib.snippet_client_v2 import Config
from utilities.main_utils import get_test_args

def actions_setup(test_class):
    test_class.ads = test_class.register_controller(android_device)
    test_class.main_device = android_device.get_device(test_class.ads, label='auto')

    test_args = get_test_args(shell_escape=True)
    snippet_config = Config(am_instrument_options=test_args)
    test_class.main_device.load_snippet('mbs', android_device.MBS_PACKAGE, config=snippet_config)
    if not test_class.main_device.is_adb_root:
        test_class.main_device.root_adb()