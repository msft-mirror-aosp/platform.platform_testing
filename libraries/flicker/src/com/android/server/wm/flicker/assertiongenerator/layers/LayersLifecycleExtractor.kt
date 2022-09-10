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
        val elementLifecycles = LayersTraceLifecycle()
        val trace = traceDump.layersTrace ?: return null
        for ((index, entry) in trace.entries.withIndex()) {
            for (layer in entry.flattenedLayers){
                elementLifecycles[layer.id]?.let {it.states[index] = layer }
                    ?: run {
                        val statesArray: Array<Layer?> = arrayOfNulls(trace.entries.size)
                        statesArray[index] = layer
                        val layersElementLifecycle = LayersElementLifecycle(
                            statesArray.toMutableList()
                        )
                        elementLifecycles[layer.id] = layersElementLifecycle
                    }
            }
        }
        return elementLifecycles
    }
}
