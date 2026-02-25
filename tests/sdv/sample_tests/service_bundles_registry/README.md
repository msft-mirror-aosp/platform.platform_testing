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

# Sample: Service Bundles Registry (SBR)

## Sample

This test automates the special case of Service Bundles Registry for the [Service Bundles Registry samples](/system/software_defined_vehicle/core_services/samples/service_bundles_registry/README.md).

## Test Execution

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### Mobly

```bash
atest SdvSampleSBRTest
```

### CATBox

```bash
# set up
m catbox && m && (yes | cvd reset) && (yes | cvd start)
# SdvSampleSBRTest execution
NOTIFY_AS_NATIVE=0.0.0.0:6520 catbox-tradefed run commandAndExit sdv-sample-service-bundle-registry-test --{device1}serial 0.0.0.0:6520
```

## CI/CD execution

- Name: `sdv/sample/service_bundle_registry_test`
