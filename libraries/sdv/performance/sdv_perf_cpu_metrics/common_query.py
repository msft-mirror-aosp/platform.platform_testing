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

BUSY_SCHED_WITH_INTERVALS_QUERY = """
-- Discard the idle thread
DROP VIEW IF EXISTS busy_sched_with_intervals;
CREATE VIEW busy_sched_with_intervals AS
  SELECT
    interval_id,
    ts,
    interval_start_ns,
    interval_end_ns,
    cpu,
    utid,
    dur as adj_dur
  FROM sched_with_intervals
  WHERE utid NOT IN (SELECT utid FROM idle_thread);
"""

# This query is used to compute the overall CPU utilization of a trace.
# Output is a timeseries of overall CPU utilization percentage for each
# {interval_size_ns}interval.
# The final output view overall_cpu_load_per_interval has the following fields:
# interval_id|interval_start_ns|interval_end_ns|overall_cpu_perc
CALCULATION_QUERY_OVERALL_UTILIZATION = f"""
-- Overall CPU load per interval:
DROP VIEW IF EXISTS overall_cpu_load_per_interval;
CREATE VIEW overall_cpu_load_per_interval AS
  SELECT
    interval_id,
    interval_start_ns,
    interval_end_ns,
    SUM(adj_dur) / (num_cpus * size) * 100 AS overall_cpu_perc
  FROM
    busy_sched_with_intervals,
    interval_size_ns,
    cpu_count
  GROUP BY
    interval_id;

SELECT * FROM overall_cpu_load_per_interval;
"""

# This query is used to compute the per-CPU CPU utilization of a trace.
# Output is a timeseries of per-CPU CPU utilization percentage for each
# {interval_size_ns}interval.
# The final output view per_cpu_load_per_interval has the following fields:
# interval_id|interval_start_ns|interval_end_ns|cpu|cpu_util_perc
CALCULATION_QUERY_PER_CPU_UTILIZATION = f"""
-- Per-CPU load per interval:
DROP VIEW IF EXISTS per_cpu_load_per_interval;
CREATE VIEW per_cpu_load_per_interval AS
SELECT
  interval_id,
  interval_start_ns,
  interval_end_ns,
  cpu,
  SUM(adj_dur) / (size) * 100 AS overall_cpu_perc
FROM
  busy_sched_with_intervals,
  interval_size_ns
GROUP BY
  interval_id,
  cpu;
SELECT * FROM per_cpu_load_per_interval;
"""

BUSY_SCHED_WITH_INTERVALS_OF_PROCESS_QUERY_TEMPLATE = f"""
-- Define name of process for which to compute CPU load
    DROP VIEW IF EXISTS selected_process;
    CREATE VIEW selected_process AS SELECT '{{process_name}}' AS process_name;

-- Lookup user process id for specific process name and all related user thread ids
DROP VIEW IF EXISTS process_lookup;
CREATE VIEW process_lookup AS
SELECT
  p.upid,
  t.utid,
  t.is_main_thread,
  p.name,
  p.cmdline
FROM
  process p,
  selected_process sp
LEFT JOIN
  thread t
USING
  (upid)
WHERE
  p.name = sp.process_name;
--
DROP VIEW IF EXISTS busy_sched_with_intervals;
CREATE VIEW busy_sched_with_intervals AS
  SELECT
    interval_id,
    ts,
    interval_start_ns,
    interval_end_ns,
    cpu,
    utid,
    CASE
      WHEN utid IN (SELECT utid FROM idle_thread) THEN 0  -- idle thread
      WHEN utid NOT IN (SELECT utid FROM process_lookup) THEN 0 -- not the selected process
      ELSE dur
    END AS adj_dur
  FROM sched_with_intervals;
"""