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

# Sample: Sdv Gateway Service Check Test

## Sample

This test confirms that SDV Gateway starts on boot up of the IVI VM.

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

Follow [Setup Section](/platform_testing/tests/sdv/sample_tests/sdv_gateway/car_monitor_test_app/README.md) for testing environment setup.

### Mobly

```
atest SdvSampleSdvGatewayServiceCheckTest
```

### CATBox

```
NOTIFY_AS_NATIVE=0.0.0.0:6520 ./tools/catbox-tradefed run commandAndExit sdv-sample-sdv-gateway-service-check-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/sample/sdv_gateway_service_check_test`
