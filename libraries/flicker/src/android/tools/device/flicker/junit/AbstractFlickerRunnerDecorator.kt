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

import android.app.Instrumentation
import android.tools.common.CrossPlatform
import android.tools.common.FLICKER_TAG
import android.tools.common.Scenario
import android.tools.device.flicker.datastore.DataStore
import android.tools.device.flicker.legacy.FlickerBuilder
import android.tools.device.flicker.legacy.FlickerTest
import android.tools.device.flicker.legacy.runner.TransitionRunner
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.reflect.Modifier
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass

abstract class AbstractFlickerRunnerDecorator(
    protected val testClass: TestClass,
    protected val scenario: Scenario?,
    protected val inner: IFlickerJUnitDecorator?
) : IFlickerJUnitDecorator {
    protected val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    final override fun doValidateConstructor(): List<Throwable> {
        val errors = internalDoValidateConstructor().toMutableList()
        if (errors.isEmpty()) {
            inner?.doValidateConstructor()?.let { errors.addAll(it) }
        }
        return errors
    }

    final override fun doValidateInstanceMethods(): List<Throwable> {
        val errors = internalDoValidateInstanceMethods().toMutableList()
        if (errors.isEmpty()) {
            inner?.doValidateInstanceMethods()?.let { errors.addAll(it) }
        }
        return errors
    }

    private fun internalDoValidateConstructor(): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        val ctor = testClass.javaClass.constructors.firstOrNull()
        if (ctor?.parameterTypes?.none { it == FlickerTest::class.java } != false) {
            errors.add(
                IllegalStateException(
                    "Constructor should have a parameter of type " +
                        FlickerTest::class.java.simpleName
                )
            )
        }
        return errors
    }

    /** Validate that the test has one method annotated with [FlickerBuilderProvider] */
    private fun internalDoValidateInstanceMethods(): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        val methods = getCandidateProviderMethods(testClass)

        if (methods.isEmpty() || methods.size > 1) {
            val prefix = if (methods.isEmpty()) "One" else "Only one"
            errors.add(
                IllegalArgumentException(
                    "$prefix object should be annotated with @FlickerBuilderProvider"
                )
            )
        } else {
            val method = methods.first()

            if (Modifier.isStatic(method.method.modifiers)) {
                errors.add(IllegalArgumentException("Method ${method.name}() should not be static"))
            }
            if (!Modifier.isPublic(method.method.modifiers)) {
                errors.add(IllegalArgumentException("Method ${method.name}() should be public"))
            }
            if (method.returnType != FlickerBuilder::class.java) {
                errors.add(
                    IllegalArgumentException(
                        "Method ${method.name}() should return a " +
                            "${FlickerBuilder::class.java.simpleName} object"
                    )
                )
            }
            if (method.method.parameterTypes.isNotEmpty()) {
                errors.add(
                    IllegalArgumentException("Method ${method.name} should have no parameters")
                )
            }
        }

        return errors
    }

    protected fun doRunTransition(test: Any, description: Description?) {
        CrossPlatform.log.d(FLICKER_TAG, "$scenario - Setting up FaaS")
        val scenario = scenario
        requireNotNull(scenario) { "Expected to have a scenario to run" }
        if (!DataStore.containsResult(scenario)) {
            CrossPlatform.log.v(FLICKER_TAG, "Creating flicker object for $scenario")
            val builder = getFlickerBuilder(test)
            CrossPlatform.log.v(FLICKER_TAG, "Creating flicker object for $scenario")
            val flicker = builder.build()
            val runner = TransitionRunner(scenario, instrumentation)
            CrossPlatform.log.v(FLICKER_TAG, "Running transition for $scenario")
            runner.execute(flicker, description)
        }
    }

    private val providerMethod: FrameworkMethod
        get() =
            getCandidateProviderMethods(testClass).firstOrNull()
                ?: error("Provider method not found")

    private fun getFlickerBuilder(test: Any): FlickerBuilder {
        CrossPlatform.log.v(FLICKER_TAG, "Obtaining flicker builder for $testClass")
        return providerMethod.invokeExplosively(test) as FlickerBuilder
    }

    companion object {
        private fun getCandidateProviderMethods(testClass: TestClass): List<FrameworkMethod> =
            testClass.getAnnotatedMethods(FlickerBuilderProvider::class.java) ?: emptyList()
    }
}
