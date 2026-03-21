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



from image_comparison_library import image_comparison
from screenshot_util_library.screenshot_util import ScreenshotUtil
from spectatio_host_tf.core import test_runner
from functional_test.test_base import functional_test_base

import time


class MediaKeys(functional_test_base.FunctionalTestBaseClass):

    def hardkey_and_screenshot(self, hardkey, screenshot_path, sleep_after=5):
        hardkey()
        time.sleep(0.5)
        self.take_device_screenshot(screenshot_path)

        if sleep_after:
            time.sleep(sleep_after)

    def test_volume(self):
        strategy = ScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value

        swap_number = 1

        # todo : cf-specific. externalize for other platforms. b/472553534
        VOLUME_AREA = (120, 6, 974, 102)
        self.hardkey_and_screenshot(self.mbs.hardkeyVolumeUp, 'up0.png', sleep_after=0)
        tries = 100
        while tries > 0:
            self.hardkey_and_screenshot(
                self.mbs.hardkeyVolumeUp, f'up{swap_number}.png', sleep_after=0
            )
            volume_check = image_comparison.CompareImagesUsingPIL(
                f'up{swap_number}.png',
                f'up{(swap_number+2)%3}.png',
                include_area=VOLUME_AREA,
            )
            swap_number = (swap_number + 1) % 3
            tries -= 1
            if volume_check.are_images_similar():
                break

        self.hardkey_and_screenshot(self.mbs.hardkeyVolumeDown, 'down.png', sleep_after=0)
        volume_check = image_comparison.CompareImagesUsingPIL(
            'down.png',
            f'up{swap_number}.png',
            include_area=VOLUME_AREA,
        )
        self.asserts.assert_false(volume_check.are_images_similar(), "Volume down lowered volume")

    def test_mute(self):

        muted_golden_path = self.get_golden_image_path(
            golden_image_name='muted_golden.png',
            pkg_name='actions_golden_images'
        )

        unmuted_golden_path = self.get_golden_image_path(
             golden_image_name='unmuted_golden.png',
             pkg_name='actions_golden_images'
        )

        first_mute_test_path = self.get_output_path_for_image(
            image_name='first_mute.png'
       )

        second_mute_test_path = self.get_output_path_for_image(
            image_name='second_mute.png'
        )

        muted_first_check_diff_path = self.get_output_path_for_image(
            image_name='muted_first_check_diff_path.png'
        )

        unmuted_first_check_diff_path = self.get_output_path_for_image(
           image_name='unmuted_first_check_diff_path.png'
        )

        second_check_diff_path = self.get_output_path_for_image(
            image_name='second_check_diff_path.png'
        )
        self.hardkey_and_screenshot(self.mbs.hardkeyMute, self.first_mute_test_path)
        self.hardkey_and_screenshot(self.mbs.hardkeyMute, self.second_mute_test_path, sleep_after=0)

        # todo : cf-specific. externalize for other platforms. b/472553534
        ICON_AREA = (24, 37, 57, 70)
        muted_first = self.compare_images(
              muted_golden_path,
              first_mute_test_path,
              muted_first_check_diff_path,
              include_area=ICON_AREA
        )

        unmuted_first = self.compare_images(
              unmuted_golden_path,
              first_mute_test_path,
              unmuted_first_check_diff_path,
              include_area=ICON_AREA
        )

        self.asserts.assert_true(muted_first or unmuted_first, "Mute button shows sound icon")
        self.asserts.assert_true(muted_first != unmuted_first, "Sanity check (muted != unmuted)")

        second_golden = unmuted_golden_path if muted_first else first_mute_test_path

        second_check = self.compare_images(
            second_golden,
            second_mute_test_path,
            second_check_diff_path,
            include_area=ICON_AREA
        )

        self.asserts.assert_true(second_check.are_images_similar(), "Second mute press toggles back")


if __name__ == '__main__':
    test_runner.run()
