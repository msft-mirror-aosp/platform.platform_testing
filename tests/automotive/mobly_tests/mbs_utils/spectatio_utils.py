#  Copyright (C) 2023 The Android Open Source Project
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

import logging

from mobly.controllers import android_device

class CallUtils:
    """Calling sequence utility for BT calling test using Spectatio UI APIs.

    This class provides functions that execute generic call sequences. Specific
    methods
    (e.g., verify_precall_state) are left to that implementation, and thus the
    utilities housed here are meant to describe generic sequences of actions.

    """

    def __init__(self, device):
        self.device = device

    # Open contacts
    def open_phone_app(self):
        logging.info('Open phone app')
        self.device.mbs.openPhoneApp()