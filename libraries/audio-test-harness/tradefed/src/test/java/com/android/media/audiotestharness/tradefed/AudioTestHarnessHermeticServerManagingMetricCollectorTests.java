/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.media.audiotestharness.tradefed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.media.audiotestharness.server.AudioTestHarnessGrpcServer;
import com.android.media.audiotestharness.server.AudioTestHarnessGrpcServerFactory;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.metric.DeviceMetricData;
import com.android.tradefed.invoker.IInvocationContext;
import com.android.tradefed.result.ITestInvocationListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@RunWith(JUnit4.class)
public class AudioTestHarnessHermeticServerManagingMetricCollectorTests {

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock AudioTestHarnessGrpcServerFactory mAudioTestHarnessGrpcServerFactory;

    @Mock ITestDevice mTestDeviceOne;

    @Mock ITestDevice mTestDeviceTwo;

    @Mock IInvocationContext mInvocationContext;

    @Mock ITestInvocationListener mTestInvocationListener;

    @Mock AudioTestHarnessGrpcServer mAudioTestHarnessGrpcServer;

    private AudioTestHarnessHermeticServerManagingMetricCollector mMetricCollector;

    private DeviceMetricData mTestDeviceMetricData;

    @Before
    public void setUp() throws Exception {
        when(mAudioTestHarnessGrpcServerFactory.createOnNextAvailablePort())
                .thenReturn(mAudioTestHarnessGrpcServer);

        mMetricCollector =
                new AudioTestHarnessHermeticServerManagingMetricCollector(
                        mAudioTestHarnessGrpcServerFactory);

        when(mInvocationContext.getDevices())
                .thenReturn(ImmutableList.of(mTestDeviceOne, mTestDeviceTwo));
        mMetricCollector.init(mInvocationContext, mTestInvocationListener);

        mTestDeviceMetricData = new DeviceMetricData(mInvocationContext);
    }

    @Test
    public void constructor_successfullyCreatesNewMetricCollector() throws Exception {
        new AudioTestHarnessHermeticServerManagingMetricCollector();
    }

    @Test
    public void onTestRunStart_properlyAttachesLoggingHandlerToServerLogger() throws Exception {
        mMetricCollector.onTestRunStart(mTestDeviceMetricData);

        Logger serverRootLogger =
                LogManager.getLogManager()
                        .getLogger(AudioTestHarnessGrpcServer.class.getName())
                        .getParent();

        assertEquals(Level.ALL, serverRootLogger.getLevel());
        assertEquals(1, serverRootLogger.getHandlers().length);
        assertTrue(
                serverRootLogger.getHandlers()[0]
                        instanceof AudioTestHarnessServerLogForwardingHandler);
    }

    @Test
    public void onTestRunStart_buildsServerFromFactoryOnAvailablePort() {
        mMetricCollector.onTestRunStart(mTestDeviceMetricData);
        verify(mAudioTestHarnessGrpcServerFactory).createOnNextAvailablePort();
    }

    @Test
    public void onTestRunStart_startsServerProperly() throws Exception {
        mMetricCollector.onTestRunStart(new DeviceMetricData(mInvocationContext));
        verify(mAudioTestHarnessGrpcServer).open();
    }

    @Test
    public void onTestRunStart_triggersAdbPortReversalToServerPort() throws Exception {
        when(mAudioTestHarnessGrpcServer.getPort()).thenReturn(8080);

        mMetricCollector.onTestRunStart(mTestDeviceMetricData);

        verify(mTestDeviceOne).executeAdbCommand("reverse", "tcp:55555", "tcp:8080");
        verify(mTestDeviceTwo).executeAdbCommand("reverse", "tcp:55555", "tcp:8080");
    }

    @Test(expected = RuntimeException.class)
    public void onTestRunStart_throwsRuntimeException_serverFailsToStart() throws Exception {
        doThrow(RuntimeException.class).when(mAudioTestHarnessGrpcServer).open();
        mMetricCollector.onTestRunStart(mTestDeviceMetricData);
    }

    @Test(expected = RuntimeException.class)
    public void onTestRunStart_throwsRuntimeException_portReversalFailure() throws Exception {
        when(mTestDeviceOne.executeAdbCommand(any())).thenThrow(DeviceNotAvailableException.class);

        mMetricCollector.onTestRunStart(mTestDeviceMetricData);
    }

    @Test
    public void onTestRunEnd_closesGrpcServer() throws Exception {
        mMetricCollector.onTestRunStart(mTestDeviceMetricData);

        mMetricCollector.onTestRunEnd(
                mTestDeviceMetricData, /* currentRunMetrics= */ ImmutableMap.of());

        verify(mAudioTestHarnessGrpcServer).close();
    }

    @Test
    public void onTestRunEnd_closesServerFactory() throws Exception {
        mMetricCollector.onTestRunStart(mTestDeviceMetricData);

        mMetricCollector.onTestRunEnd(
                mTestDeviceMetricData, /* currentRunMetrics= */ ImmutableMap.of());

        verify(mAudioTestHarnessGrpcServerFactory).close();
    }

    @Test
    public void onTestRunEnd_properlyUndoesPortReversals() throws Exception {
        mMetricCollector.onTestRunStart(mTestDeviceMetricData);

        mMetricCollector.onTestRunEnd(
                mTestDeviceMetricData, /* currentRunMetrics= */ ImmutableMap.of());

        verify(mTestDeviceOne).executeAdbCommand("reverse", "--remove-all");
        verify(mTestDeviceTwo).executeAdbCommand("reverse", "--remove-all");
    }

    @Test
    public void onTestRunEnd_executesWithoutException_failureToUndoPortReversal() throws Exception {
        mMetricCollector.onTestRunStart(mTestDeviceMetricData);

        when(mTestDeviceOne.executeAdbCommand(any())).thenThrow(DeviceNotAvailableException.class);

        // If this call throws no exceptions, then the test passes without issue.
        mMetricCollector.onTestRunEnd(
                mTestDeviceMetricData, /* currentRunMetrics= */ ImmutableMap.of());
    }
}
