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

import enum


class SdvDeviceProperty(enum.Enum):
    # Persistent
    BURST_INTERVAL_SYS = (
        "persist.com.android.sdv.sample.someip.benchmark_interval"
    )
    BENCH_MODE_SYS = "persist.com.android.sdv.sample.someip.benchmark_mode"
    MSG_PER_BURST_SYS = (
        "persist.com.android.sdv.sample.someip.benchmark_msg_per_burst"
    )
    NR_BURST_SYS = "persist.com.android.sdv.sample.someip.benchmark_num_burst"
    LOG_TAG = "persist.log.tag"
    HEALTH_MONITOR_CONFIG_PATH = "persist.sdv.health_monitor.config_path"
    ORCHESTRATOR_CONFIG_PATH = "persist.sdv.orchestrator_config_path"
    # Read-only
    AUDIO_MIXER_CONFIG = "ro.boot.sdv.audio_mixer_config"
    BOOT_MODE = "ro.boot.sdv.boot_mode"
    HEALTH_MONITOR_AGENT_STARTUP_TIMEOUT_SEC = (
        "ro.boot.sdv.health_monitor.agent_startup_timeout_sec"
    )
    IGNORE_AVB_STATE = "ro.boot.sdv.ignore_avb_state"
    INIT_OPEN_DICE_SAMPLE_FILE = "ro.boot.sdv.init_open_dice.sample_file"
    INSTANCE_NAME = "ro.boot.sdv.instance_name"
    KEYMINT_RPC_HBK = "ro.boot.sdv.keymint.rpc.hbk"
    PREPROVISIONED_VVMTRUSTSTORE = "ro.boot.sdv.preprovisioned_vvmtruststore"
    VVMFACTORYTRUSTSTORE = "ro.boot.sdv.vvmfactorytrust"
    VIRT_ADDRESS = "ro.boot.virt.address"
    VIRT_VM2_ADDRESS = "ro.boot.virt.vm2"
    BUILD_FLAVOR = "ro.build.flavor"
    CPU_ARCH = "ro.product.cpu.abi"
    # Writable non-persistent
    AUTHZ_ENABLE = "sdv.authz.enable"
    VPM_POWER_STATE = "sdv.vpm.power.state"
    TELEMETRY_RESUMED_AT_TIMESTAMP = "sdv.telemetry.resumed_at_timestamp"


class SdvProperty:

    def __init__(self, android_device):
        """Initialize with a reference to the main device object.

        Args:
            android_device: The AndroidDevice object to use for interacting with
              the device.
        """
        self._device = android_device

    def set(
        self,
        sdv_property: SdvDeviceProperty,
        value: str,
    ):
        """Updates the value of an Android device property.

          Any subsequent actions needed for the system to fully recognize or
          utilize the updated property must be executed after this method.

        Args:
            sdv_property (SdvDeviceProperty): The specific device property to
              update.
            value (str): The value to set the property to. If
            empty string, system property is cleared.

        Raises:
            Exception:
                If value is None.
                If the property value is not set correctly.
        """
        if value is None:
            raise Exception("Value should not be None.")

        if value == "":
            self.clear(sdv_property)
            return

        self._device.log.info(
            f"Setting device system property: {sdv_property.value} = '{value}'"
        )

        self._device.adb.shell(f"setprop {sdv_property.value} {value}")

        if self.get(sdv_property) != value:
            raise Exception(
                f"Property '{sdv_property.value}' value did not set correctly."
                f" Expected '{value}', but got '{self.get(sdv_property)}'."
            )

    def get(self, sdv_property: SdvDeviceProperty) -> str:
        """Returns value of an Android device property.

        Args:
            sdv_property (SdvDeviceProperty): The Android device property.

        Returns:
            str: The value of the property. Empty string if property does
            not exist, or is empty.

        """
        # adb.getprop returns None both if property does not exist, or
        # if it is empty.
        value = self._device.adb.getprop(sdv_property.value)
        self._device.log.info(
            f"Device system property: {sdv_property.value} = '{value}'")

        if not value:
            value = ""

        return value

    def clear(self, sdv_property: SdvDeviceProperty) -> str:
        """Clears the value of an Android device property.
        Args:
            sdv_property (SdvDeviceProperty): Property to be cleared.
        """

        self._device.log.info(
            f"Clearing device system property: {sdv_property.value}"
        )

        self._device.adb.shell(f"setprop {sdv_property.value} \"\"")
