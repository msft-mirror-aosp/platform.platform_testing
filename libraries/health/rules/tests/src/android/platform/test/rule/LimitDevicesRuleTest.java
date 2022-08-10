/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.platform.test.rule;

import android.os.Build;

import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

public class LimitDevicesRuleTest {
    private Statement mStatement =
            new Statement() {
                @Override
                public void evaluate() throws Throwable {}
            };
    private Description mDescription = Description.createSuiteDescription(this.getClass());

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testApplyDeviceExist() throws Throwable {
        LimitDevicesRule rule = new LimitDevicesRule(Arrays.asList(Build.PRODUCT));
        rule.apply(mStatement, mDescription).evaluate();
    }

    @Test
    public void testEmptyList() throws Throwable {
        LimitDevicesRule rule = new LimitDevicesRule(Collections.emptyList());
        rule.apply(mStatement, mDescription).evaluate();
    }

    @Test(expected = AssumptionViolatedException.class)
    public void testDeviceNotExist() throws Throwable {
        LimitDevicesRule rule = new LimitDevicesRule(Arrays.asList("fake_device_name"));
        mDescription =
                Description.createSuiteDescription(
                        this.getClass(), FakeDeviceNoList.class.getAnnotations());
        rule.apply(mStatement, mDescription).evaluate();
    }

    @Test(expected = AssumptionViolatedException.class)
    public void testDeviceExistInAnnotation() throws Throwable {
        LimitDevicesRule rule = new LimitDevicesRule();

        mDescription =
                Description.createSuiteDescription(
                        this.getClass(), FakeDeviceWithList.class.getAnnotations());
        rule.apply(mStatement, mDescription).evaluate();
    }

    @LimitDevicesRule.LimitDevices("fake_device_name")
    private class FakeDeviceWithList {}

    @LimitDevicesRule.LimitDevices
    private class FakeDeviceNoList {}
}
