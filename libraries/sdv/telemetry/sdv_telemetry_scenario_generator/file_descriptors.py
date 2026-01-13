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

import itertools
import math
from typing import List
from google.protobuf.descriptor_pb2 import DescriptorProto, FileDescriptorProto
from google.protobuf.text_format import Parse
from sdv_telemetry_scenario_generator.generator_utils import RandomNameGenerator

FLOAT_MESSAGE_DESCRIPTOR = Parse(
    """
syntax: "proto3"
name: "Float.proto"
message_type {
  name: "Value"
  field {
    name: "value"
    number: 1
    label: LABEL_OPTIONAL
    type: TYPE_FLOAT
    json_name: "value"
  }
}
""",
    FileDescriptorProto(),
)

FLOAT_LIST_MESSAGE_DESCRIPTOR = Parse(
    """
syntax: "proto3"
name: "FloatList.proto"
message_type {
  name: "Values"
  field {
    name: "values"
    number: 1
    label: LABEL_REPEATED
    type: TYPE_FLOAT
    json_name: "values"
  }
}
""",
    FileDescriptorProto(),
)


def make_report_file_descriptor(field_count: int) -> FileDescriptorProto:
    """Creates Metrics Report descriptor"""

    file_descriptor = FileDescriptorProto()
    file_descriptor.name = "Report.proto"
    file_descriptor.syntax = "proto3"

    message_type = file_descriptor.message_type.add()
    message_type.name = "Report"
    for i in range(field_count):
        field = message_type.field.add()
        field.name = f"field_{i}"
        field.number = i + 1
        field.label = field.LABEL_OPTIONAL
        field.type = field.TYPE_FLOAT
        field.json_name = field.name

    return file_descriptor


def make_message_type(name: str, field_count: int) -> DescriptorProto:
    """Creates named message type with the given number of fields"""

    field_name_generator = RandomNameGenerator()

    message_type = DescriptorProto()
    message_type.name = name
    for i in range(field_count):
        field = message_type.field.add()
        field.name = field_name_generator.next()
        field.number = i + 1
        field.label = field.LABEL_OPTIONAL
        field.type = field.TYPE_INT64
        field.json_name = field.name

    return message_type


def make_publisher_data_descriptor(
    field_counts: List[int],
) -> FileDescriptorProto:
    """Creates FileDescriptorProto containing len(field_counts) message types.

    Message type #i contains field_counts[i] primitive fields.
    """

    assert len(field_counts) > 0, "field_counts must not be empty"

    name_generator = RandomNameGenerator()

    descriptor = FileDescriptorProto()
    descriptor.name = "Data.proto"
    descriptor.syntax = "proto3"
    for field_count in field_counts:
        name = f"Message_{name_generator.next()}"
        message_type = descriptor.message_type.add()
        message_type.CopyFrom(make_message_type(name, field_count))
    return descriptor


def make_publisher_data_descriptors(
    field_counts: List[int],
    descriptors_count: int,
) -> List[FileDescriptorProto]:
    """Creates a list of FileDescriptorProtos of given length.

    FileDescriptorProtos generated contain len(field_counts) message types
    (DescriptorProtos) in total. Message type #i contains field_counts[i]
    primitive fields.
    """

    message_types_count = len(field_counts)

    assert descriptors_count > 0, "descriptors_count must be positive"
    assert (
        descriptors_count <= message_types_count
    ), "descriptors_count must be less than or equal to message_types_count"

    chunk_size = math.ceil(message_types_count / descriptors_count)
    return [
        make_publisher_data_descriptor(batch)
        for batch in itertools.batched(field_counts, chunk_size)
    ]
