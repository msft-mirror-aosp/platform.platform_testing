## Suppressing known issues

When accessibility checks are enabled, they may find some issues that you are
not willing or able to deal with immediately. For example, these could be
confirmed false positive results (not true accessibility issues) or they could
originate from a shared component over which you do not have direct control.
These findings can be suppressed so that they do not cause your tests to fail.

The criteria for a suppression is indicated with a
[Hamcrest](http://go/java-testing/assertion_frameworks#hamcrest)
[Matcher](https://hamcrest.org/JavaHamcrest/javadoc/1.3/org/hamcrest/Matcher.html)
<[AccessibilityViewCheckResult](external/accessibility-test-framework/src/main/java/com/google/android/apps/common/testing/accessibility/framework/AccessibilityViewCheckResult.java)\>.
Findings that satisfy a Matcher will be suppressed.

BEST PRACTICE: Include a bug number in the code with each suppression so that
maintainers of the code can tell when the issue has been resolved and the
suppression can be removed. Include a short description of why the suppression
was added in the first place.

BEST PRACTICE: Make matchers fairly specific to avoid unintentionally
suppressing future accessibility issues that are similar yet distinct.

For example, if a test fails due to an issue reported by `TouchTargetSizeCheck`,
that finding could be suppressed with a Matcher that matches findings from
`TouchTargetSizeCheck`. But that could mask other touch target issues.

*** BAD EXAMPLE***
```java{.bad}
 matchesCheck(TouchTargetSizeCheck.class) // Too broad
```

If instead the Matcher matches only findings that are both from
`TouchTargetSizeCheck` **and** reference a View with a specific resource ID,
then it would not suppress touch target issues from other Views or other types
of issues from the same View.

*** GOOD EXAMPLE***
```java{.good}
// TODO: b/123456 - Increase the touch target size.
// This is suppressed because ...
allOf(
    matchesCheck(TouchTargetSizeCheck.class),
    matchesView(withResourceName(endsWith("some_view"))));
```

NOTE: Any results that were suppressed by either global or test specific
suppression methods will have a result type of `SUPPRESSED`.

## Usage


Sometimes, you might need to suppress certain errors because they are false
positives or they are real issues to be addressed in the future. Suppress
failures by adding matchers to the rule's suppressor:

```kotlin
@RunWith(AndroidJUnit4::class)
class ExampleTest {
  @get:Rule val a11yRule = PlatformUiAutomatorAccessibilityTestRule().configureSuppressions {
     // TODO: b/123456 - fix touch target sizes, then remove this suppression
     it.addSuppressingResultMatcher(
         allOf(
            matchesCheck(TouchTargetSizeCheck::class.java),
            // Match on the element with resource @id/some_view. To be more
            // specific, also include the package name.
            matchesElements(withResourceName(endsWith("some_view")))))
  }
}
```


## Matchers

In addition to the many Matchers provided by
[org.hamcrest.Matchers](https://hamcrest.org/JavaHamcrest/javadoc/1.3/org/hamcrest/Matchers.html),
such as:

  * allOf
  * anyOf
  * is
  * isOneOf
  * endsWIth

there are many convenience methods that may be useful in the specification of
suppressions.

[`ElementMatchers`](external/accessibility-test-framework/src/main/java/com/google/android/apps/common/testing/accessibility/framework/matcher/ElementMatchers.java):
These are some of the methods that can constrain properties of a
`ViewHierarchyElement`, and can be used in conjunction with `matchesElements`.

  * withText
  * withClassName
  * withContentDescription
  * withTestTag
  * withResourceName
  * withChild

NOTE: Although
`ViewMatchers.withResourceName` takes a simple resource name (ex. "ok_button"),
`ElementMatchers.withResourceName` takes a qualified name (ex. "my.package:id/ok_button").

## Combining suppressions

If there are multiple check findings in the same test file, combine the
suppressing matchers per check classes.

Use `addSuppressingResultMatcher()` to match multiple disparate checks. You may group
different offending elements in the same check class category with the `allOf()`
matcher combined with the single `matchesCheck()` matcher.

Make sure to assign a separate TODO comment and bug for each individual check
finding.

```java
@Rule
public PlatformUiAutomatorAccessibilityTestRule a11yRule = new PlatformUiAutomatorAccessibilityTestRule()
    .configureSuppressions(it -> {
        it.addSuppressingResultMatcher(
            // TODO: b/12345 - remove this suppression and fix the overlapping button
            allOf(
                matchesCheck(DuplicateClickableBoundsCheck.class),
                matchesElements(withResourceName(
                    endsWith("search_container_workspace")
                ))
            ));

        it.addSuppressingResultMatcher(
            allOf(
                matchesCheck(TouchTargetSizeCheck.class),
                matchesElements(withResourceName(is(oneOf(
                    // TODO: b/12346 - remove this suppression and fix the touch target
                    "com.android.systemui:id/carrier_combo",
                    // TODO: b/12347 - remove this suppression and fix the touch target
                    "com.android.systemui:id/no_carrier_text",
                    // TODO: b/12348 - remove this suppression and fix the touch target
                    "com.android.systemui:id/action0")))
                )
            )
        );
    });
```