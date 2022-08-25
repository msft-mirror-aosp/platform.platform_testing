package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.IElementLifecycle
import com.android.server.wm.traces.common.layers.Layer

class LayersElementLifecycle(
    override val states: MutableList<Layer?>
) : IElementLifecycle
