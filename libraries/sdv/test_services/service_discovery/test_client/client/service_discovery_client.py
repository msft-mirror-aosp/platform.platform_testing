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

"""
SDV Service Discovery Test Client
"""

import grpc
import logging

# TODO(b/358394708): Update these imports ( from proto import ...) once Python version issue is fixed.
from sdv_service_discovery_test_grpc.generated import service_discovery_pb2
from sdv_service_discovery_test_grpc.generated import service_discovery_pb2_grpc

class SdvServiceDiscoveryClient:
    def __init__(self, device, host_port = '31425', device_port = '31425'):
        self.serial = str(device.adb().get_device_serial())
        device.adb().root_device()
        device.adb().execute_forward_command(
            host_port = f"tcp:{host_port}",
            device_port = f"tcp:{device_port}"
        )
        device.adb().execute_shell_command_in_subprocess(
            'service_discovery_test_service_process',
            f"service_discovery_test_service {device_port}"
        )
        self.server_address = f"{self.serial.split(':', 1)[0]}:{host_port}"

    def create_identity(self, public_key, sdv_vm_name, sdv_package_name, service_bundle_name, service_instance_name):
        logging.info(
            'create_identity :: Connecting To Service Discovery Server: <%s> On Device: <%s>',
            self.server_address,
            self.serial
        )
        service_fqin = service_discovery_pb2.ServiceFqin(
                sdvVmName = sdv_vm_name,
                sdvPackageName = sdv_package_name,
                serviceBundleName = service_bundle_name,
                serviceInstanceName = service_instance_name
        )
        request = service_discovery_pb2.CreateIdentityRequest(
                public_key = public_key,
                service_fqin = service_fqin
        )
        with grpc.insecure_channel(self.server_address) as channel:
            stub  = service_discovery_pb2_grpc.ServiceDiscoveryStub(channel)
            response = stub.CreateIdentity(request)
            logging.info(
                'create_identity :: Create Identity Response: <%s>',
                response.response_message,
            )
            return response.response_message

    def register_service_unit(self, sdv_package_name, service_bundle_name, type_name, app_metadata_version, app_metadata_value_holder, service_unit_name):
        logging.info(
            'register_service_unit :: Connecting To Service Discovery Server: <%s> On Device: <%s>',
            self.server_address,
            self.serial
        )
        unit_type = service_discovery_pb2.UnitTypeRequest(
                sdvPackageName = sdv_package_name,
                serviceBundleName = service_bundle_name,
                typeName = type_name
        )
        app_metadata = service_discovery_pb2.AppMetadata(
                version = app_metadata_version,
                valueHolder = app_metadata_value_holder
        )
        request = service_discovery_pb2.RegisterServiceUnitRequest(
                unit_type = unit_type,
                app_metadata = app_metadata,
                service_unit_name = service_unit_name
        )
        with grpc.insecure_channel(self.server_address) as channel:
            stub  = service_discovery_pb2_grpc.ServiceDiscoveryStub(channel)
            response = stub.RegisterServiceUnit(request)
            logging.info(
                'register_service_unit :: Register Service Unit Response: <%s>',
                response.response_message,
            )
            return response.response_message

    def add_transport_metadata(self, transport_metadata_value_holder):
        logging.info(
            'add_transport_metadata :: Connecting To Service Discovery Server: <%s> On Device: <%s>',
            self.server_address,
            self.serial
        )
        request = service_discovery_pb2.AddTransportMetadataRequest(
                value_holder = transport_metadata_value_holder
        )
        with grpc.insecure_channel(self.server_address) as channel:
            stub  = service_discovery_pb2_grpc.ServiceDiscoveryStub(channel)
            response = stub.AddTransportMetadata(request)
            logging.info(
                'add_transport_metadata :: Add Transport Metadata Response: <%s>',
                response.response_message,
            )
            return response.response_message

    def get_service_units_by_type(self, sdv_package_name, service_bundle_name, type_name):
        logging.info(
            'get_service_units_by_type :: Connecting To Service Discovery Server: <%s> On Device: <%s>',
            self.server_address,
            self.serial
        )
        request = service_discovery_pb2.UnitTypeRequest(
                sdvPackageName = sdv_package_name,
                serviceBundleName = service_bundle_name,
                typeName = type_name
        )
        with grpc.insecure_channel(self.server_address) as channel:
            stub  = service_discovery_pb2_grpc.ServiceDiscoveryStub(channel)
            response = stub.GetServiceUnitsByType(request)
            logging.info(
                'get_service_units_by_type :: Get Service Units By Type Response: <%s>',
                response.response_message,
            )
            return response.response_message

    def get_service_units_by_name(self, sdv_vm_name, sdv_package_name, service_bundle_name, service_unit_name):
        logging.info(
            'get_service_units_by_name :: Connecting To Service Discovery Server: <%s> On Device: <%s>',
            self.server_address,
            self.serial
        )
        request = service_discovery_pb2.UnitNameRequest(
                sdvVmName = sdv_vm_name,
                sdvPackageName = sdv_package_name,
                serviceBundleName = service_bundle_name,
                unitName = service_unit_name
        )
        with grpc.insecure_channel(self.server_address) as channel:
            stub  = service_discovery_pb2_grpc.ServiceDiscoveryStub(channel)
            response = stub.GetServiceUnitsByName(request)
            logging.info(
                'get_service_units_by_name :: Get Service Units By Name Response: <%s>',
                response.response_message,
            )
            return response.response_message
