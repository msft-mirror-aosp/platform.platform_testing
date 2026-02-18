# UI Automator Accessibility Test Rule

Integrates Android's Accessibility Test Framework (ATF) into UiAutomator tests.

## Objective

The primary goal is to prevent accessibility barriers from being introduced into Android platform
codebases at development time. By integrating ATF with UiAutomator, we can "shift-left" to improve
accessibility outcomes for Android users.

This library provides a JUnit Test Rule (`PlatformUiAutomatorAccessibilityTestRule`) to easily enable ATF
checks within existing UiAutomator tests.
