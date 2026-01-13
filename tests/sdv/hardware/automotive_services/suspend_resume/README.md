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

# Sample: Suspend and Resume Hardware (only for local/manual execution, NOT CI/CD)

## Sample

These tests automate suspend and resume tests on ARM hardware. They currently cannot be run on CI/CD (see blocker b/417652278).

To adjust the time duration the tests wait to resume the suspended devices, adjust the 'wait_time' paramter (of the parameterized test arguments) and also increase CATBox test execution time, for example adding the `<option name="mobly-host:mobly-test-timeout" value="86400000" />` option to `tools/sdv_catbox/res/config/sdv-sample-suspend-resume-server-test.xml`.

## Test Execution

Steps to execute before running CATBox (edit OSPREY_IP accordingly):
```
export OSPREY_IP=10.42.82.102  # set to the IP of the booked osprey
ssh sdv-devlab-001.muc.corp.google.com -L 12222:$OSPREY_IP:22 -L 15555:$OSPREY_IP:5555 -L 15556:$OSPREY_IP:5556
(keep this alive)
```

Flash and connect the devices (edit HW_IP accordingly):
`export HW_IP=10.42.82.102`
`./collect-boot-logs -i $HW_IP -c sdv_8255/sdv-1.json -f -x`
`./collect-boot-logs -i $HW_IP -c sdv_8255/sdv-2.json -f -x`
--- (additional steps due to b/422387278)
--- `ssh -p 12222 root@127.0.0.1`
--- `qvm @/guests/android/sdv-1/sdv-1.conf`
`SERIAL1=127.0.0.1:15555`
`SERIAL2=127.0.0.1:15556`
`adb connect $SERIAL1`
`adb connect $SERIAL2`

Read [SDV Testing instructions](/vendor/google_testing/software_defined_vehicle/README.md) for detailed information.

### CATBox

`m catbox`

```
NOTIFY_AS_NATIVE=127.0.0.1:15555,127.0.0.1:15556 catbox-tradefed run commandAndExit sdv-sample-suspend-resume-server-test --{device1}serial 127.0.0.1:15555 --{device2}serial 127.0.0.1:15556

NOTIFY_AS_NATIVE=127.0.0.1:15555,127.0.0.1:15556 catbox-tradefed run commandAndExit sdv-sample-suspend-resume-client-test --{device1}serial 127.0.0.1:15555 --{device2}serial 127.0.0.1:15556

NOTIFY_AS_NATIVE=127.0.0.1:15555,127.0.0.1:15556 catbox-tradefed run commandAndExit sdv-sample-suspend-resume-both-test --{device1}serial 127.0.0.1:15555 --{device2}serial 127.0.0.1:15556

NOTIFY_AS_NATIVE=127.0.0.1:15555,127.0.0.1:15556 catbox-tradefed run commandAndExit sdv-sample-reboot-server-test --{device1}serial 127.0.0.1:15555 --{device2}serial 127.0.0.1:15556

NOTIFY_AS_NATIVE=127.0.0.1:15555,127.0.0.1:15556 catbox-tradefed run commandAndExit sdv-sample-reboot-client-test --{device1}serial 127.0.0.1:15555 --{device2}serial 127.0.0.1:15556
```

## CI/CD execution

Currently not supported.