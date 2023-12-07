"""
Bluetooth Base Test

This test class serves as a base class for others to inherit from.
It also serves as a device-cleaner to help reset devices between tests.

"""

import sys
import logging
import pprint


from mobly import asserts
from mobly import base_test
from mobly import test_runner
from mobly.controllers import android_device

from utilities import spectatio_utils
from utilities import bt_utils

class BluetoothBaseTest(base_test.BaseTestClass):


    def setup_class(self):
        # Registering android_device controller module, and declaring that the test
        # requires at least two Android devices.
        # This setup will need to be overwritten or extended if a test uses three devices.
        logging.info("Running basic class setup.")
        self.ads = self.register_controller(android_device, min_number=2)
        # The device used to discover Bluetooth devices.
        self.discoverer = android_device.get_device(
            self.ads, label='auto')
        # Sets the tag that represents this device in logs.
        self.discoverer.debug_tag = 'discoverer'
        # The device that is expected to be discovered
        self.target = android_device.get_device(self.ads, label='phone')
        self.target.debug_tag = 'target'
        logging.info("\tLoading Snippets.")
        self.target.load_snippet('mbs', android_device.MBS_PACKAGE)
        self.discoverer.load_snippet('mbs', android_device.MBS_PACKAGE)
        logging.info("\tInitializing Utilities")
        self.call_utils = (spectatio_utils.CallUtils(self.discoverer))
        self.bt_utils = (bt_utils.BTUtils(self.discoverer, self.target))

    def setup_test(self):
        # Make sure bluetooth is on.
        logging.info("Running basic test setup.")
        logging.info("Enable Bluetooth on Target device")
        self.target.mbs.btEnable()
        logging.info("Enable Bluetooth on Discoverer device")
        self.discoverer.mbs.btEnable()

    def teardown_test(self):
        # Turn Bluetooth off on both devices.
        logging.info("Running basic test teardown.")
        # unpair Discoverer device from Target
        logging.info("Unpair Discoverer device from Target")
        discoverer_address = self.discoverer.mbs.btGetAddress()
        logging.info(f"Discoverer device address: {discoverer_address}")
        target_paired_devices = self.target.mbs.btGetPairedDevices()
        _, target_paired_addresses = self.bt_utils.get_info_from_devices(target_paired_devices)
        logging.info(f"Paired devices to Target: {target_paired_devices}")
        if discoverer_address in target_paired_addresses:
            logging.info(f"Forget Discoverer device <{discoverer_address}> on Target device")
            self.target.mbs.btUnpairDevice(discoverer_address)
        else:
            logging.info("Discoverer device not founded on Target device")
        # unpair Target device from Discoverer
        logging.info("Unpair Target device from Discoverer")
        target_address = self.target.mbs.btGetAddress()
        logging.info(f"Target device address: {target_address}")
        discoverer_paired_devices = self.discoverer.mbs.btGetPairedDevices()
        _, discoverer_paired_addresses = self.bt_utils.get_info_from_devices(
            discoverer_paired_devices)
        logging.info(f"Paired devices to Discoverer: {discoverer_paired_devices}")
        if target_address in discoverer_paired_addresses:
            logging.info(f"Forget Target device <{target_address}> on Discoverer device")
            self.discoverer.mbs.btUnpairDevice(target_address)
        else:
            logging.info("Target device not founded on Discoverer device")
        logging.info("Disable Bluetooth on Target device")
        self.target.mbs.btDisable()
        logging.info("Disable Bluetooth on Discoverer device")
        self.discoverer.mbs.btDisable()

if __name__ == '__main__':
    common_main()
