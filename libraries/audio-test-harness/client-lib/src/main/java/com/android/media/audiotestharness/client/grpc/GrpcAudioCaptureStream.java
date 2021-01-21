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

package com.android.media.audiotestharness.client.grpc;

import com.android.media.audiotestharness.client.core.AudioCaptureStream;
import com.android.media.audiotestharness.common.Defaults;
import com.android.media.audiotestharness.proto.AudioTestHarnessGrpc;
import com.android.media.audiotestharness.proto.AudioTestHarnessService;

import com.google.common.base.Preconditions;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link AudioCaptureStream} that utilizes gRPC as its transfer mechanism.
 *
 * <p>Utilizes Piped I/O streams with the gRPC call writing to the sending side of the pipe, and the
 * exposed methods from the {@link java.io.InputStream} class being passed to the receiving end of
 * the pipe.
 */
public class GrpcAudioCaptureStream extends AudioCaptureStream {
    private static final Logger LOGGER = Logger.getLogger(GrpcAudioCaptureStream.class.getName());

    /**
     * Size of the buffer used by the {@link PipedInputStream} to cache internal messages received
     * over the gRPC connection before they are read by the client.
     *
     * <p>This value is currently equal to 35 chunks, 8960 bytes, or just about 100ms of audio
     * recorded at CD quality.
     */
    private static final int BUFFER_SIZE = 35 * Defaults.CAPTURE_CHUNK_TARGET_SIZE_BYTES;

    private final Context.CancellableContext mCancellableContext;
    private final PipedInputStream mInputStream;
    private final PipedOutputStream mOutputStream;

    /**
     * {@link Throwable} field used when the underlying gRPC call has an error. This error is
     * propagated back from the gRPC thread through a callback within the {@link
     * PipedCaptureChunkStreamObserver}. This field is volatile, as it will only be read by or
     * written to by single separate threads, but we want to make sure the reading thread is
     * immediately notified when an error occurs. Furthermore, this is safe since the underlying
     * Throwable will be immutable.
     */
    private volatile Throwable mGrpcError = null;

    private GrpcAudioCaptureStream(
            Context.CancellableContext cancellableContext,
            PipedInputStream inputStream,
            PipedOutputStream outputStream) {
        mCancellableContext = cancellableContext;
        mInputStream = inputStream;
        mOutputStream = outputStream;
    }

    static GrpcAudioCaptureStream create(
            AudioTestHarnessGrpc.AudioTestHarnessStub audioTestHarnessStub,
            ScheduledExecutorService scheduledExecutorService)
            throws IOException {
        Preconditions.checkNotNull(audioTestHarnessStub, "audioTestHarnessStub cannot be null.");
        Preconditions.checkNotNull(
                scheduledExecutorService, "scheduledExecutorService cannot be null.");

        // Create the piped streams that back the stream stream itself.
        PipedInputStream pipedInputStream = new PipedInputStream(BUFFER_SIZE);
        PipedOutputStream pipedOutputStream;
        try {
            pipedOutputStream = new PipedOutputStream(pipedInputStream);
        } catch (IOException ioe) {
            throw new IOException(
                    "Unable to create Capture Stream due to pipe creation failure", ioe);
        }

        // Start the gRPC call with a context that can be used for cancellation later.
        Context.CancellableContext grpcContext =
                Context.current()
                        .withCancellation()
                        .withDeadlineAfter(
                                Defaults.SYSTEM_TIMEOUT.getSeconds(),
                                TimeUnit.SECONDS,
                                scheduledExecutorService);

        GrpcAudioCaptureStream captureStream =
                new GrpcAudioCaptureStream(grpcContext, pipedInputStream, pipedOutputStream);

        try {
            grpcContext.call(
                    () -> {
                        audioTestHarnessStub.capture(
                                AudioTestHarnessService.CaptureRequest.getDefaultInstance(),
                                new PipedCaptureChunkStreamObserver(
                                        pipedOutputStream,
                                        (throwable) -> captureStream.mGrpcError = throwable));
                        return true;
                    });
        } catch (Exception e) {
            throw new IOException("Audio Test Harness gRPC Communication Error", e);
        }

        return captureStream;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (mGrpcError != null) {
            throw new IOException("Audio Test Harness gRPC Communication Error", mGrpcError);
        }

        try {
            return mInputStream.read(b);
        } catch (IOException ioe) {
            throw new IOException("Audio Test Harness gRPC Internal Error", ioe);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mGrpcError != null) {
            throw new IOException("Audio Test Harness gRPC Communication Error", mGrpcError);
        }

        try {
            return mInputStream.read(b, off, len);
        } catch (IOException ioe) {
            throw new IOException("Audio Test Harness gRPC Internal Error", mGrpcError);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (mGrpcError != null) {
            throw new IOException("Audio Test Harness gRPC Communication Error", mGrpcError);
        }

        return mInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        if (mGrpcError != null) {
            throw new IOException("Audio Test Harness gRPC Communication Error", mGrpcError);
        }

        return mInputStream.available();
    }

    @Override
    public void close() throws IOException {
        mCancellableContext.cancel(
                Status.CANCELLED.withDescription("Capture stopped by client").asException());

        mInputStream.close();
        mOutputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        mInputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        if (mGrpcError != null) {
            throw new IOException("Audio Test Harness gRPC Communication Error", mGrpcError);
        }

        try {
            mInputStream.reset();
        } catch (IOException ioe) {
            throw new IOException("Audio Test Harness gRPC Internal Error", ioe);
        }
    }

    @Override
    public boolean markSupported() {
        return mInputStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        if (mGrpcError != null) {
            throw new IOException("Audio Test Harness gRPC Communication Error", mGrpcError);
        }

        try {
            return mInputStream.read();
        } catch (IOException ioe) {
            throw new IOException("Audio Test Harness gRPC Internal Error", ioe);
        }
    }

    /**
     * {@link StreamObserver} that publishes audio samples received over a gRPC connection to a
     * piped output stream.
     */
    private static final class PipedCaptureChunkStreamObserver
            implements StreamObserver<AudioTestHarnessService.CaptureChunk> {
        private static final Logger LOGGER =
                Logger.getLogger(PipedCaptureChunkStreamObserver.class.getName());

        private final PipedOutputStream mPipedOutputStream;
        private final Consumer<Throwable> mOnErrorCallback;

        private PipedCaptureChunkStreamObserver(
                PipedOutputStream pipedOutputStream, Consumer<Throwable> onErrorCallback) {
            mPipedOutputStream = pipedOutputStream;
            mOnErrorCallback = onErrorCallback;
        }

        @Override
        public void onNext(AudioTestHarnessService.CaptureChunk value) {
            try {
                mPipedOutputStream.write(value.getData().toByteArray());
            } catch (IOException ioe) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to write segment of audio data, data may have been lost",
                        ioe);
            }
        }

        @Override
        public void onError(Throwable t) {
            // Immediately propagate the throwable back to the callback.
            mOnErrorCallback.accept(t);
            LOGGER.log(Level.WARNING, "onError called: ", t);

            // On error, close the stream so that any corresponding input streams will also
            // throw exceptions when attempting to read.
            try {
                mPipedOutputStream.close();
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, "Unable to close the pipedOutputStream", ioe);
            }
        }

        @Override
        public void onCompleted() {
            LOGGER.log(Level.FINE, "onCompleted called");
        }
    }
}
