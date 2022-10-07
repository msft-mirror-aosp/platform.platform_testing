package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.AssertionGenConfigTestConst.Companion.emptyDeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.layers.LayersComponentLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceLifecycle
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.Layer
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Contains [LayersLifecycleExtractor] tests.
 *
 * To run this test: `atest FlickerLibTest:LayersLifecycleExtractorTest`
 */
@RunWith(Parameterized::class)
class LayersLifecycleExtractorTest(
    val layersTraceFlattenedLayers: Map<Int, Array<Layer>>,
    val expectedElementLifecyclesMap: MutableMap<ComponentNameMatcher, LayersComponentLifecycle>,
) {
    @Test
    fun extract() {
        val layersTrace =
            ElementLifecycleExtractorTestConst.createTrace_arg(layersTraceFlattenedLayers)
        val traceDump = DeviceTraceDump(null, layersTrace)
        val layersLifecycleExtractor = LayersLifecycleExtractor()
        val elementLifecycles =
            layersLifecycleExtractor.extract(traceDump, emptyDeviceTraceConfiguration)
        val expectedElementLifecycles = LayersTraceLifecycle(expectedElementLifecyclesMap)
        elementLifecycles?.run {
            Truth.assertThat(elementLifecycles).isEqualTo(expectedElementLifecycles)
        }
            ?: throw RuntimeException("Element lifecycles unexpectedly null")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(
                    ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAssertionProducer,
                    ElementLifecycleExtractorTestConst
                        .expectedElementLifecyclesVisibilityAssertionProducer
                        as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>
                )
            )
        }
    }
}
