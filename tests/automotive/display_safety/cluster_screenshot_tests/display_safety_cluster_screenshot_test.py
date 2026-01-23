# Copyright 2025 Google LLC

import logging
import time

from display_safety_test.test_base import display_safety_test_base
from spectatio_host_tf.core import test_runner


class DisplaySafetyClusterScreenshotTest(
    display_safety_test_base.DisplaySafetyBaseTestClass
):
  """
    Display Safety Cluster Screenshot Test: Compares the screenshot of Cluster
    display with the golden image for different vehicle states.

    Default Golden Images are added as resources. Each test case has a default
    golden image with the test name as the filename in the resources.

    If Custom Golden Images are required, they can be passed as test arguments
    using --test_args=<test_name>_golden_image_path='/path/to/golden/image.jpg'
  """

  def setup_test(self):
    """
      Sets the vehicle state and prepares paths for the test.
    """
    super().setup_test()
    self.golden_image_path = self.get_golden_image_path(
        test_name=self.current_test_info.name,
        pkg_name='display_safety_cluster_test_resources',
    )
    self.test_image_path = self.get_output_path_for_image(
        f'{self.current_test_info.name}_test_image.png'
    )
    self.diff_image_path = self.get_output_path_for_image(
        f'{self.current_test_info.name}_diff_image.png'
    )

  def test_cluster_display(self):
    """
      Sets the vehicle state, takes a screenshot of the cluster display
      and compares it against a golden image.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')

    device1_system_datetime = '2025-10-02T09:00:00'
    speed = 5.0
    gear = self.Gear.DRIVE.value
    engine_rpm = 1200

    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Setting vehicle'
        f' state to speed: {speed}, gear: {gear}, engine_rpm: {engine_rpm}'
    )

    self.device1.adb.execute_shell_command(f'date -s {device1_system_datetime}')
    with self.display_safety_client() as client:
      client.post_vehicle_speed('VEHICLE_SPEED', speed)
      client.post_current_gear('GEAR', gear)
      client.post_engine_rpm('ENGINE_RPM', engine_rpm)

    # Wait for the vehicle state to be applied.
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

    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}'
    )


if __name__ == '__main__':
  test_runner.run()
