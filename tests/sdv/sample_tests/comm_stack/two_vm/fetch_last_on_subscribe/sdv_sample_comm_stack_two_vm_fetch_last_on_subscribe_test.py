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

"""SDV CommStack Test Two VM - Fetch Last on Subscribe Test"""

from mobly import asserts
import logging
import helper
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
import time

TOPIC_1_INSTANCE_NAME = "topic1"
TOPIC_2_INSTANCE_NAME = "topic2"

# Publisher process starts up, writes a single message, then sleeps 10s before
# exiting and unregistering the publication.  Delay is needed to give subscribers
# time to start up and create subscription.
PUB_COMMAND = (
    '/system/bin/sdv_comms_client_rs publish --instance-name publisher'
    ' --service-unit-name {} --message-size 16 --quantity 1'
    ' --interval-msec 10 --cool-down-delay-sec 10'
)

# Subscriber starts and tries to read a single message.  It exits either when it gets
# a message, or when the publisher exits and destroys the publication.
SUB_COMMAND = (
    '/system/bin/sdv_comms_client_rs subscribe --instance-name subscriber'
    ' --service-unit-name {} --quantity 1 {}'
)

SUB_EXPECTED_RESULT = """
{{
    "Status":"Ok",
    "Result":{{
        "duration_msec":\d+,
        "messages_read":{},
        "messages_missed":0,
        "messages_corrupted":0,
        "messages_out_of_order":0,
        "first_message":\d+
    }}
}}
"""

class SdvSampleCommStackTwoVMFetchLastOnSubscribeTest(sdv_base_test.SdvBaseTestClass):
  def setup_class(self):
    super().setup_class()
    self.sdv_device_1 = self.get_device('device1').adb()
    self.sdv_device_2 = self.get_device('device2').adb()
    # TODO(b/395067075): Update test to work with authz enabled
    self.original_authz_value_d1 = self.sdv_device_1.execute_shell_command(
        'getprop sdv.authz.enable'
    )
    logging.info(
      f'Saving d1 original sdv.authz.enable value: {self.original_authz_value_d1}'
    )
    self.original_authz_value_d2 = self.sdv_device_2.execute_shell_command(
        'getprop sdv.authz.enable'
    )
    logging.info(
          f'Saving d2 original sdv.authz.enable value: {self.original_authz_value_d2}'
    )
    self.sdv_device_1.execute_shell_command('setprop sdv.authz.enable false')
    self.sdv_device_2.execute_shell_command('setprop sdv.authz.enable false')


  def test_comm_stack_two_vm_fetch_last_message_on_subscribe_enabled_test(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )

    # Start a publisher that writes a message, then sleeps for 10s before exiting
    result_pub = self.sdv_device_1.execute_shell_command_in_subprocess_log(
      PUB_COMMAND.format(TOPIC_1_INSTANCE_NAME)
    )

    # Sleep 5 seconds to ensure publisher has started and published.
    time.sleep(5)

    # Now we try to subscribe to the topic from another VM with fetch_last_message set.
    result_sub = self.sdv_device_2.execute_shell_command(
      SUB_COMMAND.format(TOPIC_1_INSTANCE_NAME, "--fetch-last-message")
    )
    # We should see the single published message since fetch_last_message was enabled.
    expected_result = SUB_EXPECTED_RESULT.format(1)
    asserts.assert_true(
      helper.is_log_matching(expected_result, result_sub),
      'Subscriber with fetch_last_message=true did not get expected result!\n' +
         f'Expected result : {expected_result} \n matching failed with actual result: {result_sub}'
    )

  def test_comm_stack_two_vm_fetch_last_message_on_subscribe_disabled_test(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )

    # Start a publisher that writes a message, then sleeps for 10s before exiting.
    result_pub = self.sdv_device_1.execute_shell_command_in_subprocess_log(
      PUB_COMMAND.format(TOPIC_2_INSTANCE_NAME)
    )

    # Sleep 5 seconds to ensure publisher has started and published its message.
    time.sleep(5)

    # Now we try to subscribe to the topic from another VM with fetch_last_message unset.
    result_sub = self.sdv_device_2.execute_shell_command(
      SUB_COMMAND.format(TOPIC_2_INSTANCE_NAME, "")
    )

    # We should see no message since fetch_last_message was disabled.
    expected_result = SUB_EXPECTED_RESULT.format(0)
    asserts.assert_true(
      helper.is_log_matching(expected_result, result_sub),
      'Subscriber with fetch_last_message=false did not get expected result!\n' +
         f'Expected result : {expected_result} \n matching failed with actual result: {result_sub}'
    )

  def teardown_class(self):
    # TODO(b/395067075): Update test to work with authz enabled
    logging.info('Restoring original sdv.authz.enable settings')
    self.sdv_device_1.execute_shell_command(
      f'setprop sdv.authz.enable {self.original_authz_value_d1}'
    )
    self.sdv_device_2.execute_shell_command(
      f'setprop sdv.authz.enable {self.original_authz_value_d2}'
    )
    super().teardown_class()

if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework
  sdv_test_runner.run()
