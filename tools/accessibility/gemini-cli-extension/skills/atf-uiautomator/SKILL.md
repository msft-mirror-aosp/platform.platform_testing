---
name: atf-uiautomator
description:
  Use this skill to add ATF accessibility checks to your existing UiAutomator tests.
---

# ATF UiAutomator Integration

## Overview

[UiAutomator](https://developer.android.com/training/testing/other-components/ui-automator)
is an Android testing library for opaque-box testing UI testing. ATF provides
hooks to automatically run accessibility checks in your existing UiAutomator
tests. The checks will run automatically on every test action like `click()`,
`swipe()` etc.

## How to enable checks

Accessibility testing can be enabled using `PlatformUiAutomatorAccessibilityTestRule`
@platform_testing/libraries/uiautomator-accessibility/src/android/platform/uiautomatoraccessibility/PlatformUiAutomatorAccessibilityTestRule.kt

### Android Platform / Gerrit Setup

Add the `uiautomator-accessibility` library to your Android.bp file:

```
    static_libs: [
        ...
        "uiautomator-accessibility",
    ]
```

Add the rule to your test:

```kotlin
import android.platform.uiautomatoraccessibility.PlatformUiAutomatorAccessibilityTestRule

@RunWith(AndroidJUnit4::class)
class SomeTest {
  @get:Rule val a11yRule = PlatformUiAutomatorAccessibilityTestRule()
}
```

or:

```java
import android.platform.uiautomatoraccessibility.PlatformUiAutomatorAccessibilityTestRule;

@RunWith(AndroidJUnit4.class)
public final class SomeTest {
  @Rule public final PlatformUiAutomatorAccessibilityTestRule a11yRule =
      new PlatformUiAutomatorAccessibilityTestRule();
}
```

## Suppressions

Later, you may need to suppress certain findings that are causing your test to fail.
See the `references/suppression.md` file for instructions.

For Ui Automator tests, always use Element-based matchers. Never use View-based matchers.

**NEVER REMOVE SUPPRESSIONS!** Only add new ones. The existing ones are suppressing
additional test failures that you would see.