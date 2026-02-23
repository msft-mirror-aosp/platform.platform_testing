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
from spectatio_host_tf.core import test_runner
from functional_test.test_base import functional_test_base

import time

class VhalSensors(functional_test_base.FunctionalTestBaseClass):

    def test_night_mode(self):
        """Turn night mode on and off and check that the HUD responds."""
        strategy = ScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value

        nightmode_golden_path = self.get_golden_image_path(
                  golden_image_name='nightmode_golden.png',
                  pkg_name='actions_golden_images'
        )

        nightmode_test_path = self.get_output_path_for_image(
                 image_name='nightmode.png'
        )

        nightmode_diff_path = self.get_output_path_for_image(
                 image_name='nightmode_diff.png'
        )

        self.mbs.pressHome()
        self.mbs.setNightMode("true")

        ANIMATION_WAIT_SECONDS = 5
        time.sleep(ANIMATION_WAIT_SECONDS)

        self.take_device_screenshot(nightmode_test_path)
        is_similar = self.compare_images(
            nightmode_golden_path,
            nightmode_test_path,
            nightmode_diff_path,
            include_area=(312, 57, 1080, 528),
        )
        self.asserts.assert_true(is_similar, "Night mode matches golden")


        self.mbs.setNightMode("false")
        # TODO: check screenshot against day mode golden


if __name__ == '__main__':
    test_runner.run()
