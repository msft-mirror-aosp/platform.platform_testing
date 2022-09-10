package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.layers.LayersElementLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceLifecycle
import com.android.server.wm.traces.common.DeviceTraceDump
import com.google.common.truth.Truth
import org.junit.Test

/**
 * Contains [LayersLifecycleExtractor] tests.
 *
 * To run this test: `atest FlickerLibTest:LayersLifecycleExtractorTest`
 */
class LayersLifecycleExtractorTest {
    @Test
    fun extract() {
        val layersTrace = ElementLifecycleExtractorTestConst.createTrace()
        val traceDump = DeviceTraceDump(null, layersTrace)
        val layersLifecycleExtractor = LayersLifecycleExtractor()
        val elementLifecycles = layersLifecycleExtractor.extract(traceDump)
        Truth.assertThat(elementLifecycles!!.lifecycleMap).isEqualTo(
            LayersTraceLifecycle(
                ElementLifecycleExtractorTestConst.expectedElementLifecycles
                as MutableMap<Int, LayersElementLifecycle>
            ).lifecycleMap
        )
    }
}
