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

"""SDV Sample for running service bundle with custom user id.

Tests runs on single SDV VM.
"""
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvSampleCustomUserIdTest(
    sdv_base_test.SdvBaseTestClass
):
    ASSERT_MESSAGE = "Current process user id 7050. Expected user id to be 7050"
    ERROR_MESSAGE = '\n[FAILURE]: Current process user id message is not found'

    SERVICE_BUNDLE_FQIN = "sdv_service_bundle start local-vm:com.sdv.google.sample.lifecycle.userid.apex.LifecycleCustomUserIdSampleServiceBundle/instance-1"

    SAMPLES_LOGCAT_ARGS = "oem_service_bundle_custom_userid_sample:*"

    def create_service_bundle(self):
        return self.sdv_device.adb().execute_shell_command(self.SERVICE_BUNDLE_FQIN)

    def verify_user_id(self):
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device.adb(),
            grep_text=self.ASSERT_MESSAGE,
            logcat_args=self.SAMPLES_LOGCAT_ARGS,
            assert_msg=self.ERROR_MESSAGE,
        )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")

    def test_custom_user_id(self):
        self.log_test_info("Started")

        self.create_service_bundle()
        self.verify_user_id()

        self.log_test_info("Finished")

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
