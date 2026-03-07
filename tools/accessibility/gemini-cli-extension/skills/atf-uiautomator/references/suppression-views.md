## View Matchers

In addition to the many Matchers provided by
[org.hamcrest.Matchers](https://hamcrest.org/JavaHamcrest/javadoc/1.3/org/hamcrest/Matchers.html)
there are many convenience methods that may be useful in the specification of
suppressions.

The following matchers **only work for Views** (i.e. Robolectric and Espresso tests). They will not
work for UI Automator tests, because UI Automator is based on AccessibilityNodeInfo instead of View.
Make sure you determine what your test rule is using before choosing these matchers.

[`AccessibilityCheckResultUtils`](external/accessibility-test-framework/src/main/java/com/google/android/apps/common/testing/accessibility/framework/AccessibilityCheckResultUtils.java):
These are some of the methods that can constrain properties of an
`AccessibilityViewCheckResult`:

  * matchesCheck
  * matchesTypes
  * matchesElements
  * matchesViews
  * matchesResultId

[`ViewMatchers`](external/androidx_test/espresso/core/java/androidx/test/espresso/matcher/ViewMatchers.java):
These are some of the methods that can constrain properties of a`View`, and can
be used in conjunction with `matchesViews`.

  * withId
  * withText
  * withContentDescription
  * withClassName
  * withResourceName
  * isAssignableFrom
  * withTagKey