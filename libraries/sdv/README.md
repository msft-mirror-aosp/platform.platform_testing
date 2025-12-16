<!-- Copyright (C) 2025 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. -->

# SDV Testing

This folder contains testing libraries for Software Defined Vehicle (SDV) platform.

See [Android partner documentation](https://docs.partner.android.com/automotive/sdv/overview)
for general information about SDV.

## SDV Test Framework

The SDV Test Framework support the creation of the following type of tests and
testing resources:
  - [sdv_test_fw](sdv_test_fw) facilitates the creation of the tests, based on **Mobly**.
    Tests are written in Python and allow end-to-end and communication between devices.
  - [test_services](test_services/) contains sample services that can be used in the tests.
  - [host_orchestrator_util](host_orchestrator_util/) contains utility class that provides methods to interact with the host orchestrator.
  - [perfromance](performnace/) contains utility classes used for performance testing.
  - [telemetry](telemetry/) contains utility classes used for telemetry testing.