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

"""SDV HM Agent Dumpsys Report Test"""


from mobly import asserts
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
import random
import time


class SdvHmDumpsysOutputTest(sdv_base_test.SdvBaseTestClass):
    DUMPSYS_COMMAND = "dumpsys {binder_name}"
    MAX_INSTANCE_ID = 999999999

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()
        self.sdv_device.root_device()

    def wait_for_bundles_to_register(self, timeout=10):
        registration_proof = "Registered health configuration:"
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            if self.sdv_device.advance_logcat().nth_message(2, registration_proof) is not None:
                return
            time.sleep(0.1)
        asserts.fail(
            f"Bundles failed to register for monitoring with timeout={timeout}"
        )

    def test_health_monitor_dump_output(self):
        logging.info("Starting test_health_monitor_dump_output")

        instance_1_id = random.randint(0, self.MAX_INSTANCE_ID)
        instance_2_id = random.randint(0, self.MAX_INSTANCE_ID)
        while instance_1_id == instance_2_id:
            instance_2_id = random.randint(0, self.MAX_INSTANCE_ID)

        self.monitored_service_1_fqin = f"instance1:com.android.sdv.sample.oem.health.monitored.SampleHMBundle/i{instance_1_id}"
        self.monitored_service_2_fqin = f"instance1:com.android.sdv.sample.oem.health.monitored.SampleHMBundle/i{instance_2_id}"
        start_service_command = "sdv_service_bundle start {fqin}"
        self.destroy_service_command = "sdv_service_bundle destroy {fqin}"

        hm_binder_name = "com.google.sdv.ISdvAgent/hm"

        expected_pre_bundle_start = [
            "AGENT NAME: SDV Agent dump - Health Monitor\n"
            "AGENT FQIN:",

            "INTERNAL STATE:\n"
            "background_thread running: true\n"
            "should_run: true",

            "HEARTBEAT MONITORING:\n"
            "NO ACTIVE MONITORS",

            "RECOVERY MONITORING:\n"
            "a. Agent monitoring:",

            "MONITOR 0:",

            'ID: Agent: sdv_dt_agent\n'
            "linked_binder: google.sdv.data_tunnel.IAgentService/default\n"
            "alive: true",

            'ID: Agent: sdv_init_open_dice\n'
            "linked_binder: google.sdv.init_open_dice.IDiceChainProvider/default\n"
            "alive: true",

            'ID: Agent: sdv_sd_agent\n'
            "linked_binder: google.sdv.service_discovery.discovery.IServiceDiscoveryAgent/default\n"
            "alive: true",

            "b. SB monitoring:\n"
            "NO ACTIVE MONITORS",
        ]

        expected_post_bundle_start = [
            "AGENT NAME: SDV Agent dump - Health Monitor\n"
            "AGENT FQIN:",

            "INTERNAL STATE:\n"
            "background_thread running: true\n"
            "should_run: true",

            "HEARTBEAT MONITORING:\n",

            f"ID: FQIN: {self.monitored_service_1_fqin}\n"
            "is_healthy: Healthy\n",

            f"ID: FQIN: {self.monitored_service_2_fqin}\n"
            "is_healthy: Healthy\n",

            'ID: Agent: sdv_dt_agent\n'
            "linked_binder: google.sdv.data_tunnel.IAgentService/default\n"
            "alive: true",

            'ID: Agent: sdv_init_open_dice\n'
            "linked_binder: google.sdv.init_open_dice.IDiceChainProvider/default\n"
            "alive: true",

            'ID: Agent: sdv_sd_agent\n'
            "linked_binder: google.sdv.service_discovery.discovery.IServiceDiscoveryAgent/default\n"
            "alive: true",
        ]

        # verify dumpsys output when no service bundles are monitored:
        report = self.sdv_device.execute_shell_command(
            self.DUMPSYS_COMMAND.format(binder_name=hm_binder_name))
        for s in expected_pre_bundle_start:
            asserts.assert_in(
                s, report,
                f"Monitored bundles not active case. Did not find substring:\n{s}\n\nin dumpsys report:\n{report}"
            )

        # add monitored bundles:
        self.sdv_device.execute_shell_command(
            start_service_command.format(fqin=self.monitored_service_1_fqin))
        self.sdv_device.execute_shell_command(
            start_service_command.format(fqin=self.monitored_service_2_fqin))

        # wait for bundles to log registration with HM:
        self.wait_for_bundles_to_register()

        # dumpsys again, after bundles monitored:
        report = self.sdv_device.execute_shell_command(
            self.DUMPSYS_COMMAND.format(binder_name=hm_binder_name)
        )

        # check for some expected substrings inside report:
        for s in expected_post_bundle_start:
            asserts.assert_in(
                s, report,
                f"Monitored bundles active case. Did not find substring:{s} in dumpsys report: {report}"
            )

        logging.info("Finished")

    def teardown_test(self):
        super().teardown_test()
        self.sdv_device.execute_shell_command(
            self.destroy_service_command.format(
                fqin=self.monitored_service_1_fqin)
        )
        self.sdv_device.execute_shell_command(
            self.destroy_service_command.format(
                fqin=self.monitored_service_2_fqin)
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
