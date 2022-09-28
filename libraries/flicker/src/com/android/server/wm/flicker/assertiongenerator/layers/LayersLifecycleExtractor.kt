package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.common.ILifecycleExtractor
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.Layer

class LayersLifecycleExtractor(
) : ILifecycleExtractor {
    /**
     * @{inheritDoc}
     */
    override fun extract(
        traceDump: DeviceTraceDump
    ): ITraceLifecycle? {
        val elementLifecycles = LayersTraceLifecycle()
        val trace = traceDump.layersTrace ?: return null
        val traceLength = trace.entries.size
        for ((index, entry) in trace.entries.withIndex()) {
            for (layer in entry.flattenedLayers) {
                val componentMatcher = Utils.componentNameMatcherFromName(layer.name)
                var layersElementLifecycleWasInitialized = false
                componentMatcher?.run {
                    elementLifecycles[componentMatcher]?.let { it ->
                        it[layer.id]?.let {
                            it.states[index] = layer
                            layersElementLifecycleWasInitialized = true
                        }
                    }
                    if (!layersElementLifecycleWasInitialized) {
                        val statesArray: Array<Layer?> = arrayOfNulls(traceLength)
                        statesArray[index] = layer
                        val layersElementLifecycle = LayersElementLifecycle(
                            statesArray.toMutableList()
                        )
                        elementLifecycles.add(componentMatcher, layer.id, layersElementLifecycle)
                    }
                }
            }
        }
        return elementLifecycles
    }
}
