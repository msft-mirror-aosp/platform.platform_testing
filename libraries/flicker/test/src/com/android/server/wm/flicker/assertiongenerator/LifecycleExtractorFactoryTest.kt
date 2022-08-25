package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.common.LifecycleExtractorFactory
import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.LayersTrace

class LifecycleExtractorFactoryTest {
    private lateinit var traceDump: DeviceTraceDump
    var layersTrace: LayersTrace? = null
    lateinit var layersLifecycleExtractor: LayersLifecycleExtractor
    var lifecycleExtractorFactory: LifecycleExtractorFactory = LifecycleExtractorFactory()
}
