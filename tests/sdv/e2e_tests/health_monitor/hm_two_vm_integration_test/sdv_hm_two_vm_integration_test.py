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
This test verifies the multi-VM health monitoring functionality.

Two VMs are launched, each running:

1. A Health Monitor instance.
2. A sample service publishing heartbeats, monitored by the local Health Monitor.

One VM also hosts a Vehicle Health Monitor, which consumes VM health reports from both Health Monitor instances.

The test simulates unhealthy scenarios by briefly stopping heartbeat publishing on each VM. It then verifies that the Vehicle Health Monitor receives reports stating that each VM is unhealthy, and validates the list of unhealthy services within the unhealthy VM.
"""

from mobly import asserts
import time
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.logcat import log_processor
from sdv_test_fw.device.sdv_property import SdvDeviceProperty

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
VM_HEALTHY_LOG_TEMPLATE = "{fqin} reported: VM is HEALTHY"
VM_UNHEALTHY_LOG_TEMPLATE = "{fqin} reported: VM is UNHEALTHY"
UNHEALTHY_SERVICES_LOG_TEMPLATE = "List of unhealthy services in VM {device_name}: [{monitored_service_fqin}]"

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


class SdvHmTwoVmIntegrationTest(
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

        self.sdv_authz_enable_value_device1 = self.device1.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.device1.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, "permissions_only")
        self.sdv_authz_enable_value_device2 = self.device2.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.device2.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, "permissions_only")

    def teardown_class(self):
        self.device1.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value_device1)
        self.device2.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value_device2)
        super().teardown_class()

    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('HM multi-vm test setup')
        self.monitored_service_device_1 = self.device1.interactive_session()
        self.monitored_service_device_2 = self.device2.interactive_session()
        self.vm_monitoring_service_device = self.device2.interactive_session()
        self.logcat_processor = VmHealthMonitorLogProcessor(self.device2)

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
        instance_vmh_id = time.time_ns()
        while instance_1_id == instance_2_id:
            time.sleep(0.01)
            instance_2_id =  time.time_ns()
        while instance_1_id == instance_vmh_id | instance_2_id == instance_vmh_id:
            time.sleep(0.01)
            instance_vmh_id =  time.time_ns()
        monitored_service_one_fqin = MONITORED_SERVICE_FQIN_TEMPLATE.format(device_name=self.device1_name, instance_name=f"i{instance_1_id}")
        monitored_service_two_fqin = MONITORED_SERVICE_FQIN_TEMPLATE.format(device_name=self.device2_name, instance_name=f"i{instance_2_id}")

        vm_monitoring_service_fqin = VM_MONITORING_SERVICE_FQIN_TEMPLATE.format(device_name=self.device2_name, instance_name=f"i{instance_vmh_id}")

        health_monitor_fqin_device1 = HEALTH_MONITOR_SERVICE_FQIN_TEMPLATE.format(device_name=self.device1_name, instance_name="instance1")  # Assuming instance name
        health_monitor_fqin_device2 = HEALTH_MONITOR_SERVICE_FQIN_TEMPLATE.format(device_name=self.device2_name, instance_name="instance1")  # Assuming instance name

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

        # Start vm monitoring service and assert that both VMs are healthy
        self.vm_monitoring_service_device.send_command(f'sdv_service_bundle start {vm_monitoring_service_fqin}')
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device1),
        )
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device2),
        )

        # Start first monitored service heartbeats publishing and assert the first VM is healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(REGISTER_CONFIGURATION_CMD, [REGISTER_CONFIGURATION_CMD_RESPONSE])
        self.monitored_service_device_1.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device1),
        )

        # Start second monitored service heartbeats publishing and assert that the second VM is healthy
        self.monitored_service_device_2.send_command_and_wait_for_outputs(REGISTER_CONFIGURATION_CMD, [REGISTER_CONFIGURATION_CMD_RESPONSE])
        self.monitored_service_device_2.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device2),
        )

        # Stop first healthy service and assert first VM is unhealthy while second is healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(STOP_HEARTBEATS_PUBLISHING_CMD, [STOP_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.logcat_processor.wait_for_log(
            VM_UNHEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device1)
        )
        self.logcat_processor.wait_for_log(
             UNHEALTHY_SERVICES_LOG_TEMPLATE.format(device_name=self.device1_name, monitored_service_fqin=f'"{monitored_service_one_fqin}"')
        )
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device2)
        )

        # Resume first healthy service and assert both VMs are healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device1),
        )
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device2),
        )

        # Stop second healthy service and assert second VM is unhealthy while the first is healthy
        self.monitored_service_device_2.send_command_and_wait_for_outputs(STOP_HEARTBEATS_PUBLISHING_CMD, [STOP_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device1),
        )
        self.logcat_processor.wait_for_log(
            VM_UNHEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device2),
        )
        self.logcat_processor.wait_for_log(
            f'List of unhealthy services in VM {self.device2_name}: ["{monitored_service_two_fqin}"]',
        )

        # Resume second healthy service and assert both VMs are healthy
        self.monitored_service_device_2.send_command_and_wait_for_outputs(START_HEARTBEATS_PUBLISHING_CMD, [START_HEARTBEATS_PUBLISHING_CMD_RESPONSE])
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device1),
        )
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device2),
        )

        # Unregister both service bundles and assert both VMs are healthy
        self.monitored_service_device_1.send_command_and_wait_for_outputs(DEREGISTER_CONFIGURATION_CMD, [DEREGISTER_CONFIGURATION_CMD_RESPONSE])
        self.monitored_service_device_2.send_command_and_wait_for_outputs(DEREGISTER_CONFIGURATION_CMD, [DEREGISTER_CONFIGURATION_CMD_RESPONSE])
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device1),
        )
        self.logcat_processor.wait_for_log(
            VM_HEALTHY_LOG_TEMPLATE.format(fqin=health_monitor_fqin_device2),
        )


class VmHealthMonitorLogProcessor:
    """
    This class processes logcat logs to verify the health status
    of VMs. It searches for specific log messages related to VM health and
    provides methods to wait for expected logs within a timeout.

    It keeps track of the latest message timestamp (self.latest_message_time)
    to avoid matching against the same message twice. This ensures that each
    call to wait_for_log() only considers new log entries.
    """
    def __init__(self, device):
        self.device = device
        self.latest_message_time = None

    def _grep_logs(self):
        return self.device.grep_from_logcat("oem_sample_vm_health")

    def _find_message(self, message):
        logs_processor = log_processor.LogcatProcessor(
           self._grep_logs()
        )
        timestamp, message = logs_processor.find_message_after_timestamp(message, self.latest_message_time)

        if timestamp is not None:
            self.latest_message_time = timestamp

        return (timestamp, message)

    def wait_for_log(self, expected_log, timeout=10):
        logging.info(f'Waiting for log: {expected_log}')

        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            _, message = self._find_message(expected_log)
            if message:
                logging.info(f'Found expected log "{expected_log}" within "{message}"')
                return True  # Indicate success

            time.sleep(0.1)

        asserts.fail(
            f'Logcat result not found in device "{self.device.get_device_serial()}"'
            f'was expecting the following log {expected_log}'
            f'found the following logs: {self._grep_logs()}'
            f'within timeout: {timeout} seconds.'
        )

if __name__ == "__main__":
    sdv_test_runner.run()
