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
from enum import Enum
import io
from pathlib import Path
import random
from typing import Dict, List, Tuple
import uuid

from google.protobuf.descriptor_pb2 import FileDescriptorProto
from sdv_telemetry_scenario_generator.file_descriptors import FLOAT_LIST_MESSAGE_DESCRIPTOR, FLOAT_MESSAGE_DESCRIPTOR, make_publisher_data_descriptors, make_report_file_descriptor
from sdv_telemetry_scenario_generator.generator_utils import CSV_DELIMITER, as_text_proto, generate_schedule, random_partition_set
from system.software_defined_vehicle.telemetry.proto.metrics_configuration.expression_pb2 import CombinationNode, Node as ExpressionNode
from system.software_defined_vehicle.telemetry.proto.metrics_configuration.metrics_configuration_pb2 import ConditionalTrigger, MetricsConfig, DataSource
from system.software_defined_vehicle.telemetry.simulator.proto.simulation_actions_pb2 import MetricsConfigAction, SimulationAction, SimulationActions
from system.software_defined_vehicle.telemetry.simulator.proto.simulation_publisher_pb2 import PublishingStrategy, SimulationPublisher

WORST_CASE_SCENARIO = {
    'METRICS_CONFIGS': 150,
    'ACTIVE_METRICS_CONFIGS': 100,
    'SUBSCRIBED_TOPICS': 300,
    'GETTER_TOPICS': 700,
    'DATA_TRIGGERS': 300,
    'ACTIVE_DATA_TRIGGERS': (
        200
    ),  # int(0.4 * 500): 40% of the total of 500 triggers should be data triggers
    'PERIODIC_TRIGGERS': 225,
    'ACTIVE_PERIODIC_TRIGGERS': (
        150
    ),  # int(0.3 * 500): 30% of the total of 500 triggers should be data triggers
    'CONDITIONAL_TRIGGERS': 225,
    'ACTIVE_CONDITIONAL_TRIGGERS': (
        150
    ),  # int(0.3 * 500): 30% of the total of 500 triggers should be data triggers
    'AVERAGE_REPORTS_PER_SECOND': 10,
    'AVERAGE_TOPIC_CHANGES_PER_SECOND': 250,
}

assert (
    WORST_CASE_SCENARIO['SUBSCRIBED_TOPICS']
    == WORST_CASE_SCENARIO['DATA_TRIGGERS']
)
ACTIVE_TRIGGERS = (
    WORST_CASE_SCENARIO['DATA_TRIGGERS']
    + WORST_CASE_SCENARIO['PERIODIC_TRIGGERS']
    + WORST_CASE_SCENARIO['CONDITIONAL_TRIGGERS']
)

TYPICAL_METRICS_CONFIG = {
    'DATA_TRIGGERS': 3,
    'PERIODIC_TRIGGERS': 2,
    'CONDITIONAL_TRIGGERS': 2,
    'GETTER_PUBLISHERS': 20,
    'AGGREGATION_PUBLISHERS': 6,
    'METRICS_REPORT_CONFIGS': 3,
}

TYPICAL_METRICS_REPORT = {
    'FIELD_COUNT': 20,
}

REPORT_MESSAGE_DESCRIPTOR = make_report_file_descriptor(
    TYPICAL_METRICS_REPORT['FIELD_COUNT']
)

FILE_DESCRIPTORS_COUNT = 30

class PublisherType(Enum):
    DT = 1
    RPC = 2
    CONFIGURABLE = 3


DT_MESSAGE_COUNT = 200
DT_MESSAGE_SIZE = 2048

# TODO: b/466363305: Increase number of DT and RPC publishers in KPI Scenario Test
PUBLISHER_TYPE_DISTRIBUTION = {
    'DT': 3,
    'RPC': 3,
    'CONFIGURABLE': 994,
}

assert (
    WORST_CASE_SCENARIO['SUBSCRIBED_TOPICS']
    + WORST_CASE_SCENARIO['GETTER_TOPICS']
    == PUBLISHER_TYPE_DISTRIBUTION['DT']
    + PUBLISHER_TYPE_DISTRIBUTION['RPC']
    + PUBLISHER_TYPE_DISTRIBUTION['CONFIGURABLE']
)


# A thin wrapper around a publisher topic name.
class Topic:
    name: str

    def __init__(self, name: str) -> None:
        self.name = name


def make_topics() -> Tuple[List[Topic], List[Topic]]:
    getter_topics = list()
    for i in range(WORST_CASE_SCENARIO['GETTER_TOPICS']):
        getter_topics.append(Topic(f'com.google.sdv.telemetry.Getter{i:0=4}'))

    subscribed_topics = list()
    for i in range(WORST_CASE_SCENARIO['SUBSCRIBED_TOPICS']):
        subscribed_topics.append(
            Topic(f'com.google.sdv.telemetry.Subscribed{i:0=4}')
        )

    return [getter_topics, subscribed_topics]


class Context:
    _data_trigger_slots: List[int]
    _periodic_trigger_slots: List[int]
    _getter_publisher_slots: List[int]

    _periodic_trigger_durations: List[timedelta]

    getter_topics: List[Topic]
    subscribed_topics: List[Topic]

    def __init__(self) -> None:
        self._data_trigger_slots = self._calculate_slots(
            WORST_CASE_SCENARIO['DATA_TRIGGERS'],
            TYPICAL_METRICS_CONFIG['DATA_TRIGGERS']
            * WORST_CASE_SCENARIO['METRICS_CONFIGS'],
        )
        self._periodic_trigger_slots = self._calculate_slots(
            WORST_CASE_SCENARIO['PERIODIC_TRIGGERS'],
            TYPICAL_METRICS_CONFIG['PERIODIC_TRIGGERS']
            * WORST_CASE_SCENARIO['METRICS_CONFIGS'],
        )
        self._getter_publisher_slots = self._calculate_slots(
            WORST_CASE_SCENARIO['GETTER_TOPICS'],
            TYPICAL_METRICS_CONFIG['GETTER_PUBLISHERS']
            * WORST_CASE_SCENARIO['METRICS_CONFIGS'],
        )

        self._periodic_trigger_durations = [
            timedelta(milliseconds=random.randrange(20, 30000))
            for _ in range(WORST_CASE_SCENARIO['PERIODIC_TRIGGERS'])
        ]

        [self.getter_topics, self.subscribed_topics] = make_topics()

    def subscribed_service_publisher_topics_for_metrics_config(
        self, metrics_config_index: int
    ) -> List[Topic]:
        topics: List[Topic] = []
        start_idx = (
            metrics_config_index * TYPICAL_METRICS_CONFIG['DATA_TRIGGERS']
        )
        for idx in self._data_trigger_slots[
            start_idx : start_idx + TYPICAL_METRICS_CONFIG['DATA_TRIGGERS']
        ]:
            topics.append(self.subscribed_topics[idx])
        return topics

    def periodic_trigger_durations_for_metrics_config(
        self, metrics_config_index: int
    ) -> List[timedelta]:
        durations: List[timedelta] = []
        start_idx = (
            metrics_config_index * TYPICAL_METRICS_CONFIG['PERIODIC_TRIGGERS']
        )
        for idx in self._periodic_trigger_slots[
            start_idx : start_idx + TYPICAL_METRICS_CONFIG['PERIODIC_TRIGGERS']
        ]:
            durations.append(self._periodic_trigger_durations[idx])
        return durations

    def getter_service_publisher_topics_for_metrics_config(
        self, metrics_config_index: int
    ) -> List[Topic]:
        topics: List[Topic] = []
        start_idx = (
            metrics_config_index * TYPICAL_METRICS_CONFIG['GETTER_PUBLISHERS']
        )
        for idx in self._getter_publisher_slots[
            start_idx : start_idx + TYPICAL_METRICS_CONFIG['GETTER_PUBLISHERS']
        ]:
            topics.append(self.getter_topics[idx])
        return topics

    # Given `entity_count` entities (such as data triggers or getter topics),
    # and `slot_count` "slots" (such as metrics config publishers) that each
    # refer to any one of these entities, return a list of indices for each
    # "slot" that is the index of the entity it shall refer to.
    #
    # While we could just randomly pick from the entities for each slot, this
    # method ensures that:
    # 1. Each entity is referred to at least once.
    # 2. The overall ratio of entities only referred to once vs. multiple times
    #    is equal to `entity_count / slot_count`.
    def _calculate_slots(self, entity_count: int, slot_count: int) -> List[int]:
        assert slot_count >= entity_count

        unique_ratio = float(entity_count) / slot_count

        unique_count: int = round(entity_count * unique_ratio)
        shared_count: int = entity_count - unique_count
        assert shared_count + unique_count == entity_count

        # Determine which slots will point to a unique entity and which will point
        # to a shared trigger.
        unique_slot_indexes, shared_slot_indexes = random_partition_set(
            set(range(slot_count)), unique_count
        )

        slots = [None] * slot_count

        # These are the slots that refer to entities that are guaranteed to only be
        # referred to once.
        for i, unique_slot_index in enumerate(unique_slot_indexes):
            slots[unique_slot_index] = i

        a, b = random_partition_set(shared_slot_indexes, shared_count)
        # These are the slots that refer to entities that may be referred to
        # multiple times. The split into two loops ensures that each entity is
        # referred to at least once.
        for i, a_index in enumerate(a):
            slots[a_index] = unique_count + i
        for b_index in b:
            slots[b_index] = random.choice(
                range(unique_count, unique_count + shared_count)
            )

        assert list(sorted(set(slots))) == list(range(entity_count))
        return slots


class TopicDescriptorData:
    descriptor: FileDescriptorProto
    message_type_name: str
    field_names: List[str]

    def __init__(
        self,
        descriptor: FileDescriptorProto,
        message_type_name: str,
        field_names: List[str],
    ):
        self.descriptor = descriptor
        self.message_type_name = message_type_name
        self.field_names = field_names


def choose_field_count():
    # 30% probability for just 1 field, 60% probability for 2-10 fields, 10%
    # probability for 11-100 fields.
    field_count_range = random.choices(
        [[1], list(range(2, 11)), list(range(11, 101))],
        weights=[30, 60, 10],
        k=1,
    )[0]
    field_count = random.choice(field_count_range)
    return field_count


def make_topic_descriptors(
    ctx: Context,
) -> Dict[str, TopicDescriptorData]:
    topics_count = len(ctx.subscribed_topics) + len(ctx.getter_topics)
    field_counts = [choose_field_count() for _ in range(topics_count)]

    # Generate a descriptor for each topic. The descriptors are distributed
    # among descriptors_count FileDescriptorProtos. This is done as these proto
    # objects are quite heavy when using Rust Protobuf Library, which has
    # previously led to excessive RAM consumption by Telemetry Service. We'd
    # like to be able to track optimizations or regressions related to this, so
    # we set up descriptors_count in a way it corresponds to some non-trivial
    # real-world scenario of descriptors setup.
    descriptors = make_publisher_data_descriptors(
        field_counts, descriptors_count=FILE_DESCRIPTORS_COUNT
    )

    all_message_types = []
    for descriptor in descriptors:
        for message_type in descriptor.message_type:
            all_message_types.append((descriptor, message_type))

    assert len(all_message_types) == topics_count, (
        'Total number of message types should equal total number of topics.'
        f' Got {len(all_message_types)} message types and {topics_count}'
        ' topics.'
    )

    return {
        topic.name: TopicDescriptorData(
            descriptor,
            message_type.name,
            [field.name for field in message_type.field],
        )
        for topic, (descriptor, message_type) in zip(
            ctx.subscribed_topics + ctx.getter_topics, all_message_types
        )
    }


# Returns a list of simulation publishers, each of which is a tuple containing
# the proto and the csv contents.
def make_simulation_publishers(
    ctx: Context,
    topic_descriptors: Dict[str, TopicDescriptorData],
) -> List[Tuple[SimulationPublisher, str]]:
    publisher_types = (
        [PublisherType.DT] * PUBLISHER_TYPE_DISTRIBUTION['DT']
        + [PublisherType.RPC] * PUBLISHER_TYPE_DISTRIBUTION['RPC']
        + [PublisherType.CONFIGURABLE]
        * PUBLISHER_TYPE_DISTRIBUTION['CONFIGURABLE']
    )
    random.shuffle(publisher_types)
    assert len(publisher_types) == len(ctx.subscribed_topics) + len(
        ctx.getter_topics
    )

    return [
        make_simulation_publisher(
            topic.name, topic_descriptors[topic.name], publisher_type
        )
        for topic, publisher_type in zip(
            ctx.subscribed_topics + ctx.getter_topics, publisher_types
        )
    ]


def make_simulation_publisher(
    topic_name: str,
    topic_descriptor_data: TopicDescriptorData,
    publisher_type: PublisherType,
) -> Tuple[SimulationPublisher, str]:
    publisher = SimulationPublisher()
    publisher.service_name = topic_name
    publisher.data_format_protos.append(topic_descriptor_data.descriptor)
    publisher.data_format_message_name = (
        f'.{topic_descriptor_data.message_type_name}'
    )

    match publisher_type:
        case PublisherType.DT:
            publisher.publishing_strategy.sdv_comms_data_tunnel.message_count = (
                DT_MESSAGE_COUNT
            )
            publisher.publishing_strategy.sdv_comms_data_tunnel.message_size = (
                DT_MESSAGE_SIZE
            )
        case PublisherType.RPC:
            publisher.publishing_strategy.sdv_comms_rpc.SetInParent()
        case PublisherType.CONFIGURABLE:
            publisher.publishing_strategy.configurable_publisher_registry.SetInParent()

    average_topic_changes_per_second_per_subscribed_publisher = (
        WORST_CASE_SCENARIO['AVERAGE_TOPIC_CHANGES_PER_SECOND']
        / WORST_CASE_SCENARIO['SUBSCRIBED_TOPICS']
    )

    data_generator = publisher.data_generators.add()
    data_generator.loop_data = True
    data_generator.default_delay.FromTimedelta(
        timedelta(
            seconds=random.uniform(
                0.7 * average_topic_changes_per_second_per_subscribed_publisher,
                1.3 * average_topic_changes_per_second_per_subscribed_publisher,
            )
        )
    )
    data_generator.csv_file.delimiter = CSV_DELIMITER

    csv_contents = io.StringIO()
    csv_writer = csv.DictWriter(
        csv_contents,
        delimiter=CSV_DELIMITER,
        fieldnames=['_delay_before_publishing']
        + topic_descriptor_data.field_names,
    )

    csv_writer.writeheader()
    for _ in range(100):
        # `_delay_before_publishing` is never set, so we only use the
        # `default_delay` from above.
        csv_writer.writerow({
            field_name: random.randint(-1_000_000, 1_000_000)
            for field_name in topic_descriptor_data.field_names
        })

    return publisher, csv_contents.getvalue()


# Returns a list of metrics config protos.
def make_metrics_configs(
    ctx: Context,
    topic_descriptors: Dict[str, TopicDescriptorData],
) -> List[MetricsConfig]:
    metrics_configs = list()
    for i in range(WORST_CASE_SCENARIO['METRICS_CONFIGS']):
        metrics_configs.append(make_metrics_config(ctx, i, topic_descriptors))
    return metrics_configs


# Creates an expression node for the provided relational operator
def make_relational_combination_node(
    left_idx: int, op: str, right_idx: int
) -> ExpressionNode:
    expression_node = ExpressionNode()
    expression_node.combination_node.left_index = left_idx
    expression_node.combination_node.right_index = right_idx
    if op == '<':
        expression_node.combination_node.relational_operator = (
            CombinationNode.RelationalOperator.LT
        )
    elif op == '<=':
        expression_node.combination_node.relational_operator = (
            CombinationNode.RelationalOperator.LT_OR_EQ
        )
    elif op == '>':
        expression_node.combination_node.relational_operator = (
            CombinationNode.RelationalOperator.GT
        )
    elif op == '>=':
        expression_node.combination_node.relational_operator = (
            CombinationNode.RelationalOperator.GT_OR_EQ
        )
    else:
        assert False, f"Unsupported operator '{op}'"
    return expression_node


def make_metrics_config(
    ctx: Context,
    metrics_config_idx: int,
    topic_descriptors: Dict[str, TopicDescriptorData],
) -> MetricsConfig:
    metrics_config = MetricsConfig()
    metrics_config.uuid = str(uuid.uuid4())
    metrics_config.version = 1

    descriptor_proto = metrics_config.descriptor_protos.add()
    descriptor_proto.CopyFrom(FLOAT_MESSAGE_DESCRIPTOR)

    descriptor_proto = metrics_config.descriptor_protos.add()
    descriptor_proto.CopyFrom(FLOAT_LIST_MESSAGE_DESCRIPTOR)

    descriptor_proto = metrics_config.descriptor_protos.add()
    descriptor_proto.CopyFrom(REPORT_MESSAGE_DESCRIPTOR)

    # Adds the provided expression node to the metrics config and returns the
    # index of the newly added node.
    def add_expression_node(node: ExpressionNode) -> int:
        idx = len(metrics_config.expression_nodes)
        metrics_config.expression_nodes.append(node)
        return idx

    # Create constant expression node with a value of 5
    expression_node = ExpressionNode()
    expression_node.constant_leaf_node.int64_value = 5
    five_idx = add_expression_node(expression_node)

    # Create constant expression node with a value of 0
    expression_node = ExpressionNode()
    expression_node.constant_leaf_node.int64_value = 0
    zero_idx = add_expression_node(expression_node)

    # List to hold the indices of all expression nodes that are field leaf nodes
    # for service publishers (both subscription and getter publishers).
    service_publisher_expression_node_indexes: List[int] = list()
    # List to hold the indices of all expression nodes that are field leaf nodes
    # for aggregation publishers.
    aggregation_publisher_expression_node_indexes: List[int] = list()
    # List to hold the names of all data triggers for subscribtion service
    # publishers.
    data_trigger_names: List[str] = list()

    # Create subscription service publishers, service publisher triggers, and
    # expression nodes for each service publisher.
    for i, topic in enumerate(
        ctx.subscribed_service_publisher_topics_for_metrics_config(
            metrics_config_idx
        )
    ):
        publisher = metrics_config.sources.add()
        publisher.name = f'publisher_service_subscription_{i}'
        publisher.data_source.source_identifier = topic.name
        publisher.data_source.connection_type = (
            DataSource.ConnectionType.SUBSCRIPTION
        )

        trigger = metrics_config.triggers.add()
        trigger.name = f'trigger_data_{i}'
        trigger.data_trigger.source_name = publisher.name
        data_trigger_names.append(trigger.name)

        expression_node = ExpressionNode()
        expression_node.field_leaf_node.source_name = publisher.name
        expression_node.field_leaf_node.field_names.append(
            random.choice(topic_descriptors[topic.name].field_names)
        )
        service_publisher_expression_node_indexes.append(
            add_expression_node(expression_node)
        )

    # Create getter service publishers, and expression nodes for each getter
    # publisher.
    for i, topic in enumerate(
        ctx.getter_service_publisher_topics_for_metrics_config(
            metrics_config_idx
        )
    ):
        publisher = metrics_config.sources.add()
        publisher.name = f'publisher_service_getter_{i}'
        publisher.data_source.source_identifier = topic.name
        publisher.data_source.connection_type = (
            DataSource.ConnectionType.ON_DEMAND
        )

        expression_node = ExpressionNode()
        expression_node.field_leaf_node.source_name = publisher.name
        expression_node.field_leaf_node.field_names.append(
            random.choice(topic_descriptors[topic.name].field_names)
        )
        service_publisher_expression_node_indexes.append(
            add_expression_node(expression_node)
        )

    # Create aggregation publishers, and expression nodes for each aggregation
    # publisher.
    for i in range(TYPICAL_METRICS_CONFIG['AGGREGATION_PUBLISHERS']):
        publisher = metrics_config.sources.add()
        publisher.name = f'publisher_aggregation_{i}'
        publisher.aggregator.reset_on_get = False
        publisher.aggregator.trigger_names.extend(
            random.choices(data_trigger_names, k=1)
        )
        publisher.aggregator.message_builder.message_type = '.Value'

        expression_node_index = random.choice(
            service_publisher_expression_node_indexes
        )

        # Aggregate the values of a random service publisher
        field_assignment = (
            publisher.aggregator.message_builder.field_assignments.add()
        )
        field_assignment.field_name = 'value'
        field_assignment.avg_aggregation.expression_node_index = (
            expression_node_index
        )

        # In 50% of cases, generate an additional aggregation publisher that uses
        # vector aggregation.
        if random.choice([True, False]):
            # The result of the vector aggregation publisher is ignored (ideally, we'd
            # use `min()` (or similar) on the result to convert the `FloatList` back
            # to a `Float`, but list operators are not currently supported in
            # expression evaluation).
            #
            # For now, this is just here to simulate the use of vector aggregation
            # publishers, which have considerably higher memory usage than the other
            # aggregation publishers.
            vec_publisher = metrics_config.sources.add()
            vec_publisher.name = f'publisher_aggregation_{i}_vec'
            vec_publisher.aggregator.reset_on_get = False

            vec_publisher.aggregator.trigger_names.extend(
                random.choices(data_trigger_names, k=1)
            )
            vec_publisher.aggregator.message_builder.message_type = (
                '.Values'
            )
            vec_field_assignment = (
                vec_publisher.aggregator.message_builder.field_assignments.add()
            )
            vec_field_assignment.field_name = 'values'
            vec_field_assignment.vector_aggregation.expression_node_index = (
                expression_node_index
            )
            vec_field_assignment.vector_aggregation.max_length = random.randint(
                5, 200
            )

            vec_expression_node = ExpressionNode()
            vec_expression_node.field_leaf_node.source_name = (
                vec_publisher.name
            )
            vec_expression_node.field_leaf_node.field_names.append('values')
            add_expression_node(vec_expression_node)

        expression_node = ExpressionNode()
        expression_node.field_leaf_node.source_name = publisher.name
        expression_node.field_leaf_node.field_names.append('value')
        aggregation_publisher_expression_node_indexes.append(
            add_expression_node(expression_node)
        )

    # Create periodic triggers.
    for i, periodic_trigger_duration in enumerate(
        ctx.periodic_trigger_durations_for_metrics_config(metrics_config_idx)
    ):
        trigger = metrics_config.triggers.add()
        trigger.name = f'trigger_periodic_{i}'
        trigger.periodic_trigger.interval.FromTimedelta(
            periodic_trigger_duration
        )

    # Create conditional triggers and the corresponding expression nodes.
    for i in range(TYPICAL_METRICS_CONFIG['CONDITIONAL_TRIGGERS']):
        # The following code generates expression nodes for the following
        # expression:
        #
        # (ABS(A) < 5 || (B > 5 && B < 5)) || (ABS(MOD(C, 5)) < 5 && ABS(D) >= 0)
        #
        # Note that the expression is deliberately written in a way where it will
        # always evaluate to `true` (= worst-case scenario), but only after
        # evaluating the whole expression (= no short-circuiting).
        [
            field_leaf_node_a_idx,
            field_leaf_node_b_idx,
            field_leaf_node_c_idx,
            field_leaf_node_d_idx,
        ] = random.sample(service_publisher_expression_node_indexes, 4)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = field_leaf_node_a_idx
        expression_node.combination_node.arithmetic_operator = (
            CombinationNode.ArithmeticOperator.ABSOLUTE
        )
        abs_a_idx = add_expression_node(expression_node)

        expression_node = make_relational_combination_node(
            abs_a_idx, '<', five_idx
        )
        abs_a_cmp_5_idx = add_expression_node(expression_node)

        expression_node = make_relational_combination_node(
            field_leaf_node_b_idx, '>', five_idx
        )
        b_cmp_5_gt_idx = add_expression_node(expression_node)
        expression_node = make_relational_combination_node(
            field_leaf_node_b_idx, '<', five_idx
        )
        b_cmp_5_lt_idx = add_expression_node(expression_node)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = b_cmp_5_gt_idx
        expression_node.combination_node.right_index = b_cmp_5_lt_idx
        expression_node.combination_node.logical_operator = (
            CombinationNode.LogicalOperator.AND
        )
        b_and_b_idx = add_expression_node(expression_node)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = abs_a_cmp_5_idx
        expression_node.combination_node.right_index = b_and_b_idx
        expression_node.combination_node.logical_operator = (
            CombinationNode.LogicalOperator.OR
        )
        a_or_b_idx = add_expression_node(expression_node)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = field_leaf_node_c_idx
        expression_node.combination_node.right_index = five_idx
        expression_node.combination_node.arithmetic_operator = (
            CombinationNode.ArithmeticOperator.MODULO_TRUNC
        )
        c_mod_5_idx = add_expression_node(expression_node)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = c_mod_5_idx
        expression_node.combination_node.arithmetic_operator = (
            CombinationNode.ArithmeticOperator.ABSOLUTE
        )
        c_mod_5_abs_idx = add_expression_node(expression_node)

        expression_node = make_relational_combination_node(
            c_mod_5_abs_idx, '<', five_idx
        )
        c_mod_5_abs_cmp_5_idx = add_expression_node(expression_node)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = field_leaf_node_d_idx
        expression_node.combination_node.arithmetic_operator = (
            CombinationNode.ArithmeticOperator.ABSOLUTE
        )
        abs_d_idx = add_expression_node(expression_node)

        expression_node = make_relational_combination_node(
            abs_d_idx, '>=', zero_idx
        )
        abs_d_cmp_0_idx = add_expression_node(expression_node)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = c_mod_5_abs_cmp_5_idx
        expression_node.combination_node.right_index = abs_d_cmp_0_idx
        expression_node.combination_node.logical_operator = (
            CombinationNode.LogicalOperator.AND
        )
        c_and_d_idx = add_expression_node(expression_node)

        expression_node = ExpressionNode()
        expression_node.combination_node.left_index = a_or_b_idx
        expression_node.combination_node.right_index = c_and_d_idx
        expression_node.combination_node.logical_operator = (
            CombinationNode.LogicalOperator.OR
        )
        condition_idx = add_expression_node(expression_node)

        trigger = metrics_config.triggers.add()
        trigger.name = f'trigger_conditional_{i}'
        trigger.conditional_trigger.trigger_names.extend(
            random.choices(data_trigger_names, k=1)
        )
        trigger.conditional_trigger.selector_node_index = condition_idx
        trigger.conditional_trigger.is_true.SetInParent()

    # Create report configs. All of them are based on dedicated periodic triggers
    # with the interval chosen manually to achieve the expected number of reports
    # per second (over the whole simulation period) be equal to AVERAGE_REPORTS_PER_SECOND
    # TODO: b/449004707: Make generator produce AVERAGE_REPORTS_PER_SECOND reports
    # per each second and not the just over the duration of the simulation.
    for i in range(TYPICAL_METRICS_CONFIG['METRICS_REPORT_CONFIGS']):
        report_trigger = metrics_config.triggers.add()
        report_trigger.name = f'trigger_periodic_report_{i}'
        report_trigger.periodic_trigger.interval.FromTimedelta(
            timedelta(seconds=random.uniform(20, 30))
        )
        report_config = metrics_config.metrics_report_configs.add()
        report_config.name = f'report_config_{i}'
        report_config.trigger_names.extend([report_trigger.name])
        report_config.report_incomplete = False

        report_config.message_builder.message_type = '.Report'
        for j in range(TYPICAL_METRICS_REPORT['FIELD_COUNT']):
            field_assignment = (
                report_config.message_builder.field_assignments.add()
            )
            field_assignment.field_name = f'field_{j}'
            field_assignment.no_aggregation.expression_node_index = (
                random.choice(
                    service_publisher_expression_node_indexes
                    + aggregation_publisher_expression_node_indexes
                )
            )

    return metrics_config


def make_simulation_action(
    delay: timedelta, uuid: str, action: MetricsConfigAction.ActionType
) -> SimulationAction:
    simulation_action = SimulationAction()
    simulation_action.delay.FromTimedelta(delay)
    simulation_action.metrics_config_action.config_uuid = uuid
    simulation_action.metrics_config_action.action_type = action
    return simulation_action


def make_simulation_actions(
    metrics_configs: List[MetricsConfig],
    data_collection_time: timedelta,
    simulation_time: timedelta,
) -> SimulationActions:
    uuids = [config.uuid for config in metrics_configs]

    # Add all metrics config at start
    simulation_actions = SimulationActions()

    for i, uuid in enumerate(uuids):
        delay = timedelta(seconds=5) if i == 0 else timedelta(seconds=0)
        action = make_simulation_action(
            delay, uuid, MetricsConfigAction.ActionType.ADD
        )
        simulation_actions.actions.append(action)

    # Get the schedule of metrics config activations
    schedule = generate_schedule(
        WORST_CASE_SCENARIO['ACTIVE_METRICS_CONFIGS'],
        WORST_CASE_SCENARIO['METRICS_CONFIGS'],
        data_collection_time,
    )

    def get_action_priority(action_type: SimulationAction) -> int:
        """Defines which action should go first if both should be performed at the same time."""

        # If two actions happen at the same time, the deactivation should happen
        # first as otherwise we can violate ACTIVE_METRICS_CONFIGS requirement
        # (even though for an extremely short time period)
        if action_type == MetricsConfigAction.ActionType.DEACTIVATE:
            return 0
        if action_type == MetricsConfigAction.ActionType.ACTIVATE:
            return 1
        # Should not be reached with the current schedule generation.
        return 2

    # The schedule is in the form [uuid, start, end], but SimulationActions
    # expect actions to be listed in chronological order with delays between
    # consecutive actions, so we have to transform schedule representation
    schedule = sorted(
        [
            (uuids[i], start, MetricsConfigAction.ActionType.ACTIVATE)
            for i, start, _ in schedule
        ]
        + [
            (uuids[i], end, MetricsConfigAction.ActionType.DEACTIVATE)
            for i, _, end in schedule
        ],
        key=lambda k: (k[1], get_action_priority(k[2])),
    )

    # Add config activations and deactivations
    last_timestamp = timedelta(seconds=0)
    for uuid, timestamp, action_type in schedule:
        action = make_simulation_action(
            timestamp - last_timestamp, uuid, action_type
        )
        simulation_actions.actions.append(action)
        last_timestamp = timestamp

    # Remove all metrics configs one-by-one
    for uuid in uuids:
        action = make_simulation_action(
            timedelta(seconds=0), uuid, MetricsConfigAction.ActionType.REMOVE
        )
        simulation_actions.actions.append(action)

    last_action_timestamp = sum(
        (action.delay.ToTimedelta() for action in simulation_actions.actions),
        timedelta(),
    )

    assert (
        last_action_timestamp <= simulation_time
    ), 'Last action timestamp exceeds simulation time'

    return simulation_actions


# Generates a sample scenario in the provided output directory. The output
# directory must exist.
def generate(
    output_directory: Path,
    data_collection_time: timedelta,
    simulation_time: timedelta,
) -> None:
    ctx = Context()

    topic_descriptors = make_topic_descriptors(ctx)

    metrics_configs = make_metrics_configs(ctx, topic_descriptors)
    simulation_publishers = make_simulation_publishers(ctx, topic_descriptors)
    simulation_actions = make_simulation_actions(
        metrics_configs, data_collection_time, simulation_time
    )

    for i, metrics_config in enumerate(metrics_configs):
        binary_proto = metrics_config.SerializeToString()
        text_proto = as_text_proto(metrics_config)

        prefix = f'metrics_config_{i:0=4}'
        with open(output_directory / f'{prefix}.pb', 'wb') as f:
            f.write(binary_proto)
        with open(output_directory / f'{prefix}.textproto', 'w') as f:
            f.write(text_proto)

    for i, (simulation_publisher, csv_contents) in enumerate(
        simulation_publishers
    ):
        prefix = f'simulation_publisher_{i:0=4}'

        assert len(simulation_publisher.data_generators) == 1
        data_generator = simulation_publisher.data_generators[0]
        data_generator.csv_file.file_path = f'{prefix}.csv'

        binary_proto = simulation_publisher.SerializeToString()
        text_proto = as_text_proto(simulation_publisher)

        with open(output_directory / f'{prefix}.pb', 'wb') as f:
            f.write(binary_proto)
        with open(output_directory / f'{prefix}.textproto', 'w') as f:
            f.write(text_proto)
        with open(output_directory / f'{prefix}.csv', 'w') as f:
            f.write(csv_contents)

    binary_proto = simulation_actions.SerializeToString()
    text_proto = as_text_proto(simulation_actions)
    with open(output_directory / 'simulation_actions.pb', 'wb') as f:
        f.write(binary_proto)
    with open(output_directory / 'simulation_actions.textproto', 'w') as f:
        f.write(text_proto)
