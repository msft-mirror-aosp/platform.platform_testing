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

package com.android.media.audiotestharness.server;

import com.android.media.audiotestharness.server.utility.PortUtility;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Factory for {@link AudioTestHarnessGrpcServer} instances.
 *
 * <p>This class is not meant to be extended, however is left non-final for mocking purposes.
 */
public class AudioTestHarnessGrpcServerFactory implements AutoCloseable {
    private static final Logger LOGGER =
            Logger.getLogger(AudioTestHarnessGrpcServerFactory.class.getName());

    /** Default port used for testing purposes. */
    private static final int TESTING_PORT = 8080;

    /**
     * Default number of threads that should be used for task execution.
     *
     * <p>This value is not used when using a provided {@link ExecutorService} and thus can be
     * overridden in cases where necessary.
     */
    private static final int DEFAULT_THREAD_COUNT = 16;

    /**
     * {@link Executor} used for task execution throughout the system.
     *
     * <p>This executor is used both by the gRPC server as well as in underlying libraries such as
     * the javasoundlib which uses the Executor to handle background capture while another thread
     * handles gRPC actions.
     */
    private final ExecutorService mExecutorService;

    private final Injector mInjector;

    private AudioTestHarnessGrpcServerFactory(ExecutorService executorService, Injector injector) {
        mExecutorService = executorService;
        mInjector = injector;
    }

    /**
     * Creates a new {@link AudioTestHarnessGrpcServerFactory} with the default ExecutorService,
     * which is a {@link java.util.concurrent.ThreadPoolExecutor} with {@link #DEFAULT_THREAD_COUNT}
     * threads.
     */
    public static AudioTestHarnessGrpcServerFactory createFactory() {
        ExecutorService executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        return new AudioTestHarnessGrpcServerFactory(
                Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT),
                Guice.createInjector(AudioTestHarnessServerModule.create(executorService)));
    }

    /**
     * Creates a new {@link AudioTestHarnessGrpcServerFactory} with the provided ExecutorService.
     *
     * <p>All created AudioTestHarnessGrpcServer instances will make use of this executor for tasks.
     * Furthermore, this {@link ExecutorService} will be shutdown whenever the {@link #close()}
     * method is invoked on this factory.
     */
    public static AudioTestHarnessGrpcServerFactory createFactoryWithExecutorService(
            ExecutorService executorService) {
        return createInternal(
                executorService,
                Guice.createInjector(AudioTestHarnessServerModule.create(executorService)));
    }

    @VisibleForTesting
    static AudioTestHarnessGrpcServerFactory createInternal(
            ExecutorService executorService, Injector injector) {
        return new AudioTestHarnessGrpcServerFactory(
                Preconditions.checkNotNull(executorService, "ExecutorService cannot be null."),
                Preconditions.checkNotNull(injector, "Injector cannot be null."));
    }

    /**
     * Creates a new {@link AudioTestHarnessGrpcServer} on the specified port.
     *
     * <p>This port is not reserved or used until the server's {@link
     * AudioTestHarnessGrpcServer#open()} method is called.
     */
    public AudioTestHarnessGrpcServer createOnPort(int port) {
        LOGGER.finest(String.format("createOnPort(%d)", port));
        return new AudioTestHarnessGrpcServer(port, mExecutorService, mInjector);
    }

    /** Creates a new {@link AudioTestHarnessGrpcServer} on the {@link #TESTING_PORT}. */
    public AudioTestHarnessGrpcServer createOnTestingPort() {
        LOGGER.finest("createOnTestingPort()");
        return createOnPort(TESTING_PORT);
    }

    /**
     * Creates a new {@link AudioTestHarnessGrpcServer} on the next available port within the
     * dynamic port range.
     */
    public AudioTestHarnessGrpcServer createOnNextAvailablePort() {
        LOGGER.finest("createOnNextAvailablePort()");
        return createOnPort(PortUtility.nextAvailablePort());
    }

    /** Shuts down the {@link ExecutorService} used by the factory. */
    @Override
    public void close() {
        LOGGER.fine("Shutting down internal ExecutorService");
        mExecutorService.shutdownNow();
    }
}
