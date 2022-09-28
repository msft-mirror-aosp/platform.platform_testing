package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.common.LifecycleExtractorFactory
import com.android.server.wm.flicker.assertiongenerator.layers.LayersComponentLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceLifecycle
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.DeviceTraceDump
import com.google.common.truth.Truth
import org.junit.Test

/**
 * Contains [LayersLifecycleExtractor] tests.
 *
 * To run this test: `atest FlickerLibTest:LifecycleExtractorFactoryTest`
 */
class LifecycleExtractorFactoryTest {
    @Test
    fun extract(){
        val layersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayers
        )
        val layersTraceDump = DeviceTraceDump(null, layersTrace)
        val nullTraceDump = DeviceTraceDump(null, null)
        val traceDumps = arrayOf(layersTraceDump, nullTraceDump)
        val elementLifecycles = LifecycleExtractorFactory.extract(traceDumps)
        val expectedElementLifecycles = listOf(LayersTraceLifecycle(
            ElementLifecycleExtractorTestConst.expectedElementLifecycles
                as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>))
        try {
            Truth.assertThat(elementLifecycles == expectedElementLifecycles).isTrue()
        } catch (err: AssertionError) {
            throw RuntimeException(
                "Expected:\n$expectedElementLifecycles\nActual:\n$elementLifecycles"
            )
        }
    }

    @Test
    fun extract_sameComponentMatcher(){
        val layersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayers_SameComponentMatcher
        )
        val layersTraceDump = DeviceTraceDump(null, layersTrace)
        val nullTraceDump = DeviceTraceDump(null, null)
        val traceDumps = arrayOf(layersTraceDump, nullTraceDump)
        val elementLifecycles = LifecycleExtractorFactory.extract(traceDumps)
        val expectedElementLifecycles = listOf(LayersTraceLifecycle(
            ElementLifecycleExtractorTestConst.expectedElementLifecycles_SameComponentMatcher
                as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>))
        try {
            Truth.assertThat(elementLifecycles == expectedElementLifecycles).isTrue()
        } catch (err: AssertionError) {
            throw RuntimeException(
                "Expected:\n$expectedElementLifecycles\nActual:\n$elementLifecycles"
            )
        }
    }
}
