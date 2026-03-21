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

"""SDV Sample Media Audio Playback Test

Tests Audio Playback on one SDV VM
"""

from mobly import asserts
from absl.testing import parameterized
import logging
import os
import time
from audio import compare_wav_energy_and_quality, detect_multiple_audio_sources, wav_files_roughly_same_wave
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleMediaAudioPlaybackTest(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

  EXPECTED_OUTPUT = '[\n    50,\n    50,\n]'
  VICTORY_WAV = 'victory.wav'
  ROAD_TRIP_WAV = 'Road_Trip.48000.wav'
  VICTORY_WAV_PATH = os.path.join(
      os.path.dirname(os.path.abspath(__file__)), 'victory.wav'
  )
  ROAD_TRIP_WAV_PATH = os.path.join(
      os.path.dirname(os.path.abspath(__file__)), 'Road_Trip.48000.wav'
  )
  PROPERTY_CONFIG = '/data/local/tmp/mixer.conf'

  def setup_class(self):
    super().setup_class()
    self.sdv_device = self.get_device('device1').adb()
    # TODO: b/400645925 - Remove this once the bug is fixed.
    result = self.sdv_device.execute_shell_command('getprop ro.boot.sdv.audio_mixer_config')
    if result != self.PROPERTY_CONFIG:
      self.sdv_device.execute_shell_command('setprop ro.boot.sdv.audio_mixer_config /data/local/tmp/mixer.conf')

  def execute_common_setup(self):
    """Executes common setup commands for audio playback tests."""
    # TODO: b/399624649 - Remove this once the bug is fixed.
    self.sdv_device.execute_shell_command('setenforce 0')
    self.sdv_device.execute_shell_command('stop sdv_audio_mixer')
    self.sdv_device.execute_shell_command('start sdv_audio_mixer')

  def audio_record_start(self, filename, duration=30):
    """Starts audio recording on the device."""
    self.sdv_device.execute_shell_command_in_subprocess(
        'tinycap', f'tinycap2 /data/local/tmp/{filename} -D4 -d1 -t {duration}'
    )

  def audio_record_stop(self):
    """Starts audio recording on the device."""
    time.sleep(1)
    self.sdv_device.execute_shell_command('pkill -l SIGINT tinycap2')

  def play_audio(self, filename, card='1', device='0', subprocess=False):
    """Plays an audio file on the device."""
    command = f'tinyplay2 -D{card} -d{device} /data/local/tmp/{filename}'
    if subprocess:
      self.sdv_device.execute_shell_command_in_subprocess('tinyplay', command)
    else:
      self.sdv_device.execute_shell_command(command)

  def pull_audio(self, remote_filename, local_filename):
    """Pulls recorded audio from the device to the test machine."""
    self.sdv_device.pull(
        [f'/data/local/tmp/{remote_filename}', f'/tmp/{local_filename}']
    )

  @parameterized.named_parameters(
      {
          'testcase_name': 'card_1',
          'card': '1',
      },
      {
          'testcase_name': 'card_2',
          'card': '2',
      },
      {
          'testcase_name': 'card_3',
          'card': '3',
      },
  )
  def test_audio_playback_same(self, card):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )
    self.execute_common_setup()
    self.audio_record_start('capture_same.wav')
    self.play_audio(self.VICTORY_WAV, card=card)
    self.audio_record_stop()
    self.pull_audio('capture_same.wav', 'capture_same.wav')
    asserts.assert_true(
        wav_files_roughly_same_wave(self.VICTORY_WAV_PATH, '/tmp/capture_same.wav'),
        'WAV files are not similar enough.',
    )
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed.'
    )

  def test_audio_playback_different(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )
    self.execute_common_setup()
    self.audio_record_start('capture_diff.wav')
    self.play_audio(self.ROAD_TRIP_WAV)
    self.audio_record_stop()
    self.pull_audio('capture_diff.wav', 'capture_diff.wav')
    asserts.assert_false(
        wav_files_roughly_same_wave(self.VICTORY_WAV_PATH, '/tmp/capture_diff.wav'),
        'WAV files are too similar.',
    )
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed.'
    )

  def test_wav_compare(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )
    asserts.assert_true(
      wav_files_roughly_same_wave(self.VICTORY_WAV_PATH, self.VICTORY_WAV_PATH),
      'Same wav file comparison failed (VICTORY_WAV).'
    )
    asserts.assert_true(
        wav_files_roughly_same_wave(self.ROAD_TRIP_WAV, self.ROAD_TRIP_WAV),
        'Same wav file comparison failed (ROAD_TRIP_WAV).'
    )
    asserts.assert_false(
        wav_files_roughly_same_wave(self.VICTORY_WAV_PATH, self.ROAD_TRIP_WAV),
        'Different wav file comparison failed.'
    )
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed.'
    )

  def test_audio_playback_double(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )
    self.execute_common_setup()
    self.audio_record_start('capture_double.wav')
    self.play_audio(self.VICTORY_WAV, device=0, subprocess=True)
    self.play_audio(self.VICTORY_WAV, device=1)
    self.audio_record_stop()
    self.pull_audio('capture_double.wav', 'capture_double.wav')
    asserts.assert_true(
        detect_multiple_audio_sources(
            self.VICTORY_WAV_PATH, '/tmp/capture_double.wav'
        ),
        'Double audio playback not detected.',
    )
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed.'
    )

  def test_audio_playback_volume_adjustment(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )
    self.execute_common_setup()
    self.sdv_device.execute_shell_command(
        'mixerctl set-volume --device hw:1,0 --volume-percent 50'
    )
    result = self.sdv_device.execute_shell_command(
        'mixerctl get-volume --device hw:1,0'
    )
    asserts.assert_equal(result, self.EXPECTED_OUTPUT, 'mixerctl command result did not match expected output')
    self.audio_record_start('capture_volume_adjustment.wav')
    self.play_audio(self.VICTORY_WAV)
    self.audio_record_stop()
    self.pull_audio(
        'capture_volume_adjustment.wav',
        'capture_volume_adjustment.wav',
    )
    asserts.assert_true(
        compare_wav_energy_and_quality(
            self.VICTORY_WAV_PATH, '/tmp/capture_volume_adjustment.wav'
        ),
        'WAV files energy ratio is not within expected range or quality is not good enough.',
    )
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed.'
    )


if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework
  sdv_test_runner.run()
