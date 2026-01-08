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
"""Crystalball exporter for SDV performance dashboard.

This module provides a function to export data to CrystalBall format for SDV
performance dashboard.

Example:
```
  from sdv_perf_dashboard import crystalball_exporter
  crystalball_exporter.export_to_crystalball(
      data,
      output_dir,
      test_name,
      omit_base_name,
      name_field_key,
  )
```

"""

import logging
import os
from typing import Any, Dict, Optional


# Crystalball results file name
CB_FILENAME = 'test_results.txt'

def export_to_crystalball(
    data: Dict[str, Any],
    output_dir: str,
    test_name: str,
    omit_base_name: bool = True,
    name_field_key: Optional[str] = None,
) -> None:
  """Writes data to a CrystalBall output file.

  The data is first converted to a flattened format by compressing the keys.

  Repeated calls with the same output dir will append data to the same file.

  Args:
    data: input data
    output_dir: directory of the output file
    test_name: name used by CrystalBall to identify the set of metrics
    omit_base_name: omit the base metric name from each entry key
    name_field_key: if not None, use the value of this field to be partial of
      the flattened format key name
  """
  cb_path = os.path.join(output_dir, CB_FILENAME)
  logging.debug(
      'Exporting Perfetto results of %s to CrystalBall at %s.',
      test_name,
      cb_path,
  )
  metrics = _flatten_dict(
      data, is_root=omit_base_name, name_field_key=name_field_key
  )
  # write to file, append if file already exists
  with open(cb_path, 'a') as f:
    f.write(test_name + '\n\n')
    f.writelines(['%s:%s\n' % (key, value) for key, value in metrics.items()])
    f.write('\n\n')


def _get_name_field_value(
    data: Dict[str, Any], name_field_key: Optional[str] = None
) -> Optional[str]:
  """Gets the value of the first existing name_field_key from the dictionary.

  Args:
    data: the dictionary to get the name field value from
    name_field_key: the key of the name field in the dictionary, the field value
      is used to construct the prefix. If None, returns None.

  Returns:
    the value of the first existing name_field_key from the dictionary, or None
    if name_field_key is None.

  """
  if name_field_key:
    name_field_list = name_field_key.replace(' ', '').split('|')
    for key in name_field_list:
      if isinstance(data.get(key), str):
        return data[key].replace(' ', '_').replace('-', '_')
  return None


def _construct_prefix(
    prefix: str,
    index: int,
    data: Dict[str, Any],
    name_field_key: Optional[str] = None,
) -> str:
  """Constructs the prefix for the flattened format key name.
  If 'name_field_key' is  'name|process_name', the prefix inserts the value of
  the dict with key='name|process_name' otherwise inserts the index.

  Args:
    prefix: the prefix to be used for the flattened format key name, if empty
      returns empty string
    index: the index of the entry in a list, used to construct the prefix if
      'name_field_key' is None
    data: the dictionary to get the name field value from
    name_field_key: the key of the name field in the dictionary, the field value
      is used to construct the prefix. If None, the index is used to construct
      the prefix.

  Returns:
    the constructed prefix for the flattened format key name or empty string if
    'prefix' is empty.
  """
  if not prefix:
    return ''

  name_field_value = _get_name_field_value(data, name_field_key)
  if name_field_value:
    # Replace the key of the parent dict with the value of the name field
    prefix = '-'.join(prefix.split('-')[:-1] + [name_field_value])
  elif index:
    # Add index to the prefix if entry is part of a list
    prefix += '#%d' % index
  return prefix


def _flatten_dict(
    data: Dict[str, Any],
    is_root: bool = False,
    name_field_key: Optional[str] = None,
    prefix: str = '',
    index: int = 0,
) -> Dict[str, str]:
  """Flattens the nested data dict by compressing the keys.

  Only numeric values are recorded. Repeated keys are marked by index.

  Example :
    Input data
    {
      'android_cpu': {
        'process_info': [
          {
            'name': 'proc1',
            'mcycles': 50,
            'avg_freq_khz': 800000
          },
          {
            'process_name': 'proc2',
            'mcycles': 25,
            'avg_freq_khz': 500000
          }
        ]
      }
    }
    If "name_field_key" is "name|process_name", the output is:
    {
      'android_cpu-proc1-mcycles': 50,
      'android_cpu-proc1-avg_freq_khz': 800000,
      'android_cpu-proc2-mcycles': 25,
      'android_cpu-proc2-avg_freq_khz': 500000
    }.
    If "name_field_key" is None, the output is:
    {
      'android_cpu-process_info#1-mcycles': 50,
      'android_cpu-process_info#1-avg_freq_khz': 800000,
      'android_cpu-process_info#2-mcycles': 25,
      'android_cpu-process_info#2-avg_freq_khz': 500000
    }.

  Args:
    data: source data
    is_root: set to True to omit the top level key from output
    name_field_key: if not None, use the values of this field to be partial of
      the flattened format key name. The multiplefield names are separated by '|'
    prefix: used internally
    index: used internally

  Returns:
    flattened data with compressed keys

  Raises:
    ValueError: if is_root is True for non-root dict
  """
  out = {}
  full_key = ''

  if not isinstance(data, dict):
    return out
  prefix = _construct_prefix(prefix, index, data, name_field_key)
  for key, value in data.items():
    if not is_root:
      key = key.replace('-', '_')
      full_key = '%s-%s' % (prefix, key) if prefix else key

    # Crystalball only recognizes numeric values (int or float) as metrics
    # Because isinstance(bool_value, int) returns True, so we need to check if
    # the value is a boolean explicitly.
    if isinstance(value, (int, float)) and not isinstance(value, bool):
      if is_root:
        raise ValueError('Should not set is_root to True for non-root dict')
      out[full_key] = value
    elif isinstance(value, list): # dict value is list
      for i, entry in enumerate(value):
        out.update(
            _flatten_dict(
                data=entry,
                name_field_key=name_field_key,
                prefix=full_key,
                index=i + 1,
            )
        )
    else: # dict value is dict
      out.update(
          _flatten_dict(
              data=value, name_field_key=name_field_key, prefix=full_key
          )
      )
  return out
