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

/** CommunicationService is responsible for Device-Host request processing. */
package com.android.media.audio.test.harness.server;

import io.grpc.stub.StreamObserver;

import com.android.media.audio.test.harness.proto.CommunicationServiceGrpc;
import com.android.media.audio.test.harness.proto.CommunicationServiceOuterClass.StreamAudioRecordRequest;
import com.android.media.audio.test.harness.proto.CommunicationServiceOuterClass.AudioData;

/**
 * CommunicationServiceImpl implements methods defined in communication_service.proto.
 *
 * <p>This class is responsible for processing request sent from Device, executes the corresponding
 * commands on Host and sends back response if required.
 */
public class CommunicationServiceImpl
        extends CommunicationServiceGrpc.CommunicationServiceImplBase {

    @Override
    public void captureAudioStream(
            StreamAudioRecordRequest request, StreamObserver<AudioData> responseObserver) {}
}
