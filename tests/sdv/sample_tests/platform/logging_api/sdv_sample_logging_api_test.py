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

"""SDV Logging API Test"""

from mobly import asserts
import collections
import contextlib
import datetime
import logging
import os.path
import re

from typing import List
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

class SdvSampleLoggingAPITest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):

    IN_ASSERT_MESSAGE = 'The {} do not contain the expected string "{}"'
    REGEX_ASSERT_MESSAGE = 'The {} do not contain a match of the expected regex "{}"'
    NOT_IN_ASSERT_MESSAGE = 'The {} should not contain the string "{}"'
    NOT_REGEX_ASSERT_MESSAGE = 'The {} should not contain any match of the expected regex "{}"'
    REGEX_ASSERT_MESSAGE = 'Logs do not contain the {} regular expression'
    TIMESTAMP_ASSERT_MESSAGE = (
        'Logs do not contain expected timestamp (between {} and {}, got {})'
    )

    EXTENDED_REGEX_GREP_FLAG = '-E'
    SYSTEM_EVENTS_LOGCAT_FLAG = '-b events'
    DESCRIPTIVE_SYSTEM_EVENTS_LOGCAT_FLAG = (
        SYSTEM_EVENTS_LOGCAT_FLAG + ' -v descriptive'
    )

    REDIRECT_ERROR_TO_OUTPUT = '2>&1'

    SAMPLES_ROOT_DIR = '/apex/com.sdv.google.sample.logging/bin'

    @contextlib.contextmanager
    def property_override(self, name: str, value: str):
      original_value = None
      try:
        original_value = self.sdv_device.execute_shell_command(f"getprop '{name}'")
        self.sdv_device.execute_shell_command(f"setprop '{name}' '{value}'")
        yield
      finally:
        if original_value is not None:
          self.sdv_device.execute_shell_command(f"setprop '{name}' '{original_value}'")


    def sample_path(self, sample_name):
        return os.path.join(self.SAMPLES_ROOT_DIR, sample_name)

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def setup_test(self):
        super().setup_test()
        self.sdv_device.clear_logcat()

    def test_basic_log(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        simple_result_regex = (
            'logging_sdv_sample|sdv_library|'
            + 'log_sdv_rust_lib_sample|F DEBUG'
        )

        # Allow exception here because sample produces an error as part of its flow
        self.sdv_device.execute_shell_command(
            self.sample_path('logging_sdv_sample'),
            raise_exception=False,
        )
        log = self.sdv_device.grep_from_logcat(
            simple_result_regex, grep_args='-E'
        )
        basic_expected_logs = [
            'I logging_sdv_sample: test INFO log',
            'W logging_sdv_sample: test WARNING log',
            'E logging_sdv_sample: test ERROR log',
            'I sdv_library: INFO log from C++ library',
            'W sdv_library: WARNING log from C++ library',
            'E sdv_library: ERROR log from C++ library',
            'I log_sdv_rust_lib_sample: INFO log from Rust library',
            'W log_sdv_rust_lib_sample: WARNING log from Rust library',
            'E log_sdv_rust_lib_sample: ERROR log from Rust library',
        ]
        self.check_contains_all(log, basic_expected_logs, label='logs')
        basic_expected_regexes = [
            'F logging_sdv_sample: logging.cpp:[0-9]+] test FATAL log',
            "F DEBUG\s+: Abort message: 'test FATAL log'",
        ]
        self.check_matches_all(log, basic_expected_regexes, label='logs')

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    def test_basic_log_failed_check(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        simple_result_regex = (
            'logging_sdv_sample|sdv_library|'
            + 'log_sdv_rust_lib_sample|F DEBUG'
        )

        # Run the sample with failed check
        # Allow exception here because sample produces an error as part of its flow
        self.sdv_device.execute_shell_command(
            self.sample_path('logging_sdv_sample') + ' test',
            raise_exception=False,
        )
        log = self.sdv_device.grep_from_logcat(
            simple_result_regex, grep_args='-E'
        )
        failed_check_expected_logs = [
            "F DEBUG   : Abort message: 'Check failed: argc <= 1 "
            + "(argc=2, 1=1) test CHECK'",
        ]
        failed_check_unexpected_regexes = [
            'F logging_sdv_sample: logging.cpp:.+] test FATAL log',
            "F DEBUG.+: Abort message: 'test FATAL log'",
        ]
        self.check_contains_all(log, failed_check_expected_logs, label='logs')
        self.check_matches_none(
            log, failed_check_unexpected_regexes, label='logs'
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    def test_structured_log(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.sdv_device.execute_shell_command(
            self.sample_path('structured_logging_sdv_sample'),
        )

        # Get unstructured event log
        unstructured_event_log = self.sdv_device.grep_from_logcat(
            'logging_sample_event',
            logcat_args=self.SYSTEM_EVENTS_LOGCAT_FLAG,
            grep_args=self.EXTENDED_REGEX_GREP_FLAG,
        )

        # Get structured event log
        structured_event_log = self.sdv_device.grep_from_logcat(
            'logging_sample_event',
            logcat_args=self.DESCRIPTIVE_SYSTEM_EVENTS_LOGCAT_FLAG,
            grep_args=self.EXTENDED_REGEX_GREP_FLAG,
        )

        unstructured_log_expected_regex = (
            r'.*I logging_sample_event: \[\d+,\d+,string,\d+\.\d+\].*'
        )
        asserts.assert_regex(
            unstructured_event_log,
            unstructured_log_expected_regex,
            self.REGEX_ASSERT_MESSAGE.format('unstructured event log'),
        )

        structured_log_expected_regex = (
            r'.*I logging_sample_event: \[test i32=\d+,test i64=\d+,test'
            + r' string=string,test float=\d+.\d+].*'
        )
        asserts.assert_regex(
            structured_event_log,
            structured_log_expected_regex,
            self.REGEX_ASSERT_MESSAGE.format('structured event log'),
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    def test_structured_rust_log(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        # TODO: b/361610453 - include in com.sdv.google.sample.logging
        self.sdv_device.execute_shell_command(
            'structured_logging_sdv_rust_sample'
        )

        # Get logs
        log = self.sdv_device.grep_from_logcat(
            'logging_sample_event',
            logcat_args=self.SYSTEM_EVENTS_LOGCAT_FLAG,
            grep_args=self.EXTENDED_REGEX_GREP_FLAG,
        )

        structured_rust_expected_logs = [
            'I logging_sample_event: 5',
            'I logging_sample_event: 15',
            'I logging_sample_event: [test message,10.000000]',
            'I logging_sample_event: [test message,17,[10.000000]]',
            (
                'I logging_sample_event: [test'
                ' message,17,[10.000000,[2,3],[20,30]]]'
            ),
        ]
        self.check_contains_all(
            log, structured_rust_expected_logs, label='logs'
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    def basic_rust_expected_logs(self, max_level: str, use_regex: bool) -> List[str]:
        asserts.assert_in(
            max_level, 'VDIWEF',
            ('max_level must be one of: V (for VERBOSE), D (DEBUG), I (INFO), ' +
             'W (WARNING), E (ERROR), F (FATAL)'))
        if use_regex:
            basic_rust_logs = ['E logging_sdv_rust_sample: sdv_log: panicked at system/'
            + 'software_defined_vehicle/samples/logging/src/logging.rs:[0-9]+:[0-9]+:']
        else:
            basic_rust_logs = [
                'V logging_sdv_rust_sample: logging: test VERBOSE log 42',
                'D logging_sdv_rust_sample: logging: test DEBUG log',
                'I logging_sdv_rust_sample: logging: test INFO log',
                'W logging_sdv_rust_sample: logging: test WARNING log',
                'E logging_sdv_rust_sample: logging: test ERROR log',
                'V sdv_library: VERBOSE log from C++ library',
                'D sdv_library: DEBUG log from C++ library',
                'I sdv_library: INFO log from C++ library',
                'W sdv_library: WARNING log from C++ library',
                'E sdv_library: ERROR log from C++ library',
                (
                    'V logging_sdv_rust_sample: log_sdv_rust_lib_sample: VERBOSE'
                    ' log from Rust library'
                ),
                (
                    'D logging_sdv_rust_sample: log_sdv_rust_lib_sample: DEBUG log'
                    ' from Rust library'
                ),
                (
                    'I logging_sdv_rust_sample: log_sdv_rust_lib_sample: INFO log'
                    ' from Rust library'
                ),
                (
                    'W logging_sdv_rust_sample: log_sdv_rust_lib_sample: WARNING'
                    ' log from Rust library'
                ),
                (
                    'E logging_sdv_rust_sample: log_sdv_rust_lib_sample: ERROR log'
                    ' from Rust library'
                ),
                'E logging_sdv_rust_sample: test FATAL log',
                "F DEBUG   : Abort message: 'test FATAL log'",
            ]

        # Higher number = more verbose log
        def verbosity(s: str) -> int:
            return {
                'V': 6,
                'D': 5,
                'I': 4,
                'W': 3,
                'E': 2,
                'F': 1,
            }[s]

        return [log for log in basic_rust_logs if verbosity(log[0]) <= verbosity(max_level)]

    def log_level_basic_rust_test(self, max_level: str):
        expected_results_exact_contains = self.basic_rust_expected_logs(max_level, False)
        expected_results_regex_contains = self.basic_rust_expected_logs(max_level, True)

        # Allow exception here because sample produces an error as part of its flow
        self.sdv_device.execute_shell_command(
            self.sample_path('logging_sdv_rust_sample'),
            raise_exception=False,
        )
        rust_result_regex = 'logging_sdv_rust_sample|sdv_library|F DEBUG'
        log = self.sdv_device.grep_from_logcat(
            rust_result_regex,
            grep_args=self.EXTENDED_REGEX_GREP_FLAG,
        )
        self.check_contains_all(log, expected_results_exact_contains, label='logs')
        self.check_matches_all(log, expected_results_regex_contains, label='logs')

    def test_basic_rust_log(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.log_level_basic_rust_test(max_level='V')

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    def test_default_effective_log_level_is_info(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.log_level_basic_rust_test(max_level='I')

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    def test_persist_log_tag_overrides_default_log_level_info_at_boot(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        with self.property_override('persist.log.tag', 'W'):
            self.sdv_device.reboot_device()
            self.log_level_basic_rust_test(max_level='W')

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    @parameterized.named_parameters(
        {
            'testcase_name': 'debug',
            'level': 'DEBUG',
            'expected_results': (
                'D log_filtering_sdv_sample: test DEBUG log',
                'I log_filtering_sdv_sample: test INFO log',
                'W log_filtering_sdv_sample: test WARNING log',
                'E log_filtering_sdv_sample: test ERROR log',
            ),
        },
        {
            'testcase_name': 'info',
            'level': 'INFO',
            'expected_results': (
                'I log_filtering_sdv_sample: test INFO log',
                'W log_filtering_sdv_sample: test WARNING log',
                'E log_filtering_sdv_sample: test ERROR log',
            ),
        },
        {
            'testcase_name': 'warning',
            'level': 'WARNING',
            'expected_results': (
                'W log_filtering_sdv_sample: test WARNING log',
                'E log_filtering_sdv_sample: test ERROR log',
            ),
        },
        {
            'testcase_name': 'error',
            'level': 'ERROR',
            'expected_results': ('E log_filtering_sdv_sample: test ERROR log',),
        },
        {
            'testcase_name': 'verbose',
            'level': 'VERBOSE',
            'expected_results': (
                'V log_filtering_sdv_sample: test VERBOSE log',
                'D log_filtering_sdv_sample: test DEBUG log',
                'I log_filtering_sdv_sample: test INFO log',
                'W log_filtering_sdv_sample: test WARNING log',
                'E log_filtering_sdv_sample: test ERROR log',
            ),
        },
    )
    def test_filter_logs_level(self, level, expected_results):
        self.filter_logs_test(level, expected_results)

    def test_log_timestamps_from_system_clock(self):
        """Verify that logcat logs carry timestamps, and the timestamps come from

        the system clock (as reported by the `date` command).
        """
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        DATE_FORMAT = '%m-%d %H:%M:%S.%N'
        STRPTIME_FORMAT = '%m-%d %H:%M:%S.%f'

        def strip_timestamp(timestamp):
            m = re.search(r'\d\d-\d\d \d\d:\d\d:\d\d\.\d{3}', timestamp)
            if not m:
                raise ValueError(f'no timestamp found in: {timestamp}')
            return m.group(0)

        def get_device_time():
            device_time = self.sdv_device.execute_shell_command(
                f'date +"{DATE_FORMAT}"'
            )
            return datetime.datetime.strptime(
                strip_timestamp(device_time), STRPTIME_FORMAT
            )

        # Pre-initialize the time to avoid DST issues
        self.sdv_device.execute_shell_command('date -u -s "2000-01-01 00:00"')

        timestamp_start = get_device_time()
        # Allow exception here because sample produces an error as part of its flow
        self.sdv_device.execute_shell_command(
            self.sample_path('logging_sdv_sample'),
            raise_exception=False,
        )
        timestamp_end = get_device_time()

        # Get logs
        simple_result_regex = (
            'logging_sdv_sample|sdv_library|log_sdv_rust_lib_sample'
        )
        all_logs = self.sdv_device.grep_from_logcat(
            simple_result_regex, grep_args='-E'
        )
        all_logs = list(all_logs.split('\n'))

        asserts.assert_greater(
            len(all_logs), 0, self.REGEX_ASSERT_MESSAGE.format('logging sample')
        )

        for log in all_logs:
            timestamp = datetime.datetime.strptime(
                strip_timestamp(log), STRPTIME_FORMAT
            )
            asserts.assert_greater_equal(
                timestamp,
                timestamp_start,
                self.TIMESTAMP_ASSERT_MESSAGE.format(
                    timestamp_start, timestamp_end, timestamp
                ),
            )
            asserts.assert_less_equal(
                timestamp,
                timestamp_end,
                self.TIMESTAMP_ASSERT_MESSAGE.format(
                    timestamp_start, timestamp_end, timestamp
                ),
            )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    @parameterized.named_parameters(
        {
            'testcase_name': 'cpp',
            'tag': 'compile_time_log_filtering_sdv_sample',
            'executable': 'compile_time_log_filtering_sdv_sample',
        },
        {
            'testcase_name': 'rust',
            'tag': 'compile_time_log_filtering_rust_sample: compile_time_log_filtering',
            'executable': 'compile_time_log_filtering_sdv_rust_sample',
        },
    )
    def test_compile_time_log_filtering(self, tag, executable):
        """Verify that logs below certain level are compiled out of the binary."""
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        # Allow exception here because sample produces an error as part of its flow
        self.sdv_device.execute_shell_command(
            self.sample_path(executable), raise_exception=False
        )

        # Get logs
        log = self.sdv_device.grep_from_logcat(
            tag, grep_args=self.EXTENDED_REGEX_GREP_FLAG
        )
        # Extract all string literals from the binary executable.
        # Non-null-terminated strings (like Rust &'static str) may show up
        # concatenated with other ones.
        binary_strings = self.sdv_device.execute_shell_command(
            'strings ' + self.sample_path(executable)
        )

        self.check_contains_all(
            log,
            [
                f'I {tag}: test INFO log',
                f'W {tag}: test WARNING log',
                f'E {tag}: test ERROR log',
            ],
            label='logs',
        )
        self.check_contains_all(
            binary_strings,
            [
                'test INFO log',
                'test WARNING log',
                'test ERROR log',
            ],
            label=f'{executable} binary',
        )

        # these should not be logged, and not exist in the binary at all
        self.check_contains_none(
            log,
            [
                f'V {tag}: test VERBOSE log',
                f'D {tag}: test DEBUG log',
            ],
            label='logs',
        )
        self.check_contains_none(
            binary_strings,
            [
                'test VERBOSE log',
                'test DEBUG log',
            ],
            label=f'{executable} binary',
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

    @parameterized.named_parameters(
        {
            'testcase_name': 'cpp',
            'service_bundle_fqin': 'local-vm:com.sdv.google.sample.logging.LoggingSampleServiceBundleCpp/instance',
            'expected_tag': 'com_sdv_google_sample_logging_LoggingSampleServiceBundleCpp_instance',
            'expected_log': 'Logging sample service bundle CPP successfully {}!'
        },
        {
            'testcase_name': 'rust',
            'service_bundle_fqin': 'local-vm:com.sdv.google.sample.logging.LoggingSampleServiceBundle/instance',
            'expected_tag': 'com_sdv_google_sample_logging_LoggingSampleServiceBundle_instance',
            'expected_log': 'Logging sample service bundle successfully {}!'
        },
    )
    def test_service_bundle_log_tag_format(self, service_bundle_fqin, expected_tag, expected_log):
        """Verify that logs inside a Service Bundle are expected."""
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        # Verifies log after creating the service bundle
        self.sdv_device.execute_shell_command(f'sdv_service_bundle create {service_bundle_fqin}')
        WaitingMethods.wait_and_verify_expected_logs(self.sdv_device, expected_tag, expected_log.format('created'))

        # Verifies log after starting the service bundle
        self.sdv_device.execute_shell_command(f'sdv_service_bundle start {service_bundle_fqin}')
        WaitingMethods.wait_and_verify_expected_logs(self.sdv_device, expected_tag, expected_log.format('started'))

        # Verifies log after stopping the service bundle
        self.sdv_device.execute_shell_command(f'sdv_service_bundle stop {service_bundle_fqin}')
        WaitingMethods.wait_and_verify_expected_logs(self.sdv_device, expected_tag, expected_log.format('stopped'))

    @parameterized.named_parameters(
        {
            'testcase_name': 'cpp',
            'service_bundle_fqin': 'local-vm:com.sdv.google.sample.logging.LoggingSampleServiceBundleWithCustomTagCpp/instance',
            'expected_tag': 'logging_sample_service_bundle_with_custom_tag_cpp',
            'expected_log': 'Logging with custom log tag sample service bundle CPP successfully {}!'
        },
        {
            'testcase_name': 'rust',
            'service_bundle_fqin': 'local-vm:com.sdv.google.sample.logging.LoggingSampleServiceBundleWithCustomTag/instance',
            'expected_tag': 'logging_sample_service_bundle_with_custom_tag',
            'expected_log': 'Logging with custom log tag sample service bundle successfully {}!'
        },
    )
    def test_service_bundle_custom_log_tag(self, service_bundle_fqin, expected_tag, expected_log):
        """Verify that custom log tag of a Service Bundle is expected."""
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        # Verifies log after creating the service bundle
        self.sdv_device.execute_shell_command(f'sdv_service_bundle create {service_bundle_fqin}')
        WaitingMethods.wait_and_verify_expected_logs(self.sdv_device, expected_tag, expected_log.format('created'))

        # Verifies log after starting the service bundle
        self.sdv_device.execute_shell_command(f'sdv_service_bundle start {service_bundle_fqin}')
        WaitingMethods.wait_and_verify_expected_logs(self.sdv_device, expected_tag, expected_log.format('started'))

        # Verifies log after stopping the service bundle
        self.sdv_device.execute_shell_command(f'sdv_service_bundle stop {service_bundle_fqin}')
        WaitingMethods.wait_and_verify_expected_logs(self.sdv_device, expected_tag, expected_log.format('stopped'))

    def test_service_bundle_log_tag_consistency(self):
        """
        Once the logger is initialized, all logs from a Service Bundle process
        are expected to have the same log tag.
        """

        tags_per_pid = collections.defaultdict(set)

        with self.property_override('persist.log.tag', 'V'):
            # Collect all verbose logs until the device fully boots.
            self.sdv_device.reboot_device()
            WaitingMethods.wait_and_verify_expected_logs(self.sdv_device, 'Finished processing mode update Power', 'POWER_OFF_EXIT', poll_interval=1, timeout=120)
            service_bundle_logs = self.sdv_device.execute_shell_command('logcat -d -v brief -s SdvServiceManagerServer | grep ServiceFqin.*serviceBundleName.*UID.*PID.*')

        # The regex pattern looks for the literal strings 'serviceBundleName: ' followed by string enclosed in double quotes and 'PID ' followed by one or
        # more digits that it captures.
        pattern = re.compile(r'serviceBundleName:\s*"([^"]+)".+PID\s+(\d+)')
        pid_to_service_bundle_map = {
            int(match.group(2)): match.group(1) for line in service_bundle_logs.splitlines() if (match := pattern.search(line))
        }

        for pid, service_bundle_name in pid_to_service_bundle_map.items():
            # Filters out fake Service Bundles that are not started by Lifecycle Manager
            required_log = self.sdv_device.execute_shell_command(f'logcat -d -v -s lifecycle_manager | grep {service_bundle_name}.*is.*started', False)
            if not required_log:
                continue

            all_logs_in_process = self.sdv_device.execute_shell_command(f'logcat -d -v brief --pid={pid}')
            for line in all_logs_in_process.splitlines():
                # Example log in brief format:
                # I(    0) logdr: UID=2000 GID=2000 PID=12974 n tail=0 logMask=99 pid=0 start=0ns deadline=0ns  (logd)
                if m := re.match(r'^[VDIWEF]/(.*?)\(\s*(\d+)\):.*$', line.strip()):
                    tag = m.group(1).strip()
                    tags_per_pid[pid].add(tag)

        # Ignores PID 0 - it's used in kernel logs that use all sorts of tags
        violations = {pid_to_service_bundle_map[pid]: tags for pid, tags in tags_per_pid.items() if len(tags) > 1 and pid != 0}
        if violations:
            asserts.fail(
                'Some Service Bundles used multiple log tags:\n'
                + '\n'.join(f'* {service_bundle}: {", ".join(sorted(tags))}' for service_bundle, tags in sorted(violations.items(), key=lambda x: x[0]))
            )

    def check_contains_all(self, all_results, expected_results, label):
        for result in expected_results:
            asserts.assert_in(
                result,
                all_results,
                self.IN_ASSERT_MESSAGE.format(label, result),
            )

    def check_matches_all(self, all_results, expected_regexes, label):
        for regex in expected_regexes:
            asserts.assert_regex(
                all_results,
                regex,
                self.REGEX_ASSERT_MESSAGE.format(label, regex),
            )

    def check_contains_none(self, all_results, not_expected_results, label):
        for result in not_expected_results:
            asserts.assert_not_in(
                result,
                all_results,
                self.NOT_IN_ASSERT_MESSAGE.format(label, result),
            )

    def check_matches_none(self, all_results, not_expected_regexes, label):
        for regex in not_expected_regexes:
            asserts.assert_not_regex(
                all_results,
                regex,
                self.NOT_REGEX_ASSERT_MESSAGE.format(label, regex),
            )

    def filter_logs_test(self, level, expected_results):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        result = self.sdv_device.execute_shell_command(
            [
                f'LOGLEVEL={level}',
                self.sample_path('log_filtering_sdv_sample'),
                self.REDIRECT_ERROR_TO_OUTPUT,
            ],
        )
        # check the output
        self.check_contains_all(
            result,
            [
                f'LOGLEVEL = {level}',
                'LOGTAG = (default)',
                f'setting severity to {level}',
            ],
            label='logs',
        )
        # check the logs
        log = self.sdv_device.grep_from_logcat(
            'log_filtering_sdv_sample',
            grep_args=self.EXTENDED_REGEX_GREP_FLAG,
        )
        self.check_contains_all(log, expected_results, label='logs')

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} finished'
        )

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
