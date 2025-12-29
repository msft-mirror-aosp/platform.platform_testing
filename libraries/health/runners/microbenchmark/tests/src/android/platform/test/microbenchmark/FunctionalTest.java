/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.platform.test.microbenchmark;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeFalse;

import android.platform.test.microbenchmark.Microbenchmark.NoMetricAfter;
import android.platform.test.microbenchmark.Microbenchmark.NoMetricBefore;
import android.platform.test.rule.TestWatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link Functional} runner.
 */
@RunWith(JUnit4.class)
public final class FunctionalTest {
    // Static logs are needed to validate dynamic rules and tests that use TestRequestBuilder, where
    // objects are instantiated with reflection and not directly accessible.
    private static List<String> sLogs = new ArrayList<>();

    @Before
    public void before() {
        sLogs.clear();
    }

    @After
    public void after() {
        sLogs.clear();
    }

    @Test
    public void successTest_reportsSuccess() throws InitializationError {
        Functional runner = new Functional(LoggingTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@Test method body",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void assumptionFailed_inNoMetricBefore_reportsAssumptionFailure_andRunsAfters()
            throws InitializationError {
        Functional runner = new Functional(AssumptionFailedNoMetricBeforeTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(result.getAssumptionFailureCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        // No @Before because failed assumption is in a @NoMetricBefore block,
                        // which happens before @Before
                        "AssumptionFailedTest#noMetricBefore",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void assumptionFailed_inBefore_reportsAssumptionFailure_andRunsAfters()
            throws InitializationError {
        Functional runner = new Functional(AssumptionFailedBeforeTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(result.getAssumptionFailureCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "AssumptionFailedTest#before",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void assumptionFailed_inTest_reportsAssumptionFailure_andRunsAfters()
            throws InitializationError {
        Functional runner = new Functional(AssumptionFailedDuringTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(result.getAssumptionFailureCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@Test method body",
                        "AssumptionFailedTest#test",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void assumptionFailed_inAfter_reportsAssumptionFailure_andRunsAfters()
            throws InitializationError {
        Functional runner = new Functional(AssumptionFailedAfterTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(result.getAssumptionFailureCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@Test method body",
                        "AssumptionFailedTest#after",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void assumptionFailed_inNoMetricAfter_reportsAssumptionFailure_andRunsAfters()
            throws InitializationError {
        Functional runner = new Functional(AssumptionFailedNoMetricAfterTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(result.getAssumptionFailureCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@Test method body",
                        "@After",
                        "AssumptionFailedTest#noMetricAfter",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void failedTest_reportsFailure() throws InitializationError {
        Functional runner = new Functional(LoggingFailedTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isFalse();
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void testNoMetricBeforeFailure_reportsFailedTest() throws InitializationError {
        Functional runner = new Functional(LoggingNoMetricBeforeFailure.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isFalse();
        assertThat(result.getRunCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void testNoMetricAfterFailure_runsTestBodyAndReportsFailedTest() throws InitializationError {
        Functional runner = new Functional(LoggingNoMetricAfterFailure.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isFalse();
        assertThat(result.getRunCount()).isEqualTo(1);
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricRule starting",
                        "@Rule starting",
                        "@NoMetricBefore",
                        "@Before",
                        "@Test method body",
                        "@After",
                        "@NoMetricAfter",
                        "@Rule finished",
                        "@NoMetricRule finished")
                .inOrder();
    }

    @Test
    public void annotationsWithInheritance_runsSuperclassFirstAndLast() throws InitializationError {
        Functional runner = new Functional(GrandchildClassLoggingTest.class);

        Result result = new JUnitCore().run(runner);

        assertThat(result.wasSuccessful()).isTrue();
        assertThat(sLogs)
                .containsExactly(
                        "@NoMetricBefore: super class",
                        "@NoMetricBefore: child class",
                        "@NoMetricBefore: grandchild class",
                        "@Before: super class",
                        "@Before: child class",
                        "@Before: grandchild class",
                        "@Test method body",
                        "@After: grandchild class",
                        "@After: child class",
                        "@After: super class",
                        "@NoMetricAfter: grandchild class",
                        "@NoMetricAfter: child class",
                        "@NoMetricAfter: super class")
                .inOrder();
    }

    /**
     * A test that logs {@link NoMetricBefore}, {@link Before}, {@link After}, {@link
     * NoMetricAfter}, {@link Test} included, used in conjunction with {@link Functional} to
     * determine all {@link Statement}s are evaluated in the proper order.
     */
    @RunWith(Functional.class)
    public static class LoggingTest {
        @Microbenchmark.NoMetricRule
        public NoMetricRule noMetricRule = new NoMetricRule();

        @Rule
        public LoggingRule loggingRule = new LoggingRule();

        @NoMetricBefore
        public void noMetricBeforeMethod() {
            sLogs.add("@NoMetricBefore");
        }

        @Before
        public void beforeMethod() {
            sLogs.add("@Before");
        }

        @Test
        public void testMethod() {
            sLogs.add("@Test method body");
        }

        @After
        public void afterMethod() {
            sLogs.add("@After");
        }

        @NoMetricAfter
        public void noMetricAfterMethod() {
            sLogs.add("@NoMetricAfter");
        }
    }

    public static class LoggingTestWithRules extends LoggingTest {
        @ClassRule
        public static TestRule hardCodedClassRule =
                new TestWatcher() {
                    @Override
                    public void starting(Description description) {
                        sLogs.add("hardcoded class rule starting");
                    }

                    @Override
                    public void finished(Description description) {
                        sLogs.add("hardcoded class rule finished");
                    }
                };

        @Rule
        public TestRule hardCodedRule =
                new TestWatcher() {
                    @Override
                    public void starting(Description description) {
                        sLogs.add("hardcoded test rule starting");
                    }

                    @Override
                    public void finished(Description description) {
                        sLogs.add("hardcoded test rule finished");
                    }
                };
    }

    public static class LoggingFailedTest extends LoggingTest {
        @Test
        public void testMethod() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class AssumptionFailedNoMetricBeforeTest extends LoggingTest {
        @NoMetricBefore
        public void setUpAssume() {
            sLogs.add("AssumptionFailedTest#noMetricBefore");
            assumeFalse(true);
        }
    }

    public static class AssumptionFailedBeforeTest extends LoggingTest {
        @Before
        public void setUpAssume() {
            sLogs.add("AssumptionFailedTest#before");
            assumeFalse(true);
        }
    }

    public static class AssumptionFailedDuringTest extends LoggingTest {
        @Override
        @Test
        public void testMethod() {
            super.testMethod();
            sLogs.add("AssumptionFailedTest#test");
            assumeFalse(true);
        }
    }

    public static class AssumptionFailedAfterTest extends LoggingTest {
        @After
        public void tearDownAssume() {
            sLogs.add("AssumptionFailedTest#after");
            assumeFalse(true);
        }
    }

    public static class AssumptionFailedNoMetricAfterTest extends LoggingTest {
        @NoMetricAfter
        public void tearDownAssume() {
            sLogs.add("AssumptionFailedTest#noMetricAfter");
            assumeFalse(true);
        }
    }

    public static class LoggingTestCreationFailure extends LoggingTest {
        public LoggingTestCreationFailure() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class LoggingNoMetricBeforeFailure extends LoggingTest {
        @NoMetricBefore
        public void noMetricBeforeFailure() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class LoggingNoMetricAfterFailure extends LoggingTest {
        @NoMetricAfter
        public void noMetricAfterFailure() {
            throw new RuntimeException("I failed.");
        }
    }

    public static class LoggingRule extends TestWatcher {
        @Override
        public void starting(Description description) {
            sLogs.add("@Rule starting");
        }

        @Override
        public void finished(Description description) {
            sLogs.add("@Rule finished");
        }
    }

    public static class NoMetricRule extends TestWatcher {
        @Override
        public void starting(Description description) {
            sLogs.add("@NoMetricRule starting");
        }

        @Override
        public void finished(Description description) {
            sLogs.add("@NoMetricRule finished");
        }
    }

    public abstract static class SuperClassLoggingTest {
        /** */
        @NoMetricBefore
        public void superClassNoMetricBefore() {
            sLogs.add("@NoMetricBefore: super class");
        }

        @Before
        public void beforeMethod() {
            sLogs.add("@Before: super class");
        }

        @After
        public void afterMethod() {
            sLogs.add("@After: super class");
        }

        /** */
        @NoMetricAfter
        public void superClassNoMetricAfter() {
            sLogs.add("@NoMetricAfter: super class");
        }
    }

    public abstract static class ChildClassLoggingTest extends SuperClassLoggingTest {
        /** */
        @NoMetricBefore
        public void childClassNoMetricBefore() {
            sLogs.add("@NoMetricBefore: child class");
        }

        @Before
        public void childBeforeMethod() {
            sLogs.add("@Before: child class");
        }

        @After
        public void childAfterMethod() {
            sLogs.add("@After: child class");
        }

        /** */
        @NoMetricAfter
        public void childClassNoMetricAfter() {
            sLogs.add("@NoMetricAfter: child class");
        }
    }

    @RunWith(Functional.class)
    public static class GrandchildClassLoggingTest extends ChildClassLoggingTest {
        /** */
        @NoMetricBefore
        public void grandchildClassNoMetricBefore() {
            sLogs.add("@NoMetricBefore: grandchild class");
        }

        @Before
        public void grandchildBeforeMethod() {
            sLogs.add("@Before: grandchild class");
        }

        /** */
        @Test
        public void testMethod() {
            sLogs.add("@Test method body");
        }

        @After
        public void grandchildAfterMethod() {
            sLogs.add("@After: grandchild class");
        }

        /** */
        @NoMetricAfter
        public void grandchildClassNoMetricAfter() {
            sLogs.add("@NoMetricAfter: grandchild class");
        }
    }
}
