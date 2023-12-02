"""Test of calling phone number from history using Mobly and Mobile Harness.


- Requires a testbed with one phone in it that can make calls.
- Requires sl4a be installed on Android devices. See README.md for details.
- Optional to use --define="dialer_test_phone_number={$PHONE_NUMBER_VALUE}"
- Default dialer_test_phone_number=dialer_test_phone_number

 Steps include:
        1) Pre-call state check on IVI and phone devices. (OK)
        2) Make a call to any digits number using IVI
        3) Assert calling number on IVI same as called
        4) End call on IVI
        5) Open call history
        6) Call most recent phone number
        7) Assert calling number on IVI same as called
        8) End call
"""
from utilities import constants
from utilities.main_utils import common_main
from bluetooth_test import bluetooth_base_test


class MakeCallFromHistotyTest(bluetooth_base_test.BluetoothBaseTest):
  """Implement calling to ten digits number test."""
  def setup_test(self):

    # Pair the devices
    self.bt_utils.pair_primary_to_secondary()
  def test_dial_large_digits_number(self):
    """Tests the calling tten digits number functionality."""
    #Variable
    dialer_test_phone_number = constants.DIALER_THREE_DIGIT_NUMBER
    #Tests the calling three digits number functionality

    self.call_utils.dial_a_number(dialer_test_phone_number)
    self.call_utils.make_call()
    self.call_utils.wait_with_log(5)
    self.call_utils.verify_dialing_number(dialer_test_phone_number)
    self.call_utils.end_call()
    self.call_utils.wait_with_log(5)
    self.call_utils.open_call_history()
    self.call_utils.call_most_recent_call_history()
    self.call_utils.wait_with_log(5)
    self.call_utils.verify_dialing_number(dialer_test_phone_number)

  def teardown_test(self):
    # End call if test failed
    self.call_utils.end_call_using_adb_command(self.target)

if __name__ == '__main__':
  common_main()