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

import csv
from datetime import timedelta
import io
from itertools import groupby
import os
from pathlib import Path
import tempfile
import unittest
from uuid import UUID
from google.protobuf import text_format
from sdv_telemetry_scenario_generator.file_descriptors import FLOAT_LIST_MESSAGE_DESCRIPTOR, FLOAT_MESSAGE_DESCRIPTOR, make_publisher_data_descriptors, make_report_file_descriptor
from sdv_telemetry_scenario_generator.generate import WORST_CASE_SCENARIO, generate
from sdv_telemetry_scenario_generator.generator_utils import random_partition_set
from system.software_defined_vehicle.telemetry.proto.metrics_configuration.metrics_configuration_pb2 import MetricsConfig
from system.software_defined_vehicle.telemetry.simulator.proto.simulation_actions_pb2 import MetricsConfigAction, SimulationActions
from system.software_defined_vehicle.telemetry.simulator.proto.simulation_publisher_pb2 import SimulationPublisher


def is_binary_proto(ext):
    return ext in [".pb", ".bin", ".binpb"]


def is_text_proto(ext):
    return ext in [".textproto", ".txtpb"]


class TestUtils(unittest.TestCase):

    def test_random_partition_set(self):
        input = set(["a", "b", "c", "d", "e", "f"])

        a, b = random_partition_set(input, 2)
        self.assertSetEqual(a | b, input)
        self.assertEqual(len(a), 2)
        self.assertEqual(len(b), 4)
        self.assertSetEqual(input - a, b)
        self.assertSetEqual(input - b, a)

    def test_random_partition_set_edge_case_1(self):
        input = set(["a", "b", "c"])

        a, b = random_partition_set(input, 0)
        self.assertSetEqual(a, set())
        self.assertSetEqual(b, input)

    def test_random_partition_set_edge_case_2(self):
        input = set(["a", "b", "c"])

        a, b = random_partition_set(input, 3)
        self.assertSetEqual(a, input)
        self.assertSetEqual(b, set())


class TestFileDescriptors(unittest.TestCase):

    def test_float_descriptor(self):
        file_descriptor = FLOAT_MESSAGE_DESCRIPTOR
        self.assertEqual(len(file_descriptor.message_type), 1)

        message_descriptor = file_descriptor.message_type[0]
        self.assertEqual(message_descriptor.name, "Value")
        self.assertEqual(len(message_descriptor.field), 1)

        field_descriptor = message_descriptor.field[0]
        self.assertEqual(field_descriptor.name, "value")
        self.assertEqual(field_descriptor.type, field_descriptor.TYPE_FLOAT)
        self.assertEqual(
            field_descriptor.label, field_descriptor.LABEL_OPTIONAL
        )

    def test_float_list_descriptor(self):
        file_descriptor = FLOAT_LIST_MESSAGE_DESCRIPTOR
        self.assertEqual(len(file_descriptor.message_type), 1)

        message_descriptor = file_descriptor.message_type[0]
        self.assertEqual(message_descriptor.name, "Values")
        self.assertEqual(len(message_descriptor.field), 1)

        field_descriptor = message_descriptor.field[0]
        self.assertEqual(field_descriptor.name, "values")
        self.assertEqual(field_descriptor.type, field_descriptor.TYPE_FLOAT)
        self.assertEqual(
            field_descriptor.label, field_descriptor.LABEL_REPEATED
        )

    def test_report_descriptor(self):
        file_descriptor = make_report_file_descriptor(3)
        self.assertEqual(len(file_descriptor.message_type), 1)

        message_descriptor = file_descriptor.message_type[0]
        self.assertEqual(message_descriptor.name, "Report")
        self.assertEqual(len(message_descriptor.field), 3)

        for i, field_descriptor in enumerate(message_descriptor.field):
            self.assertEqual(field_descriptor.name, f"field_{i}")
            self.assertEqual(field_descriptor.type, field_descriptor.TYPE_FLOAT)
            self.assertEqual(
                field_descriptor.label, field_descriptor.LABEL_OPTIONAL
            )

    def test_publisher_data_descriptors(self):
        test_cases = {
            "simple": {"field_counts": [3, 1, 2], "descriptor_count": 1},
            "simple_multiple_descriptors": {
                "field_counts": [3, 1, 2],
                "descriptor_count": 2,
            },
            "many_fields": {"field_counts": [10] * 1000, "descriptor_count": 1},
            "many_fields_many_descriptors": {
                "field_counts": [10] * 1000,
                "descriptor_count": 30,
            },
        }

        for name, params in test_cases.items():
            with self.subTest(name):
                self.run_publisher_data_descriptors(
                    params["field_counts"], params["descriptor_count"]
                )

    def run_publisher_data_descriptors(self, field_counts, descriptor_count):
        descriptors = make_publisher_data_descriptors(
            field_counts, descriptor_count
        )
        self.assertEqual(len(descriptors), descriptor_count)

        message_descriptors = [
            message_descriptor
            for descriptor in descriptors
            for message_descriptor in descriptor.message_type
        ]

        self.assertEqual(len(message_descriptors), len(field_counts))
        for message_descriptor, field_count in zip(
            message_descriptors, field_counts
        ):
            self.assertEqual(len(message_descriptor.field), field_count)
            self.assertGreater(len(message_descriptor.name), 0)

            for i, field_descriptor in enumerate(message_descriptor.field):
                self.assertGreater(len(field_descriptor.name), 0)
                self.assertEqual(
                    field_descriptor.type, field_descriptor.TYPE_INT64
                )
                self.assertEqual(
                    field_descriptor.label, field_descriptor.LABEL_OPTIONAL
                )


class TestGenerate(unittest.TestCase):

    def test_generate(self):
        metrics_config_count = 0
        simulation_publisher_count = 0
        simulation_duration = timedelta(seconds=120)
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_dir = Path(temp_dir)

            generate(temp_dir, simulation_duration)

            for file in os.listdir(temp_dir):
                file_path = temp_dir / file

                ext = os.path.splitext(file)[1]
                if file.startswith("metrics_config_"):
                    if is_binary_proto(ext):
                        metrics_config_count += 1

                    metrics_config = self.parse_proto(
                        file_path, ext, MetricsConfig
                    )
                    uuid = UUID(metrics_config.uuid)
                    self.assertEqual(uuid.version, 4)
                elif file.startswith("simulation_publisher_"):
                    if ext == ".csv":
                        csv_reader = csv.DictReader(
                            io.StringIO(file_path.read_text()), delimiter=","
                        )
                        self.assertIn(
                            "_delay_before_publishing",
                            csv_reader.fieldnames,
                            f"hmmm :( {file_path.read_text()}",
                        )
                        self.assertTrue(len(list(csv_reader)) > 50)
                        continue

                    if is_binary_proto(ext):
                        simulation_publisher_count += 1

                    simulation_publisher = self.parse_proto(
                        file_path, ext, SimulationPublisher
                    )
                    self.assertIn("_topic_", simulation_publisher.service_name)
                elif file.startswith("simulation_actions"):
                    actions = self.parse_proto(
                        file_path, ext, SimulationActions
                    )
                    self.validate_simulation_actions(
                        actions, simulation_duration
                    )
                else:
                    self.fail(f"Unexpected generated file found: {file}")

        self.assertEqual(
            metrics_config_count, WORST_CASE_SCENARIO["METRICS_CONFIGS"]
        )
        self.assertEqual(
            simulation_publisher_count,
            WORST_CASE_SCENARIO["GETTER_TOPICS"]
            + WORST_CASE_SCENARIO["SUBSCRIBED_TOPICS"],
        )

    def parse_proto(self, file_path: Path, ext, entity_class):
        entity = entity_class()
        if is_binary_proto(ext):
            entity.ParseFromString(file_path.read_bytes())
        elif is_text_proto(ext):
            text_format.Parse(file_path.read_text(), entity)
        else:
            self.fail(f"Unexpected generated file found: {file}")
        return entity

    def validate_simulation_actions(
        self, actions: SimulationActions, time_horizon: timedelta
    ):
        TOTAL_CONFIGS = WORST_CASE_SCENARIO["METRICS_CONFIGS"]
        ACTIVE_CONFIGS = WORST_CASE_SCENARIO["ACTIVE_METRICS_CONFIGS"]

        # Validate that each config has been added and activated at least once
        added_configs = set()
        activated_configs = set()
        for action in actions.actions:
            if not action.HasField("metrics_config_action"):
                self.fail(f"Unexpected action: {action}")

            config_action = action.metrics_config_action
            action_type = config_action.action_type
            if action_type == MetricsConfigAction.ActionType.ADD:
                added_configs.add(config_action.config_uuid)
            elif action_type == MetricsConfigAction.ActionType.ACTIVATE:
                activated_configs.add(config_action.config_uuid)

        self.assertEqual(len(added_configs), TOTAL_CONFIGS)
        self.assertEqual(len(activated_configs), TOTAL_CONFIGS)

        # Validate that there are exactly `ACTIVE_CONFIGS` active configs
        # at each timestamp
        active_configs = 0
        for idx, action in enumerate(actions.actions):
            if action.delay.ToTimedelta() != timedelta(seconds=0):
                # new timestamp comes - there were no actions on the interval
                # between this timestamp and the previous one, so this interval
                # can be validated as a whole
                if active_configs != ACTIVE_CONFIGS:
                    self.fail(f"Not enough active configs, action index: {idx}")

            action_type = action.metrics_config_action.action_type
            if action_type == MetricsConfigAction.ActionType.ACTIVATE:
                active_configs += 1
            elif action_type == MetricsConfigAction.ActionType.DEACTIVATE:
                active_configs -= 1

            if active_configs > ACTIVE_CONFIGS:
                self.fail(
                    f"Too many active metrics configs, action index: {idx}"
                )

        # Validate the very last interval, except that it is allowed to have
        # less than `ACTIVE_CONFIGS` configs if the last action timestamp is
        # equal to `time_horizon`.
        #
        # This check is needed if, for instance, we have actions
        # (activate, 0) and (deactivate, time_horizon) - the number of active
        # configs after all events are processed is zero, which is fine.
        # On the contrast, it is not forced to explicitly specify the
        # deactivation action, but then one has to validate that config was
        # active until the very end.
        last_timestamp = sum(
            (action.delay.ToTimedelta() for action in actions.actions),
            timedelta(seconds=0),
        )
        if last_timestamp < time_horizon and active_configs != ACTIVE_CONFIGS:
            self.fail(
                f"Not enough active metrics configs between last action"
                f" timestamp and time horizon"
            )


if __name__ == "__main__":
    unittest.main(verbosity=2)
