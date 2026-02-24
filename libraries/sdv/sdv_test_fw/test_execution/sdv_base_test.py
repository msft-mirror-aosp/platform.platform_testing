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

import argparse
import logging
import re
import signal
import sys
import traceback

from mobly import base_test
from mobly import signals
from mobly.controllers import android_device
from mobly.controllers.android_device_lib.services import logcat
from sdv_test_fw.device import sdv_device
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


class SdvBaseTestClass(base_test.BaseTestClass):
    """SDV Test Framework Base Test Class

    Each test must extend SdvBaseTestClass

    Example usage:

        class TestClassName(sdv_base_test.SdvBaseTestClass):
            setup_class
            setup_test
            teardown_test
            test_sometest
    """

    __DEFAULT_NUMBER_OF_DEVICES = 1
    __NUMBER_OF_DEVICES = 'number_of_devices'

    def __parse_test_args(self):
        """Get Test Args

        Extracts the test arguments from the System Args

        usage:
        python3 <test> -- -c /tmp/config.yaml --test_args=k1=v1
        --test_args=k2=v2

        CATBox usage:
        catbox-tradefed run commandAndExit <test-plan> --mobly-options
        --test_args=k1=v1 --mobly-options --test_args=k2=v2

        atest usage:
        atest <test> --
        --test-arg
        com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-options:--test_args=k1=v1
        --test-arg
        com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-options:--test_args=k2=v2

        Returns: Dictionary with key-value pair
        e.g.
        {
            k1: v1,
            k2: v2
        }
        """
        parser = argparse.ArgumentParser(description='Parse Test Args.')
        group = parser.add_mutually_exclusive_group(required=False)
        group.add_argument(
            '--test_args',
            action='append',
            nargs='+',
            type=str,
            help='A list of test args for the test.',
        )
        parsed_test_args = parser.parse_known_args(sys.argv)[0]

        if not parsed_test_args.test_args:
            return {}

        test_args_list = [
            test_arg
            for test_args in parsed_test_args.test_args
            for test_arg in test_args
        ]
        test_args = {
            test_arg.split('=')[0]: test_arg.split('=')[1]
            for test_arg in test_args_list
        }
        return test_args

    def __get_instance_number(self, instance_name):
        return int(instance_name[len('instance') :])

    def __is_instance_name_in_valid_format(self, instance_name):
        # Check for instance name format: instance{number}
        return re.search('^instance[0-9]+$', instance_name)

    def __is_instance_number_valid(self, instance_name):
        instance_number = self.__get_instance_number(instance_name)
        return instance_number > 0 and instance_number <= self.__num_of_devices

    def __is_device_tag_update_needed(self, instance_name, device_number):
        # Check if instance_name exist
        # and if instance_name is in the format of instance{number}
        # and if the instance id is between 1 and self.__num_of_devices,
        # and if the instance id does not match current device number
        # then update the device tag
        return (
            instance_name
            and self.__is_instance_name_in_valid_format(instance_name)
            and self.__is_instance_number_valid(instance_name)
            and self.__get_instance_number(instance_name) != device_number
        )

    def __prepare_logcat(self, device):
        """Enable verbose logs and restart logcat service with 'clear_log' set to False"""

        # Enable verbose logs. By default, only INFO and higher are enabled.
        # Use persist property to keep it across reboots.
        device.adb().prop.set(SdvDeviceProperty.LOG_TAG, 'V')

        device.services().logcat.stop()
        device.services().logcat.update_config(logcat.Config(clear_log=False))
        device.services().logcat.start()
        device.adb().verify_logcat_is_running()

    def __get_device_list(self):
        device_list = {}
        default_device_list = {}

        for index in range(self.__num_of_devices):
            device_tag = f'device{index+1}'

            # The Device Host Interaction (DHI) strategy depends on the execution environment.
            # We explicitly pass the environment context (local vs. remote) because the
            # device object cannot infer it otherwise.
            device = sdv_device.SdvDevice(
                android_device.get_device(self.__ads, label=device_tag),
                is_local_run=self.is_local_run(),
            )

            default_device_list[device_tag] = device

            """
        It is not possible to specify the device in a particular order
        for atest. Also, sometimes the order of devices is not maintained
        on CI/CD which causes the test to fail, resulting in flakiness.
        To avoid this, we use the instance name on the device to assign
        the devices in correct order for the test. If the instance name is
        not provided or is not in the correct format, we will use the
        default order.
      """
            if self.__is_device_tag_update_needed(
                device.info.instance_name, index + 1
            ):
                device_tag = f'device{self.__get_instance_number(device.info.instance_name)}'

            device_list[device_tag] = device

        # Fail Safe or Fallback: This happens when same instance name is
        # assigned to multiple devices or when the instance names are not
        # assigned correctly. e.g. device1->instance2, device2->no instance name
        if len(device_list) != self.__num_of_devices:
            device_list.clear()
            assigned_devices = [{
                tag: device.adb().get_device_serial()
                for tag, device in default_device_list.items()
            }]
            logging.info(f'Assigned devices : {assigned_devices}')
            return default_device_list

        default_device_list.clear()
        assigned_devices = [{
            tag: device.adb().get_device_serial()
            for tag, device in device_list.items()
        }]
        logging.info(f'Assigned devices : {assigned_devices}')
        return device_list

    def _debug_sigterm_handler(self, signum, frame):
        """Catches SIGTERM (triggered by test timeouts or external aborts) and dumps

        stack traces of all threads to aid debugging.
        """
        debug_msg = [
            '\n\n'
            '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n'
            '!!! SIGTERM RECEIVED: Test timed out or was aborted externally.   '
            ' !!!\n'
            '!!! DUMPING STACK TRACES FOR ALL THREADS BELOW                    '
            ' !!!\n'
            '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
        ]
        for thread_id, stack_frame in sys._current_frames().items():
            debug_msg.append(f'\n--- Thread ID: {thread_id} ---')
            debug_msg.append(''.join(traceback.format_stack(stack_frame)))

        logging.error(''.join(debug_msg))

        raise signals.TestAbortAll(
            'Test aborted due to SIGTERM (timeout or external abort). Stack'
            ' traces dumped above.'
        )

    def __init__(self, configs):
        super().__init__(configs)
        signal.signal(signal.SIGTERM, self._debug_sigterm_handler)

    def setup_class(self):
        logging.info('Start SDV Base Test Class Setup')

        self.__suite_name = self.__class__.__name__
        self.__num_of_devices = self.__DEFAULT_NUMBER_OF_DEVICES
        if self.__NUMBER_OF_DEVICES in self.user_params:
            self.__num_of_devices = self.user_params[self.__NUMBER_OF_DEVICES]
        else:
            logging.warning(
                '%s is not in testbed config. Using default value: %d',
                self.__NUMBER_OF_DEVICES,
                self.__DEFAULT_NUMBER_OF_DEVICES,
            )
        logging.info(
            'Registering %d device(s) for test execution', self.__num_of_devices
        )
        self.__ads = self.register_controller(
            android_device, min_number=self.__num_of_devices
        )

        logging.info('Get the list of devices for test execution')
        self.__device_list = self.__get_device_list()

        # Because 'clear_log' is True by default and we would like to keep logs
        # even when logcat is restarted.
        for device in self.__device_list.values():
            self.__prepare_logcat(device)

        logging.info('Reading And Parse Test Arguments')
        self.__test_args = self.__parse_test_args()

        logging.info('End SDV Base Test Class Setup')

    def setup_test(self):
        logging.info('Start SDV Test Setup')
        self.clear_all_devices()
        logging.info('End SDV Test Setup')

    def teardown_test(self):
        logging.info('Start SDV Test Teardown')
        self.clear_all_devices()
        logging.info('End SDV Test Teardown')

    def clear_all_devices(self):
        for _, device in self.__device_list.items():
            device.adb().remove_all_temp_files()
            device.adb().terminate_all_subprocesses()

    def get_device(self, device_label):
        if device_label not in self.__device_list:
            raise Exception(f'Device with label {device_label} not available.')

        return self.__device_list[device_label]

    def get_device_list(self):
        return self.__device_list

    def set_devices(self, num_devices):
        """This method is used to set the number of devices for the test.

        It creates variables for each device in the format sdv_device{i} where i
        is the device number.

        Args:
          num_devices: The number of devices used in the test.
        """
        for i in range(1, num_devices + 1):
            setattr(self, f'sdv_device{i}', self.get_device(f'device{i}'))

    def log_test_info(self, message):
        """Logs information in a test, along with the suite and test name."""
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} ::'
            f' {message}'
        )

    def get_test_arg(self, test_arg_name):
        if test_arg_name not in self.__test_args:
            raise Exception(f'Test argument {test_arg_name} not available.')

        return self.__test_args[test_arg_name]

    def get_suite_name(self):
        return self.__suite_name

    def is_local_run(self):
        """Checks if the test execution environment is local.

        Relies on the 'env' user parameter existing and being set to 'local'.
        When running tests locally, ensure that the test uses the testbed that
        sets this parameter: "*_local.yaml"

        Returns:
          bool: True if the test is running locally, False otherwise.
        """
        return self.user_params.get('env') == 'local'
