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

"""Sdv Sample Diagnostics Communication Test

Tests communication between Two SDV VMs using IPv4
"""
from mobly import asserts

from inspect import cleandoc
import re

from absl.testing import parameterized
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods


class SdvSampleDiagCommunicationTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):
    LOGCAT_GREP_TEXT_AGENT = 'sdv_diagnostics_agent'

    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_STARTING = [cleandoc(r"""
        Starting interaction with EnableConditions""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_LISTENING = [cleandoc(r"""
        Listening for service units registration by type : com\.sdv\.google\.diagnostics\.event\.EnableCondition""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_REGISTERED = [
        cleandoc(r"""
        Service unit registered: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance / enable-conditions"""),
        cleandoc(r"""
        Discovered unit: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance : enable-conditions""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_FREEZING_TEMPERATURE = [cleandoc(r"""
        Enable condition change received: EnableCondition \{
            id: \"freezing_temperature\",
            enabled: true,
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]

    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_STARTING = [cleandoc(r"""
        Starting interaction with OperationCycles""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_LISTENING = [cleandoc(r"""
        Listening for service units registration by type : com\.sdv\.google\.diagnostics\.event\.OperationCycle""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_REGISTERED = [
        cleandoc(r"""
        Service unit registered: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance / operation-cycles"""),
        cleandoc(r"""
        Discovered unit: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance : operation-cycles""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_IGNITION = [cleandoc(r"""
        Operaction cycle command received: OperationCycle \{
            id: "ignition",
            command: START,
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]

    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_STARTING = [cleandoc(r"""
        Starting interaction with Events""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_LISTENING = [cleandoc(r"""
        Listening for service units registration by type : com\.sdv\.google\.diagnostics\.event\.Event""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_REGISTERED = [cleandoc(r"""
        Service unit registered: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance / overheating-event"""),
                                                                            cleandoc(r"""
        Discovered unit: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance : overheating-event""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_RECEIVED = [
        cleandoc(r"""
        Event received: Event \{
            status: PRE_PASS,
            extended_data: Some\(
                \[
                    10,
                    8,
                    116,
                    111,
                    111,
                    32,
                    104,
                    111,
                    116,
                    33,
                \],
            \),
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}"""),
        cleandoc(r"""
        Event extended data received: \[
            10,
            8,
            116,
            111,
            111,
            32,
            104,
            111,
            116,
            33,
        \]""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_FOUND_DECLARATION = [
        cleandoc(r"""
        Found declaration for event: id: \"overheating\" extended_data_message_name: \"com\.sdv\.oem\.sample\.diagnostics\.OverheatingData\" fault_listener_mask: 1 service_unit_name: \"overheating-event\""""),
        cleandoc(r"""
        Event extended data in UDS format: Ok\(
            \[
                116,
                111,
                111,
                32,
                104,
                111,
                116,
                33,
                0,
                0,
            \],
        \)""")]

    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_STARTING = [cleandoc(r"""
        Starting interaction with DataItems""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_LISTENING = [cleandoc(r"""
        Listening for service units registration by type : com\.sdv\.google\.diagnostics\.data_item\.DataItemService""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_REGISTERED = [
        cleandoc(r"""
        Service unit registered: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithDataItem/instance / com-sdv-google-diagnostics-data-item-data-item-service"""),
        cleandoc(r"""
        Discovered unit: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithDataItem/instance : com-sdv-google-diagnostics-data-item-data-item-service""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_DIAG_DECLARATION = [
        cleandoc(r"""
    DataItems diagnostics declaration of a instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithDataItem/instance : \[
        DataItem \{
            id: \"pressure\",
            message_name: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
            is_writable: false,
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \},
    \]"""),
        cleandoc(r"""
    Trying to read data item: DataItem \{
        id: \"pressure\",
        message_name: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
        is_writable: false,
        special_fields: SpecialFields \{
            unknown_fields: UnknownFields \{
                fields: None,
            \},
            cached_size: CachedSize \{
                size: 0,
            \},
        \},
    \}""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_PERFORM = [cleandoc(r"""
        Performing RPC call: com\.sdv\.google\.diagnostics\.data_item\.DataItemService:ReadDataItem""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_RESPONSE = [cleandoc(r"""
        Response for RPC call \[ReadDataItem\]: Ok\(ReadDataItemResponse \{ value: \[13, 51, 51, 200, 66\], response_code: POSITIVE_RESPONSE, special_fields: SpecialFields \{ unknown_fields: UnknownFields \{ fields: None \}, cached_size: CachedSize \{ size: 0 \} \} \}\)""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_READ_DATA_ITEM = [cleandoc(r"""
        Read data item: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithDataItem/instance\#pressure, proto_value: \[13, 51, 51, 200, 66\], uds_value \[66, 200, 51, 51\]""")]

    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_IO_CONTROL_INTERACTION_STARTING = [cleandoc(r"""
        Starting interaction with IoControls""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_IO_CONTROL_INTERACTION_LISTENING = [cleandoc(r"""
        Listening for service units registration by type : com\.sdv\.google\.diagnostics\.io_control\.IoControlService""")]

    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ROUTING_CONTROL_INTERACTION_STARTING = [cleandoc(r"""
        Starting interaction with RoutineControls""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ROUTING_CONTROL_INTERACTION_LISTENING = [cleandoc(r"""
        Listening for service units registration by type : com\.sdv\.google\.diagnostics\.routine_control\.RoutineControlService""")]

    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_STARTING = [cleandoc(r"""
        Starting interaction with FaultListeners""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_LISTENING = [cleandoc(r"""
        Listening for service units registration by type : com\.sdv\.google\.diagnostics\.fault\.FaultListenerService""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_REGISTERED = [
        cleandoc(r"""
        Service unit registered: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance / overheating-fault"""),
        cleandoc(r"""
        Discovered unit: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance : overheating-fault""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_DIAG_DECLARATION = [
        cleandoc(r"""
        FaultListeners diagnostics declaration of a instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsSampleWithEvent/instance : \[
            Event \{
                id: \"overheating\",
                extended_data_message_name: \"com\.sdv\.oem\.sample\.diagnostics\.OverheatingData\",
                fault_listener_mask: 1,
                service_unit_name: \"overheating-event\",
                special_fields: SpecialFields \{
                    unknown_fields: UnknownFields \{
                        fields: None,
                    \},
                    cached_size: CachedSize \{
                        size: 0,
                    \},
                \},
            \},
        \]"""),
        cleandoc(r"""
        Trying to update fault status: Event \{
            id: \"overheating\",
            extended_data_message_name: \"com\.sdv\.oem\.sample\.diagnostics\.OverheatingData\",
            fault_listener_mask: 1,
            service_unit_name: \"overheating-event\",
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_RPC_CALL = [
        cleandoc(r"""
        Performing RPC call: com\.sdv\.google\.diagnostics\.fault\.FaultListenerService:OnFaultStatusChange"""),
        cleandoc(r"""
        Response for RPC call \[OnFaultStatusChange\]: Ok\(OnFaultStatusChangeResponse \{ special_fields: SpecialFields \{ unknown_fields: UnknownFields \{ fields: None \}, cached_size: CachedSize \{ size: 0 \} \} \}\)
        """)]

    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_EVENT_PRE_FAIL = [cleandoc(r"""
        Event received: Event \{
            status: PRE_FAIL,
            extended_data: None,
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_REGISTERED = [cleandoc(r"""
        Service unit registered: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default / com-sdv-google-diagnostics-io-control-io-control-service""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_DISCOVERED = [cleandoc(r"""
        Discovered unit: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default : com-sdv-google-diagnostics-io-control-io-control-service""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_DIAG_DECLARATION = [cleandoc(r"""
        IoControls diagnostics declaration of a instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default : \[
            InputOutputControlData \{
                id: \"pressure\",
                message_name: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
                special_fields: SpecialFields \{
                    unknown_fields: UnknownFields \{
                        fields: None,
                    \},
                    cached_size: CachedSize \{
                        size: 0,
                    \},
                \},
            \},
        \]""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_TRY_FREEZE = [cleandoc(r"""
        Trying to freeze current state for: InputOutputControlData \{
            id: \"pressure\",
            message_name: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_RPC_CALL = [
        cleandoc(r"""
        Performing RPC call: com\.sdv\.google\.diagnostics\.io_control\.IoControlService:FreezeCurrentState"""),
        cleandoc(r"""
        Response for RPC call \[FreezeCurrentState\]: Ok\(IoControlResponse \{ value: \[13, 51, 51, 200, 66\], response_code: POSITIVE_RESPONSE, special_fields: SpecialFields \{ unknown_fields: UnknownFields \{ fields: None \}, cached_size: CachedSize \{ size: 0 \} \} \}\)""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_FREEZE = [cleandoc(r"""
        Freeze current state for: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default\#pressure, proto_response: \[13, 51, 51, 200, 66\], uds_response: \[66, 200, 51, 51\]""")]

    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_REGISTERED = [cleandoc(r"""
        Service unit registered: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default / com-sdv-google-diagnostics-routine-control-routine-control-service""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_DISCOVERED = [cleandoc(r"""
        Discovered unit: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default : com-sdv-google-diagnostics-routine-control-routine-control-service""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_DIAG_DECLARATION = [cleandoc(r"""
        RoutinControls diagnostics declaration of a instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default : \[
            Routine \{
                id: \"pressurization\",
                start: MessageField\(
                    Some\(
                        Method \{
                            request: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
                            response: \"google\.protobuf\.Empty\",
                            special_fields: SpecialFields \{
                                unknown_fields: UnknownFields \{
                                    fields: None,
                                \},
                                cached_size: CachedSize \{
                                    size: 0,
                                \},
                            \},
                        \},
                    \),
                \),
                stop: MessageField\(
                    Some\(
                        Method \{
                            request: \"google\.protobuf\.Empty\",
                            response: \"google\.protobuf\.Empty\",
                            special_fields: SpecialFields \{
                                unknown_fields: UnknownFields \{
                                    fields: None,
                                \},
                                cached_size: CachedSize \{
                                    size: 0,
                                \},
                            \},
                        \},
                    \),
                \),
                result: MessageField\(
                    Some\(
                        Method \{
                            request: \"google\.protobuf\.Empty\",
                            response: \"google\.protobuf\.Empty\",
                            special_fields: SpecialFields \{
                                unknown_fields: UnknownFields \{
                                    fields: None,
                                \},
                                cached_size: CachedSize \{
                                    size: 0,
                                \},
                            \},
                        \},
                    \),
                \),
                special_fields: SpecialFields \{
                    unknown_fields: UnknownFields \{
                        fields: None,
                    \},
                    cached_size: CachedSize \{
                        size: 0,
                    \},
                \},
            \},
        \]""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_TRY_ROUTINE = [cleandoc(r"""
        Trying to start routine: Routine \{
            id: \"pressurization\",
            start: MessageField\(
                Some\(
                    Method \{
                        request: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
                        response: \"google\.protobuf\.Empty\",
                        special_fields: SpecialFields \{
                            unknown_fields: UnknownFields \{
                                fields: None,
                            \},
                            cached_size: CachedSize \{
                                size: 0,
                            \},
                        \},
                    \},
                \),
            \),
            stop: MessageField\(
                Some\(
                    Method \{
                        request: \"google\.protobuf\.Empty\",
                        response: \"google\.protobuf\.Empty\",
                        special_fields: SpecialFields \{
                            unknown_fields: UnknownFields \{
                                fields: None,
                            \},
                            cached_size: CachedSize \{
                                size: 0,
                            \},
                        \},
                    \},
                \),
            \),
            result: MessageField\(
                Some\(
                    Method \{
                        request: \"google\.protobuf\.Empty\",
                        response: \"google\.protobuf\.Empty\",
                        special_fields: SpecialFields \{
                            unknown_fields: UnknownFields \{
                                fields: None,
                            \},
                            cached_size: CachedSize \{
                                size: 0,
                            \},
                        \},
                    \},
                \),
            \),
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_RPC_CALL = [cleandoc(r"""
        Performing RPC call: com\.sdv\.google\.diagnostics\.routine_control\.RoutineControlService:StartRoutine""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_RPC_RESPONSE = [cleandoc(r"""
        Response for RPC call \[StartRoutine\]: Ok\(RoutineControlResponse \{ routine_info: 0, response: \[\], response_code: POSITIVE_RESPONSE, special_fields: SpecialFields \{ unknown_fields: UnknownFields \{ fields: None \}, cached_size: CachedSize \{ size: 0 \} \} \}\)""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_TEST_RELEVANT_LOG = [cleandoc(r"""
        \(TEST RELEVANT LOG\) For start method of routine: pressurization, typed response: Empty \{ special_fields: SpecialFields \{ unknown_fields: UnknownFields \{ fields: None \}, cached_size: CachedSize \{ size: 0 \} \} \}""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_START_ROUTINE = [cleandoc(r"""
        Start routine: instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default\#pressurization, proto_value: \[\], uds_value \[\]""")]

    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_DIAG_DECLARATION = [cleandoc(r"""
        FaultListeners diagnostics declaration of a instance1:com\.sdv\.oem\.sample\.diagnostics\.DiagnosticsProvider1\/default : \[
            Event \{
                id: \"lowpressure\",
                extended_data_message_name: \"com\.sdv\.oem\.sample\.diagnostics\.LowPressureData\",
                fault_listener_mask: 3,
                service_unit_name: \"\",
                special_fields: SpecialFields \{
                    unknown_fields: UnknownFields \{
                        fields: None,
                    \},
                    cached_size: CachedSize \{
                        size: 0,
                    \},
                \},
            \},
            Event \{
                id: \"pressure-change\",
                extended_data_message_name: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
                fault_listener_mask: 0,
                service_unit_name: \"pressure-change-publisher\",
                special_fields: SpecialFields \{
                    unknown_fields: UnknownFields \{
                        fields: None,
                    \},
                    cached_size: CachedSize \{
                        size: 0,
                    \},
                \},
            \},
        \]""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_UPDATE_STATUS = [cleandoc(r"""
        Trying to update fault status: Event \{
            id: \"lowpressure\",
            extended_data_message_name: \"com\.sdv\.oem\.sample\.diagnostics\.LowPressureData\",
            fault_listener_mask: 3,
            service_unit_name: \"\",
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_RPC_CALL = [cleandoc(r"""
        Performing RPC call: com\.sdv\.google\.diagnostics\.fault\.FaultListenerService:OnFaultStatusChange""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_RPC_RESPONSE = [cleandoc(r"""
        Response for RPC call \[OnFaultStatusChange\]: Ok\(OnFaultStatusChangeResponse \{ special_fields: SpecialFields \{ unknown_fields: UnknownFields \{ fields: None \}, cached_size: CachedSize \{ size: 0 \} \} \}\)""")]
    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_TRY_UPDATE = [cleandoc(r"""
        Trying to update fault status: Event \{
            id: \"pressure-change\",
            extended_data_message_name: \"com\.sdv\.oem\.sample\.diagnostics\.Pressure\",
            fault_listener_mask: 0,
            service_unit_name: \"pressure-change-publisher\",
            special_fields: SpecialFields \{
                unknown_fields: UnknownFields \{
                    fields: None,
                \},
                cached_size: CachedSize \{
                    size: 0,
                \},
            \},
        \}""")]

    DIAGNOSTICS_AGENT_SUCCESSFUL_LOAD_MESSAGE = 'sdv_diagnostics_agent has started successfully'
    DIAGNOSTICS_TEST_SUCCESSFUL_LOAD_MESSAGE = 'AGENT STATE: Diagnostics Test Ok'
    DIAGNOSTICS_PROVIDER_BINDER_NAME = "com.google.sdv.ISdvAgent/diagnostics"
    DIAGNOSTICS_TEST_BINDER_NAME = "com.google.sdv.ISdvAgent/diagnostics_test"

    def setup_class(self):

        super().setup_class()
        self.sdv_device_server = self.get_device('device1').adb()
        self.sdv_device_client = self.get_device('device2').adb()

        # Save the current values of sdv.authz.enable
        self.sdv_authz_enable_value_server = (
            self.sdv_device_server.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        )
        self.sdv_authz_enable_value_client = (
            self.sdv_device_client.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        )

        # Enforce SDV Comm Stack authorization
        self.sdv_device_server.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'true')
        self.sdv_device_client.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'true')

        dump_lines_expected_before = [
            self.DIAGNOSTICS_AGENT_SUCCESSFUL_LOAD_MESSAGE,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_STARTING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_LISTENING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_REGISTERED,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_FREEZING_TEMPERATURE,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_STARTING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_LISTENING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_REGISTERED,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_IGNITION,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_STARTING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_LISTENING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_REGISTERED,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_RECEIVED,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_FOUND_DECLARATION,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_STARTING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_LISTENING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_REGISTERED,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_DIAG_DECLARATION,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_PERFORM,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_RESPONSE,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_READ_DATA_ITEM,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_IO_CONTROL_INTERACTION_STARTING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_IO_CONTROL_INTERACTION_LISTENING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ROUTING_CONTROL_INTERACTION_STARTING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ROUTING_CONTROL_INTERACTION_LISTENING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_STARTING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_LISTENING,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_REGISTERED,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_DIAG_DECLARATION,
            self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_RPC_CALL,]

        for lines in dump_lines_expected_before:
            assert_msg = (
                f'Timeout waiting for dumpsys chunks: {lines}.'
                ' Check debug logs for the last dumpsys report.'
            )
            WaitingMethods.wait_for_true(
                self._check_dumpsys,
                self.sdv_device_server,
                lines,
                self.DIAGNOSTICS_PROVIDER_BINDER_NAME,
                assert_msg=assert_msg,
            )

        self.report_before_running_sample_command = self.sdv_device_server.dumpsys(
            self.DIAGNOSTICS_PROVIDER_BINDER_NAME)

        self.sdv_device_server.prop.set(SdvDeviceProperty.ORCHESTRATOR_CONFIG_PATH, '/etc/orch/vm_diagnostics_provider1_sample_orch_config.textproto')
        self.sdv_device_server.reboot_device()
        self.sdv_device_server.wait_for_device_online()

        dump_lines_expected_after = [
            self.DIAGNOSTICS_TEST_SUCCESSFUL_LOAD_MESSAGE,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_REGISTERED,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_DISCOVERED,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_DIAG_DECLARATION,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_TRY_FREEZE,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_RPC_CALL,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_FREEZE,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_REGISTERED,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_DISCOVERED,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_DIAG_DECLARATION,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_TRY_ROUTINE,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_RPC_CALL,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_RPC_RESPONSE,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_TEST_RELEVANT_LOG,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_START_ROUTINE,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_DIAG_DECLARATION,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_UPDATE_STATUS,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_RPC_CALL,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_RPC_RESPONSE,
            self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_TRY_UPDATE,]

        for lines in dump_lines_expected_after:
            assert_msg = (
                f'Timeout waiting for dumpsys chunks: {lines}.'
                ' Check debug logs for the last dumpsys report.'
            )
            WaitingMethods.wait_for_true(
                self._check_dumpsys,
                self.sdv_device_server,
                lines,
                self.DIAGNOSTICS_PROVIDER_BINDER_NAME,
                assert_msg=assert_msg,
            )

        self.report_after_running_sample_command = self.sdv_device_server.dumpsys(
            self.DIAGNOSTICS_PROVIDER_BINDER_NAME)

    def teardown_class(self):
        # Reset SDV Comm Stack authorization
        self.sdv_device_server.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value_server)
        self.sdv_device_client.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value_client)
        super().teardown_class()

    def _check_dumpsys(self, device, expected_chunks, binder_name):
        """
        Helper function to check dumpsys for expected text chunks.

        Args:
            device: device on which to check the dumpsys output.
            expected_chunks: chunks of text to search for.
            binder_name: getting dumpsys for a specific binder name
        """
        report = device.dumpsys(binder_name)
        found = all(
            re.search(re.compile(t), report) is not None for t in expected_chunks
        )
        return found

    @parameterized.named_parameters(
        {
            'testcase_name': 'ListenToEnableConditionsStarting',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_STARTING,
        },
        {
            'testcase_name': 'ListenToEnableConditionsListening',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_LISTENING,
        },
        {
            'testcase_name': 'ListenToEnableConditionsRegistered',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_REGISTERED,
        },
        {
            'testcase_name': 'ListenToEnableConditionsFreezingTemperature',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ENABLE_CONDITIONS_FREEZING_TEMPERATURE,
        },
        {
            'testcase_name': 'ListenToOperationCyclesStarting',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_STARTING,
        },
        {
            'testcase_name': 'ListenToOperationCyclesListening',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_LISTENING,
        },
        {
            'testcase_name': 'ListenToOperationCyclesRegistered',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_REGISTERED,
        },
        {
            'testcase_name': 'ListenToOperationCyclesIgnition',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_OPERATION_CYCLES_IGNITION,
        },
        {
            'testcase_name': 'ListenToEventsStarting',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_STARTING,
        },
        {
            'testcase_name': 'ListenToEventsListening',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_LISTENING,
        },
        {
            'testcase_name': 'ListenToEventsRegistered',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_REGISTERED,
        },
        {
            'testcase_name': 'ListenToEventsReceived',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_RECEIVED,
        },
        {
            'testcase_name': 'ListenToEventsFoundDeclaration',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_EVENTS_FOUND_DECLARATION,
        },
        {
            'testcase_name': 'ListenToDataItemInteractionStarting',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_STARTING,
        },
        {
            'testcase_name': 'ListenToDataItemInteractionListening',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_LISTENING,
        },
        {
            'testcase_name': 'ListenToDataItemInteractionRegistered',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_REGISTERED,
        },
        {
            'testcase_name': 'ListenToDataItemInteractionDiagDeclaration',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_DIAG_DECLARATION,
        },
        {
            'testcase_name': 'ListenToDataItemInteractionRpcCallPerform',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_PERFORM,
        },
        {
            'testcase_name': 'ListenToDataItemInteractionRpcCallCallResponse',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_RESPONSE,
        },
        {
            'testcase_name': 'ListenToDataItemInteractionRpcCallReadDataItem',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_DATA_ITEM_INTERACTION_RPC_CALL_READ_DATA_ITEM,
        },
        {
            'testcase_name': 'ListenToIoControlInteractionStarting',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_IO_CONTROL_INTERACTION_STARTING,
        },
        {
            'testcase_name': 'ListenToIoControlInteractionListening',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_IO_CONTROL_INTERACTION_LISTENING,
        },
        {
            'testcase_name': 'ListenToRoutingControlInteractionStarting',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ROUTING_CONTROL_INTERACTION_STARTING,
        },
        {
            'testcase_name': 'ListenToRoutingControlInteractionListening',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_ROUTING_CONTROL_INTERACTION_LISTENING,
        },
        {
            'testcase_name': 'ListenToFaultControlInteractionStarting',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_STARTING,
        },
        {
            'testcase_name': 'ListenToFaultControlInteractionListening',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_LISTENING,
        },
        {
            'testcase_name': 'ListenToFaultControlInteractionRegistered',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_REGISTERED,
        },
        {
            'testcase_name': 'ListenToFaultControlInteractionDiagDeclaration',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_DIAG_DECLARATION,
        },
        {
            'testcase_name': 'ListenToFaultControlInteractionRpcCall',
            'expected_lines': EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE_LISTEN_TO_FAULT_CONTROL_INTERACTION_RPC_CALL,
        },
    )
    def test_diag_v1_provider1_sample_agent_start_success(self, expected_lines):
        if any(re.search(re.compile(line), self.report_before_running_sample_command) is None for line in expected_lines):
            asserts.fail(
                f'Messages not found [{expected_lines}] in dumpsys report: {self.report_before_running_sample_command}',
            )

    @parameterized.named_parameters(
        {
            'testcase_name': 'IoControlRegistered',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_REGISTERED,
        },
        {
            'testcase_name': 'IoControlDiscovered',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_DISCOVERED,
        },
        {
            'testcase_name': 'IoControlDiagDeclaration',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_DIAG_DECLARATION,
        },
        {
            'testcase_name': 'IoControlTryFreeze',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_TRY_FREEZE,
        },
        {
            'testcase_name': 'IoControlRpcCall',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_RPC_CALL,
        },
        {
            'testcase_name': 'IoControlFreeze',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_IO_CONTROL_FREEZE,
        },
        {
            'testcase_name': 'RoutineControlRegistered',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_REGISTERED,
        },
        {
            'testcase_name': 'RoutineControlDiscovered',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_DISCOVERED,
        },
        {
            'testcase_name': 'RoutineControlDiagDeclaration',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_DIAG_DECLARATION,
        },
        {
            'testcase_name': 'RoutineControlTryRoutine',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_TRY_ROUTINE,
        },
        {
            'testcase_name': 'RoutineControlRpcCall',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_RPC_CALL,
        },
        {
            'testcase_name': 'RoutineControlRpcResponse',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_RPC_RESPONSE,
        },
        {
            'testcase_name': 'RoutineControlRelevantLog',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_TEST_RELEVANT_LOG,
        },
        {
            'testcase_name': 'FaultControlStartRoutine',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_ROUTINE_CONTROL_START_ROUTINE,
        },
        {
            'testcase_name': 'FaultControlDiagDeclaration',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_DIAG_DECLARATION,
        },
        {
            'testcase_name': 'FaultControlUpdateStatus',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_UPDATE_STATUS,
        },
        {
            'testcase_name': 'FaultControlRpcCall',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_RPC_CALL,
        },
        {
            'testcase_name': 'FaultControlRpcResponse',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_RPC_RESPONSE,
        },
        {
            'testcase_name': 'FaultControlTryUpdate',
            'expected_lines': EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE_FAULT_CONTROL_TRY_UPDATE,
        },
    )
    def test_diag_v1_provider1_sample(self, expected_lines):
        if any(re.search(re.compile(line), self.report_after_running_sample_command) is None for
               line in expected_lines):
            asserts.fail(
                f'Message not found [{expected_lines}] in dumpsys report: {self.report_after_running_sample_command}',
            )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
