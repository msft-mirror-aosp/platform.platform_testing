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

from file_utils_library.file_util import find_resource_path
from image_comparison_library import image_comparison
from screenshot_util_library.screenshot_util import ScreenshotUtil
from spectatio_host_tf.core import test_base, test_runner


class VhalSensors(test_base.SpectatioHostBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.mbs = self.device1.load_bundled_snippets()
        self.device1.adb.root()

        self.register_service_factory('screenshot', ScreenshotUtil)

    def setup_test(self):
        pass

    def teardown_test(self):
        pass

    def test_night_mode(self):
        """Turn night mode on and off and check that the HUD responds."""
        strategy = ScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value

        self.mbs.pressHome()
        self.mbs.setNightMode("true")
        night_test_path = 'nightmode.png'
        night_golden_path = find_resource_path('actions_golden_images', 'golden_images/nightmode_golden.png')
        self.screenshot.take_screenshot(
            screenshot_strategy = strategy,
            device = self.device1,
            screenshot_path = night_test_path,
        )

        night_check = image_comparison.CompareImagesUsingPIL(
            night_test_path,
            night_golden_path,
            (0, 0, 1080, 200),  # exclusion rectangle -- left, top, right, bottom
        )
        is_similar = night_check.are_images_similar()
        night_check.save_diff_image('nightmode_diff.png')
        self.asserts.assert_true(is_similar, "Night mode matches golden")


        self.mbs.setNightMode("false")
        # TODO: check screenshot against day mode golden


if __name__ == '__main__':
    test_runner.run()
