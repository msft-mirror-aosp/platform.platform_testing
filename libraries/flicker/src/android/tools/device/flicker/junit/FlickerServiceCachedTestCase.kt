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

import android.tools.common.Cache
import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.IFlickerService
import android.tools.common.flicker.assertors.IFaasAssertion
import android.tools.device.flicker.IFlickerServiceResultsCollector
import java.lang.reflect.Method
import org.junit.Assume
import org.junit.runner.Description
import org.junit.runner.notification.Failure

class FlickerServiceCachedTestCase(
    private val assertion: IFaasAssertion,
    private val flickerService: IFlickerService,
    method: Method,
    private val onlyBlocking: Boolean,
    private val metricsCollector: IFlickerServiceResultsCollector?,
    private val isLast: Boolean,
    injectedBy: IFlickerJUnitDecorator,
    paramString: String = "",
) : InjectedTestCase(method, "FaaS_${assertion.name}$paramString", injectedBy) {
    override fun execute(description: Description) {
        val result = flickerService.executeAssertion(assertion)

        if (isLast) {
            metricsCollector?.testStarted(description)
        }
        try {
            Assume.assumeTrue(
                !onlyBlocking ||
                    result.assertion.stabilityGroup == AssertionInvocationGroup.BLOCKING
            )
            result.assertionError?.let { throw it }
        } catch (e: Throwable) {
            metricsCollector?.testFailure(Failure(description, e))
            throw e
        } finally {
            if (isLast) {
                Cache.clear()
                metricsCollector?.testFinished(description)
            }
        }
    }
}
