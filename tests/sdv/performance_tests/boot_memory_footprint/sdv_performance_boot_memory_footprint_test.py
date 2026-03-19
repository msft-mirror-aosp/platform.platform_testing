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

"""Boot Performance Memory Footprint Test."""

import logging
import time

from sdv_perf_dashboard import crystalball_exporter
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class SdvPerformanceBootMemoryFootprintTest(sdv_base_test.SdvBaseTestClass):
  """Measures memory footprint (RSS, SHR) of top 50 processes."""

  def setup_class(self):
    super().setup_class()
    self.crystalball_dict = {}
    self.device = self.get_device('device1').adb()

  def teardown_class(self):
    crystalball_exporter.export_to_crystalball(
        self.crystalball_dict,
        self.device.log_path(),
        'memory_footprint',
        omit_base_name=False,
    )
    super().teardown_class()

  def _parse_size_to_kb(self, size_str: str) -> int:
    size_str = size_str.strip().upper()
    try:
      if size_str.endswith('G'):
        return int(float(size_str[:-1]) * 1024 * 1024)
      elif size_str.endswith('M'):
        return int(float(size_str[:-1]) * 1024)
      elif size_str.endswith('K'):
        return int(float(size_str[:-1]))
      else:
        return int(size_str)
    except ValueError:
      return 0

  def get_memory_metrics(self, prefix: str):
    """Measures and records the memory footprint (RSS, SHR) of the top 50 processes.

        Executes a filtered shell command to retrieve memory statistics, actively
        avoiding observer-effect processes (like `ps` or `sort`). It sanitizes
        process names for Crystalball compatibility and records only the highest
        memory instance for each unique process into `crystalball_dict`.

        Args:
            prefix: A string prefix (e.g., 'boot' or '60s') prepended to metric keys.
        """
    # We explicitly filter out sh, kworker, sort, head, and ps to prevent observer effect
    cmd = (
        'ps -Ao pid,rss,shr,args | while read -r pid rss shr cmd_line; do if ['
        ' "$pid" = "PID" ] || [ "$cmd_line" = "<defunct>" ]; then continue; fi;'
        " if [ \"$rss\" -eq 0 ]; then continue; fi; full_cmd=$(tr '\\0' ' ' <"
        ' /proc/"$pid"/cmdline 2>/dev/null); case "$full_cmd" in sh | sh\\ * |'
        ' *kworker* | sort* | head* | ps*) continue;; esac; printf'
        ' "%s\\t%s\\t%s\\n" "$rss" "$shr" "$full_cmd"; done | sort -rn | head'
        ' -n 50'
    )
    output = self.device.execute_shell_command(cmd)

    for line in output.splitlines():
      line = line.strip()
      if not line:
        continue
      parts = line.split('\t')
      if len(parts) >= 3:
        rss = self._parse_size_to_kb(parts[0])
        shr = self._parse_size_to_kb(parts[1])
        raw_cmd = parts[2].strip()

        # Clean the process name so it acts as a valid Crystalball metric key
        safe_proc_name = (
            raw_cmd.split(' ')[0]
            .split('/')[-1]
            .replace('.', '_')
            .replace('-', '_')
        )

        # Create the stable metric keys without the rank
        rss_key = f'{prefix}_{safe_proc_name}_rss_kb'
        shr_key = f'{prefix}_{safe_proc_name}_shr_kb'

        # Since the output is sorted by RSS descending, the first time we see a process
        # name it will be the highest memory consumer. We only record this highest one.
        if rss_key not in self.crystalball_dict:
          self.crystalball_dict[rss_key] = rss
          self.crystalball_dict[shr_key] = shr

  def test_memory_footprint(self):
    logging.info('Rebooting device to measure memory footprint after boot...')
    self.device.reboot_device()
    self.device.wait_for_device_online()

    logging.info('Measuring memory footprint immediately after boot...')
    self.get_memory_metrics('boot')

    logging.info('Waiting 60 seconds before second measurement...')
    time.sleep(60)

    logging.info('Measuring memory footprint 60 seconds after boot...')
    self.get_memory_metrics('60s')


if __name__ == '__main__':
  sdv_test_runner.run()
