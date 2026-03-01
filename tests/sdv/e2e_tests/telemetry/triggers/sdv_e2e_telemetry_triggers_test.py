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

"""E2E Tests validating trigger usage scenarios in Telemetry Service"""

from datetime import timedelta
from pathlib import Path
from typing import List, Optional
from mobly import asserts
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


# Validate that subscribers of Start Trigger act when it fires
class ActOnStartTriggerParams:
    SIMULATION_TIME = timedelta(seconds=2)
    METRICS_CONFIG_UUID = "f630897c-44c3-48c1-be0f-2f76d12398f5"
    METRICS_REPORT_NAMES = [
        "on_start_trigger",
        "on_aggregation_publisher",
        "on_conditional_trigger",
    ]
    METRICS_CONFIG_FILE_NAME = "act_on_start_trigger.textproto"
    HOST_METRICS_CONFIG_PATH = (
        Path("act_on_start_trigger") / METRICS_CONFIG_FILE_NAME
    )
    METRICS_PUBLISHER_PATHS = []


# Aggregation, reporting and finishing the Metrics Config are happening under
# the same trigger.
class AggregateReportFinishParams:
    SIMULATION_TIME = timedelta(seconds=2)
    METRICS_CONFIG_UUID = "27db53b8-ece1-4007-b259-bac50bbcaacf"
    METRICS_REPORT_NAME = "report"
    METRICS_CONFIG_FILE_NAME = "aggregate_report_finish_config.textproto"
    HOST_METRICS_CONFIG_PATH = (
        Path("aggregate_report_finish") / METRICS_CONFIG_FILE_NAME
    )
    METRICS_PUBLISHER_PATHS = [Path("acceleration_publisher.textproto")]


# A scenario to calculate the distance traveled during the journey.
class DistanceTraveledParams:
    SIMULATION_TIME = timedelta(seconds=2)
    METRICS_CONFIG_UUID = "369aae14-ab05-4b7e-89e9-fb11aac43917"
    METRICS_REPORT_NAME = "report"
    METRICS_CONFIG_FILE_NAME = "distance_traveled.textproto"
    HOST_METRICS_CONFIG_PATH = (
        Path("distance_traveled") / METRICS_CONFIG_FILE_NAME
    )
    METRICS_PUBLISHER_PATHS = [
        Path("distance_publisher.textproto"),
        Path("event_publisher.textproto"),
    ]


class JourneyStatusFirstOffParams:
    SIMULATION_TIME = timedelta(seconds=1)
    METRICS_CONFIG_UUID = "bcbffd01-42ba-4444-ae35-908b907c48b8"
    METRICS_REPORT_NAME = "report"
    METRICS_CONFIG_FILE_NAME = "journey_status.textproto"
    HOST_METRICS_CONFIG_PATH = Path("journey_status") / METRICS_CONFIG_FILE_NAME
    METRICS_PUBLISHER_PATHS = [
        Path("journey_source_publisher_first_off.textproto")
    ]
    SIM_ACTIONS_FILE_NAME = "journey_config_sim_actions.textproto"


class JourneyStatusFirstOnParams:
    SIMULATION_TIME = timedelta(seconds=1)
    METRICS_CONFIG_UUID = "bcbffd01-42ba-4444-ae35-908b907c48b8"
    METRICS_REPORT_NAME = "report"
    METRICS_CONFIG_FILE_NAME = "journey_status.textproto"
    HOST_METRICS_CONFIG_PATH = Path("journey_status") / METRICS_CONFIG_FILE_NAME
    METRICS_PUBLISHER_PATHS = [
        Path("journey_source_publisher_first_on.textproto"),
    ]
    SIM_ACTIONS_FILE_NAME = "journey_config_sim_actions.textproto"


# Validate that the data collection is performed only in-between Start and End
# Trigger and that Finish Trigger deactivates the Metrics Config.
class LifecycleTriggersParams:
    SIMULATION_TIME = timedelta(seconds=5)
    METRICS_CONFIG_UUID = "27f28563-9352-4f39-86d9-d5e67e793084"
    METRICS_REPORT_NAME = "report"
    METRICS_CONFIG_FILE_NAME = "lifecycle_triggers.textproto"
    HOST_METRICS_CONFIG_PATH = (
        Path("lifecycle_triggers") / METRICS_CONFIG_FILE_NAME
    )
    METRICS_PUBLISHER_PATHS = [Path("lifecycle_publisher.textproto")]


# Tests that Periodic Triggers are not ticking until the Start Trigger has
# fired.
class PeriodicTriggerParams:
    SIMULATION_TIME = timedelta(seconds=2)
    METRICS_CONFIG_UUID = "91a83072-fb29-4c85-822e-06b8290f9e50"
    METRICS_REPORT_NAME = "report"
    METRICS_CONFIG_FILE_NAME = "periodic_trigger.textproto"
    HOST_METRICS_CONFIG_PATH = (
        Path("periodic_trigger") / METRICS_CONFIG_FILE_NAME
    )
    METRICS_PUBLISHER_PATHS = []


class SdvE2ETelemetryTriggersTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp")
    _SIMULATOR_OUT_DIR = _SIMULATION_CONFIG_DIR / "out"

    def _get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        simulation_time: timedelta,
        metrics_config_file_name: str,
        publisher_config_paths: List[Path],
        sim_actions_file_name: Optional[str],
    ) -> str:
        # TODO: b/461465688: Prettify this by moving the command creation to the
        # Telemetry Framework

        command = [
            self.get_simulator_binary(device),
            "--max-simulation-time",
            f"seconds:{simulation_time.total_seconds()}",
        ]

        if publisher_config_paths:
            command.append("full-simulation")
            command.extend([
                "--publisher-configs",
                *[
                    str(self._SIMULATION_CONFIG_DIR / p)
                    for p in publisher_config_paths
                ],
            ])
        else:
            command.append("telemetry-client")

        command.extend([
            "--metrics-configs",
            str(self._SIMULATION_CONFIG_DIR / metrics_config_file_name),
            "--output-directory",
            str(self._SIMULATOR_OUT_DIR),
        ])

        if sim_actions_file_name:
            command.extend([
                "--simulation-actions",
                str(self._SIMULATION_CONFIG_DIR / sim_actions_file_name),
            ])

        cd_command = shlex_join(["cd", str(self._SIMULATION_CONFIG_DIR)])
        simulator_command = shlex_join(command)
        return f"{cd_command} && {simulator_command}"

    def setup_class(self):
        super().setup_class()
        # Custom setup here
        self.sdv_device = self.get_device("device1")
        self.sdv_device.adb().root_device()

    def teardown_class(self):
        # Custom teardown here
        super().teardown_class()

    def setup_test(self):
        super().setup_test()
        # Custom setup here

    def teardown_test(self):
        # Custom teardown here
        super().teardown_test()

    # The test verifies that Aggregation Publishers, Conditional Triggers and
    # Metrics Report Generators which are subscribed to a trigger registered as
    # Start Trigger are able to receive notifications when this trigger fires.
    #
    # The Metrics Config contains each of these 3 object types having name of
    # the trigger used as Start Trigger in their trigger_names field, and there
    # are 3 Metrics Reports Generator which are used as a way to verify that
    # objects are indeed notified on the fire event, i.e. they perform an
    # aggregation, evaluate the underlying condition and generate the report
    # correspondingly.
    def test_act_on_start_trigger(self):
        self._run_simulation(ActOnStartTriggerParams)

        for report_name in ActOnStartTriggerParams.METRICS_REPORT_NAMES:
            report_count = len(
                self.find_report_paths(
                    device=self.sdv_device,
                    simulator_out_dir=self._SIMULATOR_OUT_DIR,
                    config_uuid=ActOnStartTriggerParams.METRICS_CONFIG_UUID,
                    report_name=report_name,
                )
            )
            asserts.assert_equal(
                report_count,
                1,
                "Expected to generate a single Metrics Report with name:"
                f" {report_name}",
            )

    # The test verifies that an Aggregation Publisher and Metrics Report
    # Generator sharing the same trigger which is also used as Finish Trigger
    # perform their actions on trigger fire event deterministically. That means
    # the publisher performs its aggregation first, and the generator (which
    # refers to the publisher in its message builder) considers the freshly
    # aggregated value. Finish Trigger, in its turn, waits until the report is
    # generated before deactivating the config.
    #
    # The corresponding Metrics Configuration refers to the following scenario:
    # "Aggregate vehicle's acceleration in a list every 300 ms. When 1 second
    # passes, append the current acceleration to this list, report it, and
    # finish the computations".
    def test_perform_aggregation_reporting_on_finish_trigger_firing(self):
        self._run_simulation(AggregateReportFinishParams)

        report_payload = self._read_report(
            config_uuid=AggregateReportFinishParams.METRICS_CONFIG_UUID,
            report_name=AggregateReportFinishParams.METRICS_REPORT_NAME,
            report_number=1,
            host_metrics_config_path=AggregateReportFinishParams.HOST_METRICS_CONFIG_PATH,
        )

        asserts.assert_equal(
            report_payload.value, [1, 1, -2, -2], "Unexpected report value"
        )

    # Verifies the intended behavior of Start, End and Finish Triggers. In
    # particular, when Start Trigger fires, it should start data collection and
    # activate End Trigger. Similarly, End Trigger fire event should pause data
    # collection and activate Start Trigger back. Finish Trigger finishes the
    # Metrics Config, i.e. it completely stops data collection and deactivates
    # the config.
    #
    # The Metrics Config used contains Start Trigger being Periodic Trigger with
    # an interval of 500ms, and End Trigger being Periodic Trigger with an
    # interval of 400ms. Finish Trigger is a Periodic Trigger with interval of
    # 2s, and simulation duration is set to 5s. That all means data collection
    # should be active during the intervals [500ms; 900ms] and [1400ms; 1800ms].
    # The Simulation Publisher is publishing 30 increasing integers starting
    # with 0 every 100ms with the first message published at 50ms, so this way
    # the risk of flakiness is minimized.
    #
    # 0ms     200 ms      500ms          900ms                   1400ms               1800ms
    #  v       v           |xxxxxxxxxxxxxxx|                       |xxxxxxxxxxxxxxxxxxx|
    #  | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 |
    #    ^   ^-- message "1" is published at 150ms.                                              ^
    #    |                                                     Finish Trigger fires at 2000ms --/
    #    L------ message "0" is publisher at 50ms.
    #
    def test_lifecycle_triggers(self):
        self._run_simulation(LifecycleTriggersParams)

        # Validate that exactly 2 Metrics Report were generated
        report_count = len(
            self.find_report_paths(
                device=self.sdv_device,
                simulator_out_dir=self._SIMULATOR_OUT_DIR,
                config_uuid=LifecycleTriggersParams.METRICS_CONFIG_UUID,
                report_name=LifecycleTriggersParams.METRICS_REPORT_NAME,
            )
        )
        asserts.assert_equal(
            report_count, 2, "Expected to generate two Metrics Reports"
        )

        # Validate the contents of the first Metrics Report
        report_payload = self._read_report(
            config_uuid=LifecycleTriggersParams.METRICS_CONFIG_UUID,
            report_name=LifecycleTriggersParams.METRICS_REPORT_NAME,
            report_number=1,
            host_metrics_config_path=LifecycleTriggersParams.HOST_METRICS_CONFIG_PATH,
        )
        asserts.assert_equal(
            report_payload.value,
            [5, 6, 7, 8],
            "Unexpected report value",
        )

        # Validate the contents of the second Metrics Report
        report_payload = self._read_report(
            config_uuid=LifecycleTriggersParams.METRICS_CONFIG_UUID,
            report_name=LifecycleTriggersParams.METRICS_REPORT_NAME,
            report_number=2,
            host_metrics_config_path=LifecycleTriggersParams.HOST_METRICS_CONFIG_PATH,
        )
        asserts.assert_equal(
            report_payload.value,
            [5, 6, 7, 8, 14, 15, 16, 17],
            "Unexpected report value",
        )

    # The Metrics config used in the test has two Periodic Triggers. The first
    # trigger has its interval of ticking set to 400ms and is also used as a
    # Start Trigger. This in particular means the trigger should only ever tick
    # once as no End Trigger is set in the config, and, as it is a Start Trigger
    # with no additional subscribers, it gets deactivated after firing. The
    # second Periodic Trigger is used to generate a report each 600ms since the
    # data collection starts i.e. since Start Trigger fires. As the simulation
    # duration is 2 seconds, this trigger should only fire twice (at timestamps
    # 1000ms and 1600ms) producing 2 Metrics Reports.
    def test_periodic_triggers_activation_time(self):
        self._run_simulation(PeriodicTriggerParams)

        # Validate that the Periodic Trigger has fired exactly twice.
        report_count = len(
            self.find_report_paths(
                device=self.sdv_device,
                simulator_out_dir=self._SIMULATOR_OUT_DIR,
                config_uuid=PeriodicTriggerParams.METRICS_CONFIG_UUID,
                report_name=PeriodicTriggerParams.METRICS_REPORT_NAME,
            )
        )
        asserts.assert_equal(
            report_count,
            2,
            "The periodic trigger should have fired exactly twice "
            "producing 2 Metrics Reports",
        )

    # The corresponding Metrics Configuration represents the following scenario:
    # "Calculate the distance a vehicle traveled between journey start and end".
    # Aggregation Publisher subscribed to Start Trigger should be able to
    # perform the aggregation when the trigger fires.
    def test_distance_traveled(self):
        self._run_simulation(DistanceTraveledParams)

        report_payload = self._read_report(
            config_uuid=DistanceTraveledParams.METRICS_CONFIG_UUID,
            report_name=DistanceTraveledParams.METRICS_REPORT_NAME,
            report_number=1,
            host_metrics_config_path=DistanceTraveledParams.HOST_METRICS_CONFIG_PATH,
        )
        asserts.assert_equal(report_payload.value, 7, "Unexpected report value")

    # Test's metrics configuration contains a data source which publishes
    # current vehicle journey status. The test verifies that Telemetry Service
    # is properly tracking status changes.
    #
    # The test is set up in a way that metrics configuration is only added and
    # activated after some status has already been published (via simulation
    # actions configuration).
    #
    # Two similar sequences of statuses are being tested, with a difference in
    # the status published before the configuration activation. The
    # configuration produces a metrics report on each new journey start and end.
    def test_journey_status(self):
        self.enter_context(self.disable_authz(self.sdv_device))

        for params in [JourneyStatusFirstOffParams, JourneyStatusFirstOnParams]:
            self.sdv_device.adb().log().info(
                "Running test_journey_status with parameters:"
                f" {params.__name__}"
            )
            self._run_simulation(params)

            report_count = len(
                self.find_report_paths(
                    device=self.sdv_device,
                    simulator_out_dir=self._SIMULATOR_OUT_DIR,
                    config_uuid=params.METRICS_CONFIG_UUID,
                    report_name=params.METRICS_REPORT_NAME,
                )
            )
            asserts.assert_equal(
                report_count,
                6,
                "Expected 6 metrics reports: there are 3 intervals of data"
                " collection, with reports produced on its start and end",
            )
            # Clear output directory as the metrics config is shared between both tests
            self.sdv_device.adb().execute_shell_command(
                f"rm -rf {self._SIMULATOR_OUT_DIR}"
            )

    def _run_simulation(self, params):
        self.sdv_device.adb().log().info("Starting Simulator")

        self.sdv_device.adb().execute_shell_command(
            self._get_simulator_command(
                self.sdv_device,
                params.SIMULATION_TIME,
                params.METRICS_CONFIG_FILE_NAME,
                params.METRICS_PUBLISHER_PATHS,
                getattr(params, "SIM_ACTIONS_FILE_NAME", None),
            )
        )
        self.sdv_device.adb().log().info("Simulation finished")

    def _read_report(
        self,
        config_uuid: str,
        report_name: str,
        report_number: int,
        host_metrics_config_path: Path,
    ):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=config_uuid,
            report_name=report_name,
            report_number=report_number,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        metrics_config = self.parse_textproto_metrics_config(
            host_metrics_config_path
        )
        report_payload = self.decode_report_payload(
            metrics_config.descriptor_protos, report
        )
        return report_payload


if __name__ == "__main__":
    sdv_test_runner.run()
