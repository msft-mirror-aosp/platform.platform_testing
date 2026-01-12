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

""" Note: metrics should be collected on sdv_core_perf* image.
Depends on CustomModeDispatcherServiceBundle, available only on perf image
"""

from dataclasses import dataclass
from enum import Enum
import logging
import time
from typing import Callable

from common_hw.arm_hardware_suspend_resume import SdvQnxDevice
from common_hw.arm_hardware_suspend_resume import suspend_and_resume_device
from mobly import asserts
import numpy as np
from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


# Python 3.10 style StrEnum, CI runner uses 3.10
class StrEnum(str, Enum):
    pass


class BundleState(StrEnum):
    STARTED = "Started"
    CREATED = "Created"
    DESTROYED = "Destroyed"


@dataclass
class MetricConfiguration:
    name: str
    start_event_re: str
    end_event_re: str


class BundleManagementMetricsCollectionTest(sdv_base_test.SdvBaseTestClass):
    NR_MEASUREMENTS = 2

    def setup_class(self):
        super().setup_class()
        self.sdv_device_adb = self.get_device('device1').adb()
        self.metrics_dict = {}

        self.architecture = self.sdv_device_adb.prop.get(SdvDeviceProperty.CPU_ARCH)

        # logd spam causes logcat to miss logs. Reduce verbosity, as test relies on logged events:
        self.sdv_device_adb.prop.set(SdvDeviceProperty.LOG_TAG, "I")

    def teardown_class(self):
        perfetto_trace_processor.export_to_crystalball(
            self.metrics_dict,
            self.sdv_device_adb.log_path(),
            "bundle_management",
            omit_base_name=False,
        )
        super().teardown_class()

    def set_and_wait_for_default_vm_state(self):
        # clear logat, wait for main ring buffer
        self.sdv_device_adb.clear_logcat()
        self._poll_logcat_grep(re="beginning of main")

        # reset any vpm state persisted by previous test
        self.sdv_device_adb.prop.set(SdvDeviceProperty.VPM_POWER_STATE, "POWER_OFF_EXIT")

        self.sdv_device_adb.reboot_device_and_verify_logcat()

        self._poll_logcat_grep(
            r"Finished enforcing mode .*POWER_OFF_EXIT")
        self._poll_orch_dump_bundle_state(BundleState.STARTED)

    def test_collect_bundle_metrics_cold_boot(self):
        self.collect_metrics(
            state_setup_command=self.set_and_wait_for_default_vm_state,
            configs=[
                MetricConfiguration(
                    name="cold_boot_process_bundles_",
                    start_event_re=r"VPM callback received vpm update.*POWER_OFF_EXIT",
                    end_event_re=r"Finished enforcing mode .*POWER_OFF_EXIT",
                )
            ],
        )

    def test_collect_bundle_metrics_suspend_to_ram(self):
        def reboot_and_change_vpm_power_state():
            self.set_and_wait_for_default_vm_state()
            session = self.sdv_device_adb.interactive_session()
            # VPM logic allows ram suspension only post power-on state, move to power-on first:
            session.send_command_and_wait_for_outputs(
                "vepsm power-state power-on",
                ["Received power-state-report from VPM ON , reason HOST_REQUESTED",]
            )
            self._poll_orch_dump_bundle_state(BundleState.STARTED)
            session.send_command_and_wait_for_outputs(
                "vepsm power-state prepare ram",
                ["Received power-state-report from VPM WAIT_FOR_FINISH",],
            )
            session.close()
            self._poll_orch_dump_bundle_state(BundleState.CREATED)
        self.collect_metrics(
            state_setup_command=reboot_and_change_vpm_power_state,
            configs=[
                MetricConfiguration(
                    name="pre_suspend_stop_bundles_",
                    start_event_re=r"VPM callback received vpm update.*SUSPEND_TO_RAM_ENTER",
                    end_event_re=r"Finished enforcing mode .*SUSPEND_TO_RAM_ENTER",
                )
            ]
        )

    def test_collect_bundle_metrics_custom_mode_destroy_bundles(self):
        def reboot_and_send_custom_mode():
            self.set_and_wait_for_default_vm_state()
            session = self.sdv_device_adb.interactive_session()
            session.send_command_and_wait_for_outputs(
                "performance_custom_mode_dispatcher destroy",
                [r"Set custom mode result was: Ok\(SetCustomStateResponse",]
            )
            session.close()
            self._poll_orch_dump_bundle_state(BundleState.DESTROYED)
        self.collect_metrics(
            state_setup_command=reboot_and_send_custom_mode,
            configs=[
                MetricConfiguration(
                    name="destroy_bundles_",
                    start_event_re=r"VPM callback received vpm update.*performance_control",
                    end_event_re=r"Finished enforcing mode .*performance_control",
                )
            ]
        )

    def test_collect_bundle_metrics_resume_from_ram_cuttlefish(self):
        def reboot_suspend_to_ram_and_resume():
            self.set_and_wait_for_default_vm_state()
            session = self.sdv_device_adb.interactive_session()
            session.send_command_and_wait_for_outputs(
                "vepsm power-state power-on",
                ["Received power-state-report from VPM ON , reason HOST_REQUESTED",]
            )
            session.send_command_and_wait_for_outputs(
                "vepsm power-state prepare ram",
                ["Received power-state-report from VPM WAIT_FOR_FINISH",],
            )
            self._poll_orch_dump_bundle_state(BundleState.CREATED)
            self.sdv_device_adb.execute_shell_command("rtcwake -m no -s 5")
            session.send_command_and_wait_for_outputs(
                "vepsm power-state finish ram",
                ["Received power-state-report from VPM SUSPEND_TO_RAM_EXIT , reason HOST_REQUESTED",],
            )
            session.close()

            self._poll_orch_dump_bundle_state(BundleState.STARTED)

        asserts.skip_if(self.architecture != 'x86_64', 'Device architecture is not x86_64')

        configs = [
            MetricConfiguration(
                name="orch_start_bundle_post_resume_",
                start_event_re=r"VPM callback received vpm update.*SUSPEND_TO_RAM_EXIT",
                end_event_re=r"Finished enforcing mode .*SUSPEND_TO_RAM_EXIT"
            ),
            MetricConfiguration(
                name="notify_oem_post_resume_",
                start_event_re=r"sdv_vpm::power_state: sdv_vpm_agent has resumed successfully",
                end_event_re=r"sdv_vpm::power_service: Successfully notified all callbacks, notifying OEM for SUSPEND_TO_RAM_EXIT!"
            ),
            MetricConfiguration(
                name="full_start_bundle_post_resume_",
                start_event_re=r"pm_system_irq_wakeup:.*triggered",
                end_event_re=r"Finished enforcing mode .*SUSPEND_TO_RAM_EXIT"
            )
        ]

        self.collect_metrics(
            state_setup_command=reboot_suspend_to_ram_and_resume,
            configs=configs
        )

    def test_collect_bundle_metrics_resume_from_ram_arm64(self):
        def reboot_suspend_to_ram_and_resume():
            self.set_and_wait_for_default_vm_state()
            suspend_and_resume_device(
                SdvQnxDevice(
                    adb_device=self.sdv_device_adb,
                    qnx_guest_id=1
                )
            )
            self._poll_orch_dump_bundle_state(BundleState.STARTED)

        asserts.skip_if(self.architecture != 'arm64-v8a', 'Device architecture is not arm64-v8a')

        configs = [
            MetricConfiguration(
                name="orch_start_bundle_post_resume_",
                start_event_re=r"VPM callback received vpm update.*SUSPEND_TO_RAM_EXIT",
                end_event_re=r"Finished enforcing mode .*SUSPEND_TO_RAM_EXIT"
            ),
            MetricConfiguration(
                name="notify_oem_post_resume_",
                start_event_re=r"sdv_vpm::power_state: sdv_vpm_agent has resumed successfully",
                end_event_re=r"sdv_vpm::power_service: Successfully notified all callbacks, notifying OEM for SUSPEND_TO_RAM_EXIT!"
            ),
            MetricConfiguration(
                name="full_start_bundle_post_resume_",
                start_event_re=r"pm_system_irq_wakeup:.*triggered",
                end_event_re=r"Finished enforcing mode .*SUSPEND_TO_RAM_EXIT"
            )
        ]

        self.collect_metrics(
            state_setup_command=reboot_suspend_to_ram_and_resume,
            configs=configs
        )

    def collect_metrics(self, state_setup_command, configs: list[MetricConfiguration]):
        ds = {}
        for _ in range(self.NR_MEASUREMENTS):
            state_setup_command()
            for config in configs:
                ds[config.name] = self.get_duration_between_log_events(
                    config.start_event_re, config.end_event_re
                )

        for metric_name, d in ds.items():
            for key, value in self._calculate_avg_max_p90(d).items():
                self.metrics_dict[metric_name + key] = value

    def _calculate_avg_max_p90(self, d: list[float]):
        """calculates metrics for a given distribution"""
        d_array = np.array(d)
        d_avg = np.mean(d_array)
        d_max = np.max(d_array)
        d_p90 = np.percentile(d_array, 90)

        return {
            "avg_s": d_avg,
            "max_s": d_max,
            "p90_s": d_p90
        }

    def get_duration_between_log_events(self, start_event_re: str, end_event_re: str):
        ts = self._get_log_message_timestamp(start_event_re)
        te = self._get_log_message_timestamp(end_event_re)
        return te - ts

    def _get_log_message_timestamp(self, re: str):
        log_grep_last_line = self._poll_logcat_grep(re).splitlines()[-1]
        time_s = float(log_grep_last_line.split()[0])
        return time_s

    def _poll_logcat_grep(self, re):
        lines = BundleManagementMetricsCollectionTest._poll_fun(
            lambda: self.sdv_device_adb.grep_from_logcat(
                re, logcat_args="-v time,monotonic", grep_args="-E"),)
        if lines:
            logging.info(
                f'Found lines "{lines}" for regex "{re}"')
            return lines
        else:
            asserts.fail(
                f'Found no loglines for regex {re}!')

    def _poll_orch_dump_bundle_state(self, expected_state: BundleState):
        def is_expected_state() -> bool:
            BUNDLE_NAME_PATTERN = "com.sdv.google.performance.apex"
            dump = self.sdv_device_adb.execute_shell_command(
                "dumpsys com.google.sdv.ISdvAgent/orch"
            )
            for line in dump.splitlines():
                if BUNDLE_NAME_PATTERN in line and expected_state not in line:
                    logging.info(
                        f"Unexpected orch dump report. Expected: {expected_state}, Dump: {dump}")
                    return False
            return True

        if BundleManagementMetricsCollectionTest._poll_fun(is_expected_state):
            logging.info(
                f"Orch reports perf bundles in expected state: {expected_state}")
        else:
            asserts.fail(
                "Orch reports perf bundles in unexpected state")

    def _poll_fun(fun: Callable[[], str | bool | None], timeout: int = 30):
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            lines = fun()
            if lines:
                return lines
            time.sleep(0.5)

    def teardown_test(self):
        super().teardown_test()


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
