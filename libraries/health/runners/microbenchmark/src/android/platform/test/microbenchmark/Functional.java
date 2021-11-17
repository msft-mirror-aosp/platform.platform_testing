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
package android.platform.test.microbenchmark;

import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.util.List;

/**
 * Runner for functional tests that's compatible with annotations used in microbenchmark
 * tests. @Before/@After are nested inside @NoMetricBefore/@NoMetricAfter. TODO(b/205019000): this
 * class is seen as a temporary solution, and is supposed to be eventually replaced with a permanent
 * one.
 */
public class Functional extends BlockJUnit4ClassRunner {
    public Functional(Class<?> klass) throws InitializationError {
        super(new TestClass(klass));
    }

    @Override
    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        statement = super.withBefores(method, target, statement);

        // Add @NoMetricBefore's
        List<FrameworkMethod> befores =
                getTestClass().getAnnotatedMethods(Microbenchmark.NoMetricBefore.class);
        return befores.isEmpty() ? statement : new RunBefores(statement, befores, target);
    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        // Add @NoMetricAfter's
        List<FrameworkMethod> afters =
                getTestClass().getAnnotatedMethods(Microbenchmark.NoMetricAfter.class);
        statement = afters.isEmpty() ? statement : new RunAfters(statement, afters, target);

        return super.withAfters(method, target, statement);
    }
}
