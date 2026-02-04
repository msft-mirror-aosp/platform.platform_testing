# Run tests (Atest)

Atest is a command line tool that allows users to build, install, and run
Android tests locally, greatly speeding up test re-runs without requiring
knowledge of [Trade Federation test harness](/docs/core/tests/tradefed)
command line options. This page explains how to use Atest to run Android
tests.

For general information on writing tests for Android, see
[Android Platform Testing](/docs/core/tests/index.md).

For information on the overall structure of Atest, refer to the
[Atest Developer Guide](https://android.googlesource.com/platform/tools/asuite/+/{{ androidLatestReleaseBranch }}/atest/docs/atest_structure.md).

For information on running tests in TEST_MAPPING files through Atest, see
[Running tests in TEST_MAPPING files](/docs/core/tests/development/test-mapping#running_tests_with_atest).

To add a feature to Atest, follow the
[Atest Developer Workflow](https://android.googlesource.com/platform/tools/asuite/+/{{ androidLatestReleaseBranch }}/atest/docs/developer_workflow.md).

## Set up your environment {:#setup}

To set up your Atest environment, follow the instructions in [Setting up the
environment](/docs/setup/build/building#initialize), [Choosing a target](/docs/setup/build/building#choose-a-target), and [Building the code](/docs/setup/build/building#build-the-code).

## Basic usage {:#basics}

Atest commands take the following form:

<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> [<var>optional-arguments</var>]</code>
</pre>

### Optional arguments {:#optional-args}

The following table lists the most commonly used arguments. A complete list is
available through `atest --help`.

<table>
  <thead>
    <tr>
      <th>Option</th>
      <th>Long option</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>-b</code></td>
      <td><code>--build</code></td>
      <td>Builds test targets. (default)</td>
    </tr>
    <tr>
      <td><code>-i</code></td>
      <td><code>--install</code></td>
      <td>Installs test artifacts (APKs) on device. (default)</td>
    </tr>
    <tr>
      <td><code>-t</code></td>
      <td><code>--test</code></td>
      <td>Runs the tests. (default)</td>
    </tr>
    <tr>
      <td><code>-s</code></td>
      <td><code>--serial</code></td>
      <td>Runs the tests on the specified device. One device can be tested at a time.</td>
    </tr>
    <tr>
      <td><code>-d</code></td>
      <td><code>--disable-teardown</code></td>
      <td>Disables test teardown and cleanup.</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--dry-run</code></td>
      <td>Dry-runs Atest without actually building, installing, or running tests.</td>
    </tr>
    <tr>
      <td><code>-m</code></td>
      <td><code>--rebuild-module-info</code></td>
      <td>Forces a rebuild of the <code>module-info.json</code> file.</td>
    </tr>
    <tr>
      <td><code>-w</code></td>
      <td><code>--wait-for-debugger</code></td>
      <td>Waits for debugger to finish before executing.</td>
    </tr>
    <tr>
      <td><code>-v</code></td>
      <td><code>--verbose</code></td>
      <td>Displays DEBUG level logging.</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--iterations</code></td>
      <td>Loop-runs tests until the max iteration is reached. (10 by default)</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--rerun-until-failure [COUNT=10]</code></td>
      <td>Reruns all tests until a failure occurs or the max iteration is
        reached. (10 by default)</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--retry-any-failure [COUNT=10]</code></td>
      <td>Reruns failed tests until passed or the max iteration is reached. (10
        by default)</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--start-avd</code></td>
      <td>Automatically creates an AVD and runs tests on the virtual device.</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--acloud-create</code></td>
      <td>Creates an AVD using the <code>acloud</code> command.</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--[CUSTOM_ARGS]</code></td>
      <td>Specifies custom arguments for the test runners.</td>
    </tr>
    <tr>
      <td><code>-a</code></td>
      <td><code>--all-abi</code></td>
      <td>Runs the tests for all available device architectures.</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--host</code></td>
      <td>Runs the test completely on the host without a device.<br/>
      Note: Running a host test that requires a device with <code>--host</code>
      will fail.</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--history</code></td>
      <td>Shows test results in chronological order.</td>
    </tr>
    <tr>
      <td><code></code></td>
      <td><code>--latest-result</code></td>
      <td>Prints the latest test result.</td>
    </tr>
  </tbody>
</table>

For more information on `-b`, `-i` and `-t`, see the
[Specify steps: build, install, or run](#specify-steps) section.

## Specify tests {:#specify-test}

To run tests, specify one or more tests using one of the following
identifiers:

* Module name
* Module:Class
* Class name
* Tradefed integration test
* File path
* Package name

Separate references to multiple tests with spaces, like this:

<pre>
<code class="devsite-terminal">atest <var>test-identifier-1</var> <var>test-identifier-2</var></code>
</pre>

### Module name {:#module-name}

To run an entire test module, use its module name. Input the name as it appears
in the `LOCAL_MODULE` or `LOCAL_PACKAGE_NAME` variables in that test's
`Android.mk` or `Android.bp` file.

Examples:

<pre>
<code class="devsite-terminal">atest FrameworksServicesTests</code>
<code class="devsite-terminal">atest CtsVideoTestCases</code>
</pre>

Note: To run non-module tests integrated into TradeFed (TF), use [TF Integration
Test](#tf-integration) as an identifier instead.



### Module:Class {:#module-class}

To run a single class within a module, use **Module:Class**. **Module** is the
same as described in [Module name](#module_name). **Class** is the name of the
test class in the `.java` file, and can be the fully qualified class name or the
basic name.

Examples:

<pre>
<code class="devsite-terminal">atest CtsVideoTestCases:VideoEncoderDecoderTest</code>
<code class="devsite-terminal">atest FrameworksServicesTests:ScreenDecorWindowTests</code>
<code class="devsite-terminal">atest FrameworksServicesTests:com.android.server.wm.ScreenDecorWindowTests</code>
</pre>

Note: To reduce the time Atest takes to locate the correct test, we recommend
using the full class path as shown in the last example.

### Class name {:#class-name}

To run a single class without explicitly stating a module name, use the class
name.

Examples:

<pre>
<code class="devsite-terminal">atest ScreenDecorWindowTests</code>
<code class="devsite-terminal">atest VideoEncoderDecoderTest</code>
</pre>

Note: To reduce the time Atest takes to locate the correct test, we recommend
using [Module:Class](#module-class) instead of only the class name whenever possible.

### Tradefed integration test {:#tf-integration}

To run tests that are integrated directly into TradeFed (non-modules), input the
name as it appears in the output of the `tradefed.sh list configs` command. For
example:

To run the
[`reboot.xml` test](https://android.googlesource.com/platform/tools/tradefederation/contrib/+/{{ androidLatestReleaseBranch }}/res/config/example/reboot.xml):

<pre>
<code class="devsite-terminal">atest example/reboot</code>
</pre>

To run the
[`native-benchmark.xml` test](https://android.googlesource.com/platform/tools/tradefederation/+/{{ androidLatestReleaseBranch }}/res/config/native-benchmark.xml):

<pre>
<code class="devsite-terminal">atest native-benchmark</code>
</pre>

### File path {:#file-path}

Atest supports running both module-based tests and integration-based tests by
inputting the path to their test file or directory as appropriate. It also
supports running a single class by specifying the path to the class's Java file.
Both relative and absolute paths are supported.

#### Run a module {:#run-module-file-path}

The following examples show two ways to run the `CtsVideoTestCases` module using
a file path.

Run from Android `repo-root`:

<pre>
<code class="devsite-terminal">atest cts/tests/video</code>
</pre>

Run from Android `repo-root/cts/tests/video`:

<pre>
    <code class="devsite-terminal">atest .</code>
</pre>

#### Run a test class {:#run-class-file-path}

The following example shows how to run a specific class within the
`CtsVideoTestCases` module using a file path.

From Android `repo-root`:

<pre>
    <code class="devsite-terminal">atest cts/tests/video/src/android/video/cts/VideoEncoderDecoderTest.java</code>
</pre>

#### Run an integration test {:#run-integration-file-path}

The following example shows how to run an integration test using a file path
from Android `repo-root`:

<pre>
    <code class="devsite-terminal">atest tools/tradefederation/contrib/res/config/example/reboot.xml</code>
</pre>

### Package name {:#package-name}

Atest supports searching for tests by package name.

Examples:

<pre>
    <code class="devsite-terminal">atest com.android.server.wm</code>
    <code class="devsite-terminal">atest com.android.uibench.janktests</code>
</pre>

## Specify steps: Build, install, or run {:#specify-steps}

Use the `-b`, `-i`, and `-t` options to specify which steps to run. If
you don't specify an option, then all steps run.

Note: `-b` and `-t` can run individually, but `-i` can't be run without `-t`.

* Build targets only: <code>atest -b <var>test-to-run</var></code>
* Run tests only: <code>atest -t <var>test-to-run</var></code>
* Install apk and run tests: <code>atest -it <var>test-to-run</var></code>
* Build and run, but don't install: <code>atest -bt
    <var>test-to-run</var></code>

Atest can force a test to skip the cleanup or teardown step. Many tests, such as
CTS, clean up the device after the test is run, so trying to rerun your test
with `-t` will fail without the `--disable-teardown` parameter. Use `-d` before
`-t` to skip the test clean up step and test iteratively.

<pre>
<code class="devsite-terminal">atest -d <var>test-to-run</var></code>
<code class="devsite-terminal">atest -t <var>test-to-run</var></code>
</pre>

Note: Because `-t` disables both **setup/install** and **teardown/cleanup** of
the device, you can rerun your test with <code>atest -t <var>test-to-run</var></code>
as many times as you want.

## Run specific methods {:#run-methods}

Atest supports running specific methods within a test class. Although the whole
module needs to be built, this reduces the time needed to run the tests. To run
specific methods, identify the class using any of the ways supported for
identifying a class (Module:Class, file path, etc) and append the name of the
method:

<pre>
<code class="devsite-terminal">atest <var>reference-to-class</var>#<var>method1</var></code>
</pre>

When specifying multiple methods, separate them with commas:

<pre>
<code class="devsite-terminal">atest <var>reference-to-class</var>#<var>method1</var>,<var>method2</var>,<var>method3</var></code>
</pre>

Examples:

<pre>
<code class="devsite-terminal">atest com.android.server.wm.ScreenDecorWindowTests#testMultipleDecors</code>
<code class="devsite-terminal">atest FrameworksServicesTests:ScreenDecorWindowTests#testFlagChange,testRemoval</code>
</pre>

The following two examples show the preferred ways to run a single method,
`testFlagChange`. These examples are preferred over only using the class name
because specifying the module or the Java file location allows Atest to find the
test much more quickly.

Using Module:Class:

<pre>
<code class="devsite-terminal">atest FrameworksServicesTests:ScreenDecorWindowTests#testFlagChange</code>
</pre>

From Android <var>repo-root</var>:

<pre>
<code class="devsite-terminal">atest frameworks/base/services/tests/wmtests/src/com/android/server/wm/ScreenDecorWindowTests.java#testFlagChange</code>
</pre>

Multiple methods can be run from different classes and modules:

<pre>
<code class="devsite-terminal">atest FrameworksServicesTests:ScreenDecorWindowTests#testFlagChange,testRemoval ScreenDecorWindowTests#testMultipleDecors</code>
</pre>

## Run multiple classes {:#multiple-classes}

To run multiple classes, separate them with spaces in the same way as for running
multiple tests. Atest builds and runs classes efficiently, so specifying a
subset of classes in a module improves performance over running the whole
module.

To run two classes in the same module:

<pre>
<code class="devsite-terminal">atest FrameworksServicesTests:ScreenDecorWindowTests FrameworksServicesTests:DimmerTests</code>
</pre>

To run two classes in different modules:

<pre>
<code class="devsite-terminal">atest FrameworksServicesTests:ScreenDecorWindowTests CtsVideoTestCases:VideoEncoderDecoderTest</code>
</pre>

## Run GTest binaries {:#gtest-binaries}

Atest can run GTest binaries. Use `-a` to run these tests for all available
device architectures, which in this example are `armeabi-v7a` (ARM 32-bit) and
`arm64-v8a` (ARM 64-bit).

Example input test:

<pre>
<code class="devsite-terminal">atest -a libinput_tests inputflinger_tests</code>
</pre>

Note: If you only need to run tests for a specific device architecture, use
`atest -- --abi arm64-v8a` or `atest -- --abi armeabi-v7a`

To select a specific GTest binary to run, use a colon (:) to specify the test
  name, and a hashtag (#) to further specify an individual method.

For example, for the following test definition:
<pre>
<code class="lang-c++">TEST_F(InputDispatcherTest, InjectInputEvent_ValidatesKeyEvents)</code>
</pre>

Run the following to specify the entire test:
<pre>
<code class="devsite-terminal">atest inputflinger_tests:InputDispatcherTest</code>
</pre>

Or run an individual test using the following:
<pre>
<code class="devsite-terminal">atest inputflinger_tests:InputDispatcherTest#InjectInputEvent_ValidatesKeyEvents</code>
</pre>

## Run tests in TEST_MAPPING {:#test-mapping}

Atest can run tests in `TEST_MAPPING` files.

### Run presubmit tests implicitly {:#implicit}

Run presubmit tests in `TEST_MAPPING` files in current and parent directories:

<pre>
<code class="devsite-terminal">atest</code>
</pre>

Run presubmit tests in `TEST_MAPPING` files in <var>/path/to/project</var> and
its parent directories:

<pre>
<code class="devsite-terminal">atest --test-mapping <var>/path/to/project</var></code>
</pre>

### Run a specified test group {:#test-group}

The available test groups are: `presubmit`(default), `postsubmit`,
`mainline-presubmit`, and `all`.

Run postsubmit tests in TEST_MAPPING files in current and parent directories:

<pre>
<code class="devsite-terminal">atest :postsubmit</code>
</pre>

Run tests from all groups in TEST_MAPPING files:

<pre>
<code class="devsite-terminal">atest :all</code>
</pre>

Run postsubmit tests in TEST_MAPPING files in <var>/path/to/project</var> and
its parent directories:

<pre>
<code class="devsite-terminal">atest --test-mapping <var>/path/to/project</var>:postsubmit</code>
</pre>

Run mainline tests in TEST_MAPPING files in <var>/path/to/project</var> and
its parent directories:

<pre>
<code class="devsite-terminal">atest --test-mapping <var>/path/to/project</var>:mainline-presubmit</code>
</pre>

### Run tests in subdirectories {:#sub-directories}

By default, Atest only searches for tests in TEST_MAPPING files upwards (from
the current or the given directory to its parent directories). If you also want
to run tests in TEST_MAPPING files in the subdirectories, use `--include-subdirs`
to force Atest to include those tests as well:

<pre><code class="devsite-terminal">atest --include-subdirs <var>/path/to/project</var></code></pre>


## Run tests in iteration {:#iteration}

Run tests in iteration by passing the `--iterations` argument. Whether it passes
or fails, Atest will repeat the test until the max iteration is reached.

Examples:

By default, Atest iterates 10 times. The number of iterations must be a positive
integer.

<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> --iterations</code>
<code class="devsite-terminal">atest <var>test-to-run</var> --iterations 5</code>
</pre>

The following approaches make it easier to detect flaky tests:

Approach 1:  Run all tests until a failure occurs or the max iteration is reached.

* Stop when a failure occurs or the iteration reaches the 10th (by default) round.
<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> --rerun-until-failure</code>
</pre>
* Stop when a failure occurs or the iteration reaches the 100th round.
<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> --rerun-until-failure 100</code>
</pre>

Approach 2:  Run only failed tests until passed or the max iteration is reached.

* Assume <code><var>test-to-run</var></code> has multiple test cases and one of
  the tests fails. Run only the failed test 10 times (by default) or until the
  test passes.
<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> --retry-any-failure</code>
</pre>
* Stop running the failed test when it passes or reaches the 100th round.
<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> --retry-any-failure 100</code>
</pre>

## Run tests on AVDs {:#avds}

Atest is able to run tests on a newly created AVD. Run `acloud create` to create
an AVD and build artifacts, then use the following examples to run your tests.

Start an AVD and run tests on it:

<pre>
<code class="devsite-terminal">acloud create --local-instance --local-image &amp;&amp; atest <var>test-to-run</var></code>
</pre>

Start an AVD as part of a test run:

<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> --acloud-create "--local-instance --local-image"</code>
</pre>

For more information, run `acloud create --help`.

Note: `--acloud-create` must be the LAST optional argument. Any arguments that
follow it are treated as positional arguments for the AVD.

Note: `--acloud-create` and `--start-avd` don't delete newly created AVDs.
Users must delete them manually.

## Pass options to module {:#pass-options}

Atest is able to pass options to test modules. To add TradeFed command line
options to your test run, use the following structure and make sure your custom
arguments follow the Tradefed command line option format.

<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> -- [CUSTOM_ARGS]</code>
</pre>

Pass test module options to target preparers or test runners defined in the
test config file:

<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> -- --module-arg module-name:option-name:option-value</code>
<code class="devsite-terminal">atest <var>GtsPermissionTestCases</var> -- --module-arg GtsPermissionTestCases:ignore-business-logic-failure:true</code>
</pre>

Pass options to a runner type or class:

<pre>
<code class="devsite-terminal">atest <var>test-to-run</var> -- --test-arg test-class:option-name:option-value</code>
<code class="devsite-terminal">atest <var>CtsVideoTestCases</var> -- --test-arg com.android.tradefed.testtype.JarHosttest:collect-tests-only:true</code>
</pre>

For more information on test-only options, see
[Pass options to the modules](/docs/core/tests/tradefed/testing/through-suite/option-passing#pass_options_to_the_modules).