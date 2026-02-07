# Copyright (C) 2026 The Android Open Source Project
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

'''
SDV Media Multi-display test

Tests sdv_multi_display_sample_rust CLI utility
'''
from mobly import asserts
import logging
import os
import tempfile
import time
import re
from pathlib import Path
from PIL import Image

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvMediaMultiDisplayTest(sdv_base_test.SdvBaseTestClass):

    RENDERER_COMMAND = 'sdv_multi_display_sample_rust'
    SCREENSHOT_FOLDER = '/data/local/tmp/screenshots'
    SCREENSHOT_COMMAND = f'sdv_screencap --output-dir {SCREENSHOT_FOLDER}'
    RENDERING_STARTED_LOG = 'enter render loop'
    FPS_LOG = 'FPS: '
    SCREENSHOT_SIZE = (800, 600)

    PIXEL_RED_ID = 0
    PIXEL_GREEN_ID = 1
    PIXEL_BLUE_ID = 2

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def setup_test(self):
        # Prepare screenshot folder
        self.sdv_device.execute_shell_command(f'mkdir -p {self.SCREENSHOT_FOLDER}')

    def teardown_test(self):
        # Delete screenshot folder
        self.sdv_device.execute_shell_command(f'rm -rf {self.SCREENSHOT_FOLDER}')

    def identify_color(self, color):
        match color:
            case (255, 0, 0, 255):
                return self.PIXEL_RED_ID
            case (0, 255, 0, 255):
                return self.PIXEL_GREEN_ID
            case (0, 0, 255, 255):
                return self.PIXEL_BLUE_ID
        return None

    def test_three_displays(self):
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} started')

        # Start rendering
        renderer_stdout_file = self.sdv_device.execute_shell_command_in_subprocess_log(
            self.RENDERER_COMMAND
        )

        # Wait until first FPS counter is reported - this should serve as a guarantee that rendering
        # loop went through at least one iteration
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.RENDERING_STARTED_LOG,
            assert_msg="Failed to detect rendering loop start.",
        )
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.FPS_LOG,
            assert_msg="Failed to detect FPS report.",
        )

        # Capture a screenshot
        result = self.sdv_device.execute_shell_command(self.SCREENSHOT_COMMAND)
        self.sdv_device.execute_shell_command(f'killall {self.RENDERER_COMMAND}')

        # Get the output paths from screenshot command output
        paths = re.findall(r'\/\S+', result)
        asserts.assert_equal(
            len(paths),
            3,
            f'expected 3 screenshot, got {len(paths)}: {paths}\n{result}'
        )

        # TODO(b/421378105): Check that colors match exact display we expect them to be rendered on
        colors_set = set()
        with tempfile.TemporaryDirectory() as tempdir:
        # Verify screenshot contents
            for path in paths:
                self.sdv_device.pull([path, tempdir])

                screenshot = os.path.join(tempdir, os.path.basename(path))
                img = Image.open(screenshot)

                # Size-optimized PNG may use a palette instead of RGBA.
                # Normalize the pixel format so that direct pixel data
                # comparison makes sense.
                img = img.convert('RGBA')

                asserts.assert_equal(
                    img.size,
                    self.SCREENSHOT_SIZE,
                    f'Screenshot size {img.size} does not match expected {self.SCREENSHOT_SIZE}'
                )

                width, height = img.size
                pixels = img.load()
                color_id = self.identify_color(pixels[0, 0])
                asserts.assert_not_equal(
                    color_id,
                    None,
                    f'Failed to identify color {pixels[0, 0]} from {path}'
                )
                asserts.assert_false(
                    color_id in colors_set,
                    f'Same color {color_id} was present in previous screenshots'
                )
                colors_set.add(color_id)

                for y in range(height):
                    for x in range(width):
                        asserts.assert_equal(
                            pixels[0, 0],
                            pixels[x, y],
                            f'Screenshot is not a solid color image {path}'
                        )

        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} completed.')

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
