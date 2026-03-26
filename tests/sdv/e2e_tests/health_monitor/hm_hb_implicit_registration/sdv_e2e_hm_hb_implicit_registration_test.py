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

"""SDV E2E Hm Hb Implicit Registration Test"""


from mobly import asserts
import logging
import time

from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.verification import polling
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


class SDVE2EHmHbImplicitRegistrationTest(sdv_base_test.SdvBaseTestClass):

    HM_BINDER_NAME = "com.google.sdv.ISdvAgent/hm"
    DEVICE_NAME = "device1"

    ORCH_START_COMMAND = "orch_custom_mode_sample E2E-TESTS start-for-impl-monit-test"
    ORCH_STOP_COMMAND = "orch_custom_mode_sample E2E-TESTS stop-for-impl-monit-test"

    # We match the static parts of the chunk as timestamps vary.
    EXPECTED_REPORT = (
        "ID: FQIN: instance1:com.android.sdv.sample.oem.health.qos_monitoring.QosMonitoredSubscriberBundle/instance1\n"
        "is_healthy: Healthy\n"
        "last_hb_event: Ok(TrackingHeartbeat(ReceivedHeartbeat(SystemTime { tv_sec:"
    )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device(self.DEVICE_NAME).adb()

        self.sdv_authz_enable_value = self.sdv_device.prop.get(
            SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device.prop.set(
            SdvDeviceProperty.AUTHZ_ENABLE, "permissions_only")

    def teardown_class(self):
        self.sdv_device.prop.set(
            SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value)
        self.sdv_device.execute_shell_command_in_subprocess(
            "subprocess3",
            self.ORCH_STOP_COMMAND)

        self.sdv_device.terminate_all_subprocesses()
        super().teardown_class()

    def _get_hm_dump(self):
        return self.sdv_device.dumpsys(self.HM_BINDER_NAME)

    def test_implicit_registration(self):
        logging.info("Starting test_implicit_registration")

        # 1. Send start custom mode.
        logging.info("Sending start-for-impl-monit-test custom mode")
        self.sdv_device.execute_shell_command_in_subprocess(
            "subprocess1",
            self.ORCH_START_COMMAND)

        # 2. Verify chunk is present in dumpsys.
        logging.info("Verifying implicit registration in HM dumpsys")
        polling.wait_for_true(
            lambda: self.EXPECTED_REPORT in self._get_hm_dump(),
            timeout=30,
            assert_msg=f"Expected implicit registration chunk not found in HM dumpsys. Current dump: {self._get_hm_dump()}"
        )

        # 3. Send stop custom mode.
        logging.info("Sending stop-for-impl-monit-test custom mode")
        self.sdv_device.execute_shell_command_in_subprocess(
            "subprocess2",
            self.ORCH_STOP_COMMAND)

        # 4. Verify chunk is gone.
        logging.info(
            "Verifying implicit registration is removed from HM dumpsys")
        polling.wait_for_true(
            lambda: not self.EXPECTED_REPORT in self._get_hm_dump(),
            timeout=30,
            assert_msg=f"Implicit registration chunk still present in HM dumpsys after stop. Current dump: {self._get_hm_dump()}"
        )

        logging.info("test_implicit_registration passed")


if __name__ == '__main__':
    sdv_test_runner.run()
