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

"""SDV E2E Hm Qos Monitoring Test"""


from mobly import asserts
import logging
import time

from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.verification import polling
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


class SDVE2EHmQosMonitoringTest(sdv_base_test.SdvBaseTestClass):

    HM_BINDER_NAME = "com.google.sdv.ISdvAgent/hm"
    DEVICE_NAME = "device1"

    SUBSCRIBER_FQIN = "ignored:com.android.sdv.sample.oem.health.qos_monitoring.QosMonitoredSubscriberBundle/instance1"
    PUBLISHER_FQIN = "ignored:com.android.sdv.sample.oem.health.qos_monitoring.QosMonitoredPublisherBundle/instance1"

    ENTRY_1_1 = (
        "* HB topic: qos-hb-right-fog-light-status\n"
        "* HB unit_name: com-android-sdv-health-qos-heartbeat-qos-hb-right-fog-light-status\n"
        "* FQIN of receiver: instance1:com.android.sdv.sample.oem.health.qos_monitoring.QosMonitoredSubscriberBundle/instance1\n"
        "MONITOR INTERNAL DATA:\n"
        "health status: Ok(QosStatus { hb_frequency_violation_detected: false, wire_frequency_violation_detected: false, latency_violation_detected: false })\n"
        "task running: true"
    )

    ENTRY_1_2 = (
        "* HB topic: qos-hb-left-fog-light-status\n"
        "* HB unit_name: com-android-sdv-health-qos-heartbeat-qos-hb-left-fog-light-status\n"
        "* FQIN of receiver: instance1:com.android.sdv.sample.oem.health.qos_monitoring.QosMonitoredSubscriberBundle/instance1\n"
        "MONITOR INTERNAL DATA:\n"
        "health status: Ok(QosStatus { hb_frequency_violation_detected: true, wire_frequency_violation_detected: false, latency_violation_detected: false })\n"
        "task running: true"
    )

    ENTRY_1_3 = (
        "* HB topic: qos-hb-gearbox-status\n"
        "* HB unit_name: com-android-sdv-health-qos-heartbeat-qos-hb-gearbox-status\n"
        "* FQIN of receiver: instance1:com.android.sdv.sample.oem.health.qos_monitoring.QosMonitoredSubscriberBundle/instance1\n"
        "MONITOR INTERNAL DATA:\n"
        "health status: Ok(QosStatus { hb_frequency_violation_detected: false, wire_frequency_violation_detected: false, latency_violation_detected: true })\n"
        "task running: true"
    )

    ENTRY_2_1 = (
        "* HB topic: qos-hb-right-fog-light-status\n"
        "* HB unit_name: com-android-sdv-health-qos-heartbeat-qos-hb-right-fog-light-status\n"
        "* FQIN of receiver: instance1:com.android.sdv.sample.oem.health.qos_monitoring.QosMonitoredSubscriberBundle/instance1\n"
        "MONITOR INTERNAL DATA:\n"
        "health status: Ok(QosStatus { hb_frequency_violation_detected: true, wire_frequency_violation_detected: false, latency_violation_detected: false })\n"
        "task running: true"
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
        super().teardown_class()

    def teardown_test(self):
        self.sdv_device.execute_shell_command(
            f"sdv_service_bundle stop {self.PUBLISHER_FQIN}")
        self.sdv_device.execute_shell_command(
            f"sdv_service_bundle stop {self.SUBSCRIBER_FQIN}")
        super().teardown_test()

    def _get_hm_dump(self):
        return self.sdv_device.dumpsys(self.HM_BINDER_NAME)

    def test_qos_monitoring(self):
        logging.info("Starting test_qos_monitoring")

        # 1. Start subscriber first.
        logging.info("Starting subscriber bundle")
        self.sdv_device.execute_shell_command(
            f"sdv_service_bundle start {self.SUBSCRIBER_FQIN}")

        # 2. Start publisher bundle.
        logging.info("Starting publisher bundle")
        self.sdv_device.execute_shell_command(
            f"sdv_service_bundle start {self.PUBLISHER_FQIN}")

        # 3. Check 1: verify 3 entries are present.
        logging.info("Checking for healthy and unhealthy QoS entries")
        polling.wait_for_true(
            lambda: self.ENTRY_1_1 in self._get_hm_dump(),
            timeout=30,
            assert_msg=f"Expected entry:\n{self.ENTRY_1_1}\n not found in HM dumpsys. Current dump:\n{self._get_hm_dump()}"
        )
        polling.wait_for_true(
            lambda: self.ENTRY_1_2 in self._get_hm_dump(),
            timeout=30,
            assert_msg=f"Expected entry:\n{self.ENTRY_1_2}\n not found in HM dumpsys. Current dump:\n{self._get_hm_dump()}"
        )
        polling.wait_for_true(
            lambda: self.ENTRY_1_3 in self._get_hm_dump(),
            timeout=30,
            assert_msg=f"Expected entry:\n{self.ENTRY_1_3}\n not found in HM dumpsys. Current dump:\n{self._get_hm_dump()}"
        )

        # 4. Stop publisher bundle.
        logging.info("Stopping publisher bundle")
        self.sdv_device.execute_shell_command(
            f"sdv_service_bundle stop {self.PUBLISHER_FQIN}")

        # 5. Check 2: verify old entries are gone, and Entry 2.1 is present.
        logging.info(
            "Verifying entries after stopping publisher bundle")
        polling.wait_for_true(
            lambda: self.ENTRY_1_1 not in self._get_hm_dump() and
            self.ENTRY_1_2 not in self._get_hm_dump() and
            self.ENTRY_1_3 not in self._get_hm_dump() and
            self.ENTRY_2_1 in self._get_hm_dump(),
            timeout=30,
            assert_msg=f"Unexpected HM dumpsys state after stopping publisher. Current dump: {self._get_hm_dump()}"
        )

        logging.info("test_qos_monitoring passed")


if __name__ == '__main__':
    sdv_test_runner.run()
