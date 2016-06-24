/**
 * Copyright 2016 Digital Asset Holdings, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.api.connector;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protos.ConsensusApiGrpc;
import protos.ConsensusApiGrpc.ConsensusApiBlockingStub;
import protos.ConsensusApiGrpc.ConsensusApiStub;
import protos.ConsensusApiOuterClass.Dummy;
import protos.ConsensusApiOuterClass.Payload;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConsensusApiConnector {
    private static final Logger log = LoggerFactory.getLogger(ConsensusApiConnector.class);

    private final ManagedChannel channel;
    private final ConsensusApiStub stub;

    public ConsensusApiConnector(String host, int port, Consumer<Payload> callback) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
        stub = ConsensusApiGrpc.newStub(channel);
        setUpConsensusStream(callback);
    }

    private void setUpConsensusStream(Consumer<Payload> callback) {
        ConsensusApiBlockingStub blockingStub = ConsensusApiGrpc.newBlockingStub(channel);
        Dummy dummy = Dummy.newBuilder().setSuccess(true).build();
        stub.getConsensusStream(dummy, new StreamObserver<Payload>() {
            @Override
            public void onNext(Payload payload) {
                callback.accept(payload);
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn("Consensus stream threw error: {}", throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                log.debug("Consensus stream closed");
            }
        });
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void consentData(byte[] data) {
        Payload payload = Payload.newBuilder().setPayload(ByteString.copyFrom(data)).build();
        stub.consentData(payload, new StreamObserver<Dummy>() {
            @Override
            public void onNext(Dummy dummy) {
                log.debug("ConsentData returned with {}", dummy.getSuccess());
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn("ConsentData threw error: {}", throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                log.debug("Consentdate observer closed");
            }
        });
    }

}
