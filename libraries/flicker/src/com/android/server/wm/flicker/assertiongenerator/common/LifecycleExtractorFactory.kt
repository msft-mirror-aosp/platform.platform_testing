package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.traces.common.DeviceTraceDump

class LifecycleExtractorFactory {
    val extractors: Set<ILifecycleExtractor> = setOf(
        LayersLifecycleExtractor()
    )

    fun extract(traceDumps: Array<DeviceTraceDump>): List<ITraceLifecycle> {
        return traceDumps.flatMap { traceDump ->
            extractors.map{
                extractor -> extractor.extract(traceDump)
            }
        }.filterNotNull()
    }
}
