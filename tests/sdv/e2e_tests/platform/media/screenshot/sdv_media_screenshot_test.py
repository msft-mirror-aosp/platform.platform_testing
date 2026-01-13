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
SDV Media Screenshot test

Tests sdv_screenshot CLI utility
'''
from mobly import asserts
import logging
import os
import tempfile
import importlib.resources
import time
import glob
from pathlib import Path
from PIL import Image, ImageChops

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

class SdvMediaScreenshotTest(sdv_base_test.SdvBaseTestClass):

    RENDERER_COMMAND = 'sdv_gl_gen_texture'
    SCREENSHOT_FOLDER = '/data/local/tmp/screenshots'
    SCREENSHOT_COMMAND = f'sdv_screencap --output-dir {SCREENSHOT_FOLDER}'

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def setup_test(self):
        # Prepare screenshot folder
        self.sdv_device.execute_shell_command(f'mkdir -p {self.SCREENSHOT_FOLDER}')

    def teardown_test(self):
        # Delete screenshot folder
        self.sdv_device.execute_shell_command(f'rm -rf {self.SCREENSHOT_FOLDER}')

    def open_golden_file(self):
        GOLDEN_FILENAME = 'sdv_gl_gen_texture.png'
        if self.user_params.get('env') == 'local':
            return importlib.resources.files('goldens').joinpath(GOLDEN_FILENAME).open('rb')

        log_path = Path(self.sdv_device.log_path())
        test_dir = str(Path().joinpath(*log_path.parts[:3]))
        search_path = f"{test_dir}/**/{GOLDEN_FILENAME}"
        logging.debug(f"Search pattern (CI): {search_path}")
        file_names = glob.glob(search_path, recursive=True)
        if not file_names:
            raise Exception(f"Failed to find files matching '{search_path}'")
        return open(file_names[0], 'rb')

    def test_capture_screenshot(self):
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} started')

        # Start rendering
        renderer_stdout_file = self.sdv_device.execute_shell_command_in_subprocess_log(
            self.RENDERER_COMMAND
        )

        # Wait until a couple of frames have been rendered
        render_result = False
        deadline = time.perf_counter() + 5 # Timeout is 5 seconds
        while time.perf_counter() < deadline:
            frames_count = 0
            for line in self.sdv_device.read_file(renderer_stdout_file).splitlines():
                if line.strip() == "End frame render":
                    frames_count += 1
            # Waiting for >2 frames to be rendered to make sure sceencap catches it
            if frames_count > 2:
                render_result = True
                break
            time.sleep(0.1)

        if not render_result:
            asserts.fail(
                f'Frame render failed: {self.sdv_device.read_file(renderer_stdout_file)}'
            )

        # Capture a screenshot
        result = self.sdv_device.execute_shell_command(self.SCREENSHOT_COMMAND)
        self.sdv_device.execute_shell_command(f'killall {self.RENDERER_COMMAND}')

        # Get the output paths from screenshot command output
        screenshots = result.rstrip('\n').split('\n')
        asserts.assert_equal(
            len(screenshots),
            1,
            f'expected 1 screenshot, got {len(screenshots)}: {screenshots}'
        )

        # Verify screenshot contents
        with tempfile.TemporaryDirectory() as tempdir:
            # this has to be open the entire time image contents are in use
            with self.open_golden_file() as golden_f:
                self.sdv_device.pull([screenshots[0], tempdir])

                screenshot = os.path.join(tempdir, os.path.basename(screenshots[0]))
                actual = Image.open(screenshot)
                expected = Image.open(golden_f)

                try:
                    # Size-optimized PNG may use a palette instead of RGBA.
                    # Normalize the pixel format so that direct pixel data
                    # comparison makes sense.
                    actual = actual.convert('RGBA')
                    expected = expected.convert('RGBA')

                    asserts.assert_equal(
                        actual.size,
                        expected.size,
                        f'screenshot size {actual.size} does not match expected {expected.size}'
                    )
                    # The images we're comparing will be large. Don't put the
                    # pixel data in asserts to avoid timing out on printing
                    # enormous strings that are too unreadable to be useful.
                    if list(actual.getdata()) != list(expected.getdata()):
                        asserts.fail(f'screenshot is not what was expected')
                except Exception as e:
                    # Verification failed. Store the images for later inspection.
                    # Save as PNG to save some space, it's lossless so conversion
                    # from BMP doesn't matter.
                    log_dir = self.sdv_device.log_path()
                    actual.save(os.path.join(log_dir, 'screen_actual.png'))
                    expected.save(os.path.join(log_dir, 'screen_expected.png'))
                    ImageChops.difference(actual, expected).save(os.path.join(log_dir, 'screen_diff.png'))
                    raise e

        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} completed.')

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
