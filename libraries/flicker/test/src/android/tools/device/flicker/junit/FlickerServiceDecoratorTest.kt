package android.tools.device.flicker.junit

import android.app.Instrumentation
import android.os.Bundle
import android.tools.common.flicker.FlickerConfig
import android.tools.common.flicker.FlickerService
import android.tools.common.flicker.annotation.ExpectedScenarios
import android.tools.common.flicker.config.FlickerServiceConfig
import android.tools.utils.KotlinMockito
import org.junit.Test
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import org.mockito.Mockito

class FlickerServiceDecoratorTest {
    @Test
    fun sendsInstrumentationUpdatesOWhenComputingTestMethods() {
        val instrumentation = Mockito.mock(Instrumentation::class.java)
        val testClass = Mockito.mock(TestClass::class.java)
        val innerDecorator = Mockito.mock(IFlickerJUnitDecorator::class.java)
        val method = Mockito.mock(FrameworkMethod::class.java)
        val flickerService = Mockito.mock(FlickerService::class.java)

        val flickerConfigProviderMethods = Mockito.mock(FrameworkMethod::class.java)

        Mockito.`when`(testClass.getAnnotatedMethods(ExpectedScenarios::class.java))
            .thenReturn(listOf(method))
        Mockito.`when`(
                testClass.getAnnotatedMethods(
                    android.tools.common.flicker.annotation.FlickerConfigProvider::class.java
                )
            )
            .thenReturn(listOf(flickerConfigProviderMethods))
        Mockito.`when`(flickerConfigProviderMethods.invokeExplosively(testClass))
            .thenReturn(FlickerConfig().use(FlickerServiceConfig.DEFAULT))
        Mockito.`when`(method.annotations).thenReturn(emptyArray())
        Mockito.`when`(innerDecorator.getTestMethods(KotlinMockito.any(Object::class.java)))
            .thenReturn(listOf(method))

        val test = Mockito.mock(Object::class.java)
        val decorator =
            FlickerServiceDecorator(
                testClass = testClass,
                paramString = null,
                skipNonBlocking = false,
                inner = innerDecorator,
                instrumentation = instrumentation,
                flickerService = flickerService
            )
        decorator.getTestMethods(test)

        Mockito.verify(instrumentation)
            .sendStatus(
                Mockito.anyInt(),
                KotlinMockito.argThat<Bundle> {
                    this.getString(Instrumentation.REPORT_KEY_STREAMRESULT)
                        ?.contains("Running setup")
                        ?: false
                }
            )
        Mockito.verify(instrumentation)
            .sendStatus(
                Mockito.anyInt(),
                KotlinMockito.argThat {
                    this.getString(Instrumentation.REPORT_KEY_STREAMRESULT)
                        ?.contains("Running transition")
                        ?: false
                }
            )
        Mockito.verify(instrumentation)
            .sendStatus(
                Mockito.anyInt(),
                KotlinMockito.argThat {
                    this.getString(Instrumentation.REPORT_KEY_STREAMRESULT)
                        ?.contains("Running teardown")
                        ?: false
                }
            )
    }
}