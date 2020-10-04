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
package com.android.media.audiotestharness.service;

import com.android.media.audiotestharness.proto.AudioTestHarnessGrpc;
import com.android.media.audiotestharness.proto.AudioTestHarnessService;

import io.grpc.stub.StreamObserver;

/**
 * {@inheritDoc}
 *
 * <p>Core service implementation for the Audio Test Harness that utilizes that Java Sound API to
 * expose audio devices connected to a host to client devices for capture and playback.
 */
public final class AudioTestHarnessImpl extends AudioTestHarnessGrpc.AudioTestHarnessImplBase {

    private AudioTestHarnessImpl() {}

    public static AudioTestHarnessImpl create() {
        return new AudioTestHarnessImpl();
    }

    @Override
    public void capture(
            AudioTestHarnessService.CaptureRequest request,
            StreamObserver<AudioTestHarnessService.CaptureChunk> responseObserver) {
        // TODO(b/168801581): Implement this procedure to allow for opening of capture sessions by a
        // client.
    }
}
