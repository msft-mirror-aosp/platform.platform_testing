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
test harness to test all Android devices for security
patch tests that don't fall into the Compatibility Test Suite. These tests are
exclusively for fixes that are associated (or will be associated) with a
Common Vulnerabilities and Exposures (CVE).

The SDK allows development of STS tests outside of the Android source tree
using Android Studio or the standard Android SDK. It includes all utilities
that are needed to build and run an STS test.

## Get started using Android Studio {:#get-started-using-android-studio}

After extracting the archive, open the directory in Android Studio as an
existing project. Run the `assembleSTSARM` or `assembleSTSx86` build target to
build the skeleton test, depending on the architecture of the target Android
device. Run the `runSTS` build target to run the skeleton test on the connected
device (ADB must be authorized).

## Get started using Gradle {:#get-started-using-gradle}

After extracting the archive, set the `sdk.dir` property in the
`local.properties` file at the root of the Gradle project, then run the
`assembleSTSARM` Gradle task to build the skeleton test. After the build is
finished, the test can be run by navigating (`cd`) into
`build/android-sts/tools` and executing the `sts-tradefed` wrapper.

```shell
$ echo 'sdk.dir=/home/<myusername>/Android/Sdk' > local.properties
$ ./gradlew assembleSTSARM
$ cd build/android-sts/tools
$ ./sts-tradefed run sts-dynamic-develop -m hostsidetest
```

## Write an STS test {:#write-sts-test}

There are three parts to an STS test:

1.  A host-side Tradefed test that interacts with the device through adb, in the
    `sts-test` subdirectory.
1.  An optional native proof-of-concept attack that is pushed onto the
    device through `adb push` and executed by the host-side test in the
    `native-poc` subdirectory.
1.  An optional app or service APK that is installed onto the device through
    `adb install` and also launched by the host-side test. The app or service
    can also contain its own set of JUnit assertions that is reported to the
    host-side runner. This is in the `test-app` subdirectory.

A typical STS test flow usually follows one of two patterns:

-  Native proof-of-concept:

     1.  The host-side test pushes and launches a native executable on the device.
     1.  The native program crashes or returns a specific exit code.
     1.  The host-side test checks for crashes, looks at the logcat backtrace, or
         looks for the specific exit code to determine whether the attack
         succeeded.

-  Instrumented test app:

     1.  The host-side test pushes an APK consisting of an app or service onto
         the device.
     1.  The host-side test starts the device-side JUnit tests that is bundled
         with the APK through `runDeviceTest()`
     1.  The device-side JUnit tests taps buttons and watches the app using
         UIAutomator, or otherwise accesses the Android system in ways that
         reveal security vulnerabilities.
     1.  The success or failure of the device-side JUnit tests is returned to
         the host-side test, which can be used to determine if the test passed
         or not.

A combination of the two patterns (for example, running of a native program in
conjunction with device-side tests) is also possible. Some other instrumentation
frameworks, such as `frida-inject`, are also available.
For details, see the
[Security Test Suite reference docs](/reference/sts/packages).

### My proof-of-concept attack does not need a test app and/or native executable

Most tests will not need both a device-side app and a native executable.

If your test does not involve the use of an on-device app/service, simply delete
the `test-app` subdirectory. Similarly, if your test does not use a native
executable, delete the `native-poc` subdirectory then Gradle-sync the project.
The project is set up to automatically skip building those modules when they
don't exist.

### My proof-of-concept attack involves a second app/service

Add a new Module to your project for your second app/service and write that
like you would any other APK. Then edit `build.gradle` at the root of this
directory and add your module following the instructions in `copyArtifacts`,
`assembleStsARM`, and `assembleStsx86`. Then Gradle-sync the project.

