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
import com.android.media.audiotestharness.client.core.AudioTestHarnessClient;

/** {@link AudioTestHarnessClient} that uses gRPC as its communication method. */
public class GrpcAudioTestHarnessClient extends AudioTestHarnessClient {

    // TODO(b/168812333): Implement this class to contain the required Executor and ManagedChannel
    // for spinning up new connections to the AudioTestHarness gRPC Server.

    private GrpcAudioTestHarnessClient() {}

    public static GrpcAudioTestHarnessClient create() {
        return new GrpcAudioTestHarnessClient();
    }

    @Override
    public AudioCaptureStream startCapture() {
        return null;
    }

    @Override
    public void close() {}
}
