package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.IElementLifecycle
import com.android.server.wm.traces.common.layers.Layer

class LayersElementLifecycle(override val states: MutableList<Layer?>) : IElementLifecycle {

    override fun hashCode(): Int {
        return states.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LayersElementLifecycle) return false

        if (states != other.states) return false

        return true
    }

    override fun toString(): String {
        return states.toString()
    }
}
