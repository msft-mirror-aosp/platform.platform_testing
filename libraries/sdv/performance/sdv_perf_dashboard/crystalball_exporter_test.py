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

import unittest

from sdv_perf_dashboard import crystalball_exporter


class CrystalballExporterTest(unittest.TestCase):

  def test_flatten_dict_basic_raise_error(self):
    data = {
        'a': 1,
        'b': 2.5,
        'c': 'string',
        'd': True,
    }
    with self.assertRaises(
        ValueError,
        msg="Should not set is_root to True for non-root dict"):
            crystalball_exporter._flatten_dict(data,is_root=True)

  def test_flatten_dict_basic(self):
    data = {
        'a': 1,
        'b': 2.5,
        'c': 'string',
        'd': True,
    }
    expected = {
        'a': 1.0,
        'b': 2.5,
    }
    self.assertEqual(crystalball_exporter._flatten_dict(data), expected)

  def test_flatten_dict_nested(self):
    data = {
        'a': {
            'b': 1,
            'c': 'string',
            'd': {
                'e': 2,
                'f': 3.5,
            },
        }
    }
    expected = {
        'a-b': 1,
        'a-d-e': 2,
        'a-d-f': 3.5,
    }
    self.assertEqual(crystalball_exporter._flatten_dict(data), expected)

  def test_flatten_dict_list(self):
    data = {
        'a': [
            {'name': 'proc1', 'b': 1, 'c': 2},
            {'name': 'proc2', 'b': 3, 'c': 4},
        ]
    }
    expected = {
        'a#1-b': 1,
        'a#1-c': 2,
        'a#2-b': 3,
        'a#2-c': 4,
    }
    self.assertEqual(crystalball_exporter._flatten_dict(data), expected)

  def test_flatten_dict_is_root(self):
    data = {
        'a': {
            'b': 1,
            'c': 2,
        }
    }
    expected = {
        'b': 1,
        'c': 2,
    }
    self.assertEqual(
        crystalball_exporter._flatten_dict(data, is_root=True), expected
    )

  def test_flatten_dict_name_field_key(self):
    data = {
        'a': [
            {'name': 'proc1', 'b': 1, 'c': 2},
            {'name': 'proc2', 'b': 3, 'c': 4},
        ]
    }
    expected = {
        'proc1-b': 1,
        'proc1-c': 2,
        'proc2-b': 3,
        'proc2-c': 4,
    }
    self.assertEqual(
        crystalball_exporter._flatten_dict(data, name_field_key='name'),
        expected,
    )

  def test_flatten_dict_complex(self):
    data = {
        'android_cpu': {
            'process_info': [
                {'name': 'proc1', 'mcycles': 50, 'avg_freq_khz': 800000},
                {'name': 'proc2', 'mcycles': 25, 'avg_freq_khz': 500000},
            ]
        }
    }
    expected = {
        'android_cpu-process_info#1-mcycles': 50,
        'android_cpu-process_info#1-avg_freq_khz': 800000,
        'android_cpu-process_info#2-mcycles': 25,
        'android_cpu-process_info#2-avg_freq_khz': 500000,
    }
    self.assertEqual(crystalball_exporter._flatten_dict(data), expected)

  def test_flatten_dict_complex_name_field_key(self):
    data = {
        'android_cpu': {
            'process_info': [
                {'name': 'proc1', 'mcycles': 50, 'avg_freq_khz': 800000},
                {'process_name': 'proc2', 'mcycles': 25, 'avg_freq_khz': 500000},
            ]
        }
    }
    expected = {
        'android_cpu-proc1-mcycles': 50,
        'android_cpu-proc1-avg_freq_khz': 800000,
        'android_cpu-proc2-mcycles': 25,
        'android_cpu-proc2-avg_freq_khz': 500000,
    }
    self.assertEqual(
        crystalball_exporter._flatten_dict(data, name_field_key='name|process_name'),
        expected,
    )


if __name__ == '__main__':
  unittest.main()