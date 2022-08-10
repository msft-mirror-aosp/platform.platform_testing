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
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Rule to prepare the environment for systemui screenshot tests */
public class LimitDevicesRule implements TestRule {
    private final List<String> mDevices;

    /** Device rule allows all devices except those defined in #LimitDevices */
    public LimitDevicesRule() {
        mDevices = Collections.emptyList();
    }

    /** @param devices Default list of devices used if the annotation is empty */
    public LimitDevicesRule(List<String> devices) {
        mDevices = devices;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        LimitDevices devices = description.getAnnotation(LimitDevices.class);
        if (devices != null) {
            if (devices.value().length > 0) {
                List<String> specifiedDevices = Arrays.asList(devices.value());
                if (specifiedDevices.contains(Build.PRODUCT)) return base;
                else return makeErrorStatement(specifiedDevices);
            }

            // if devices not specified, use default
            if (mDevices.size() == 0 || mDevices.contains(Build.PRODUCT)) {
                return base;
            }
            return makeErrorStatement(mDevices);
        }
        return base;
    }

    private Statement makeErrorStatement(List<String> devices) {
        return new Statement() {
            @Override
            public void evaluate() {
                throw new AssumptionViolatedException(
                        "Skipping the test on target "
                                + Build.PRODUCT
                                + " which in not in "
                                + devices);
            }
        };
    }

    /**
     * Takes a list of device names to run screenshot tests on The absence of this annotation
     * results in the test rule executing for all devices
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface LimitDevices {
        String[] value() default {};
    }
}
