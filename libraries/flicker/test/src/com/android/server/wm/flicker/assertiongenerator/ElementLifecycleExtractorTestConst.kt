package com.android.server.wm.flicker.assertiongenerator

import com.android.server.wm.flicker.assertiongenerator.layers.LayersComponentLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersElementLifecycle
import com.android.server.wm.traces.common.ActiveBuffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.Matrix33
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.HwcCompositionType
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntryBuilder
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.region.Region

class ElementLifecycleExtractorTestConst {
    companion object{
        val layer_id1_t1 = createTestLayer("NavigationBar0", 5, 1, 0)
        val layer_id1_t2 = createTestLayer("NavigationBar0", 6, 1, 0)

        val layer_id2_t1 = createTestLayer("StatusBar", 7, 2, 0)
        val layer_id2_t2 = createTestLayerWithEmptyRegion(
            "StatusBar",
            0,
            2,
            0)
        val layer_id2_t3 = createTestLayer("StatusBar", 6, 2, 0)

        val layer_id3_t2 = createTestLayer("navBarChild", 7, 3, 1)

        val layer_id4_t1 = createTestLayerWithEmptyRegion(
            "com.google.android.apps.nexuslauncher/" +
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity#1039",
            4,
            4,
            0)
        val layer_id4_t2 = createTestLayer(
            "com.google.android.apps.nexuslauncher/" +
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity#1039",
            5,
            4,
            0)
        val layer_id4_t3 = createTestLayer(
            "com.google.android.apps.nexuslauncher/" +
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity#1039",
            7,
            4,
            0)

        val layer_id5_sameComponentMatcher = createTestLayerWithEmptyRegion(
            "StatusBar",
            1,
            5,
            0)

        val layer_id6_t1 = null
        val layer_id6_t2 = createTestLayerWithEmptyRegion(
            "openPackage/openApp",
            6,
            6,
            0)
        val layer_id6_t3 = createTestLayer(
            "openPackage/openApp",
            8,
            6,
            0)

        val mapOfFlattenedLayers: Map<Int, Array<Layer>> = mapOf(
                1 to arrayOf(layer_id1_t1, layer_id2_t1),
                2 to arrayOf(layer_id1_t2, layer_id2_t2, layer_id3_t2),
                3 to arrayOf(layer_id2_t3)
        )

        val mapOfFlattenedLayersAssertionProducer: Map<Int, Array<Layer>> = mapOf(
            1 to arrayOf(layer_id1_t1, layer_id2_t1, layer_id4_t1),
            2 to arrayOf(layer_id1_t2, layer_id2_t2, layer_id3_t2, layer_id4_t2),
            3 to arrayOf(layer_id2_t3, layer_id4_t3),
        )

        val mapOfFlattenedLayersAllVisibilityAssertions: Map<Int, Array<Layer>> = mapOf(
            1 to arrayOf(),
            2 to arrayOf(layer_id2_t1),
            3 to arrayOf(layer_id2_t2),
        )

        val mapOfFlattenedLayers_SameComponentMatcher: Map<Int, Array<Layer>> = mapOf(
            1 to arrayOf(layer_id2_t1, layer_id5_sameComponentMatcher),
            2 to arrayOf(layer_id2_t2, layer_id5_sameComponentMatcher),
            3 to arrayOf(layer_id2_t3, layer_id5_sameComponentMatcher),
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

        val expectedElementLifecycle_id4 = LayersElementLifecycle(
            mutableListOf(layer_id4_t1, layer_id4_t2, layer_id4_t3)
        )

        val expectedElementLifecycle_id6 = LayersElementLifecycle(
            mutableListOf(layer_id6_t1, layer_id6_t2, layer_id6_t3)
        )

        val expectedElementLifecycle_id5_sameComponentMatcher = LayersElementLifecycle(
            mutableListOf(
                layer_id5_sameComponentMatcher,
                layer_id5_sameComponentMatcher,
                layer_id5_sameComponentMatcher
            )
        )

        val expectedComponentMatcherLifecycle_Navbar = LayersComponentLifecycle(
            mutableMapOf(1 to expectedElementLifecycle_id1)
        )

        val expectedComponentMatcherLifecycle_Statusbar = LayersComponentLifecycle(
            mutableMapOf(2 to expectedElementLifecycle_id2)
        )

        val expectedComponentMatcherLifecycle_Launcher = LayersComponentLifecycle(
            mutableMapOf(4 to expectedElementLifecycle_id4)
        )

        val expectedComponentMatcherLifecycle_OpenApp = LayersComponentLifecycle(
            mutableMapOf(6 to expectedElementLifecycle_id6)
        )

        val expectedComponentMatcherLifecycle_Statusbar_sameComponentMatcher =
            LayersComponentLifecycle(
                mutableMapOf(
                    2 to expectedElementLifecycle_id2,
                    5 to expectedElementLifecycle_id5_sameComponentMatcher
                )
            )

        val expectedElementLifecycles = mapOf(
            ComponentNameMatcher.NAV_BAR to expectedComponentMatcherLifecycle_Navbar,
            ComponentNameMatcher.STATUS_BAR to expectedComponentMatcherLifecycle_Statusbar,
            // ComponentNameMatcher.LAUNCHER to expectedComponentMatcherLifecycle_Launcher
        )

        val expectedElementLifecycles_SameComponentMatcher = mapOf(
            ComponentNameMatcher.STATUS_BAR to
                expectedComponentMatcherLifecycle_Statusbar_sameComponentMatcher
        )

        val expectedElementLifecycles_OpenApp = mapOf(
            ComponentNameMatcher("openPackage", "openApp") to
                expectedComponentMatcherLifecycle_OpenApp
        )

        val expectedElementLifecycle_AllVisibilityAssertions_id2 = LayersElementLifecycle(
            mutableListOf(null, layer_id2_t1, layer_id2_t2)
        )

        val expectedFailElementLifecycle1_AllVisibilityAssertions_id2 = LayersElementLifecycle(
            mutableListOf(layer_id2_t1, layer_id2_t2)
        )

        val mapOfFlattenedLayersAllVisibilityAssertions_fail1: Map<Int, Array<Layer>> = mapOf(
            1 to arrayOf(layer_id2_t1),
            2 to arrayOf(layer_id2_t2),
        )

        val expectedFailElementLifecycle2_AllVisibilityAssertions_id2 = LayersElementLifecycle(
            mutableListOf(null, layer_id2_t1)
        )

        // Passed                : notContains(StatusBar)
        // Assertion never failed: isVisible(StatusBar)
        // Untested              : isInvisible(StatusBar)
        // Trace                 : 1/3:[notContains(StatusBar)]	Entry: 1/3 LayerTraceEntrySubject(0d0h0m0s0ms (timestamp=1))
        // Trace                 : 1/3:[notContains(StatusBar)]	Entry: 2/3 LayerTraceEntrySubject(0d0h0m0s0ms (timestamp=2))
        // Trace                 : 2/3:[isVisible(StatusBar)]	Entry: 2/3 LayerTraceEntrySubject(0d0h0m0s0ms (timestamp=2))
        // Trace                 : 2/3:[isVisible(StatusBar)]	Entry: 3/3 LayerTraceEntrySubject(0d0h0m0s0ms (timestamp=3))
        val mapOfFlattenedLayersAllVisibilityAssertions_fail2: Map<Int, Array<Layer>> = mapOf(
            1 to arrayOf(),
            2 to arrayOf(layer_id2_t1),
            3 to arrayOf(layer_id2_t1)
        )

        val expectedFailElementLifecycle3_AllVisibilityAssertions_id2 = LayersElementLifecycle(
            mutableListOf(null)
        )

        // Assertion never failed: notContains(StatusBar)
        // Untested              : isVisible(StatusBar)
        // Untested              : isInvisible(StatusBar)
        // Trace                 : 1/3:[notContains(StatusBar)]	Entry: 1/3 LayerTraceEntrySubject(0d0h0m0s0ms (timestamp=1))
        // Trace                 : 1/3:[notContains(StatusBar)]	Entry: 2/3 LayerTraceEntrySubject(0d0h0m0s0ms (timestamp=2))
        // Trace                 : 1/3:[notContains(StatusBar)]	Entry: 3/3 LayerTraceEntrySubject(0d0h0m0s0ms (timestamp=3))
        val mapOfFlattenedLayersAllVisibilityAssertions_fail3: Map<Int, Array<Layer>> = mapOf(
            1 to arrayOf(),
            2 to arrayOf(),
            3 to arrayOf()
        )

        val expectedFailElementLifecycle4_AllVisibilityAssertions_id2 = LayersElementLifecycle(
            mutableListOf(layer_id2_t1)
        )

        // Assertion   : isVisible(StatusBar)
        // Is Invisible: Crop is 0x0
        // Is Invisible: Bounds is 0x0
        // Is Invisible: Transform is invalid
        // Is Invisible: Visible region calculated by Composition Engine is empty
        val mapOfFlattenedLayersAllVisibilityAssertions_fail4: Map<Int, Array<Layer>> = mapOf(
            1 to arrayOf(),
            2 to arrayOf(layer_id2_t2),
            3 to arrayOf()
        )

        val expectedElementLifecycles_deprecated = mapOf(
            1 to expectedElementLifecycle_id1,
            2 to expectedElementLifecycle_id2,
            3 to expectedElementLifecycle_id3
        )

        val expectedElementLifecyclesVisibilityAssertionProducer_deprecated = mapOf(
            1 to expectedElementLifecycle_id1,
            2 to expectedElementLifecycle_id2,
            3 to expectedElementLifecycle_id3,
            4 to expectedElementLifecycle_id4
        )

        val expectedElementLifecyclesVisibilityAssertionProducer = mapOf(
            ComponentNameMatcher.NAV_BAR to expectedComponentMatcherLifecycle_Navbar,
            ComponentNameMatcher.STATUS_BAR to expectedComponentMatcherLifecycle_Statusbar,
            ComponentNameMatcher.LAUNCHER to expectedComponentMatcherLifecycle_Launcher
        )

        val expectedComponentMatcherLifecycle_AllVisibilityAssertions =
            LayersComponentLifecycle(
                mutableMapOf(2 to expectedElementLifecycle_AllVisibilityAssertions_id2)
            )

        val expectedElementLifecyclesAllVisibilityAssertions = mapOf(
            ComponentNameMatcher.STATUS_BAR to
                expectedComponentMatcherLifecycle_AllVisibilityAssertions,
        )

        private fun createTraceEntries(): Array<BaseLayerTraceEntry> {
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

        private fun createTraceEntries_arg(
            mapOfFlattenedLayers: Map<Int, Array<Layer>>
        ): Array<BaseLayerTraceEntry> {
            var traceEntries = arrayOf<BaseLayerTraceEntry>()
            for ((timestamp, flattenedLayers) in mapOfFlattenedLayers) {
                val layerTraceEntryBuilder = LayerTraceEntryBuilder(
                    timestamp,
                    flattenedLayers,
                    arrayOf(),
                    timestamp.toLong()
                )
                val layerTraceEntry = layerTraceEntryBuilder.build()
                traceEntries += layerTraceEntry
            }
            return traceEntries
        }

        fun createTrace_arg(mapOfFlattenedLayers: Map<Int, Array<Layer>>): LayersTrace {
            val traceEntries: Array<BaseLayerTraceEntry> = createTraceEntries_arg(
                mapOfFlattenedLayers
            )
            return LayersTrace(traceEntries)
        }

        private fun createTestLayerWithEmptyRegion(
            name: String,
            index: Int,
            id: Int,
            parentId: Int
        ): Layer {
            val rect: RectF = RectF.from(
                left = 0.toFloat(),
                top = 0.toFloat(),
                right = 0.toFloat(),
                bottom = 0.toFloat()
            )
            return createTestLayer(name, index, id, parentId, rect)
        }

        // copy of WindowManagerStateHelperTest::createImaginaryLayer,
        // but it is private, so couldn't use it
        private fun createTestLayer(
            name: String,
            index: Int,
            id: Int,
            parentId: Int,
            rect: RectF = RectF.from(
                left = index.toFloat(),
                top = index.toFloat(),
                right = index.toFloat() + 1,
                bottom = index.toFloat() + 1
            )
        ): Layer {
            val transform = Transform.from(0, Matrix33.EMPTY)
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
                    hwcCompositionType = HwcCompositionType.INVALID,
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
