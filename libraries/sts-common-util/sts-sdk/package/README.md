Project: /_project.yaml
Book: /_book.yaml

{% include "_versions.html" %}

<!--
  Copyright 2022 The Android Open Source Project

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

# Android Security Test Suite Development Kit (STS SDK)

Security Test Suite Trade Federation (sts-tradefed) is built on top of the
[Android Trade Federation](https://source.android.com/docs/core/test_infra/tradefed)
test harness to test all android devices for security
patch tests that do not fall into the Compatibility Test Suite. These tests are
exclusively for fixes that are associated (or will be associated) with a CVE.

The SDK allows development of STS tests outside of the Android source tree
using Android Studio or the standard Android SDK. It includes all utilities
that are needed to build and run an STS test.

## Getting started using Android Studio

After extracting the archive, open the directory in Android Studio as an
existing project. Run `assembleSTSARM` or `assembleSTSx86` build target to
build the skeleton test, depending on the architecture of the target Android
device. Run `runSTS` build target to run the skeleton test on the connected
device (ADB must be authorized).

## Getting started using Gradle

After extracting the archive, set the `sdk.dir` property in `local.properties`
file at the root of the gradle project, then run `assembleSTSARM` gradle task
to build the skeleton test. After the build is finished, the test can be run by
`cd`'ing into `build/android-sts/tools` and execute the `sts-tradefed` wrapper.

```shell
$ echo 'sdk.dir=/home/<myusername>/Android/Sdk' > local.properties
$ ./gradlew assembleSTSARM
$ cd build/android-sts/tools
$ ./sts-tradefed run sts-dynamic-develop -m hostsidetest
```

## Writing an STS test

There are 3 parts to an STS test:

1. A host-side tradefed test that interacts with the device via ADB, in the
   `sts-test` subdirectory
1. An optional native proof-of-concept attack that gets adb push’d onto the
   device and executed by the host-side test, in the `native-poc` subdirectory
1. An optional app/service APK that gets adb install’d onto the device and also
   launched by the host-side test. The app/service can also contain its own set
   of JUnit assertions that will be reported to the host-side runner. This is
   in the `test-app` subdirectory.

A typical STS test flow usually follows one of two patterns:

1. Native proof-of-concept:
     * the host-side test pushes and launches a native executable on the device
     * the native program either crashes or returns a specific exit code
     * the host-side test checks for crashes, looks at logcat backtrace, or
       look for the specific exit code to determine whether the attack
       succeeded.

2. Instrumented test app:
     * the host-side test pushes an APK consisting of an app or service onto
       the device.
     * the host-side test starts the device-side JUnit tests that is bundled
       with the APK via `runDeviceTest()`
     * the device-side JUnit tests clicks buttons and watches the app using
       UIAutomator, or otherwise access the Android system in ways that reveal
       security vulnerabilities
     * success or failure of the device-side JUnit tests will be returned to
       the host-side test which can be used to determine if the test passed or
       not.


A combination (e.g. running of a native program in conjunction with device-side
tests) is also possible. Some other instrumentation frameworks, like
`frida-inject`, is also available. Check JavaDoc for details.

