package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.common.LifecycleExtractorFactory
import com.android.server.wm.flicker.assertiongenerator.layers.LayersElementLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceLifecycle
import com.android.server.wm.traces.common.DeviceTraceDump
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
        assert(elementLifecycles == listOf(LayersTraceLifecycle(
            ElementLifecycleExtractorTestConst.expectedElementLifecycles
            as MutableMap<Int, LayersElementLifecycle>))
        )
    }
}
