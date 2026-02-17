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
This test verifies the multi-VM health monitoring functionality using the SOME/IP Vehicle Health Monitor.

Two VMs are launched, each running:

1. A Health Monitor instance.
2. A sample service publishing heartbeats, monitored by the local Health Monitor.

One VM also hosts a SOME/IP Vehicle Health Monitor, which subscribes to and consumes VM health reports from both Health Monitor instances via SOME/IP.

The test simulates unhealthy scenarios by briefly stopping heartbeat publishing on each VM. It then verifies that the SOME/IP Vehicle Health Monitor receives reports stating that each VM is unhealthy, and subsequently healthy when heartbeats resume.
"""


import time
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
import re

# Commands for monitored service bundle
REGISTER_CONFIGURATION_CMD = "RegisterConfiguration"
START_HEARTBEATS_PUBLISHING_CMD = "StartHeartbeatsPublishing"
STOP_HEARTBEATS_PUBLISHING_CMD = "StopHeartbeatsPublishing"
DEREGISTER_CONFIGURATION_CMD = "DeregisterConfiguration"

# Responses of monitored service bundle
REGISTER_CONFIGURATION_CMD_RESPONSE = "Registering monitoring configuration"
START_HEARTBEATS_PUBLISHING_CMD_RESPONSE = "Publishing heartbeats"
STOP_HEARTBEATS_PUBLISHING_CMD_RESPONSE = "Stopping heartbeats Publishing"
DEREGISTER_CONFIGURATION_CMD_RESPONSE = "Deregistering configuration"

# String templates for service bundle FQINs
MONITORED_SERVICE_FQIN_TEMPLATE = "{device_name}:com.android.sdv.sample.oem.health.monitored.SampleHMBundle/{instance_name}"
VM_MONITORING_SERVICE_FQIN_TEMPLATE = "{device_name}:com.android.sdv.sample.oem.health.monitoring.VmHMBundle/{instance_name}"
HEALTH_MONITOR_SERVICE_FQIN_TEMPLATE = "{device_name}:com.android.sdv.health.HealthMonitorServiceBundle/{instance_name}"

# String templates for log messages
VM_HEALTHY_LOG_TEMPLATE = "[INFO] Received health report from {device_name}. All monitored service bundles healthy? True All service bundles alive? True"
VM_UNHEALTHY_LOG_TEMPLATE = "[INFO] Received health report from {device_name}. All monitored service bundles healthy? False All service bundles alive? True"

# VSOMEIP settings
VSOMEIP_CONFIGURATION_PATH = "VSOMEIP_CONFIGURATION=/vendor/etc/vsomeip/vehicle_health_monitor_sample.json VSOMEIP_BASE_PATH=/data/vendor/vsomeip/ sdv_vsomeip_vehicle_health_monitor_sample_target"

class MonitoredServiceBundleConfiguration:
    bundle_fqin: str
    initial_delay_ms: int
    period_ms: int
    num_periods: int
    task_duration_ms: int

    def __init__(self, bundle_fqin: str, is_heartbeat_monitored: bool, initial_delay_ms: int, period_ms: int, num_periods: int, task_duration_ms: int):
        self.bundle_fqin = bundle_fqin
        self.is_heartbeat_monitored = is_heartbeat_monitored
        self.initial_delay_ms = initial_delay_ms
        self.period_ms = period_ms
        self.num_periods = num_periods
        self.task_duration_ms = task_duration_ms


class SdvHmTwoVmSomeIpIntegrationTest(
    sdv_base_test.SdvBaseTestClass
):
    def setup_class(self):
        super().setup_class()
        self.device1 = self.get_device('device1').adb()
        self.device2 = self.get_device('device2').adb()

        self.device1_name = self.device1.execute_shell_command(
            'getprop ro.boot.sdv.instance_name', raise_exception=True
        )
        self.device2_name = self.device2.execute_shell_command(
            'getprop ro.boot.sdv.instance_name', raise_exception=True
        )

    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('HM multi-vm test setup')
        self.monitored_service_device_1 = self.device1.interactive_session()
        self.monitored_service_device_2 = self.device2.interactive_session()
        self.vm_monitoring_service_device = self.device1.interactive_session()

    def teardown_test(self):
        self.monitored_service_device_1.close()
        self.monitored_service_device_2.close()
        self.vm_monitoring_service_device.close()

    def start_monitored_service(self, session, config: MonitoredServiceBundleConfiguration):
        session.send_command("sdv_integration_test_health_monitored_service")
        session.send_command(f'{config.bundle_fqin}')
        session.send_command(f'{config.initial_delay_ms}')
        session.send_command(f'{config.task_duration_ms}')
        session.send_command(f'{config.num_periods}')
        session.send_command(f'{config.period_ms}')

    def test_two_monitored_services_and_vm_monitor(self):
        # Generate random instance ids to prevent the AlreadyExists SD error at restart
        instance_1_id = time.time_ns()
        instance_2_id = time.time_ns()
        while instance_1_id == instance_2_id:
            time.sleep(0.01)
            instance_2_id =  time.time_ns()
        monitored_service_one_fqin = MONITORED_SERVICE_FQIN_TEMPLATE.format(device_name=self.device1_name, instance_name=f"i{instance_1_id}")
        monitored_service_two_fqin = MONITORED_SERVICE_FQIN_TEMPLATE.format(device_name=self.device2_name, instance_name=f"i{instance_2_id}")

        self.start_monitored_service(self.monitored_service_device_1, MonitoredServiceBundleConfiguration(
            bundle_fqin=monitored_service_one_fqin,
            is_heartbeat_monitored=True,
            initial_delay_ms=50,
            task_duration_ms=5,
            num_periods=1,
            period_ms=5,
        ))
        self.start_monitored_service(self.monitored_service_device_2, MonitoredServiceBundleConfiguration(
            bundle_fqin=monitored_service_two_fqin,
            is_heartbeat_monitored=True,
            initial_delay_ms=50,
            task_duration_ms=5,
            num_periods=1,
            period_ms=5,
        ))

        # Start vm monitoring service and assert that both VMs are healthy, no service bundles report heartbeats
        self.vm_monitoring_service_device.send_command(VSOMEIP_CONFIGURATION_PATH)
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device1_name)),]
        )
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device2_name)),]
        )

        # Start first monitored service heartbeats publishing and assert the first VM is healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(REGISTER_CONFIGURATION_CMD, [REGISTER_CONFIGURATION_CMD_RESPONSE])
        self.monitored_service_device_1.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device1_name)),]
        )

        # Start second monitored service heartbeats publishing and assert that the second VM is healthy
        self.monitored_service_device_2.send_command_and_wait_for_outputs(REGISTER_CONFIGURATION_CMD, [REGISTER_CONFIGURATION_CMD_RESPONSE])
        self.monitored_service_device_2.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device2_name)),]
        )

        # Stop first healthy service and assert first VM is unhealthy while second is healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(STOP_HEARTBEATS_PUBLISHING_CMD, [STOP_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_UNHEALTHY_LOG_TEMPLATE.format(device_name=self.device1_name)),]
        )
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device2_name)),]
        )

        # Resume first healthy service and assert both VMs are healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device1_name)),]
        )
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device2_name)),]
        )

        # Stop second healthy service and assert second VM is unhealthy while the first is healthy
        self.monitored_service_device_2.send_command_and_wait_for_outputs(STOP_HEARTBEATS_PUBLISHING_CMD, [STOP_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device1_name)),]
        )
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_UNHEALTHY_LOG_TEMPLATE.format(device_name=self.device2_name)),]
        )

        # Resume second healthy service and assert both VMs are healthy
        self.monitored_service_device_2.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device1_name)),]
        )
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device2_name)),]
        )

        # Unregister both service bundles and assert both VMs are healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(DEREGISTER_CONFIGURATION_CMD, [DEREGISTER_CONFIGURATION_CMD_RESPONSE])
        self.monitored_service_device_2.send_command_and_wait_for_outputs(DEREGISTER_CONFIGURATION_CMD, [DEREGISTER_CONFIGURATION_CMD_RESPONSE])
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device1_name)),]
        )
        self.vm_monitoring_service_device.expect_outputs(
            outputs=[re.escape(VM_HEALTHY_LOG_TEMPLATE.format(device_name=self.device2_name)),]
        )

if __name__ == "__main__":
    sdv_test_runner.run()
