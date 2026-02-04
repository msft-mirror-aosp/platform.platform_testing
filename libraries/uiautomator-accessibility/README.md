# UI Automator Accessibility Test Rule

Integrates Android's Accessibility Test Framework (ATF) into UiAutomator tests.

## Objective

The primary goal is to prevent accessibility barriers from being introduced into Android platform
codebases at development time. By integrating ATF with UiAutomator, we can "shift-left" to improve
accessibility outcomes for Android users.

This library provides a JUnit Test Rule (`PlatformUiAutomatorAccessibilityTestRule`) to easily enable ATF
checks within existing UiAutomator tests.

## Quick Start

To add accessibility checks to your existing UI Automator tests, try our Gemini CLI Extension:

1. Install the a11y-check-integrator Gemini CLI extension. `cd` to your Android repo root, then:
    ```
    gemini extension link \
      platform_testing/tools/accessibility/gemini-cli-extension
    ```
2. Set up your build environment in the normal way: `. build/envsetup.sh && lunch`
3. Connect a test device or run an emulated one using `acloud`. This is so the agent can run atest
   to verify the fixes.
4. Run Gemini CLI.
5. Type the slash command `/a11y/add-checks [path_to_your_test_file]`
6. The agent will find the best place to add the checks, as well as run atest and add suppressions
   for any failing checks, all automatically.
7. Review all of the generated suppressions. For each suppression:
    *   Understand the rationale behind the accessibility finding.
    *   If the finding represents a true accessibility bug, file a TODO bug to fix the issue in the
        codebase and delete the suppression.
    *   If the finding is a false positive, add comments and TODOs to the generated suppression
        explaining why it is being suppressed.