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

"""SDV Multivd Iptables IPv4 Test

Tests communication between Two SDV VMs using IPv4 and Iptables Rules set
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvMultivdIpTest(sdv_base_test.SdvBaseTestClass):

    GET_VLANID = 'getprop ro.boot.virt.address'
    SET_IP = 'ifconfig eth1 192.168.98.$(getprop ro.boot.virt.address)'
    PING_IP = 'ping -c 1 192.168.98.$(getprop ro.boot.virt.vm2)'
    PACKET_SUCCESS = '0% packet loss'
    PACKET_LOSS = '100% packet loss'
    CREATE_IPTABLES_RULES = (
        'iptables -A INPUT -p icmp --icmp-type echo-request -j DROP'
    )
    DELETE_IPTABLES_RULES = (
        'iptables -D INPUT -p icmp --icmp-type echo-request -j DROP'
    )
    LIST_INTERFACES = 'ip -o link show | cut -d " " -f 2'
    DELETE_VLAN = 'ip link delete eth1.'
    SERVER_SUBNET = '97'
    CLIENT_SUBNET = '98'

    def setup_class(self):
        logging.info('Setting Up SDV Multivd Iptables IPv4 Test Class')
        super().setup_class()
        # Get the device uisng label
        self.sdv_device_server = self.get_device('device1')
        self.sdv_device_client = self.get_device('device2')
        logging.info('End Setup Class For SDV Multivd Iptables IPv4 Test')

    def setup_ip_and_vlan(self, device, subnet_id):
        # Start Device IP Setup
        logging.info('Starting Device IP Setup')
        # Set static IP for device
        device.adb().execute_shell_command(self.SET_IP)
        # Create and setup vlan
        vlanid_str = device.adb().execute_shell_command(self.GET_VLANID)
        vlanid = int(vlanid_str) + 100
        create_vlan = (
            'ip link add link eth1 name eth1.'
            + str(vlanid)
            + ' type vlan id '
            + str(vlanid)
        )
        device.adb().execute_shell_command(create_vlan)
        add_ip_to_vlan1 = (
            'ip addr add 192.168.'
            + subnet_id
            + '.$(getprop ro.boot.virt.address)/24 dev eth1.'
            + str(vlanid)
        )
        device.adb().execute_shell_command(add_ip_to_vlan1)
        setup_vlan = 'ip link set up dev eth1.' + str(vlanid)
        device.adb().execute_shell_command(setup_vlan)
        logging.info('Completing Device IP Setup')

    def setup_test(self):
        logging.info('SDV Multivd Iptables IPv4 Test Setup')
        super().setup_test()

        # Server Device IP and VLAN Setup
        logging.info('Starting Server Device Setup')
        self.setup_ip_and_vlan(self.sdv_device_server, self.SERVER_SUBNET)
        logging.info('Completing Server Device Setup')

        # Client Device IP and VLAN Setup
        logging.info('Starting Client Device Setup')
        self.setup_ip_and_vlan(self.sdv_device_client, self.CLIENT_SUBNET)
        logging.info('Completing Client Device Setup')

        logging.info('End SDV Multivd Iptables IPv4 Test Setup')

    def test_multivd_ipv4(self):
        logging.info(
            'Start SDV Multivd Iptables IPv4 Test: test_multivd_iptables_ipv4'
        )

        # Verify Connection Success
        logging.info(
            'Verify if device 1 is able to ping device 2 when iptables are not'
            ' setup'
        )
        ping_results = self.sdv_device_server.adb().execute_shell_command(
            self.PING_IP
        )
        asserts.assert_in(
            self.PACKET_SUCCESS,
            ping_results,
            f'Ping results [{ping_results}] does not contain Expected Result'
            f' [{self.PACKET_SUCCESS}]when iptables rules are not set.',
        )

        # Verify Connection Failure when iptables rules are set
        logging.info(
            'Verify device 1 not able to ping device 2 when iptables are set'
        )
        self.sdv_device_client.adb().execute_shell_command(
            self.CREATE_IPTABLES_RULES
        )
        ping_results = ''
        try:
            ping_results = self.sdv_device_server.adb().execute_shell_command(
                self.PING_IP, raise_exception=True
            )
        except Exception as e:
            ping_results = str(e)
        asserts.assert_in(
            self.PACKET_LOSS,
            ping_results,
            f'Ping results [{ping_results}] does not contain Expected Result'
            f' [{self.PACKET_LOSS}]when iptables rules are set.',
        )

        logging.info(
            'End SDV Multivd Iptables IPv4 Test: test_multivd_iptables_ipv4'
        )

    def delete_vlan(self, device):
        interfaces = device.adb().execute_shell_command(self.LIST_INTERFACES)
        vlanid_str = device.adb().execute_shell_command(self.GET_VLANID)
        vlanid = int(vlanid_str) + 100
        if 'eth1.' + str(vlanid) in interfaces:
            device.adb().execute_shell_command(self.DELETE_VLAN + str(vlanid))

    def teardown_test(self):
        logging.info('SDV Multivd Iptables IPv4 Test Teardown')
        # Delete vlan on server
        self.delete_vlan(self.sdv_device_server)

        # Delete vlan on client
        self.delete_vlan(self.sdv_device_client)

        # Delete iptable rules
        self.sdv_device_client.adb().execute_shell_command(
            self.DELETE_IPTABLES_RULES
        )

        # Terminate Server subprocesses
        self.sdv_device_server.adb().terminate_all_subprocesses()
        super().teardown_test()
        logging.info('End SDV Multivd Iptables IPv4 Test Teardown')


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
