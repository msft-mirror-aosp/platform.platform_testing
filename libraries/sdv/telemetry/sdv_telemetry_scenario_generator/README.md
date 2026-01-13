<!--
  Copyright (C) 2026 The Android Open Source Project
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
      http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# KPI Scenario

The KPI scenario is a worst-case scenario for SDV Telemetry usage. This folder
contains a script to generate the scenario, as well as a standalone library to
generate the metrics configs and simulation publishers for such a scenario.

## Scenario

This section describes the nature and constraints of the KPI scenario. The
scenario is supposed to represent a worst-case scenario for SDV Telemetry, and
can be used to measure performance metrics such as RAM and CPU usage under such
a scenario.

### Publishers

The scenario creates simulation publisher configs for 700 getter publishers and
300 subscribable publishers (= 1000 publishers in total). Each publisher
publishes a `Data` proto that has between 1 and 100 fields, all of which are
int64. Overall, all subscribable publishers together will publish new data
approximately 250 times per second. The data per publisher is a list of 100
randomly generated integers, and a publisher will loop their published data when
reaching the end.

### Metrics Configs

The scenario creates 150 metrics configs, which matches the maximum number of
metrics configs that can be registered/stored by the Telemetry Service at the
same time. However, as per the KPIs, only 100 metrics configs can be active at
the same time, so the last 50 generated metrics configs should not be activated,
just registered, when running this scenario.

Each metrics config subscribed to at least three data triggers, two periodic
triggers, and two conditional triggers, in addition to defining 20 getter
publishers and six aggregation publishers.

Each of the aggregation publishers is an average aggregation publisher, and for
50% of aggregation publishers, an additional vector aggregation publisher is
generated (which is the worst-case aggregation publisher from a memory
perspective).

Each metrics config defines three metrics report configs with a corresponding
trigger for each. The metrics reports all have 20 fields, the "typical" number
as per the KPIs.

All conditional triggers are use the following expression, where `A`, `B,` `C,`
and `D` are randomly selected publishers:

```text
(ABS(A) < 5 || (B > 5 && B < 5)) || (ABS(MOD(C, 5)) < 5 && ABS(D) >= 0)
```

The condition is written in a worst-case way, where it will always evaluate to
`true`, but without ever short-circuiting.

For simplicitly, metrics config triggers are always periodic triggers, which
ensures that the average number of metrics reports per second for all metrics
configs combined is 10.

## Generator CLI

The sample scenario can be generated like so:

```shell
mm sdv_telemetry_scenario_generator
sdv_telemetry_scenario_generator <output_folder>
```

This will generate metrics configs (in both binary proto and textproto formats),
as well as simulation publishers and their data for use with the simulator. The
generated files are written to the provided output folder.
