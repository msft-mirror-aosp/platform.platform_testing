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


from bluetooth_test import bluetooth_base_test

from mobly import asserts
from mobly import test_runner


class DeviceNotPairedTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_test(self):
        # Disable BT on devices
        self.target.mbs.btDisable()
        self.discoverer.mbs.btDisable()

    def test_device_not_paired(self):
        """Tests lunch Bluetooth Audio app and verify <Bluetooth Disconnected> displayed."""
        self.call_utils.open_bluetooth_media_app()
        asserts.assert_true(self.call_utils.is_bluetooth_audio_disconnected_label_visible(),
                            '<Bluetooth Audio disconnected> label should be present')

    def teardown_test(self):
        # TODO(b/302283016) remove teardown_test after bug resolved
        pass


if __name__ == '__main__':
    test_runner.main()
