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
SDV sample 'CUJ-Core-22' test.

Test is based on CORE-CUJ catalog and samples.

Verifies:

 * SDV orchestration on VM start-up,
 * Cross-vm publishing and receiving message,
 * Cross-vm SDV RPC call request and response.
 * Cross-vm ACLs sharing.
 * Authorization enforcement for Service Discovery, Data Tunnel and RPC server.
"""

import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from cuj22_common.sdv_cuj22_base import SdvCuj22Base

class SdvSampleCujCore22Test(sdv_base_test.SdvBaseTestClass, SdvCuj22Base):
    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        super().setup_class()
        self.setup_cuj22_devices(boot_logs=True)

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    ################################################
    ##            Utility functions.              ##
    ################################################

    def test_start_on_boot_service_server(self):
        self.log_enter()
        self.verify_server_bundle_started()
        self.log_exit()

    def test_start_on_boot_service_client(self):
        self.log_enter()
        self.verify_client_bundle_started()
        self.log_exit()

    def test_pub_sub_foo_message(self):
        self.log_enter()
        self.verify_logs_pub_sub_foo_message()
        self.log_exit()

    def test_rpc_foo(self):
        self.log_enter()
        self.verify_logs_rpc_foo_in_client()
        self.log_exit()

    def test_authz_message_publisher(self):
        self.log_enter()
        self.verify_logs_authz_message_publisher()
        self.log_exit()

    def test_authz_message_subscriber(self):
        self.log_enter()
        self.verify_logs_authz_message_subscriber()
        self.log_exit()

    def test_authz_rpc_server(self):
        self.log_enter()
        self.verify_logs_authz_rpc_server()
        self.log_exit()

    def test_authz_rpc_client(self):
        self.log_enter()
        self.verify_logs_authz_rpc_client()
        self.log_exit()

    def test_increasing_occurrences_of_pub_sub_foo_message(self):
        self.log_enter()
        self.verify_increasing_occurrences_of_pub_sub_foo_message()
        self.log_exit()

    def test_increasing_occurrences_of_rpc_foo(self):
        self.log_enter()
        self.verify_increasing_occurrences_of_rpc_foo_in_client()
        self.log_exit()


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
