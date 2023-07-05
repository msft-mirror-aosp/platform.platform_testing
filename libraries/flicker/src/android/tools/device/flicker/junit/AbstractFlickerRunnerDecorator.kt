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
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass

abstract class AbstractFlickerRunnerDecorator(
    protected val testClass: TestClass,
    protected val inner: IFlickerJUnitDecorator?
) : IFlickerJUnitDecorator {
    protected val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    override fun doValidateConstructor(): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        inner?.doValidateConstructor()?.let { errors.addAll(it) }
        return errors
    }

    override fun doValidateInstanceMethods(): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        inner?.doValidateInstanceMethods()?.let { errors.addAll(it) }
        return errors
    }

    override fun shouldRunBeforeOn(method: FrameworkMethod): Boolean {
        return inner?.shouldRunBeforeOn(method) ?: true
    }

    override fun shouldRunAfterOn(method: FrameworkMethod): Boolean {
        return inner?.shouldRunAfterOn(method) ?: true
    }
}
