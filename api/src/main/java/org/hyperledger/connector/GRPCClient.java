/**
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

package org.hyperledger.connector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import org.hyperledger.api.*;
import org.hyperledger.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protos.Chaincode;
import protos.Chaincode.ChaincodeID;
import protos.Chaincode.ChaincodeInput;
import protos.Chaincode.ChaincodeInvocationSpec;
import protos.Chaincode.ChaincodeSpec;
import protos.DevopsGrpc;
import protos.DevopsGrpc.DevopsBlockingStub;

import protos.Fabric;
import protos.OpenchainGrpc;
import protos.OpenchainGrpc.OpenchainBlockingStub;
import protos.Api.BlockCount;

import javax.xml.bind.DatatypeConverter;

public class GRPCClient implements HLAPI {
    private static final Logger log = LoggerFactory.getLogger(GRPCClient.class);

    final String chaincodeName = "noop";


    private DevopsBlockingStub dbs;
    private OpenchainBlockingStub obs;

    private final GRPCObserver observer;

    public GRPCClient(String host, int port, int observerPort) {
        log.debug("Trying to connect to GRPC host:port={}:{}, host:observerPort={}:{}, ", host, port, observerPort);
        ManagedChannel channel = NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.PLAINTEXT).build();
        ManagedChannel observerChannel = NettyChannelBuilder.forAddress(host, observerPort).negotiationType(NegotiationType.PLAINTEXT).build();
        dbs = DevopsGrpc.newBlockingStub(channel);
        obs = OpenchainGrpc.newBlockingStub(channel);
        observer = new GRPCObserver(observerChannel);
        observer.connect();
    }

    public void invoke(String chaincodeName, byte[] transaction) {
        String encodedTransaction = Base64.getEncoder().encodeToString(transaction);

        ChaincodeID.Builder chaincodeId = ChaincodeID.newBuilder();
        chaincodeId.setName(chaincodeName);

        ChaincodeInput.Builder chaincodeInput = ChaincodeInput.newBuilder();
        chaincodeInput.setFunction("execute");
        chaincodeInput.addArgs(encodedTransaction);

        ChaincodeSpec.Builder chaincodeSpec = ChaincodeSpec.newBuilder();
        chaincodeSpec.setChaincodeID(chaincodeId);
        chaincodeSpec.setCtorMsg(chaincodeInput);

        ChaincodeInvocationSpec.Builder chaincodeInvocationSpec = ChaincodeInvocationSpec.newBuilder();
        chaincodeInvocationSpec.setChaincodeSpec(chaincodeSpec);

        dbs.invoke(chaincodeInvocationSpec.build());
    }

    private ByteString query(String functionName, Iterable<String> args) {
        Chaincode.ChaincodeID chainCodeId = Chaincode.ChaincodeID.newBuilder()
                .setName(chaincodeName)
                .build();

        Chaincode.ChaincodeInput chainCodeInput = Chaincode.ChaincodeInput.newBuilder()
                .setFunction(functionName)
                .addAllArgs(args)
                .build();

        Chaincode.ChaincodeSpec chaincodeSpec = Chaincode.ChaincodeSpec.newBuilder()
                .setChaincodeID(chainCodeId)
                .setCtorMsg(chainCodeInput)
                .build();

        Chaincode.ChaincodeInvocationSpec chaincodeInvocationSpec = Chaincode.ChaincodeInvocationSpec.newBuilder()
                .setChaincodeSpec(chaincodeSpec)
                .build();

        Fabric.Response response = dbs.query(chaincodeInvocationSpec);

        return response.getMsg();
    }

    @Override
    public String getClientVersion() throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServerVersion() throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long ping(long nonce) throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAlertListener(AlertListener listener) throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAlertListener(AlertListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getChainHeight() throws HLAPIException {
        BlockCount height = obs.getBlockCount(com.google.protobuf.Empty.getDefaultInstance());
        return (int) height.getCount();
    }


    @Override
    public HLAPIHeader getBlockHeader(BID hash) throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public HLAPIBlock getBlock(BID hash) throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public HLAPITransaction getTransaction(TID hash) throws HLAPIException {
        String hexedHash = ByteUtils.toHex(hash.toByteArray());
        ByteString result = query("getTran", Collections.singletonList(hexedHash));
        byte[] resultStr = result.toByteArray();
        if (resultStr.length == 0) return null;
        return new HLAPITransaction(new Transaction(hash, resultStr), BID.INVALID);
    }

    @Override
    public void sendTransaction(Transaction transaction) throws HLAPIException {
        invoke(chaincodeName, transaction.getPayload());
    }

    @Override
    public void registerRejectListener(RejectListener rejectListener) throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRejectListener(RejectListener rejectListener) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void sendBlock(Block block) throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerTransactionListener(TransactionListener listener) throws HLAPIException {
        observer.subscribe(listener);
    }

    @Override
    public void removeTransactionListener(TransactionListener listener) {
        observer.unsubscribe(listener);
    }

    @Override
    public void registerTrunkListener(TrunkListener listener) throws HLAPIException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTrunkListener(TrunkListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void catchUp(List<BID> inventory, int limit, boolean headers, TrunkListener listener)
            throws HLAPIException {
        // TODO we will need this
        throw new UnsupportedOperationException();
    }
}
