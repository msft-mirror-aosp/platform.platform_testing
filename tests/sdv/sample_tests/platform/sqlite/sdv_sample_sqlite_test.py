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

"""SDV sample SQLite test"""

from mobly import asserts
import logging
import time
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleSQLiteTest(sdv_base_test.SdvBaseTestClass):

  START_SAMPLE_COMMAND = 'sdv_service_bundle start local-vm:com.sdv.google.sample.sqlite.{service_bundle}/instance-1'
  DESTROY_SAMPLE_COMMAND = 'sdv_service_bundle destroy local-vm:com.sdv.google.sample.sqlite.{service_bundle}/instance-1'
  RUST_SERVICE_BUNDLE = 'SQliteRustSampleServiceBundle'
  CPP_SERVICE_BUNDLE = 'SQliteCppSampleServiceBundle'
  LOGCAT_GREP_TEXT_RUST = 'sqlite_rust_sample'
  LOGCAT_EXPECTED_TEXT_FOR_RUST_SAMPLE = 'Users stored in the database: [User { id: 1, name: "Monsieur Durand" }, User { id: 2, name: "Erika Mustermann" }, User { id: 3, name: "John Doe" }]'
  LOGCAT_EXPECTED_TEXT_FOR_CPP_SAMPLE = 'user ID: 3, user name: John Doe'


  def setup_class(self):
    super().setup_class()
    self.sdv_device = self.get_device('device1').adb()

  def teardown_class(self):
    # Clean up the started service bundles so the next test run starts from a clean state.
    self.destroy_sqlite_sample_service_bundles()
    super().teardown_class()

  def destroy_sqlite_sample_service_bundles(self):
    self.sdv_device.execute_shell_command_in_subprocess("cpp_sqlite_sample", self.DESTROY_SAMPLE_COMMAND.format(service_bundle=self.CPP_SERVICE_BUNDLE))
    self.sdv_device.execute_shell_command_in_subprocess("rust_sqlite_sample", self.DESTROY_SAMPLE_COMMAND.format(service_bundle=self.RUST_SERVICE_BUNDLE))

  def test_sqlite(self):
    logging.info('Start SdvSampleSQLite Test')

    logging.info('Start rust sample -- create and write entries into a database')
    self.sdv_device.execute_shell_command_in_subprocess("rust_sqlite_sample", self.START_SAMPLE_COMMAND.format(service_bundle=self.RUST_SERVICE_BUNDLE))
    logging.info('checking logcat if database entries were written')
    logcat_result = self.sdv_device.grep_from_logcat(self.LOGCAT_GREP_TEXT_RUST)
    asserts.assert_in(
          self.LOGCAT_EXPECTED_TEXT_FOR_RUST_SAMPLE,
          logcat_result,
          f'The content was not found in the log',
          )

    logging.info('Start cpp sample -- open and read a database')
    self.sdv_device.execute_shell_command_in_subprocess("cpp_sqlite_sample", self.START_SAMPLE_COMMAND.format(service_bundle=self.CPP_SERVICE_BUNDLE))
    logging.info('checking logcat if database entries were read and are as expected')
    logcat_result = self.sdv_device.grep_from_logcat(self.CPP_SERVICE_BUNDLE)
    asserts.assert_in(
          self.LOGCAT_EXPECTED_TEXT_FOR_CPP_SAMPLE,
          logcat_result,
          f'The content was not found in the log',
          )

if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework
  sdv_test_runner.run()
