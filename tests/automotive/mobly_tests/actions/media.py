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

import time


class MediaKeys(test_base.SpectatioHostBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.mbs = self.device1.load_bundled_snippets()
        self.device1.adb.root()

        self.register_service_factory('screenshot', ScreenshotUtil)

    def setup_test(self):
        pass

    def teardown_test(self):
        pass

    def hardkey_and_screenshot(self, hardkey, screenshot_path, sleep_after=5):
        hardkey()
        time.sleep(0.5)
        self.screenshot.take_screenshot(
            screenshot_strategy =
                ScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value,
            device = self.device1,
            screenshot_path = screenshot_path,
        )
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
                'up0.png',
                'up1.png',
                include_area=VOLUME_AREA,
            )
            if volume_check.are_images_similar():
                break
            swap_number = 1 - swap_number
            tries -= 1

        self.hardkey_and_screenshot(self.mbs.hardkeyVolumeDown, 'down.png', sleep_after=0)
        volume_check = image_comparison.CompareImagesUsingPIL(
            'down.png',
            'up0.png',
            include_area=VOLUME_AREA,
        )
        self.asserts.assert_false(volume_check.are_images_similar(), "Volume down lowered volume")

    def test_mute(self):
        strategy = ScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value
        first_image = 'first_mute.png'
        second_image = 'second_mute.png'

        self.hardkey_and_screenshot(self.mbs.hardkeyMute, first_image)
        self.hardkey_and_screenshot(self.mbs.hardkeyMute, second_image, sleep_after=0)

        muted_golden_path = find_resource_path('actions_golden_images', 'golden_images/muted.png')
        unmuted_golden_path = find_resource_path('actions_golden_images', 'golden_images/unmuted.png')

        # todo : cf-specific. externalize for other platforms. b/472553534
        ICON_AREA = (24, 37, 57, 70)
        muted_first_check = image_comparison.CompareImagesUsingPIL(
            first_image,
            muted_golden_path,
            include_area=ICON_AREA,
        )
        unmuted_first_check = image_comparison.CompareImagesUsingPIL(
            first_image,
            unmuted_golden_path,
            include_area=ICON_AREA,
        )
        muted_first = muted_first_check.are_images_similar()
        unmuted_first = unmuted_first_check.are_images_similar()
        self.asserts.assert_true(muted_first or unmuted_first, "Mute button shows sound icon")
        self.asserts.assert_true(muted_first != unmuted_first, "Sanity check (muted != unmuted)")

        second_golden = unmuted_golden_path if muted_first else muted_golden_path
        second_check = image_comparison.CompareImagesUsingPIL(
            second_image,
            second_golden,
            include_area=ICON_AREA,
        )
        self.asserts.assert_true(second_check.are_images_similar(), "Second mute press toggles back")

if __name__ == '__main__':
    test_runner.run()
