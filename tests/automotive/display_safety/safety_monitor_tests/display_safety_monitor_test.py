# Copyright 2025 Google LLC

import logging
import time
import re

from absl.testing import parameterized
from display_safety_test.test_base import display_safety_test_base
from spectatio_host_tf.core import test_runner

class DisplaySafetyMonitorTest(
    display_safety_test_base.DisplaySafetyBaseTestClass,
    parameterized.TestCase,
):
  """
    Verifies the operation of the display safety monitor.
  """

  _SAFETY_MONITOR_BINARY = 'har_safety_monitor'
  _LOG_TAG = 'har-safety-monitor'

  def setup_test(self):
    super().setup_test()
    self.ad = self.android_devices[0]
    self.ad.adb.shell('setprop log.tag.har-safety-monitor DEBUG')
    self.reset_display_safety_ui_to_default_state()
    self._restart_safety_monitor()

  def _restart_safety_monitor(self):
    """Restarts the safety monitor service to ensure a clean state."""
    logging.info('Restarting safety monitor...')
    # Assuming it's a service we can stop/start or kill.
    # Based on init.rc, it is a service named 'har_safety_monitor'.
    self.ad.adb.shell('stop har_safety_monitor')
    self.ad.adb.shell('start har_safety_monitor')
    time.sleep(2) # Wait for it to start

    # Verify it is running
    out = self.ad.adb.shell(f'pidof {self._SAFETY_MONITOR_BINARY}')
    if not out:
        raise RuntimeError(f'{self._SAFETY_MONITOR_BINARY} is not running.')

  def _clear_logcat(self):
    self.ad.adb.shell('logcat -c')

  def _search_logcat(self, pattern: str) -> bool:
    """Searches for a pattern in logcat."""
    # -d dumps the log, -s filters by tag if needed, but grep is better for pattern
    # We look specifically for our tag to avoid noise
    cmd = f'logcat -d -s {self._LOG_TAG} | grep "{pattern}"'
    try:
        out = self.ad.adb.shell(cmd)
        return bool(out and out.strip())
    except Exception:
        return False

  def test_safety_monitor_telltale_update(self):
    """
    Verifies that the safety monitor receives telltale updates and logs them.
    """
    logging.info('Running test_safety_monitor_telltale_update')

    telltale = 'SEATBELT_DRIVER'
    # Based on main.rs, SEATBELT_DRIVER maps to "telltale/no-seatbelt"
    expected_log_topic = "telltale/no-seatbelt"

    self._clear_logcat()

    # Enable the telltale
    logging.info(f'Enabling telltale: {telltale}')
    with self.display_safety_client() as client:
      client.post_telltale_status(telltale, is_on=True)

    time.sleep(3) # Wait for processing

    # Check logs for update confirmation
    # Expected log: "Updated "telltale/no-seatbelt" to true"
    # Note: main.rs uses {:#?} which might quote the string.
    # log::debug!("Updated {:#?} to {:#?}", telltale_name, telltale_status.visibility);
    # Rust debug format for string usually adds quotes.

    # We search for the core part of the message to be safe.
    pattern = f'Updated "{expected_log_topic}" to true'
    found = self._search_logcat(pattern)

    # NOTE: The log level is DEBUG in main.rs. We need to ensure debug logs are enabled for this tag.
    # If not found, it might be due to log level.
    # But usually integration tests run with sufficient logging.
    # If it fails, we might need to setprop log.tag.har-safety-monitor DEBUG

    if not found:
        # Try without quotes just in case
        pattern_alt = f'Updated {expected_log_topic} to true'
        if self._search_logcat(pattern_alt):
            found = True

    self.asserts.assert_true(found, f'Safety monitor did not log update for {expected_log_topic}')

    # Verify NO mismatch error
    # Log: "Telltale Mismatch for ... "
    mismatch_pattern = "Telltale Mismatch"
    mismatch_found = self._search_logcat(mismatch_pattern)
    self.asserts.assert_false(mismatch_found, 'Safety monitor reported a telltale mismatch!')

if __name__ == '__main__':
  test_runner.run()
