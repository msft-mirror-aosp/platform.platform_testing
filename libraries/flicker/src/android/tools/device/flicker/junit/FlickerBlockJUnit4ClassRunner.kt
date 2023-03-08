/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.device.flicker.junit

import android.os.Bundle
import android.platform.test.util.TestFilter
import android.tools.common.Scenario
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Collections
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.internal.AssumptionViolatedException
import org.junit.internal.runners.model.EachTestNotifier
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter
import org.junit.runner.manipulation.InvalidOrderingException
import org.junit.runner.manipulation.NoTestsRemainException
import org.junit.runner.manipulation.Orderable
import org.junit.runner.manipulation.Orderer
import org.junit.runner.manipulation.Sorter
import org.junit.runner.notification.RunNotifier
import org.junit.runner.notification.StoppedByUserException
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.RunnerScheduler
import org.junit.runners.model.Statement
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.TestWithParameters

/**
 * Implements the JUnit 4 standard test case class model, parsing from a flicker DSL.
 *
 * Supports both assertions in {@link org.junit.Test} and assertions defined in the DSL
 *
 * When using this runner the default `atest class#method` command doesn't work. Instead use: --
 * --test-arg \
 *
 * ```
 *     com.android.tradefed.testtype.AndroidJUnitTest:instrumentation-arg:filter-tests:=<TEST_NAME>
 * ```
 *
 * For example: `atest FlickerTests -- \
 *
 * ```
 *     --test-arg com.android.tradefed.testtype.AndroidJUnitTest:instrumentation-arg:filter-tests\
 *     :=com.android.server.wm.flicker.close.\
 *     CloseAppBackButtonTest#launcherWindowBecomesVisible[ROTATION_90_GESTURAL_NAV]`
 * ```
 */
class FlickerBlockJUnit4ClassRunner(test: TestWithParameters?, private val scenario: Scenario?) :
    BlockJUnit4ClassRunnerWithParameters(test), IFlickerJUnitDecorator {

    private val arguments: Bundle = InstrumentationRegistry.getArguments()
    private val flickerDecorator =
        test?.let {
            FlickerServiceDecorator(
                test.testClass,
                scenario,
                inner = LegacyFlickerDecorator(test.testClass, scenario, inner = this)
            )
        }

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
        return method is FlickerServiceCachedTestCase
    }

    override fun isIgnored(child: FrameworkMethod): Boolean {
        return child.getAnnotation(Ignore::class.java) != null
    }

    /**
     * Returns the methods that run tests. Is ran after validateInstanceMethods, so
     * flickerBuilderProviderMethod should be set.
     */
    public override fun computeTestMethods(): List<FrameworkMethod> {
        val result = mutableListOf<FrameworkMethod>()
        if (scenario != null) {
            val testInstance = createTest()
            result.addAll(flickerDecorator?.getTestMethods(testInstance) ?: emptyList())
        }
        return result
    }

    override fun describeChild(method: FrameworkMethod?): Description {
        return flickerDecorator?.getChildDescription(method)
            ?: error("There are no children to describe")
    }

    /** {@inheritDoc} */
    override fun getChildren(): MutableList<FrameworkMethod> {
        val validChildren =
            super.getChildren().filter {
                val childDescription = describeChild(it)
                TestFilter.isFilteredOrUnspecified(arguments, childDescription)
            }
        return validChildren.toMutableList()
    }

    override fun methodInvoker(method: FrameworkMethod, test: Any): Statement {
        return flickerDecorator?.getMethodInvoker(method, test)
            ?: error("No statements to invoke for $method in $test")
    }

    override fun validateConstructor(errors: MutableList<Throwable>) {
        super.validateConstructor(errors)

        if (errors.isEmpty()) {
            flickerDecorator?.doValidateConstructor()?.let { errors.addAll(it) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun validateInstanceMethods(errors: MutableList<Throwable>?) {
        flickerDecorator?.doValidateInstanceMethods()?.let { errors?.addAll(it) }
    }

    /** IFlickerJunitDecorator implementation */
    override fun getTestMethods(test: Any): List<FrameworkMethod> {
        val tests = mutableListOf<FrameworkMethod>()
        tests.addAll(super.computeTestMethods())
        return tests
    }

    override fun getChildDescription(method: FrameworkMethod?): Description? {
        return super.describeChild(method)
    }

    override fun doValidateInstanceMethods(): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        super.validateInstanceMethods(errors)
        return errors
    }

    override fun doValidateConstructor(): List<Throwable> {
        val result = mutableListOf<Throwable>()
        super.validateConstructor(result)
        return result
    }

    override fun getMethodInvoker(method: FrameworkMethod, test: Any): Statement {
        return super.methodInvoker(method, test)
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
