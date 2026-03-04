/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.motion.compose

import android.annotation.SuppressLint
import android.graphics.HardwareRenderer
import android.os.Build
import android.util.Log
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.rules.RuleChain
import platform.test.motion.Defaults
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.RecordedMotion.Companion.create
import platform.test.motion.compose.ComposeToolkit.Companion.TAG
import platform.test.motion.compose.values.EnableMotionTestValueCollection
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.Feature
import platform.test.motion.golden.FrameId
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimeSeriesCaptureScope
import platform.test.motion.golden.TimestampFrameId
import platform.test.screenshot.DeviceEmulationRule
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.Displays
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.captureToBitmapAsync

/**
 * Toolkit to support Compose-based [MotionTestRule] tests.
 *
 * @param fixedConfiguration when non-null, applies the specified configuration to the content.
 * @param disableDrawDuringTest When true, will disable rendering at a [HardwareRenderer] level. On
 *   by default for cuttlefish emulators, greatly reduces test execution time when using SW
 *   rendering on emulators.
 */
class ComposeToolkit(
    val composeContentTestRule: ComposeContentTestRule,
    val testScope: TestScope,
    val fixedConfiguration: FixedConfiguration? = null,
    val disableDrawDuringTest: Boolean = isCuttlefish(),
) {
    internal companion object {
        const val TAG = "ComposeToolkit"
    }
}

/** Runs a motion test in the [ComposeToolkit.testScope] */
fun MotionTestRule<ComposeToolkit>.runTest(
    timeout: Duration = 20.seconds,
    testBody: suspend MotionTestRule<ComposeToolkit>.() -> Unit,
) {
    val motionTestRule = this
    toolkit.testScope.runTest(timeout) { testBody.invoke(motionTestRule) }
}

/**
 * Convenience to create a [MotionTestRule], including the required setup.
 *
 * In addition to the [MotionTestRule], this function also creates a [DeviceEmulationRule] and
 * [ComposeContentTestRule], and ensures these are run as part of the [MotionTestRule].
 */
@OptIn(ExperimentalTestApi::class)
fun createComposeMotionTestRule(
    goldenPathManager: GoldenPathManager,
    testScope: TestScope = TestScope(),
    deviceEmulationSpec: DeviceEmulationSpec = DeviceEmulationSpec(Displays.Phone),
): MotionTestRule<ComposeToolkit> {
    val deviceEmulationRule = DeviceEmulationRule(deviceEmulationSpec)
    val composeRule = createComposeRule(testScope.coroutineContext + Dispatchers.Main)

    return MotionTestRule(
        ComposeToolkit(composeRule, testScope),
        goldenPathManager,
        extraRules = RuleChain.outerRule(deviceEmulationRule).around(composeRule),
    )
}

/**
 * Controls the timing of the motion recording.
 *
 * The time series is recorded while the [recording] function is running.
 *
 * @param delayReadyToPlay allows delaying flipping the `play` parameter of the [recordMotion]'s
 *   content composable to true.
 * @param delayRecording allows delaying the first recorded frame, after the animation started.
 */
class MotionControl(
    val delayReadyToPlay: MotionControlFn = {},
    val delayRecording: MotionControlFn = {},
    val recording: MotionControlFn,
)

typealias MotionControlFn = suspend MotionControlScope.() -> Unit

/**
 * Returns a single semantic node matching [matcher].
 *
 * Throws if not exactly one matching node exists.
 *
 * This is a temporary replacement `onNode(matcher).fetchSemanticsNode()` for fetching many
 * [SemanticsNode] during the same animation frame. This needs to be replaced eventually with the
 * Compose-provided solution for this.
 */
fun SemanticsNodeInteractionsProvider.fetchSemanticsNodeMaybeCached(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean = false,
): SemanticsNode {
    return if (this is CachedSemanticNodeFetcher) {
        fetchSemanticsNodeCached(matcher, useUnmergedTree)
    } else {
        onNode(matcher).fetchSemanticsNode()
    }
}

/**
 * Returns all semantic node matching [matcher].
 *
 * This is a temporary replacement `onAllNodes(matcher).fetchSemanticsNodes()` for fetching many
 * [SemanticsNode] during the same animation frame. This needs to be replaced eventually with the
 * Compose-provided solution for this.
 */
fun SemanticsNodeInteractionsProvider.fetchAllSemanticsNodesMaybeCached(
    matcher: SemanticsMatcher,
    useUnmergedTree: Boolean = false,
): List<SemanticsNode> {
    return if (this is CachedSemanticNodeFetcher) {
        fetchAllSemanticsNodesCached(matcher, useUnmergedTree)
    } else {
        onAllNodes(matcher).fetchSemanticsNodes()
    }
}

interface MotionControlScope : SemanticsNodeInteractionsProvider {
    /** Waits until [check] returns true. Invoked on each frame. */
    suspend fun awaitCondition(check: () -> Boolean)

    /** Waits for [count] frames to be processed. */
    suspend fun awaitFrames(count: Int = 1)

    /** Waits for [duration] to pass. */
    suspend fun awaitDelay(duration: Duration)

    /**
     * Waits for compose to become idle.
     *
     * IMPORTANT: this is an experimental approximation; clients might need to use explicit signals
     * if this does not work.
     *
     * @param timeout the timeout in virtual time.
     */
    suspend fun awaitIdle(timeout: Duration = 1.seconds)

    /**
     * Performs touch input, and waits for the completion thereof.
     *
     * NOTE: Do use this function instead of [SemanticsNodeInteraction.performTouchInput], since
     * `performTouchInput` will also advance the time of the compose clock, making it impossible to
     * record motion while performing gestures.
     */
    suspend fun performTouchInputAsync(
        onNode: SemanticsNodeInteraction,
        gestureControl: TouchInjectionScope.() -> Unit,
    )
}

/**
 * Defines the sampling of features during a test run.
 *
 * @param motionControl defines the timing for the recording.
 * @param recordBefore Records the frame just before the animation is started (immediately before
 *   flipping the `play` parameter of the [recordMotion]'s content composable)
 * @param recordAfter Records the frame after the recording has ended (runs after awaiting idleness,
 *   after all animations have finished and no more recomposition is pending).
 * @param captureScreenshots Whether to record screenshots on each frame. Must be `true` for
 *   [filmstripMatchesGolden] to work. If `true`, the debug screenrecording will be created.
 * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
 */
data class ComposeRecordingSpec(
    val motionControl: MotionControl,
    val recordBefore: Boolean = true,
    val recordAfter: Boolean = true,
    val captureScreenshots: Boolean = Defaults.captureScreenshots(),
    val timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
) {
    constructor(
        recording: MotionControlFn,
        recordBefore: Boolean = true,
        recordAfter: Boolean = true,
        captureScreenshots: Boolean = Defaults.captureScreenshots(),
        timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
    ) : this(
        MotionControl(recording = recording),
        recordBefore,
        recordAfter,
        captureScreenshots,
        timeSeriesCapture,
    )

    companion object {
        /** Record a time-series until [checkDone] returns true. */
        fun until(
            checkDone: SemanticsNodeInteractionsProvider.() -> Boolean,
            recordBefore: Boolean = true,
            recordAfter: Boolean = true,
            captureScreenshots: Boolean = Defaults.captureScreenshots(),
            timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
        ): ComposeRecordingSpec {
            return ComposeRecordingSpec(
                motionControl = MotionControl { awaitCondition { checkDone() } },
                recordBefore,
                recordAfter,
                captureScreenshots,
                timeSeriesCapture,
            )
        }

        /** Record a time-series until [MotionControlScope.awaitIdle] completes. */
        fun untilIdle(
            recordBefore: Boolean = true,
            timeout: Duration = 1.seconds,
            captureScreenshots: Boolean = Defaults.captureScreenshots(),
            timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
        ): ComposeRecordingSpec {
            return ComposeRecordingSpec(
                motionControl = MotionControl { awaitIdle(timeout) },
                recordBefore = recordBefore,
                recordAfter = false,
                captureScreenshots = captureScreenshots,
                timeSeriesCapture = timeSeriesCapture,
            )
        }
    }
}

/**
 * Composes [content] and records the time-series of the features specified in [recordingSpec].
 *
 * The animation is recorded between flipping [content]'s `play` parameter to `true`, until the
 * [ComposeRecordingSpec.motionControl] finishes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun MotionTestRule<ComposeToolkit>.recordMotion(
    content: @Composable (play: Boolean) -> Unit,
    recordingSpec: ComposeRecordingSpec,
): RecordedMotion {
    with(toolkit.composeContentTestRule) {
        val frameIdCollector = mutableListOf<FrameId>()
        val propertyCollector = mutableMapOf<String, MutableList<DataPoint<*>>>()
        val screenshotCollector = mutableListOf<ImageBitmap>()

        lateinit var nodeProvider: SemanticsNodeInteractionsProvider

        @SuppressLint("VisibleForTests")
        fun recordFrame(frameId: FrameId) {
            Log.i(TAG, "recordFrame($frameId)")
            frameIdCollector.add(frameId)
            recordingSpec.timeSeriesCapture.invoke(
                TimeSeriesCaptureScope(nodeProvider, propertyCollector)
            )

            if (recordingSpec.captureScreenshots) {
                val view =
                    (nodeProvider.fetchSemanticsNodeMaybeCached(isRoot()).root as ViewRootForTest)
                        .view
                try {
                    screenshotCollector.add(
                        view.captureToBitmapAsync().get(10, TimeUnit.SECONDS).asImageBitmap()
                    )
                } catch (e: TimeoutException) {
                    throw Exception("Capturing screenshot timed out, see b/260824883", e)
                }
            }
        }

        val cleanupRunnables = mutableListOf<() -> Unit>()

        if (toolkit.disableDrawDuringTest && !recordingSpec.captureScreenshots) {
            val wasDrawingEnabled = HardwareRenderer.isDrawingEnabled()
            HardwareRenderer.setDrawingEnabled(false)
            cleanupRunnables.add { HardwareRenderer.setDrawingEnabled(wasDrawingEnabled) }
        }

        try {

            var playbackStarted by mutableStateOf(false)

            mainClock.autoAdvance = false

            lateinit var animationCoroutineScope: CoroutineScope
            setContent {
                animationCoroutineScope = rememberCoroutineScope()
                EnableMotionTestValueCollection {
                    val fixedConfiguration = toolkit.fixedConfiguration
                    if (fixedConfiguration != null) {
                        FixedConfigurationProvider(fixedConfiguration) { content(playbackStarted) }
                    } else {
                        content(playbackStarted)
                    }
                }
            }
            Log.i(TAG, "recordMotion() created compose content")

            waitForIdle()

            val motionControl =
                MotionControlImpl(
                        toolkit.composeContentTestRule,
                        toolkit.testScope,
                        recordingSpec.motionControl,
                        animationCoroutineScope,
                    )
                    .also {
                        nodeProvider = it
                        cleanupRunnables.add { it.unregisterSnapshotObserver }
                    }

            Log.i(TAG, "recordMotion() awaiting readyToPlay")

            // Wait for the test to allow readyToPlay
            while (!motionControl.readyToPlay) {
                motionControl.nextFrame()
            }

            if (recordingSpec.recordBefore) {
                recordFrame(SupplementalFrameId.Before)
            }
            Log.i(TAG, "recordMotion() awaiting recordingStarted")

            playbackStarted = true
            while (!motionControl.recordingStarted) {
                motionControl.nextFrame()
            }

            Log.i(TAG, "recordMotion() begin recording")

            val startFrameTime = mainClock.currentTime
            while (!motionControl.recordingEnded) {
                recordFrame(TimestampFrameId(mainClock.currentTime - startFrameTime))
                motionControl.nextFrame()
            }

            Log.i(TAG, "recordMotion() end recording")

            mainClock.autoAdvance = true
            waitForIdle()

            if (recordingSpec.recordAfter) {
                recordFrame(SupplementalFrameId.After)
            }

            val timeSeries =
                TimeSeries(
                    frameIdCollector.toList(),
                    propertyCollector.entries.map { entry -> Feature(entry.key, entry.value) },
                )

            return create(
                timeSeries,
                screenshotCollector
                    .takeIf { recordingSpec.captureScreenshots }
                    ?.map { it.asAndroidBitmap() },
            )
        } finally {
            cleanupRunnables.forEach { it() }
        }
    }
}

internal interface CachedSemanticNodeFetcher : SemanticsNodeInteractionsProvider {
    fun fetchSemanticsNodeCached(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false,
    ): SemanticsNode

    fun fetchAllSemanticsNodesCached(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false,
    ): List<SemanticsNode>
}

enum class MotionControlState {
    Start,
    WaitingToPlay,
    WaitingToRecord,
    Recording,
    Ended,
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
private class MotionControlImpl(
    val composeTestRule: ComposeTestRule,
    val testScope: TestScope,
    val motionControl: MotionControl,
    val animationCoroutineScope: CoroutineScope,
) :
    MotionControlScope,
    SemanticsNodeInteractionsProvider by composeTestRule,
    CachedSemanticNodeFetcher {

    private var state = MotionControlState.Start
    private lateinit var delayReadyToPlayJob: Job
    private lateinit var delayRecordingJob: Job
    private lateinit var recordingJob: Job
    private var lastFrameTimeMillis = 0L

    private val frameEmitter = MutableStateFlow<Long>(0)
    private val onFrame = frameEmitter.asStateFlow()

    private var isComposeIdle = false

    val broadcastFrameClock =
        animationCoroutineScope.coroutineContext[MonotonicFrameClock] as BroadcastFrameClock

    private var snapshotWasChanged = false

    val unregisterSnapshotObserver: ObserverHandle

    init {
        unregisterSnapshotObserver =
            Snapshot.registerGlobalWriteObserver { snapshotWasChanged = true }
    }

    val readyToPlay: Boolean
        get() =
            when (state) {
                MotionControlState.Start,
                MotionControlState.WaitingToPlay -> false

                else -> true
            }

    val recordingStarted: Boolean
        get() =
            when (state) {
                MotionControlState.Recording,
                MotionControlState.Ended -> true

                else -> false
            }

    val recordingEnded: Boolean
        get() =
            when (state) {
                MotionControlState.Ended -> true
                else -> false
            }

    fun nextFrame() {
        allNodesCache = null
        allNodesUnmergedCache = null

        // With `advanceTimeByFrame()` below, it is guaranteed to produce a frame.
        val lastFrameTimeFuture =
            animationCoroutineScope.async {
                var frameTimeMillis = 0L
                withFrameMillis { frameTimeMillis = it }
                frameTimeMillis
            }

        composeTestRule.mainClock.advanceTimeByFrame()
        lastFrameTimeMillis = lastFrameTimeFuture.getCompleted()

        isComposeIdle = !(broadcastFrameClock.hasAwaiters || snapshotWasChanged)
        snapshotWasChanged = false

        when (state) {
            MotionControlState.Start -> {
                delayReadyToPlayJob = motionControl.delayReadyToPlay.launch()
                state = MotionControlState.WaitingToPlay
            }

            MotionControlState.WaitingToPlay -> {
                if (delayReadyToPlayJob.isCompleted) {
                    delayRecordingJob = motionControl.delayRecording.launch()
                    state = MotionControlState.WaitingToRecord
                }
            }

            MotionControlState.WaitingToRecord -> {
                if (delayRecordingJob.isCompleted) {
                    recordingJob = motionControl.recording.launch()
                    state = MotionControlState.Recording
                }
            }

            MotionControlState.Recording -> {
                if (recordingJob.isCompleted) {
                    state = MotionControlState.Ended
                }
            }

            MotionControlState.Ended -> {}
        }

        frameEmitter.tryEmit(composeTestRule.mainClock.currentTime)
        testScope.runCurrent()

        if (state == MotionControlState.Recording && recordingJob.isCompleted) {
            state = MotionControlState.Ended
        }
    }

    override suspend fun awaitFrames(count: Int) {
        // Since this is a state-flow, the current frame is counted too. This condition must wait
        // for an additional frame to fulfill the contract
        onFrame.take(count + 1).collect {}
    }

    override suspend fun awaitDelay(duration: Duration) {
        val endTime = onFrame.value + duration.inWholeMilliseconds
        onFrame.takeWhile { it < endTime }.collect {}
    }

    override suspend fun awaitCondition(check: () -> Boolean) {
        onFrame.takeWhile { !check() }.collect {}
    }

    override suspend fun awaitIdle(timeout: Duration) {
        val timeout = composeTestRule.mainClock.currentTime + timeout.inWholeMilliseconds

        while (!isComposeIdle) {
            if (composeTestRule.mainClock.currentTime > timeout) {
                throw CancellationException("timed out while waiting for compose to become idle")
            }

            awaitFrames(1)
        }
    }

    override suspend fun performTouchInputAsync(
        onNode: SemanticsNodeInteraction,
        gestureControl: TouchInjectionScope.() -> Unit,
    ) {
        val node = onNode.fetchSemanticsNode()

        val rootForTest = fetchSemanticsNodeMaybeCached(isRoot()).root as ViewRootForTest

        val touchDispatchJob =
            testScope.launch {
                composeTestRule.doPerformTouchInputAsync(
                    onSemanticsNode = node,
                    gestureControl = gestureControl,
                    root = rootForTest,
                    frameOffsetMillis = lastFrameTimeMillis % frameDurationMillis,
                )
            }

        animationCoroutineScope.launch {
            // Request frames during the gesture replay. The reasons are two-fold:
            // - the tight withFrameNanos() loop ensures a frame is immediately scheduled,
            //   and so will happen frameDelayMillis later. Without that, a frame might be
            //   scheduled frameDelayMillis after the next input, which could result in
            //   irregular frame times
            // - without scheduling a frame, the main clock has no awaiters. So, whether a
            //   frame is produced during [performTouchInputAsync] depends on accidental side
            //   effects - for example if a recomposition is required, or an animation is
            //   running. The measureAndLayout phase is unconditionally run whenever a frame is
            //   triggered, but it's not change-observed in the test implementation. Hence,
            //   if the tests changes Snapshot state that is read in measureAndLayout only,
            //   no frame would be produced.
            while (touchDispatchJob.isActive) {
                withFrameMillis {}
            }
        }

        touchDispatchJob.join()

        // doPerformTouchInputAsync returned after dispatching the last event. That is 1ms before
        // the next frame. Thus, the next frame needs to be awaited.
        awaitFrames(1)
    }

    private fun MotionControlFn.launch(): Job {
        val function = this
        return testScope.launch { function() }
    }

    override fun fetchSemanticsNodeCached(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean,
    ): SemanticsNode {
        return fetchAllNodes(useUnmergedTree).singleOrNull { matcher.matches(it) }
            ?: throw AssertionError("Failed: assertExists")
    }

    override fun fetchAllSemanticsNodesCached(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean,
    ): List<SemanticsNode> {
        return fetchAllNodes(useUnmergedTree).filter { matcher.matches(it) }
    }

    private val allNodesMatcher = SemanticsMatcher("All Nodes") { true }
    private var allNodesCache: List<SemanticsNode>? = null
    private var allNodesUnmergedCache: List<SemanticsNode>? = null

    private fun fetchAllNodes(useUnmergedTree: Boolean): List<SemanticsNode> {
        return if (useUnmergedTree) {
            allNodesUnmergedCache
                ?: composeTestRule
                    .onAllNodes(allNodesMatcher, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .also { allNodesUnmergedCache = it }
        } else {
            allNodesCache
                ?: composeTestRule
                    .onAllNodes(allNodesMatcher, useUnmergedTree = false)
                    .fetchSemanticsNodes()
                    .also { allNodesCache = it }
        }
    }
}

// Another copy of the many ways to figure out whether this is running on a CF.
private fun isCuttlefish(): Boolean = Build.BOARD == "cutf"

/**
 * The duration of a frame in a compose test. This is always 16ms, but its hard to get this number
 * from compose, so copying our own constant.
 */
internal const val frameDurationMillis = 16
