/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker.dsl;

import static com.android.server.wm.flicker.Extensions.getDefaultFlickerOutputDir;

import android.app.Instrumentation;
import android.support.test.launcherhelper.ILauncherStrategy;
import android.support.test.launcherhelper.LauncherStrategyFactory;

import androidx.test.uiautomator.UiDevice;

import com.android.server.wm.flicker.Flicker;
import com.android.server.wm.flicker.monitor.ITransitionMonitor;
import com.android.server.wm.flicker.monitor.LayersTraceMonitor;
import com.android.server.wm.flicker.monitor.ScreenRecorder;
import com.android.server.wm.flicker.monitor.WindowAnimationFrameStatsMonitor;
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor;
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject;
import com.android.server.wm.flicker.traces.windowmanager.WmTraceSubject;

import org.junit.Assert;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import kotlin.jvm.functions.Function1;

/**
 * Flicker test builder for Java compatibility
 *
 * @deprecated Used for Java compatibility only. If possible, use the Kotlin version
 */
@Deprecated
public class FlickerBuilderJava {
    /**
     * Creates a new builder instance
     *
     * @param instrumentation Instrumentation to run the tests
     * @param launcherStrategy Strategy used to interact with the launcher
     * @param includeJankyRuns Include or discard janky runs
     * @param outputDir Output directory for the test results
     */
    public FlickerBuilderJava(
            Instrumentation instrumentation,
            ILauncherStrategy launcherStrategy,
            boolean includeJankyRuns,
            Path outputDir) {
        this.instrumentation = instrumentation;
        this.launcherStrategy = launcherStrategy;
        this.outputDir = outputDir;
        this.device = UiDevice.getInstance(instrumentation);

        if (includeJankyRuns) {
            frameStatsMonitor = null;
        } else {
            frameStatsMonitor = new WindowAnimationFrameStatsMonitor(instrumentation);
        }

        traceMonitors.add(new WindowManagerTraceMonitor(outputDir));
        traceMonitors.add(new LayersTraceMonitor(outputDir));
        traceMonitors.add(new ScreenRecorder(outputDir));
    }

    /**
     * Creates a new builder instance in the device's external storage
     *
     * @param instrumentation Instrumentation to run the tests
     * @param launcherStrategy Strategy used to interact with the launcher
     * @param includeJankyRuns Include or discard janky runs
     */
    public FlickerBuilderJava(
            Instrumentation instrumentation,
            ILauncherStrategy launcherStrategy,
            boolean includeJankyRuns) {
        this(
                instrumentation,
                launcherStrategy,
                includeJankyRuns,
                getDefaultFlickerOutputDir());
    }

    /**
     * Creates a new builder instance in the device's external storage with the standard launcher
     * strategy for the test device
     *
     * @param instrumentation Instrumentation to run the tests
     * @param includeJankyRuns Include or discard janky runs
     */
    public FlickerBuilderJava(Instrumentation instrumentation, boolean includeJankyRuns) {
        this(
                instrumentation,
                LauncherStrategyFactory.getInstance(instrumentation).getLauncherStrategy(),
                includeJankyRuns);
    }

    /**
     * Creates a new builder instance in the device's external storage with the standard launcher
     * strategy for the test device and including janky runs
     *
     * @param instrumentation Instrumentation to run the tests
     */
    public FlickerBuilderJava(Instrumentation instrumentation) {
        this(instrumentation, /* includeJankyRuns */ true);
    }

    private Instrumentation instrumentation;
    private ILauncherStrategy launcherStrategy;
    private Path outputDir;
    private UiDevice device;

    private String testTag = "";
    private Integer iterations = 1;
    private TestCommands setupCommands = new TestCommands();
    private TestCommands teardownCommands = new TestCommands();
    private List<Function1<? super Flicker, ?>> transitionCommands = new ArrayList<>();
    private AssertionTarget assertions = new AssertionTarget();
    private List<ITransitionMonitor> traceMonitors = new ArrayList<>();
    private WindowAnimationFrameStatsMonitor frameStatsMonitor;

    /**
     * Test tag used to store the test results
     *
     * <p>If reused throughout the test, only the last value is stored
     */
    public FlickerBuilderJava withTag(String testTag) {
        Assert.assertFalse(
                "The test tag can not contain spaces since it is a part of the file name",
                testTag.contains(" "));
        this.testTag = testTag;
        return this;
    }

    /**
     * Configure a [WindowManagerTraceMonitor] to obtain [WindowManagerTrace]
     *
     * <p>By default the tracing is always active. To disable tracing return null
     *
     * <p>If this tracing is disabled, the assertions for [AssertionTarget.layerAssertions] will not
     * be executed
     */
    public FlickerBuilderJava withWindowManagerTracing(WindowManagerTraceMonitor traceMonitor) {
        traceMonitors =
                traceMonitors
                        .stream()
                        .filter(p -> !(p instanceof WindowManagerTraceMonitor))
                        .collect(Collectors.toList());
        if (traceMonitor != null) {
            traceMonitors.add(traceMonitor);
        }
        return this;
    }

    /**
     * Configure a [LayersTraceMonitor] to obtain [LayersTrace].
     *
     * <p>By default the tracing is always active. To disable tracing return null
     *
     * <p>If this tracing is disabled, the assertions for [AssertionTarget.layerAssertions] will not
     * be executed
     */
    public FlickerBuilderJava withLayerTracing(LayersTraceMonitor traceMonitor) {
        traceMonitors =
                traceMonitors
                        .stream()
                        .filter(p -> !(p instanceof LayersTraceMonitor))
                        .collect(Collectors.toList());
        if (traceMonitor != null) {
            traceMonitors.add(traceMonitor);
        }
        return this;
    }

    /**
     * Configure a [ScreenRecorder].
     *
     * <p>By default the tracing is always active. To disable tracing return null
     */
    public FlickerBuilderJava withScreenRecorder(ScreenRecorder screenRecorder) {
        traceMonitors =
                traceMonitors
                        .stream()
                        .filter(p -> !(p instanceof ScreenRecorder))
                        .collect(Collectors.toList());
        if (screenRecorder != null) {
            traceMonitors.add(screenRecorder);
        }
        return this;
    }

    /** Defines how many times the test run should be repeated */
    public FlickerBuilderJava repeat(int repetitions) {
        Assert.assertTrue(
                "Number of repetitions should be greater or equal to 1", repetitions >= 1);
        iterations = repetitions;
        return this;
    }

    /** Defines the test commands executed before each test transition */
    public FlickerBuilderJava addSetupEachRun(FlickerCommandJava commands) {
        setupCommands.eachRun(
                flicker -> {
                    commands.invoke(flicker);
                    return null;
                });
        return this;
    }

    /** Defines the test commands executed once before all test repetitions */
    public FlickerBuilderJava addSetupTest(FlickerCommandJava commands) {
        setupCommands.test(
                flicker -> {
                    commands.invoke(flicker);
                    return null;
                });
        return this;
    }

    /** Defines the test commands executed after each test transition */
    public FlickerBuilderJava addTeardownEachRun(FlickerCommandJava commands) {
        teardownCommands.eachRun(
                flicker -> {
                    commands.invoke(flicker);
                    return null;
                });
        return this;
    }

    /** Defines the test commands executed once after all test repetitions */
    public FlickerBuilderJava addTeardownTest(FlickerCommandJava commands) {
        teardownCommands.test(
                flicker -> {
                    commands.invoke(flicker);
                    return null;
                });
        return this;
    }

    /** Defines the test commands executed before each test transition */
    public FlickerBuilderJava addTransition(FlickerCommandJava commands) {
        transitionCommands.add(
                flicker -> {
                    commands.invoke(flicker);
                    return null;
                });
        return this;
    }

    /** Defines the assertions for the initial entry of the layers trace */
    public FlickerBuilderJava addLayerTraceAssertionStart(
            FlickerAssertionJava<LayersTraceSubject> assertion) {
        assertions.layersTrace(
                assertionData -> {
                    assertionData.start(assertion::invoke);
                    return null;
                });
        return this;
    }

    /** Defines the assertions for the final entry of the layers trace */
    public FlickerBuilderJava addLayerTraceAssertionEnd(
            FlickerAssertionJava<LayersTraceSubject> assertion) {
        assertions.layersTrace(
                assertionData -> {
                    assertionData.end(assertion::invoke);
                    return null;
                });
        return this;
    }

    /** Defines the assertions for all entries of the layers trace */
    public FlickerBuilderJava addLayerTraceAssertionAll(
            FlickerAssertionJava<LayersTraceSubject> assertion) {
        assertions.layersTrace(
                assertionData -> {
                    assertionData.all(assertion::invoke);
                    return null;
                });
        return this;
    }

    /** Defines the assertions for the initial entry of the layers trace */
    public FlickerBuilderJava addWindowManagerTraceAssertionStart(
            FlickerAssertionJava<WmTraceSubject> assertion) {
        assertions.windowManagerTrace(
                assertionData -> {
                    assertionData.start(assertion::invoke);
                    return null;
                });
        return this;
    }

    /** Defines the assertions for the final entry of the layers trace */
    public FlickerBuilderJava addWindowManagerTraceAssertionEnd(
            FlickerAssertionJava<WmTraceSubject> assertion) {
        assertions.windowManagerTrace(
                assertionData -> {
                    assertionData.end(assertion::invoke);
                    return null;
                });
        return this;
    }

    /** Defines the assertions for all entries of the layers trace */
    public FlickerBuilderJava addWindowManagerTraceAssertionAll(
            FlickerAssertionJava<WmTraceSubject> assertion) {
        assertions.windowManagerTrace(
                assertionData -> {
                    assertionData.all(assertion::invoke);
                    return null;
                });
        return this;
    }

    /** Creates a new Flicker runner based on the current builder configuration */
    public Flicker build() {
        return new Flicker(
                instrumentation,
                device,
                launcherStrategy,
                outputDir,
                testTag,
                iterations,
                frameStatsMonitor,
                traceMonitors,
                setupCommands,
                teardownCommands,
                transitionCommands,
                assertions);
    }
}
