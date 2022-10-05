/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker

import android.platform.test.util.TestFilter
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.annotation.FlickerServiceCompatible
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.helpers.IS_FAAS_ENABLED
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.service.FlickerFrameworkMethod
import com.android.server.wm.flicker.service.FlickerTestCase
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.traces.common.Cache
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.system.measureTimeMillis
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.internal.AssumptionViolatedException
import org.junit.internal.runners.model.EachTestNotifier
import org.junit.internal.runners.statements.RunAfters
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter
import org.junit.runner.manipulation.InvalidOrderingException
import org.junit.runner.manipulation.NoTestsRemainException
import org.junit.runner.manipulation.Orderer
import org.junit.runner.manipulation.Sorter
import org.junit.runner.notification.RunNotifier
import org.junit.runner.notification.StoppedByUserException
import org.junit.runners.Parameterized
import org.junit.runners.model.FrameworkField
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.RunnerScheduler
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.TestWithParameters

/**
 * Implements the JUnit 4 standard test case class model, parsing from a flicker DSL.
 *
 * Supports both assertions in {@link org.junit.Test} and assertions defined in the DSL
 *
 * When using this runner the default `atest class#method` command doesn't work. Instead use: --
 * --test-arg \
 * ```
 *     com.android.tradefed.testtype.AndroidJUnitTest:instrumentation-arg:filter-tests:=<TEST_NAME>
 * ```
 * For example: `atest FlickerTests -- \
 * ```
 *     --test-arg com.android.tradefed.testtype.AndroidJUnitTest:instrumentation-arg:filter-tests\
 *     :=com.android.server.wm.flicker.close.\
 *     CloseAppBackButtonTest#launcherWindowBecomesVisible[ROTATION_90_GESTURAL_NAV]`
 * ```
 */
class FlickerBlockJUnit4ClassRunner
@JvmOverloads
constructor(
    test: TestWithParameters,
    private val parameters: Array<Any> = test.parameters.toTypedArray(),
    private val flickerTestParameter: FlickerTestParameter =
        parameters.filterIsInstance<FlickerTestParameter>().firstOrNull()
            ?: error("No FlickerTestParameter provided for FlickerRunner")
) : BlockJUnit4ClassRunnerWithParameters(test) {
    private var flickerBuilderProviderMethod: FrameworkMethod? = null

    private val arguments = InstrumentationRegistry.getArguments()
    // null parses to false (so defaults to running all FaaS tests)
    private val isBlockingTest = arguments.getString("faas:blocking").toBoolean()

    override fun run(notifier: RunNotifier) {
        val testNotifier = EachTestNotifier(notifier, description)
        testNotifier.fireTestSuiteStarted()
        try {
            val statement = classBlock(notifier)
            statement.evaluate()
        } catch (e: AssumptionViolatedException) {
            testNotifier.addFailedAssumption(e)
        } catch (e: StoppedByUserException) {
            throw e
        } catch (e: Throwable) {
            testNotifier.addFailure(e)
        } finally {
            testNotifier.fireTestSuiteFinished()
        }
    }

    /**
     * Implementation of Filterable and Sortable Based on JUnit's ParentRunner implementation but
     * with a minor modification to ensure injected FaaS tests are not filtered out.
     */
    @Throws(NoTestsRemainException::class)
    override fun filter(filter: Filter) {
        childrenLock.lock()
        try {
            val children: MutableList<FrameworkMethod> = getFilteredChildren().toMutableList()
            val iter: MutableIterator<FrameworkMethod> = children.iterator()
            while (iter.hasNext()) {
                val each: FrameworkMethod = iter.next()
                if (isInjectedFaasTest(each)) {
                    // Don't filter out injected FaaS tests
                    continue
                }
                if (shouldRun(filter, each)) {
                    try {
                        filter.apply(each)
                    } catch (e: NoTestsRemainException) {
                        iter.remove()
                    }
                } else {
                    iter.remove()
                }
            }
            filteredChildren = Collections.unmodifiableList(children)
            if (filteredChildren!!.isEmpty()) {
                throw NoTestsRemainException()
            }
        } finally {
            childrenLock.unlock()
        }
    }

    private fun isInjectedFaasTest(method: FrameworkMethod): Boolean {
        return method is FlickerFrameworkMethod
    }

    override fun isIgnored(child: FrameworkMethod): Boolean {
        if (child is FlickerFrameworkMethod) {
            return child.isIgnored()
        }

        return child.getAnnotation(Ignore::class.java) != null
    }

    /**
     * Implementation of ParentRunner based on BlockJUnit4ClassRunner. Modified to report Flicker
     * execution errors in the test results.
     */
    override fun runChild(method: FrameworkMethod, notifier: RunNotifier) {
        val description = describeChild(method)
        if (isIgnored(method)) {
            notifier.fireTestIgnored(description)
        } else {
            val statement: Statement =
                object : Statement() {
                    @Throws(Throwable::class)
                    override fun evaluate() {
                        if (!flickerTestParameter.isInitialized) {
                            Log.v(FLICKER_TAG, "Flicker object is not yet initialized")
                            injectFlickerOnTestParams()
                        }
                        require(flickerTestParameter.isInitialized) {
                            "flickerTestParameter not initialized"
                        }
                        val flicker = flickerTestParameter.flicker
                        var ranChecks = false
                        flicker.setAssertionsCheckedCallback { ranChecks = true }
                        methodBlock(method).evaluate()
                        require(isInjectedFaasTest(method) || ranChecks) {
                            "No Flicker assertions ran on test..."
                        }
                        val results = flickerTestParameter.result
                        requireNotNull(results) {
                            "Flicker results are null after test evaluation... "
                        }
                        // Report all the execution errors collected during the Flicker setup and
                        // transition execution
                        val executionError = results.transitionExecutionError
                        if (executionError != null) {
                            Log.e(FLICKER_TAG, "Flicker Execution Error", executionError)
                            throw executionError
                        }

                        // Only report FaaS execution errors on FaaS tests to avoid FaaS errors
                        // causing legacy tests to fail.
                        if (isInjectedFaasTest(method)) {
                            val faasExecutionError = results.faasExecutionError
                            if (faasExecutionError != null) {
                                Log.e(FLICKER_TAG, "FaaS Execution Error", faasExecutionError)
                                throw faasExecutionError
                            }
                        }
                    }
                }
            runLeaf(statement, description, notifier)
        }
    }

    /** {@inheritDoc} */
    @Deprecated("Deprecated in Java")
    override fun validateInstanceMethods(errors: MutableList<Throwable>) {
        validateFlickerObject(errors)
        super.validateInstanceMethods(errors)
    }

    /**
     * Returns the methods that run tests. Is ran after validateInstanceMethods, so
     * flickerBuilderProviderMethod should be set.
     */
    override fun computeTestMethods(): List<FrameworkMethod> {
        val tests = mutableListOf<FrameworkMethod>()
        tests.addAll(super.computeTestMethods())

        // Don't compute when called from validateInstanceMethods since this will fail
        // as the parameters will not be set. And AndroidLogOnlyBuilder is a non-executing runner
        // used to run tests in dry-run mode, so we don't want to execute in flicker transition in
        // that case either.
        val stackTrace = Thread.currentThread().stackTrace
        if (
            stackTrace.none { it.methodName == "validateInstanceMethods" } &&
                stackTrace.none {
                    it.className == "androidx.test.internal.runner.AndroidLogOnlyBuilder"
                }
        ) {
            val hasFlickerServiceCompatibleAnnotation =
                TestClass(super.createTest()::class.java)
                    .annotations
                    .filterIsInstance<FlickerServiceCompatible>()
                    .firstOrNull() != null

            if (
                IS_FAAS_ENABLED &&
                    hasFlickerServiceCompatibleAnnotation &&
                    isShellTransitionsEnabled
            ) {
                if (!flickerTestParameter.isInitialized) {
                    Log.v(FLICKER_TAG, "Flicker object is not yet initialized")
                    val test = super.createTest()
                    injectFlickerOnTestParams(test)
                }

                val faasTests: List<FrameworkMethod>
                measureTimeMillis { faasTests = computeFlickerServiceTests(isBlockingTest) }
                    .also {
                        Log.d(FLICKER_TAG, "Took ${it}ms to computed ${faasTests.size} faas tests")
                    }
                tests.addAll(faasTests)
            }
        }

        return tests
    }

    /**
     * Runs the flicker transition to collect the traces and run FaaS on them to get the FaaS
     * results and then create functional test results for each of them.
     */
    private fun computeFlickerServiceTests(onlyBlockingAssertions: Boolean): List<FrameworkMethod> {
        val flickerTestMethods = mutableListOf<FlickerFrameworkMethod>()

        val flicker = flickerTestParameter.flicker
        if (flicker.result == null) {
            flicker.execute()
        }

        // TODO: Figure out how we can report this without aggregation to have more precise and
        //       granular data on the actual failure rate.
        for (aggregatedResult in
            aggregateFaasResults(flicker.faas.assertionResults).entries.iterator()) {
            val testName = aggregatedResult.key
            val results = aggregatedResult.value

            val injectedTestCase = FlickerTestCase(results, onlyBlockingAssertions)
            val mockedTestMethod =
                TestClass(injectedTestCase.javaClass)
                    .getAnnotatedMethods(FlickerTestCase.InjectedTest::class.java)
                    .first()
            val mockedFrameworkMethod =
                FlickerFrameworkMethod(mockedTestMethod.method, injectedTestCase, testName)
            flickerTestMethods.add(mockedFrameworkMethod)
        }

        // Flush flicker data to storage to save memory and avoid OOM exceptions
        flicker.clear()

        return flickerTestMethods
    }

    /** {@inheritDoc} */
    override fun getChildren(): MutableList<FrameworkMethod> {
        val arguments = InstrumentationRegistry.getArguments()
        val validChildren =
            super.getChildren().filter {
                val childDescription = describeChild(it)
                TestFilter.isFilteredOrUnspecified(arguments, childDescription)
            }
        return validChildren.toMutableList()
    }

    /** {@inheritDoc} */
    override fun classBlock(notifier: RunNotifier): Statement {
        val statement = childrenInvoker(notifier)
        val cleanUpMethod = getFlickerCleanUpMethod()
        val frameworkMethod = FrameworkMethod(cleanUpMethod)
        val cacheCleanUp = FrameworkMethod(Cache::class.java.getMethod("clear"))
        return RunAfters(
            RunAfters(statement, listOf(frameworkMethod), flickerTestParameter),
            listOf(cacheCleanUp),
            Cache
        )
    }

    /**
     * Adds to `errors` for each method annotated with `@Test`that is not a public, void instance
     * method with no arguments.
     */
    private fun validateFlickerObject(errors: MutableList<Throwable>) {
        val methods = testClass.getAnnotatedMethods(FlickerBuilderProvider::class.java)

        if (methods.isEmpty() || methods.size > 1) {
            val prefix = if (methods.isEmpty()) "One" else "Only one"
            errors.add(Exception("$prefix object should be annotated with @FlickerObject"))
        } else {
            val method = methods.first()

            if (Modifier.isStatic(method.method.modifiers)) {
                errors.add(Exception("Method ${method.name}() should not be static"))
            }
            if (!Modifier.isPublic(method.method.modifiers)) {
                errors.add(Exception("Method ${method.name}() should be public"))
            }
            if (method.returnType != FlickerBuilder::class.java) {
                errors.add(
                    Exception(
                        "Method ${method.name}() should return a " +
                            "${FlickerBuilder::class.java.simpleName} object"
                    )
                )
            }
            if (method.method.parameterTypes.isNotEmpty()) {
                errors.add(Exception("Method ${method.name} should have no parameters"))
            }
        }

        if (errors.isEmpty()) {
            flickerBuilderProviderMethod = methods.first()
        }
    }

    /** {@inheritDoc} */
    override fun createTest(): Any {
        val test = super.createTest()
        if (!flickerTestParameter.isInitialized) {
            Log.v(FLICKER_TAG, "Flicker object is not yet initialized")
            injectFlickerOnTestParams(test)
        }

        require(flickerTestParameter.isInitialized) {
            "Failed to initialize flickerTestParameter on test creation $test"
        }
        return test
    }

    /** Builds a flicker object and assigns it to the test parameters */
    private fun injectFlickerOnTestParams(test: Any = super.createTest()) {
        val flickerBuilderProviderMethod = flickerBuilderProviderMethod
        if (flickerBuilderProviderMethod != null) {
            val testClass = test::class.java
            val testName = testClass.simpleName
            Log.v(
                FLICKER_TAG,
                "Creating flicker object for $testName and adding it into " + "test parameter"
            )

            val isFlickerServiceCompatible =
                TestClass(testClass)
                    .annotations
                    .filterIsInstance<FlickerServiceCompatible>()
                    .firstOrNull() != null
            if (IS_FAAS_ENABLED && isFlickerServiceCompatible) {
                flickerTestParameter.enableFaas()
            }

            val builder = flickerBuilderProviderMethod.invokeExplosively(test) as FlickerBuilder
            flickerTestParameter.initialize(builder, testName)
        } else {
            Log.v(
                FLICKER_TAG,
                "Missing flicker builder provider method " + "in ${test::class.java.simpleName}"
            )
        }
    }

    /** {@inheritDoc} */
    override fun validateConstructor(errors: MutableList<Throwable>) {
        super.validateConstructor(errors)

        if (errors.isEmpty()) {
            // should have only one constructor, otherwise parent
            // validator will create an exception
            val ctor = testClass.javaClass.constructors.first()
            if (ctor.parameterTypes.none { it == FlickerTestParameter::class.java }) {
                errors.add(
                    Exception(
                        "Constructor should have a parameter of type " +
                            FlickerTestParameter::class.java.simpleName
                    )
                )
            }
        }
    }

    /**
     * Obtains the method to clean up a flicker object cache, necessary to release memory after a
     * configuration is executed
     */
    private fun getFlickerCleanUpMethod() = FlickerTestParameter::class.java.getMethod("clear")

    private fun getAnnotatedFieldsByParameter(): List<FrameworkField?> {
        return testClass.getAnnotatedFields(Parameterized.Parameter::class.java)
    }

    private fun fieldsAreAnnotated(): Boolean {
        return !getAnnotatedFieldsByParameter().isEmpty()
    }

    private fun aggregateFaasResults(
        assertionResults: MutableList<AssertionResult>
    ): Map<String, List<AssertionResult>> {
        val aggregatedResults = mutableMapOf<String, MutableList<AssertionResult>>()
        for (result in assertionResults) {
            val testName = "FaaS_${result.scenario.description}_${result.assertionName}"
            if (!aggregatedResults.containsKey(testName)) {
                aggregatedResults[testName] = mutableListOf()
            }
            aggregatedResults[testName]!!.add(result)
        }
        return aggregatedResults
    }

    /**
     * ********************************************************************************************
     * START of code copied from ParentRunner to have local access to filteredChildren to ensure
     * FaaS injected tests are not filtered out.
     */

    // Guarded by childrenLock
    @Volatile private var filteredChildren: List<FrameworkMethod>? = null
    private val childrenLock: Lock = ReentrantLock()

    @Volatile
    private var scheduler: RunnerScheduler =
        object : RunnerScheduler {
            override fun schedule(childStatement: Runnable) {
                childStatement.run()
            }

            override fun finished() {
                // do nothing
            }
        }

    /**
     * Sets a scheduler that determines the order and parallelization of children. Highly
     * experimental feature that may change.
     */
    override fun setScheduler(scheduler: RunnerScheduler) {
        this.scheduler = scheduler
    }

    private fun shouldRun(filter: Filter, each: FrameworkMethod): Boolean {
        return filter.shouldRun(describeChild(each))
    }

    override fun sort(sorter: Sorter) {
        if (shouldNotReorder()) {
            return
        }
        childrenLock.lock()
        filteredChildren =
            try {
                for (each in getFilteredChildren()) {
                    sorter.apply(each)
                }
                val sortedChildren: List<FrameworkMethod> =
                    ArrayList<FrameworkMethod>(getFilteredChildren())
                Collections.sort(sortedChildren, comparator(sorter))
                Collections.unmodifiableList(sortedChildren)
            } finally {
                childrenLock.unlock()
            }
    }

    /**
     * Implementation of [Orderable.order].
     *
     * @since 4.13
     */
    @Throws(InvalidOrderingException::class)
    override fun order(orderer: Orderer) {
        if (shouldNotReorder()) {
            return
        }
        childrenLock.lock()
        try {
            var children: List<FrameworkMethod> = getFilteredChildren()
            // In theory, we could have duplicate Descriptions. De-dup them before ordering,
            // and add them back at the end.
            val childMap: MutableMap<Description, MutableList<FrameworkMethod>> =
                LinkedHashMap(children.size)
            for (child in children) {
                val description = describeChild(child)
                var childrenWithDescription: MutableList<FrameworkMethod>? = childMap[description]
                if (childrenWithDescription == null) {
                    childrenWithDescription = ArrayList<FrameworkMethod>(1)
                    childMap[description] = childrenWithDescription
                }
                childrenWithDescription.add(child)
                orderer.apply(child)
            }
            val inOrder = orderer.order(childMap.keys)
            children = ArrayList<FrameworkMethod>(children.size)
            for (description in inOrder) {
                children.addAll(childMap[description]!!)
            }
            filteredChildren = Collections.unmodifiableList(children)
        } finally {
            childrenLock.unlock()
        }
    }

    private fun shouldNotReorder(): Boolean {
        // If the test specifies a specific order, do not reorder.
        return description.getAnnotation(FixMethodOrder::class.java) != null
    }

    private fun getFilteredChildren(): List<FrameworkMethod> {
        childrenLock.lock()
        val filteredChildren =
            try {
                if (filteredChildren != null) {
                    filteredChildren!!
                } else {
                    Collections.unmodifiableList(ArrayList<FrameworkMethod>(children))
                }
            } finally {
                childrenLock.unlock()
            }
        return filteredChildren
    }

    override fun testCount(): Int {
        return super.testCount()
    }

    override fun getDescription(): Description {
        val clazz = testClass.javaClass
        // if subclass overrides `getName()` then we should use it
        // to maintain backwards compatibility with JUnit 4.12
        val description: Description =
            if (clazz == null || clazz.name != name) {
                Description.createSuiteDescription(name, *runnerAnnotations)
            } else {
                Description.createSuiteDescription(clazz, *runnerAnnotations)
            }
        for (child in getFilteredChildren()) {
            description.addChild(describeChild(child))
        }
        return description
    }

    /**
     * Returns a [Statement]: Call [.runChild] on each object returned by [.getChildren] (subject to
     * any imposed filter and sort)
     */
    override fun childrenInvoker(notifier: RunNotifier): Statement {
        return object : Statement() {
            override fun evaluate() {
                runChildren(notifier)
            }
        }
    }

    private fun runChildren(notifier: RunNotifier) {
        val currentScheduler = scheduler
        try {
            for (each in getFilteredChildren()) {
                currentScheduler.schedule { this.runChild(each, notifier) }
            }
        } finally {
            currentScheduler.finished()
        }
    }

    private fun comparator(sorter: Sorter): Comparator<in FrameworkMethod> {
        return Comparator { o1, o2 -> sorter.compare(describeChild(o1), describeChild(o2)) }
    }

    /**
     * END of code copied from ParentRunner to have local access to filteredChildren to ensure FaaS
     * injected tests are not filtered out.
     */
}
