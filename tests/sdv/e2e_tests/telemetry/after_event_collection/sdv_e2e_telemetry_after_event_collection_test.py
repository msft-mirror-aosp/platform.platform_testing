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

"""E2E Test to validate After Event Collection functionality"""

from mobly import asserts
from datetime import timedelta
from pathlib import Path
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryAfterEventCollectionTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    _SIMULATION_TIME = timedelta(seconds=20)
    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp/")

    _METRICS_CONFIG_UUID = "1c1a37e5-f03b-4e6d-bef0-3b91db4d46f8"
    _METRICS_CONFIG_FILE_NAME = "after_event_collection_config.textproto"
    _METRICS_CONFIG_PATH = _SIMULATION_CONFIG_DIR / _METRICS_CONFIG_FILE_NAME

    _SPEED_PUBLISHER_PATH = Path("/data/local/tmp/speed.textproto")
    _NON_OVERLAPPING_EVENT_PUBLISHER_PATH = Path(
        "/data/local/tmp/non_overlapping_event_publisher.textproto"
    )
    _OVERLAPPING_EVENT_PUBLISHER_PATH = Path(
        "/data/local/tmp/overlapping_event_publisher.textproto"
    )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")
        self.sdv_device.adb().root_device()

        self.metrics_config = self.parse_textproto_metrics_config(
            Path(self._METRICS_CONFIG_FILE_NAME)
        )

    def teardown_class(self):
        # Custom teardown here
        super().teardown_class()

    def setup_test(self):
        super().setup_test()

        self._simulator_out_dir = self.enter_context(
            self.create_temp_dir(self.sdv_device)
        )

    def teardown_test(self):
        # Custom teardown here
        super().teardown_test()

    def test_non_overlapping_events_data_collection(self):
        self._run_simulation(self._NON_OVERLAPPING_EVENT_PUBLISHER_PATH)

        # Since events do not overlap (i.e. event publication timestamps differ
        # by more than data collection duration of 5s), both event series
        # consist of a single event, and thus `after_series_first_event` and
        # `after_series_last_event` collected data is equal.

        self._validate_data_collection(
            report_number=1,
            collection_type="before_event",
            expected_data=[10, 10],
        )
        self._validate_data_collection(
            report_number=1,
            collection_type="after_series_first_event",
            expected_data=[11, 11, 12, 12, 13],
        )
        self._validate_data_collection(
            report_number=1,
            collection_type="after_series_last_event",
            expected_data=[11, 11, 12, 12, 13],
        )
        self._validate_data_collection(
            report_number=2,
            collection_type="before_event",
            expected_data=[12, 12, 13, 13, 14],
        )
        self._validate_data_collection(
            report_number=2,
            collection_type="after_series_first_event",
            expected_data=[14, 15, 15, 16, 16],
        )
        self._validate_data_collection(
            report_number=2,
            collection_type="after_series_last_event",
            expected_data=[14, 15, 15, 16, 16],
        )

    def test_overlapping_events_data_collection(self):
        self._run_simulation(self._OVERLAPPING_EVENT_PUBLISHER_PATH)

        # As opposed to `after_series_*_event` collection which is only possible
        # for the first and the last event of the series, `before_event`
        # collection is only possible for every single event.
        # That's why each `after_series_*_event` report contains one report per
        # series while `before_event` reports contain a report per each event.

        # The first series of events. Validations are ordered chronologically
        self._validate_data_collection(
            report_number=1,
            collection_type="before_event",
            expected_data=[],
        )
        self._validate_data_collection(
            report_number=2,
            collection_type="before_event",
            expected_data=[10, 10, 11],
        )
        self._validate_data_collection(
            report_number=1,
            collection_type="after_series_first_event",
            expected_data=[10, 10, 11, 11, 12],
        )
        self._validate_data_collection(
            report_number=3,
            collection_type="before_event",
            expected_data=[10, 11, 11, 12, 12],
        )
        self._validate_data_collection(
            report_number=1,
            collection_type="after_series_last_event",
            expected_data=[13, 13, 14, 14, 15],
        )

        # The second series of events. Validations are ordered chronologically
        self._validate_data_collection(
            report_number=4,
            collection_type="before_event",
            expected_data=[13, 14, 14, 15, 15],
        )
        self._validate_data_collection(
            report_number=5,
            collection_type="before_event",
            expected_data=[14, 14, 15, 15, 16],
        )
        self._validate_data_collection(
            report_number=2,
            collection_type="after_series_first_event",
            expected_data=[16, 16, 17, 17, 18],
        )
        self._validate_data_collection(
            report_number=2,
            collection_type="after_series_last_event",
            expected_data=[16, 17, 17, 18, 18],
        )

    def _run_simulation(self, event_publisher_path):
        self.sdv_device.adb().log().info("Starting Simulator")
        self.sdv_device.adb().execute_shell_command(
            self._get_simulator_command(self.sdv_device, event_publisher_path)
        )
        self.sdv_device.adb().log().info("Simulation finished")

    def _get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        event_publisher_path: Path,
    ) -> str:
        cd_command = shlex_join(["cd", str(self._SIMULATION_CONFIG_DIR)])
        simulator_command = shlex_join([
            self.get_simulator_binary(device),
            "--max-simulation-time",
            f"seconds:{self._SIMULATION_TIME.total_seconds()}",
            "full-simulation",
            "--metrics-configs",
            str(self._METRICS_CONFIG_PATH),
            "--publisher-configs",
            str(self._SPEED_PUBLISHER_PATH),
            str(event_publisher_path),
            "--output-directory",
            str(self._simulator_out_dir),
        ])
        return f"{cd_command} && {simulator_command}"

    def _validate_data_collection(
        self, collection_type, report_number, expected_data
    ):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._simulator_out_dir,
            config_uuid=self._METRICS_CONFIG_UUID,
            report_name=f"{collection_type}_collected_data",
            report_number=report_number,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        asserts.assert_equal(
            report_payload.speed_vector,
            expected_data,
            f"Unexpected data for report #{report_number} with collection_type"
            f" `{collection_type}`",
        )


if __name__ == "__main__":
    sdv_test_runner.run()
