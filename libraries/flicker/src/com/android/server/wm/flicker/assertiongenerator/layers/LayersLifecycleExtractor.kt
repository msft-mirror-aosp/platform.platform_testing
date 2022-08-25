package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.ILifecycleExtractor
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.Layer

class LayersLifecycleExtractor(
) : ILifecycleExtractor {
    /**
     * @{inheritDoc}
     */
    override fun extract(traceDump: DeviceTraceDump): LayersTraceLifecycle? {
        var elementLifecycles: LayersTraceLifecycle = LayersTraceLifecycle()
        val trace = traceDump.layersTrace ?: return null
        for ((index, entry) in trace.entries.withIndex()) {
            for (layer in entry.flattenedLayers){
                elementLifecycles[layer.id]?.states?.add(layer)
                ?: run {
                    var statesArray: Array<Layer?> = arrayOfNulls(index)
                    statesArray += layer
                    val layersElementLifecycle = LayersElementLifecycle(statesArray.toMutableList())
                    elementLifecycles[layer.id] = layersElementLifecycle
                }
            }
        }
        return elementLifecycles
    }
}
