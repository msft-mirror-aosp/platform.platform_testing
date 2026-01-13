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

MULTI_VM_QUERY_BASE_TEMPLATE = f"""
--------------- PARAMETER VIEW ---------------
-- Selected VM/machine: First VM (machine_id = 0)
DROP VIEW IF EXISTS selected_vm;
CREATE VIEW selected_vm AS SELECT {{vm_id}} AS machine_id;

DROP VIEW IF EXISTS interval_size_ns;
CREATE VIEW interval_size_ns AS SELECT {{interval_size_ns}} AS size;

-- Determine CPU range from selected VM
-- In first VM (machine_id = 0) ucpu range starts with 0, such as [0, 3]
-- In other VMs ucpu range might be [4096, 4099]
DROP VIEW IF EXISTS relevant_cpu_range;
CREATE VIEW relevant_cpu_range AS
SELECT
  MIN(ucpu) AS cpu_start,
  MAX(ucpu) AS cpu_end
FROM cpu
WHERE machine_id = (SELECT machine_id FROM selected_vm)
  -- First VM (0) will be null for merged traces
  OR (machine_id IS NULL AND (SELECT machine_id FROM selected_vm) = 0);


---------------- HELPER VIEWS -----------------
-- Filter sched table by relevant CPU range
DROP VIEW IF EXISTS machine_specific_sched;
CREATE VIEW machine_specific_sched AS
SELECT
  *
FROM
  sched
WHERE cpu >= (SELECT cpu_start FROM relevant_cpu_range)
  AND cpu <= (SELECT cpu_end FROM relevant_cpu_range);

-- Create a view with field num_cpus: Number of CPUs
DROP VIEW IF EXISTS cpu_count;
CREATE VIEW cpu_count AS
SELECT COUNT(DISTINCT cpu) AS num_cpus
FROM cpu
WHERE machine_id = (SELECT machine_id FROM selected_vm)
  -- First VM (0) will be null for merged traces
  OR (machine_id IS NULL AND (SELECT machine_id FROM selected_vm) = 0);

-- Determine idle thread utid (typically 0)
DROP VIEW IF EXISTS idle_thread;
CREATE VIEW idle_thread AS
SELECT
  utid
FROM
  thread
WHERE is_idle = 1
AND (machine_id = (SELECT machine_id FROM selected_vm)
    -- First VM (0) will be null for merged traces
    OR (machine_id IS NULL AND (SELECT machine_id FROM selected_vm) = 0));


-- Generate a series of interval start times
-- This view set trace_start as the first interval start time and interval_id
-- as 0, then the next interval starts at the end of the first interval (
-- trace_start + interval_size), and so on.
DROP VIEW IF EXISTS interval_series_ns;
CREATE VIEW interval_series_ns AS
  WITH RECURSIVE series AS (
    SELECT {{ts_start}} AS value, 0 AS interval_id
    UNION ALL
    SELECT value + (SELECT size FROM interval_size_ns), interval_id + 1
    FROM series
    WHERE value + (SELECT size FROM interval_size_ns) <= {{ts_end}}
  )
  SELECT value AS interval_start_ns, interval_id FROM series;

-- Series of interval start and end times
--- fields: ts, interval_start_ns, dur, interval_end_ns, interval_id
DROP VIEW IF EXISTS interval_bounds;
CREATE VIEW interval_bounds AS
  SELECT
    -- The start of the interval has to be named ts to work properly with the
    -- span_join function
    interval_start_ns AS ts,
    interval_start_ns,
    -- The duration of the interval is also utilized by the span_join function
    -- and thus has to be named dur
    (SELECT size FROM interval_size_ns) AS dur,
    interval_start_ns + (SELECT size FROM interval_size_ns) AS interval_end_ns,
    interval_id
    FROM interval_series_ns;

---------------- MAIN QUERY -----------------
-- https://perfetto.dev/docs/analysis/trace-processor#span-join
DROP TABLE IF EXISTS sched_with_intervals;
CREATE VIRTUAL TABLE sched_with_intervals
USING SPAN_JOIN(machine_specific_sched PARTITIONED cpu, interval_bounds);

{{busy_sched_with_intervals_query}}

{{metric_calculation_query}}
"""
