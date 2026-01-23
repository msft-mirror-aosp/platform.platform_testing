# Copyright 2025 Google LLC

import logging
import time
import json

from absl.testing import parameterized
from display_safety_test.test_base import display_safety_test_base
from spectatio_host_tf.core import test_runner

class SafetyMonitorOcclusionTest(
    display_safety_test_base.DisplaySafetyBaseTestClass,
    parameterized.TestCase,
):
  """
    Verifies that the safety monitor detects occlusion using the HAR debug feature.
  """

  _SAFETY_MONITOR_BINARY = 'har_safety_monitor'
  _LOG_TAG = 'har-safety-monitor'
  _DATA_JSON_PATH = '/data/vendor/com.google.display_safety.har/assets/common/artifacts/display-1/data.json'

  def setup_test(self):
    super().setup_test()
    self.ad = self.android_devices[0]
    self.ad.adb.shell('setprop log.tag.har-safety-monitor DEBUG')
    self.reset_display_safety_ui_to_default_state()
    self._restart_safety_monitor()

  def _restart_safety_monitor(self):
    """Restarts the safety monitor service to ensure a clean state."""
    logging.info('Restarting safety monitor...')
    self.ad.adb.shell(f'stop {self._SAFETY_MONITOR_BINARY}')
    self.ad.adb.shell(f'start {self._SAFETY_MONITOR_BINARY}')
    time.sleep(2) # Wait for it to start

    # Verify it is running
    out = self.ad.adb.shell(f'pidof {self._SAFETY_MONITOR_BINARY}')
    if not out:
        raise RuntimeError(f'{self._SAFETY_MONITOR_BINARY} is not running.')

  def _clear_logcat(self):
    self.ad.adb.shell('logcat -c')

  def _search_logcat(self, pattern: str) -> bool:
    """Searches for a pattern in logcat."""
    cmd = f'logcat -d -s {self._LOG_TAG} | grep "{pattern}"'
    try:
        out = self.ad.adb.shell(cmd)
        return bool(out and out.strip())
    except Exception:
        return False

  def _get_telltale_bounds(self, telltale_name: str):
    """Fetches the bounds (x, y, width, height) of a telltale from data.json."""
    try:
        content = self.ad.adb.shell(f'cat {self._DATA_JSON_PATH}')
        data = json.loads(content)
        for element in data.get('static_ui_elements', []):
            if element.get('name') == telltale_name:
                return (
                    element['x'],
                    element['y'],
                    element['width'],
                    element['height']
                )
        return None
    except Exception as e:
        logging.error(f'Failed to get telltale bounds: {e}')
        return None

  def test_safety_monitor_telltale_occlusion(self):
    """
    Verifies that the safety monitor detects telltale occlusion.
    """
    logging.info('Running test_safety_monitor_telltale_occlusion')

    telltale = 'SEATBELT_DRIVER'
    telltale_log_name = "telltale/no-seatbelt"

    # Get telltale coordinates
    bounds = self._get_telltale_bounds(telltale_log_name)
    self.asserts.assert_true(bounds, f'Could not find bounds for {telltale_log_name}')
    x, y, w, h = bounds
    center_x = x + w / 2
    center_y = y + h / 2

    # Move buggy to cover the telltale
    logging.info(f'Moving buggy to cover {telltale_log_name} at ({center_x}, {center_y})')
    self.ad.adb.shell(f'harry_rpc_client move-buggy --x {center_x} --y {center_y}')

    # Wait for UI to update
    time.sleep(2)

    self._clear_logcat()

    # Enable the telltale
    logging.info(f'Enabling telltale: {telltale}')
    with self.display_safety_client() as client:
      client.post_telltale_status(telltale, is_on=True)

    time.sleep(5) # Wait for processing and monitoring cycle

    # Verify mismatch error
    mismatch_pattern = f'Telltale Mismatch for .*{telltale_log_name}'
    found = self._search_logcat(mismatch_pattern)

    if not found:
         logging.info("Detailed mismatch pattern search...")
         found = self._search_logcat("Telltale Mismatch") and self._search_logcat(telltale_log_name)

    self.asserts.assert_true(found, 'Safety monitor failed to report telltale mismatch when occluded!')

    # Teardown: Move buggy away
    logging.info('Moving buggy away')
    self.ad.adb.shell('harry_rpc_client move-buggy --x 0 --y 0')

if __name__ == '__main__':
  test_runner.run()
