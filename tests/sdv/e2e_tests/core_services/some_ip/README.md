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

# SOME/IP Integration Tests

Set of end-to-end tests that verify SOME/IP functionality.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

## SOME/IP Broker Multipub Test

This test launches 2 instances of a SDV service bundle that publish to a multipub publication. The test launches a vSomeIp binary that subscribes to the corresponding SOME/IP event translated by the broker and verifies that the binary is able to receive messages from both instances.

### Mobly

```bash
atest SdvSomeIpMultipubDeviceIntegrationTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520 $ANDROID_HOST_OUT/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-someip-multipub-integration-test --{device1}serial 0.0.0.0:6520
```

## CI/CD Execution

Name: `sdv/sample/someip_multipub_test`

## SOME/IP Benchmark Test

The test launches a benchmark sample that performs several throughput tests for different types of messages.

### Mobly

```bash
atest SdvSomeIpBenchmarkTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520  $ANDROID_HOST_OUT/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-someip-benchmark-test --{device1}serial 0.0.0.0:6520
```

## CI/CD Execution

Name: `sdv/sample/someip_benchmark_test`

## SOME/IP Broker Dumpsys Test

The test launches a benchmark sample and checks that the dumpsys has entries about the benchmark sample's unit types.

### Mobly

```bash
atest SdvSomeIpBrokerDumpsysTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520  $ANDROID_HOST_OUT/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-someip-broker-dumpsys-test --{device1}serial 0.0.0.0:6520
```

## CI/CD Execution

Name: `sdv/sample/someip_broker_dumpsys_test`

## SOME/IP Stack Load Indicators Test

The test checks the binder interface that provides SOME/IP load indicators.

### Mobly

```bash
atest SdvSomeIpStackLoadIndicatorsTest
```

### CATBox

```bash
NOTIFY_AS_NATIVE=0.0.0.0:6520  $ANDROID_HOST_OUT/catbox/android-catbox/tools/catbox-tradefed run commandAndExit sdv-someip-stack-load-indicators-test --{device1}serial 0.0.0.0:6520
```

## CI/CD Execution

Name: `sdv/sample/someip_stack_load_indicators_test`
