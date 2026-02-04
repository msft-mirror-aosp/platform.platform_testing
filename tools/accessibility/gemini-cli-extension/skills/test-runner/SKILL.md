---
name: test-runner
description: Use this skill to run Android tests using the atest command.
---

# Test Runner

This skill helps you run Android tests using `atest`.

**IMPORTANT**: Instead of using the `atest` binary directly, you will need to use the wrapper script
in `scripts/atest-wrapper.sh`. All arguments and functionality is identical to atest.

## Workflow

### 1. Running a Specific Test Class

To run a specific test class, provide the path to the test file (try this first):

```bash
scripts/atest-wrapper.sh <path/to/TestClass.java>
```

### 2. Running Tests in TEST_MAPPING

To run tests defined in a `TEST_MAPPING` file based on a given test file path, you need to find the closest ancestor directory containing a `TEST_MAPPING` file.

1.  **Get the directory of the test file:** Start with the directory containing the test file.
2.  **Check for TEST_MAPPING:** Look for a `TEST_MAPPING` file in the current directory.
3.  **Go Up:** If not found, go to the parent directory.
4.  **Repeat:** Repeat steps 2 and 3 until a `TEST_MAPPING` file is found or you reach the root of the repository.

Once you find the directory with the `TEST_MAPPING` file:

```bash
cd <directory/with/TEST_MAPPING>
scripts/atest-wrapper.sh
```

Alternatively, you can use the `--test-mapping` flag with the directory path:

```bash
scripts/atest-wrapper.sh --test-mapping <directory/with/TEST_MAPPING>
```

This will execute the `presubmit` tests defined in the found `TEST_MAPPING` file and any `TEST_MAPPING` files in its parent directories.

## Available Resources

The following reference files provide more detailed information. You should only need to consult these if the basic instructions above are insufficient or if you encounter issues or errors:

*   `references/atest.md:` Comprehensive documentation on the `atest` command, including optional arguments, different ways to specify tests (by module, class, file path, etc.), running specific methods, and more advanced features.
*   `references/test-mapping.md:` Detailed explanation of the `TEST_MAPPING` file format, how to define test groups (like `presubmit` and `postsubmit`), how to structure `TEST_MAPPING` files in your project, and how `atest` interacts with them.
