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
SDV Update Manager Agent Test

Check that the SDV Update Manager Agent functions correctly.
"""
from mobly import asserts
from sdv_test_fw.update.android_ota_package import AndroidOTAPackage
from sdv_test_fw.update.update_manager_client import UpdateManagerClient
from enum import Enum
import glob
import logging
import os
from pathlib import Path


def get_opposite_slot(slot):
    return 1 - slot


class InterruptAction(Enum):
    Rollback = "rollback"
    Suspend = "suspend"


class UpdateManagerBaseClass:
    """ Helper class for writing tests requiring apex update or system update functionality
    Usage:
        -- This class should be inherited. Final test class should inherit both `UpdateManagerBaseClass`
    and sdv_base_test.SdvBaseTestClass, as coupling exists.
        -- Test should call 'UpdateManagerBaseClass.set_updateable_apex_name(self, apex_name)' before using any
        util functions related to apex updating
    """

    ANDROID_BUILD_TOP_ENV_VAR = "ANDROID_BUILD_TOP"
    OTA_UPDATE_FILE_PATTERN = "sdv_core_*-ota*.zip"
    INTERRUPT_THRESHOLD_ARG = "--interrupt-threshold"
    INTERRUPT_ACTION_ARG = "--interrupt-action"
    PRODUCT_POST_INSTALL_LOG = "This /product partition post_install script of UpdateEngine."
    SYSTEM_POST_INSTALL_LOG = "This is /system partition post_install script of UpdateEngine."
    # Post install scripts are executed when "Status" is "5".
    # "000%" indicdates that the first post install script has started.
    POST_INSTALL_PROGRESS_LOG = "Status: 5, Progress: 000%"
    TEST_INTERRUPT_THRESHOLD = "3,0.5"
    USER_DATA_FILE_PATH = "/data/user_data_test_file.txt"
    USER_DATA_FILE_CONTENTS = "This is user data!"

    def init_update_manager_base_class(self, apex_name, device=None, local_artifact_dir="out/**"):
        self.apex_name = apex_name
        self.apex_file_name_v2 = f"{apex_name}.v2.apex"
        self.apex_v2_remote_file_pattern = f"**/{apex_name}.v2*.apex"
        self.apex_v2_local_file_pattern = f"**{self.apex_file_name_v2}"

        if device is None:
          self.sdv_device = self.get_device("device1")
        else:
          self.sdv_device = device
        self.local_dir = local_artifact_dir
        self.test_dir = self.get_test_dir()
        self.client = UpdateManagerClient(self.sdv_device.adb())

    def is_local(self):
        return self.user_params.get("env") == "local"

    def get_test_dir(self):
        test_dir_path = None

        if self.is_local():
            android_build_top = os.getenv(self.ANDROID_BUILD_TOP_ENV_VAR)

            if not android_build_top:
                raise Exception(
                    'Environment variable {self.ANDROID_BUILD_TOP_ENV_VAR} not set. '
                    'Please run in a terminal after using "lunch" to select a target'
                )

            test_dir_path = Path(android_build_top).joinpath(self.local_dir)
        else:
            log_path = Path(self.sdv_device.adb().log_path())
            logging.debug(f"Using log path to determine test dir: {log_path}")
            # This returns the first two directories eg. //tmp/tf-workdir...
            test_dir_path = Path().joinpath(*log_path.parts[:3])

        return str(test_dir_path)

    def find_first_matching_file(self, pattern):
        search_path = f"{self.test_dir}/**/{pattern}"
        logging.info(f"Searching {Path.cwd()} for pattern: {search_path}")
        file_names = glob.glob(search_path, recursive=True)
        if not file_names:
            raise Exception(f"Failed to find matching file in {search_path}")

        return file_names[0]

    def get_apex_v2_file_pattern(self):
        return self.apex_v2_local_file_pattern if self.is_local() else self.apex_v2_remote_file_pattern

    def get_ota_update_path(self):
        return Path(
            self.find_first_matching_file(self.OTA_UPDATE_FILE_PATTERN))

    def upload_service_bundle_update_payload(self):
        apex_local_file_path = self.find_first_matching_file(
            self.get_apex_v2_file_pattern())
        if not apex_local_file_path:
            raise Exception("APEX file not found")
        logging.info(f"Using APEX found at {apex_local_file_path}")
        apex_file_name = Path(apex_local_file_path).name
        apex_pre_staging_path = Path("/data/mydir")
        self.apex_destination_path = apex_pre_staging_path.joinpath(
            self.apex_file_name_v2)
        apex_destination_path_str = str(self.apex_destination_path)

        self.adb_shell(f"mkdir -p {apex_pre_staging_path}")
        self.sdv_device.adb().push(
            [apex_local_file_path, apex_destination_path_str])

    def upload_system_update_payload(self):
        system_update_local_file_path = self.get_ota_update_path()

        system_update_local_file_path_str = str(system_update_local_file_path)
        system_update_destination_directory = Path("/data/ota_package")
        self.system_update_payload = str(
            system_update_destination_directory / system_update_local_file_path.name)

        ota = AndroidOTAPackage(system_update_local_file_path_str)
        string_pairs_string = ota.properties.decode("utf-8").strip("\n")
        string_pairs = string_pairs_string.split("\n")

        self.system_update_prepare_args = " ".join(
            [f"-o {ota.offset}", f"-s {ota.size}"] + [f"-k {p}" for p in string_pairs])
        self.sdv_device.adb().push(
            [system_update_local_file_path_str, self.system_update_payload])

    def adb_shell(self, args):
        return self.sdv_device.adb().execute_shell_command(args)

    def get_apex_version_number(self):
        active_apex_file_path = self.adb_shell(
            f"find /apex -name '{self.apex_name}@*'")
        return int(active_apex_file_path.rsplit("@", 1)[-1])

    def get_current_slot(self):
        return int(self.adb_shell("bootctl get-current-slot"))

    def get_active_boot_slot(self):
        return int(self.adb_shell("bootctl get-active-boot-slot"))

    def get_staged_apex_directories(self):
        return self.adb_shell("ls /data/app-staging").split()

    def prepare_service_bundle_update(self):
        apex_destination_path_str = str(self.apex_destination_path)
        self.client.prepare_service_bundle_update(
            [apex_destination_path_str], 2)

    # interrupt_threshold should be either None or a string in the format of
    # "<status code>,<progress>". For example: "3,0.5"
    #
    # interrupt_action should be either None, or InterruptAction
    #
    # If true, include_parsed_args will add the parsed arguments from the
    # ota payload.
    #
    # If true, skip_unsubscribe will skip the unsubscribe call at the end of
    # the command, allowing tests to simulate clients becoming unavailable.
    def prepare_system_update(self, interrupt_threshold=None, interrupt_action=None, include_parsed_args=False, skip_unsubscribe=False):
        prepare_args = self.system_update_payload

        if interrupt_threshold:
            prepare_args = prepare_args + \
                f" {self.INTERRUPT_THRESHOLD_ARG} {interrupt_threshold}"

        if interrupt_action:
            prepare_args = prepare_args + \
                f" {self.INTERRUPT_ACTION_ARG} {interrupt_action.value}"

        if include_parsed_args:
            prepare_args = prepare_args + f" {self.system_update_prepare_args}"

        return self.client.prepare_system_update(prepare_args, skip_unsubscribe)

    def get_boot_slot_info(self):
        initial_current_slot = self.get_current_slot()
        initial_active_boot_slot = self.get_active_boot_slot()
        self.assert_equal(initial_current_slot, initial_active_boot_slot)
        return initial_current_slot, initial_active_boot_slot

    # Write file to userdata partition to test CORE-UPDATES-007
    def write_user_data_file(self):
        self.adb_shell(f'echo "{self.USER_DATA_FILE_CONTENTS}" > {self.USER_DATA_FILE_PATH}')

    # Confirm user data has been preserved during the system update (CORE-UPDATES-007)
    def assert_user_data_preserved(self):
        user_data_file_contents = self.adb_shell(f'cat {self.USER_DATA_FILE_PATH}')
        self.assert_equal(user_data_file_contents, self.USER_DATA_FILE_CONTENTS)

    def assert_user_data_removed(self):
        file_check = self.adb_shell(f'[ -e "{self.USER_DATA_FILE_PATH}" ] || echo "false"')
        self.assert_equal(file_check, "false")

    def run_and_commit_system_update(self, include_parsed_args=False):
        initial_current_slot, initial_active_boot_slot = self.get_boot_slot_info()

        self.write_user_data_file()

        pre_prepare_timestamp = self.sdv_device.adb().get_current_device_timestamp()
        prepare_client_output = self.prepare_system_update()
        self.assert_in_status("PREPARE_COMPLETE")
        logcat = self.sdv_device.adb().advance_logcat()
        self.assert_post_install_scripts_executed(
            logcat, pre_prepare_timestamp)
        self.assert_in(self.POST_INSTALL_PROGRESS_LOG, prepare_client_output)

        pre_activate_timestamp = self.sdv_device.adb().get_current_device_timestamp()
        activate_client_output = self.client.activate()
        self.assert_in_status("ACTIVATE_PRE_REBOOT_COMPLETE")
        self.assert_equal(self.get_active_boot_slot(),
                          get_opposite_slot(initial_active_boot_slot))
        logcat = self.sdv_device.adb().advance_logcat()
        self.assert_post_install_scripts_executed(
            logcat, pre_activate_timestamp)
        self.assert_in(self.POST_INSTALL_PROGRESS_LOG, activate_client_output)

        self.sdv_device.adb().reboot_device()

        self.assert_equal(self.get_active_boot_slot(),
                          get_opposite_slot(initial_active_boot_slot))
        self.assert_equal(self.get_current_slot(),
                          get_opposite_slot(initial_current_slot))
        self.assert_in_status("ACTIVATE_POST_REBOOT_COMPLETE")

        self.client.commit()
        self.assert_in_status("READY")

        self.sdv_device.adb().reboot_device()

        self.assert_in_status("READY")
        self.assert_equal(self.get_active_boot_slot(),
                          get_opposite_slot(initial_active_boot_slot))
        self.assert_equal(self.get_current_slot(),
                          get_opposite_slot(initial_current_slot))

        self.assert_user_data_preserved()

    def assert_in(self, search_term, search_space):
        asserts.assert_in(search_term, search_space)

    def assert_in_status(self, expected_status, status_text=None):
        if not status_text:
            status_text = self.client.status()

        self.assert_in(expected_status, status_text)

    def assert_equal(self, a, b):
        asserts.assert_equal(a, b)

    def assert_apex_version_number(self, expected_version):
        self.assert_equal(self.get_apex_version_number(), expected_version)

    def assert_no_staged_apex_directories(self):
        self.assert_equal(self.get_staged_apex_directories(), [])

    def assert_post_install_scripts_executed(self, logcat, timestamp):
        product_post_install_log = logcat.find_message_after_timestamp(
            self.PRODUCT_POST_INSTALL_LOG, timestamp)
        asserts.assert_is_not_none(
            product_post_install_log[0])

        system_post_install_log = logcat.find_message_after_timestamp(
            self.SYSTEM_POST_INSTALL_LOG, timestamp)
        asserts.assert_is_not_none(
            system_post_install_log[0])

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
