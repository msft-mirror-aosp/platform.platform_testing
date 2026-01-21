# Copyright 2025 Google LLC

import logging
import time

from display_safety_test.test_base import display_safety_test_base
from spectatio_host_tf.core import test_runner


class DisplaySafetyClusterCommsScreenshotTest(
    display_safety_test_base.DisplaySafetyBaseTestClass
):
  """
    Display Safety Cluster Comms Screenshot Test: Compares the screenshot of Cluster
    display with the golden image for different themes and UI modes triggered via IVI.
  """

  def setup_class(self):
    super().setup_class()
    # Assuming the second device is the IVI device.
    # We use getattr because device2 might not be populated if only 1 device is allocated,
    # although the config requests two.
    self.ivi_device = getattr(self, 'device2', self.device1)
    if self.ivi_device == self.device1:
        logging.warning("device2 not found. Using device1 as IVI device.")

  def setup_test(self):
    """
      Sets up paths for the test.
    """
    super().setup_test()

    # Set date to ensure consistent time (e.g. for clock) and day/night baseline
    device_system_datetime = '2025-10-02T09:00:00'
    self.ivi_device.adb.execute_shell_command(f'date -s {device_system_datetime}')

    # Set vehicle state to match the passing test and likely golden images
    speed = 5.0
    gear = self.Gear.DRIVE.value
    engine_rpm = 1200
    with self.display_safety_client() as client:
      client.post_vehicle_speed('VEHICLE_SPEED', speed)
      client.post_current_gear('GEAR', gear)
      client.post_engine_rpm('ENGINE_RPM', engine_rpm)

    # Wait for state to apply
    time.sleep(2)

    self.golden_image_path = self.get_golden_image_path(
        test_name=self.current_test_info.name,
        pkg_name='display_safety_cluster_comms_test_resources',
    )
    self.test_image_path = self.get_output_path_for_image(
        f'{self.current_test_info.name}_test_image.png'
    )
    self.diff_image_path = self.get_output_path_for_image(
        f'{self.current_test_info.name}_diff_image.png'
    )

  def _inject_key(self, key_code):
    cmd = f'cmd car_service inject-key -d 1 {key_code}'
    logging.info(f'Executing command on IVI: {cmd}')
    self.ivi_device.adb.execute_shell_command(cmd)
    time.sleep(2) # Wait for effect

  def _set_night_mode(self, enabled: bool):
    mode = 'yes' if enabled else 'no'
    cmd = f'cmd uimode night {mode}'
    logging.info(f'Executing command on IVI: {cmd}')
    self.ivi_device.adb.execute_shell_command(cmd)
    time.sleep(2) # Wait for effect

  def _take_and_compare_screenshot(self):
    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Waiting for 5 seconds before taking screenshot.'
    )
    time.sleep(5)
    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Taking'
        ' screenshot of cluster display.'
    )
    self.take_cluster_screenshot(self.test_image_path)

    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Comparing'
        ' screenshot with golden image.'
    )
    are_images_similar = self.compare_images(
        self.golden_image_path, self.test_image_path, self.diff_image_path
    )

    self.asserts.assert_true(
        are_images_similar,
        'Device screenshot does not match the golden image. '
        f'Diff found at: {self.diff_image_path}',
    )

  def test_cluster_theme_blue(self):
    """
      Sets theme to default blue (key 8) and verifies screenshot.
    """
    self._inject_key(8)
    self._set_night_mode(False)
    self._take_and_compare_screenshot()

  def test_cluster_theme_purple(self):
    """
      Sets theme to custom purple (key 9) and verifies screenshot.
    """
    self._inject_key(9)
    self._set_night_mode(False)
    self._take_and_compare_screenshot()

  def test_cluster_light_mode(self):
    """
      Sets UI mode to day (night no) and verifies screenshot.
    """
    self._inject_key(8)
    self._set_night_mode(False)
    self._take_and_compare_screenshot()

  def test_cluster_dark_mode(self):
    """
      Sets theme to blue, then UI mode to night (night yes) and verifies screenshot.
    """
    self._inject_key(8)
    self._set_night_mode(True)
    self._take_and_compare_screenshot()


if __name__ == '__main__':
  test_runner.run()
