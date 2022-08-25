package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.layers.LayersElementLifecycle
import com.android.server.wm.traces.common.ActiveBuffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.Matrix33
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntryBuilder
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.region.Region

class ElementLifecycleExtractorTestConst {
    companion object{
        val layer_id1_t1 = createTestLayer("navBar", 5, 1, 0)
        val layer_id1_t2 = createTestLayer("navBar", 6, 1, 0)
        // val layer_id1_t3 = createTestLayer("navBar", 7, 1, 0)

        val layer_id2_t1 = createTestLayer("statusBar", 7, 2, 0)
        val layer_id2_t2 = createTestLayer("statusBar", 5, 2, 0)
        val layer_id2_t3 = createTestLayer("statusBar", 6, 2, 0)

        // val layer_id3_t1 = createTestLayer("navBarChild", 6, 3, 1)
        val layer_id3_t2 = createTestLayer("navBarChild", 7, 3, 1)
        // val layer_id3_t3 = createTestLayer("navBarChild", 8, 3, 1)
        // this is removed by the builder because it's orphan

        val mapOfFlattenedLayers: Map<Int, Array<Layer>> = mapOf<Int, Array<Layer>>(
                1 to arrayOf(layer_id1_t1, layer_id2_t1),
                2 to arrayOf(layer_id1_t2, layer_id2_t2, layer_id3_t2),
                3 to arrayOf(layer_id2_t3)
        )

        val expectedElementLifecycle_id1 = LayersElementLifecycle(
            mutableListOf(layer_id1_t1, layer_id1_t2, null)
        )

        val expectedElementLifecycle_id2 = LayersElementLifecycle(
            mutableListOf(layer_id2_t1, layer_id2_t2, layer_id2_t3)
        )

        val expectedElementLifecycle_id3 = LayersElementLifecycle(
            mutableListOf(null, layer_id3_t2, null)
        )

        val expectedElementLifecycles = mapOf(
            1 to expectedElementLifecycle_id1,
            2 to expectedElementLifecycle_id2,
            3 to expectedElementLifecycle_id3
        )

        val expectedElementLifecycleAfterMapInit_id1 = LayersElementLifecycle(
            mutableListOf(layer_id1_t1)
        )

        val expectedElementLifecycleAfterMapInit_id2 = LayersElementLifecycle(
            mutableListOf(layer_id2_t1)
        )

        val expectedLifecyclesAfterInitializeElementLifecyclesMap = mapOf(
            1 to expectedElementLifecycleAfterMapInit_id1,
            2 to expectedElementLifecycleAfterMapInit_id2
        )

        fun createTraceEntries(): Array<BaseLayerTraceEntry> {
            var traceEntries = arrayOf<BaseLayerTraceEntry>()
            for ((timestamp, flattenedLayers) in mapOfFlattenedLayers) {
                val layerTraceEntryBuilder = LayerTraceEntryBuilder(
                        timestamp,
                        flattenedLayers,
                        arrayOf(),
                        1
                )
                val layerTraceEntry = layerTraceEntryBuilder.build()
                traceEntries += layerTraceEntry
            }
            return traceEntries
        }

        fun createTrace(): LayersTrace {
            val traceEntries: Array<BaseLayerTraceEntry> = createTraceEntries()
            return LayersTrace(traceEntries)
        }

        // copy of WindowManagerStateHelperTest::createImaginaryLayer,
        // but it is private, so couldn't use it
        private fun createTestLayer(name: String, index: Int, id: Int, parentId: Int): Layer {
            val transform = Transform.from(0, Matrix33.EMPTY)
            val rect = RectF.from(
                    left = index.toFloat(),
                    top = index.toFloat(),
                    right = index.toFloat() + 1,
                    bottom = index.toFloat() + 1
            )
            return Layer.from(
                    name,
                    id,
                    parentId,
                    z = 0,
                    visibleRegion = Region.from(rect.toRect()),
                    activeBuffer = ActiveBuffer.from(1, 1, 1, 1),
                    flags = 0,
                    bounds = rect,
                    color = Color.DEFAULT,
                    isOpaque = true,
                    shadowRadius = 0f,
                    cornerRadius = 0f,
                    type = "",
                    screenBounds = rect,
                    transform = transform,
                    sourceBounds = rect,
                    currFrame = 0,
                    effectiveScalingMode = 0,
                    bufferTransform = transform,
                    hwcCompositionType = 0,
                    hwcCrop = RectF.EMPTY,
                    hwcFrame = Rect.EMPTY,
                    crop = rect.toRect(),
                    backgroundBlurRadius = 0,
                    isRelativeOf = false,
                    zOrderRelativeOfId = -1,
                    stackId = 0,
                    requestedTransform = transform,
                    requestedColor = Color.DEFAULT,
                    cornerRadiusCrop = RectF.EMPTY,
                    inputTransform = transform,
                    inputRegion = Region.from(rect.toRect())
            )
        }
    }
}
