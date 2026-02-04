# What is ATF?

The Accessibility Testing Framework (ATF) is a library that contains rules and checks that can be used by tests to automate certain aspects of accessibility testing.

The library includes detections for a variety of common accessibility issues, including:

* Missing accessibility labels required for screen readers, like TalkBack
* Touch targets that are too small to reliably support interaction
* Text and images with low color contrast against their backgrounds
* Various other UI issues that may impact usability with Android’s assistive technologies

ATF offers turnkey integrations with a variety of UI testing frameworks, including Espresso, Compose UI Tests, and, UI Automator.  It works by piggy-backing atop existing tests and running accessibility checks at various points within the test lifecycle – usually when the test performs an action that changes the UI state, like clicking on something, and at the end of each test method.  You can also explicitly invoke accessibility checks at various points within a test to maximize coverage, if desired.  When ATF identifies a potential accessibility issue, it will throw an exception, causing the applicable test to fail.