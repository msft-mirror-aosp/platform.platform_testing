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
package android.platform.test.rule

import android.os.Build
import android.platform.test.rule.DeviceProduct.CF_PHONE
import android.platform.test.rule.DeviceProduct.CF_TABLET
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Limits the test to run on devices specified by [allowed], */
@Retention(RUNTIME)
@Target(FUNCTION, CLASS)
annotation class AllowedDevices(vararg val allowed: DeviceProduct)

/** Does not run the test on device specified by [denied], */
@Retention(RUNTIME)
@Target(FUNCTION, CLASS)
annotation class DeniedDevices(vararg val denied: DeviceProduct)

/** Limits the test on default screenshot devices, or [allowed] devices if specified. */
@Retention(RUNTIME)
@Target(FUNCTION, CLASS)
annotation class ScreenshotTestDevices(vararg val allowed: DeviceProduct = [CF_PHONE, CF_TABLET])

/**
 * Only runs the test on [flakyProducts] if this configuration is running flaky tests (see
 * runningFlakyTests parameter on [LimitDevicesRule] constructor Runs it normally on all other
 * devices.
 */
@Retention(RUNTIME)
@Target(FUNCTION, CLASS)
annotation class FlakyDevices(vararg val flaky: DeviceProduct)

/**
 * Ignore LimitDevicesRule constraints when [ignoreLimit] is true. Main use case is to allow local
 * builds to bypass [LimitDevicesRule] and be able to run on any devices.
 */
@Retention(RUNTIME) @Target(FUNCTION, CLASS) annotation class IgnoreLimit(val ignoreLimit: Boolean)

/**
 * Limits a test to run specified devices.
 *
 * Devices are specified by [AllowedDevices], [DeniedDevices], [ScreenshotTestDevices], and
 * [FlakyDevices] annotations. Only one annotation on class or one per test is supported. Values are
 * matched against [thisDevice].
 *
 * To read the instrumentation args to determine whether to run on [FlakyDevices] (recommended), use
 * [readParamsFromInstrumentation] to construct the rule
 *
 * NOTE: It's not encouraged to use this to filter if it's possible to filter based on other device
 * characteristics. For example, to run a test only only on large screens or foldable,
 * [DeviceTypeRule] is encouraged. This rule should **never** be used to avoid running a test on a
 * tablet when the test is broken.
 */
class LimitDevicesRule(
    private val thisDevice: String = Build.PRODUCT,
    private val runningFlakyTests: Boolean = false
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        if (description.ignoreLimit()) {
            return base
        }

        val limitDevicesAnnotations = description.limitDevicesAnnotation()
        if (limitDevicesAnnotations.count() > 1) {
            return makeAssumptionViolatedStatement(
                "Only one LimitDeviceRule annotation is supported. Found $limitDevicesAnnotations"
            )
        }
        val deniedDevices = description.deniedDevices()
        if (thisDevice in deniedDevices) {
            return makeAssumptionViolatedStatement(
                "Skipping test as $thisDevice is in $deniedDevices"
            )
        }

        val flakyDevices = description.flakyDevices()
        if (thisDevice in flakyDevices) {
            if (!runningFlakyTests) {
                return makeAssumptionViolatedStatement(
                    "Skipping test as $thisDevice is flaky and this config excludes fakes"
                )
            }
        }

        val allowedDevices = description.allowedDevices()
        if (allowedDevices.isEmpty() || thisDevice in allowedDevices) {
            return base
        }
        return makeAssumptionViolatedStatement(
            "Skipping test as $thisDevice in not in $allowedDevices"
        )
    }

    private fun Description.allowedDevices(): List<String> =
        listOf(
                getMostSpecificAnnotation<AllowedDevices>()?.allowed,
                getMostSpecificAnnotation<ScreenshotTestDevices>()?.allowed
            )
            .collectProducts()

    private fun Description.deniedDevices(): List<String> =
        listOf(getMostSpecificAnnotation<DeniedDevices>()?.denied).collectProducts()

    private fun Description.flakyDevices(): List<String> =
        listOf(getMostSpecificAnnotation<FlakyDevices>()?.flaky).collectProducts()

    private fun Description.limitDevicesAnnotation(): Set<Annotation> =
        listOfNotNull(
                getMostSpecificAnnotation<AllowedDevices>(),
                getMostSpecificAnnotation<DeniedDevices>(),
                getMostSpecificAnnotation<ScreenshotTestDevices>(),
                getMostSpecificAnnotation<FlakyDevices>()
            )
            .toSet()

    private fun Description.ignoreLimit(): Boolean =
        getAnnotation(IgnoreLimit::class.java)?.ignoreLimit == true ||
            testClass?.getClassAnnotation<IgnoreLimit>()?.ignoreLimit == true

    private inline fun <reified T : Annotation> Description.getMostSpecificAnnotation(): T? {
        getAnnotation(T::class.java)?.let {
            return it
        }
        return testClass?.getClassAnnotation<T>()
    }

    private inline fun <reified T : Annotation> Class<*>.getClassAnnotation() =
        getLowestAncestorClassAnnotation(this, T::class.java)

    private fun List<Array<out DeviceProduct>?>.collectProducts() =
        filterNotNull().flatMap { it.toList() }.map { it.product }

    companion object {
        private fun isRunningFlakyTests(): Boolean {
            val args = InstrumentationRegistry.getArguments()
            val isRunning = args.getString(RUNNING_FLAKY_TESTS_KEY, "false").toBoolean()
            if (isRunning) {
                Log.d(TAG, "Running on flaky devices, due to $RUNNING_FLAKY_TESTS_KEY param.")
            }
            return isRunning
        }

        fun readParamsFromInstrumentation(thisDevice: String = Build.PRODUCT) =
            LimitDevicesRule(thisDevice, isRunningFlakyTests())

        private const val RUNNING_FLAKY_TESTS_KEY = "running-flaky-tests"
        private const val TAG = "LimitDevicesRule"
    }
}

enum class DeviceProduct(val product: String) {
    CF_PHONE("cf_x86_64_phone"),
    CF_TABLET("cf_x86_64_tablet"),
    CF_FOLDABLE("cf_x86_64_foldable"),
    CF_AUTO("cf_x86_64_auto"),
    TANGORPRO("tangorpro"),
    FELIX("felix"),
    ROBOLECTRIC("robolectric"),
}

private fun makeAssumptionViolatedStatement(errorMessage: String): Statement =
    object : Statement() {
        override fun evaluate() {
            throw AssumptionViolatedException(errorMessage)
        }
    }
