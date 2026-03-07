# Test mapping

This is a brief introduction of test mapping and an explanation of how to get
started configuring tests in the Android Open Source Project (AOSP).

## About test mapping {:#about}

Test mapping is a Gerrit-based approach that lets developers create presubmit
and postsubmit test rules directly in the Android source tree and leave the
decisions of branches and devices to be tested to the test infrastructure.
Test mapping definitions are JSON files named `TEST_MAPPING` that you can
place in any source directory.

[Atest](atest) can use the `TEST_MAPPING` files to run presubmit tests in the
associated directories. With test mapping, you can add the same set of tests to
presubmit checks with a minimal change inside the Android source tree.

See these examples:

*  [Add presubmit tests to `TEST_MAPPING` for
   `services.core`](https://android.googlesource.com/platform/frameworks/base/+/{{ androidLatestReleaseBranch }}/services/core/java/com/android/server/pm/dex/TEST_MAPPING)

*  [Add presubmit tests to `TEST_MAPPING` for `tools/dexter` using
   imports](https://android.googlesource.com/platform/tools/dexter/+/refs/heads/{{ androidLatestReleaseBranch }}/TEST_MAPPING)

Test mapping relies on the
[Trade Federation (TF) test harness](/docs/core/tests/tradefed) for
tests execution and results reporting.

## Define test groups {:#define-test-groups}

Test mapping groups tests with a *test group*. The name of a test group can be
any string. For example, *presubmit* can be the name for a group of tests to
run when validating changes. And *postsubmit* can be the tests used to validate
the builds after changes are merged.

## Package build script rules {:#package-rules}

In order for the [Trade Federation test harness](/docs/core/tests/tradefed)
to run test modules for a given build, these modules must have a
`test_suites` set for [Soong](blueprints) or a `LOCAL_COMPATIBILITY_SUITE` set
for Make to one of these two suites:

*   `general-tests` is for tests that don't depend on device-specific
    capabilities (such as vendor-specific hardware that most devices don't
    have). Most tests should be in the `general-tests` suite, even if they're
    specific to one ABI or bitness or hardware features like HWASan (there's
    a separate `test_suites` target for each ABI), and even if they have to run
    on a device.
*   `device-tests` is for tests that depend on device-specific capabilities.
    Typically these tests are found under `vendor/`. *Device-specific*
    refers only to capabilities that are unique to *a* device, so this applies
    to JUnit tests as well as GTest tests (which should usually be marked as
    `general-tests` even if they're ABI specific).

Examples:

```
Android.bp: test_suites: ["general-tests"],
Android.mk: LOCAL_COMPATIBILITY_SUITE := general-tests
```

## Configure tests to run in a test suite {:#configure-tests}

For a test to run inside of a test suite, the test:

*   Must not have any build provider.
*   Must clean up after it's finished, for example, by deleting any temporary
    files generated during the test.
*   Must change system settings to the default or original value.
*   Shouldn't assume a device is in a certain state, for example, root ready.
    Most tests don't require root privilege to run. If a test must require
    root, it should specify that with `RootTargetPreparer` in its
    [`AndroidTest.xml`](/docs/core/tests/tradefed/testing/through-suite/android-test-structure),
    as in the following example:

    ```
    <target_preparer class="com.android.tradefed.targetprep.RootTargetPreparer"/>
    ```

## Create test mapping files {:#create-mapping-files}

For the directory requiring test coverage, add a `TEST_MAPPING` JSON file
resembling the [example](#example). These rules ensure that the tests run in
presubmit checks when any files are touched in that directory or any of its
subdirectories.

### Follow an example {:#example}

Here is a sample `TEST_MAPPING` file (it's in JSON format but with comments
supported):

```
{
  "presubmit": [
    // JUnit test with options and file patterns.
    {
      "name": "CtsWindowManagerDeviceTestCases",
      "options": [
        {
          "include-annotation": "android.platform.test.annotations.RequiresDevice"
        }
      ],
      "file_patterns": ["(/|^)Window[^/]*\\.java", "(/|^)Activity[^/]*\\.java"]
    },
    // Device-side GTest with options.
    {
      "name" : "hello_world_test",
      "options": [
        {
          "native-test-flag": "\"servicename1 servicename2\""
        },
        {
          "native-test-timeout": "6000"
        }
      ]
    }
    // Host-side GTest.
    {
      "name" : "net_test_avrcp",
      "host" : true
    }
  ],
  "postsubmit": [
    {
      "name": "CtsDeqpTestCases",
      "options": [
        {
          // Use regex in include-filter which is supported in AndroidJUnitTest
          "include-filter": "dEQP-EGL.functional.color_clears.*"
        }
      ]
    }
  ],
  "imports": [
    {
      "path": "frameworks/base/services/core/java/com/android/server/am"
    }
  ]
}
```

### Set attributes {:#set-attributes}

In the [example](#example), `presubmit` and `postsubmit` are the names of each
test group. See [Define test groups](#define-test-groups) for more information
about test groups.

You can set the name of the test module or Trade Federation integration test
name (resource path to the test XML file, for example,
[`uiautomator/uiautomator-demo`](https://android.googlesource.com/platform/tools/tradefederation/contrib/+/{{ androidLatestReleaseBranch }}/res/config/uiautomator/uiautomator-demo.xml))
in the value of the `name` attribute. Note that the `name` field can't
use the class `name` or the test method `name`. To narrow down the tests to run,
use options such as `include-filter`. See
[`include-filter` sample usage](https://android.googlesource.com/platform/frameworks/base/+/{{ androidLatestReleaseBranch }}/services/core/java/com/android/server/pm/dex/TEST_MAPPING#7).

The `host` setting of a test indicates whether the test is a deviceless test
running on host or not. The default value is `false`, meaning the test
requires a device to run. The supported test types are
[`HostGTest`](/reference/tradefed/com/android/tradefed/testtype/HostGTest) for
GTest binaries and [`HostTest`](/docs/core/tests/development/jar) for JUnit
tests.

The `file_patterns` attribute lets you set a list of regular expression strings
for matching the relative path of any source code file (relative to the
directory containing the `TEST_MAPPING` file). In the [example](#example),
test `CtsWindowManagerDeviceTestCases` runs in presubmit only when a Java file
starts with `Window` or `Activity`, which exists in the same directory as the
`TEST_MAPPING` file or any of its subdirectories. The backslashes (\\) need to
be escaped as they're in a JSON file.

The `imports` attribute lets you include tests in other `TEST_MAPPING` files
without copying the content. The `TEST_MAPPING` files in the parent
directories of the imported path are also included. Test mapping allows
nested imports; this means two `TEST_MAPPING` files can import each other, and
test mapping can merge the included tests.

The `options` attribute contains additional Tradefed command line options.

To get a complete list of available options for a given test, run:

<pre>
<code class="devsite-terminal">tradefed.sh run commandAndExit [test_module] --help</code>
</pre>

Refer to
[Option handling in Tradefed](/docs/core/tests/tradefed/fundamentals/options)
for more details about how options work.

## Run tests with Atest {:#run-with-atest}

To execute the presubmit test rules locally:

1.  Go to the directory containing the `TEST_MAPPING` file.
1.  Run the command:

    <pre>
    <code class="devsite-terminal">atest</code>
    </pre>

All presubmit tests configured in the `TEST_MAPPING` files of the current
directory and its parent directories are run. Atest locates and runs two tests
for presubmit (A and B).

This is the most straightforward way to run presubmit tests in `TEST_MAPPING`
files in the current working directory (CWD) and parent directories. Atest
locates and uses the `TEST_MAPPING` file in CWD and all of its parent
directories.

### Structure source code {:#structure}

This example shows how you can configure `TEST_MAPPING` files across the
source tree:

```
src
├── project_1
│   └── TEST_MAPPING
├── project_2
│   └── TEST_MAPPING
└── TEST_MAPPING
```

Content of `src/TEST_MAPPING`:

```
{
  "presubmit": [
    {
      "name": "A"
    }
  ]
}
```

Content of `src/project_1/TEST_MAPPING`:

```
{
  "presubmit": [
    {
      "name": "B"
    }
  ],
  "postsubmit": [
    {
      "name": "C"
    }
  ],
  "other_group": [
    {
      "name": "X"
    }
  ]}
```

Content of `src/project_2/TEST_MAPPING`:

```
{
  "presubmit": [
    {
      "name": "D"
    }
  ],
  "import": [
    {
      "path": "src/project_1"
    }
  ]}
```

### Specify target directories {:#target-dirs}

You can specify a target directory to run tests in `TEST_MAPPING` files in that
directory. The following command runs two tests (A, B):

<pre>
<code class="devsite-terminal">atest --test-mapping src/project_1</code>
</pre>

### Run postsubmit test rules {:#postsubmit-rules}

You can also use this command to run the postsubmit test rules defined in
`TEST_MAPPING` in `src_path` (default to CWD) and its parent directories:

<pre>
<code class="devsite-terminal">atest [--test-mapping] [src_path]:postsubmit</code>
</pre>

### Run only tests that require no device {:#no-device}

You can use the option `--host` for Atest to run only those tests configured
against the host that requires no device. Without this option, Atest runs both
tests, those that require a device and those running on a host that requires no
device. The tests are run in two separate suites:

<pre>
<code class="devsite-terminal">atest [--test-mapping] --host</code>
</pre>

### Identify test groups {:#identify-groups}

You can specify test groups in the Atest command. The following command runs
all `postsubmit` tests related to files in the `src/project_1` directory, which
contains only one test (C).

Or you can use `:all` to run all tests regardless of group. The following
command runs four tests (A, B, C, X):

<pre>
<code class="devsite-terminal">atest --test-mapping src/project_1:all</code>
</pre>

### Include subdirectories {:#include-subdirs}

By default, running tests in `TEST_MAPPING` with Atest runs only presubmit
tests configured in the `TEST_MAPPING` file in CWD (or
given directory) and its parent directories. If you want to run tests in all
`TEST_MAPPING` files in the subdirectories, use the option `--include-subdir` to
force Atest to include those tests, too.

<pre>
<code class="devsite-terminal">atest --include-subdir</code>
</pre>

Without the `--include-subdir` option, Atest runs only test A. With the
`--include-subdir` option, Atest runs two tests (A, B).

### Line-level comment supported {:#line-level-comment}

You can add a line-level `//` format comment to flesh out the `TEST_MAPPING`
file with a description of the setting that follows.
[ATest](/compatibility/tests/development/atest) and Trade Federation
preprocess `TEST_MAPPING` to a valid JSON format without comments. To keep
the JSON file clean, only the line-level `//` format comment
is supported.

Example:

```
{
  // For presubmit test group.
  "presubmit": [
    {
      // Run test on module A.
      "name": "A"
    }
  ]
}
```