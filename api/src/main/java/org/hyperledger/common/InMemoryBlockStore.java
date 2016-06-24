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

package org.hyperledger.common;

import org.hyperledger.api.*;
import org.hyperledger.api.connector.ConsensusApiConnector;
import org.hyperledger.block.BID;
import org.hyperledger.block.Block;
import org.hyperledger.block.Header;
import org.hyperledger.block.HyperledgerHeader;
import org.hyperledger.merkletree.MerkleTree;
import org.hyperledger.transaction.TID;
import org.hyperledger.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protos.ConsensusApiOuterClass.Payload;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class InMemoryBlockStore implements HLAPI {
    private static final Logger log = LoggerFactory.getLogger(InMemoryBlockStore.class);

    private final ConsensusApiConnector connector;

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Map<BID, Block> blocks = new HashMap<>();
    private final Map<BID, byte[]> proofs = new HashMap<>();
    private final Map<BID, Integer> heights = new HashMap<>();
    private final Map<TID, BID> txIndex = new HashMap<>();
    private final Map<TID, Transaction> mempool = new HashMap<>();
    private BID top = BID.INVALID;

    private final List<TransactionListener> txListeners = new ArrayList<>();
    private final List<TrunkListener> trunkListeners = new ArrayList<>();

    public InMemoryBlockStore() {
        connector = null;
    }

    public InMemoryBlockStore(String host, int port) {
        Consumer<Payload> consensusResultConsumer = (Payload p) -> {
            try {
                addBlock(Block.fromByteArray(p.getPayload().toByteArray()), p.getProof().toByteArray());
            } catch (HLAPIException e) {
                log.error("Failed to add block: {}", e.getMessage());
            } catch (IOException e) {
                log.error("Failed to decode block: {}", e.getMessage());
            }
        };
        connector = new ConsensusApiConnector(host, port, consensusResultConsumer);
    }

    public void addBlock(Block block, byte[] proof) throws HLAPIException {
        rwl.writeLock().lock();
        try {
            if (getChainHeight() == 0 || block.getPreviousID() == top) {
                top = block.getID();
            }
            blocks.put(block.getID(), block);
            proofs.put(block.getID(), proof);
            heights.put(block.getID(), blocks.size());
            for (Transaction tx : block.getTransactions()) {
                mempool.remove(tx.getID());
                txIndex.put(tx.getID(), block.getID());
                notifyTransactionListeners(tx);
            }
            notifyTrunkListeners(block);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private void notifyTransactionListeners(Transaction tx) throws HLAPIException {
        HLAPITransaction transaction = toHLAPITransaction(tx);
        for (TransactionListener listener : txListeners) {
            listener.process(transaction);
        }
    }

    private void notifyTrunkListeners(Block b) {
        HLAPIBlock block = toHLAPIBlock(b);
        for (TrunkListener listener : trunkListeners) {
            listener.trunkUpdate(Collections.singletonList(block));
        }
    }

    public Block createBlock() {
        rwl.readLock().lock();
        try {
            List<Transaction> transactions = new ArrayList<>(mempool.values());
            int time = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            Header header = new HyperledgerHeader(top, MerkleTree.computeMerkleRoot(transactions), time);
            return new Block(header, transactions);
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public String getClientVersion() throws HLAPIException {
        return null;
    }

    @Override
    public String getServerVersion() throws HLAPIException {
        return null;
    }

    @Override
    public long ping(long nonce) throws HLAPIException {
        return 0;
    }

    @Override
    public void addAlertListener(AlertListener listener) throws HLAPIException {

    }

    @Override
    public void removeAlertListener(AlertListener listener) {

    }

    @Override
    public int getChainHeight() throws HLAPIException {
        rwl.readLock().lock();
        try {
            return blocks.size();
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public HLAPIHeader getBlockHeader(BID hash) throws HLAPIException {
        rwl.readLock().lock();
        try {
            return getOptionalBlock(hash)
                    .map(this::toHLAPIHeader)
                    .orElse(null);
        } finally {
            rwl.readLock().unlock();
        }
    }

    private Optional<Block> getOptionalBlock(BID hash) {
        return Optional.ofNullable(blocks.get(hash));
    }

    private HLAPIHeader toHLAPIHeader(Block block) {
        return new HLAPIHeader(block.getHeader(), heights.get(block.getID()));
    }

    @Override
    public HLAPIBlock getBlock(BID hash) throws HLAPIException {
        rwl.readLock().lock();
        try {
            return getOptionalBlock(hash)
                    .map(this::toHLAPIBlock)
                    .orElse(null);
        } finally {
            rwl.readLock().unlock();
        }
    }

    private HLAPIBlock toHLAPIBlock(Block block) {
        return new HLAPIBlock(toHLAPIHeader(block), block.getTransactions());
    }

    @Override
    public HLAPITransaction getTransaction(TID hash) throws HLAPIException {
        rwl.readLock().lock();
        try {
            return getOptionalTxBID(hash)
                    .flatMap(this::getOptionalBlock)
                    .map(b -> b.getTransaction(hash))
                    .map(this::toHLAPITransaction)
                    .orElse(null);
        } finally {
            rwl.readLock().unlock();
        }
    }

    private Optional<BID> getOptionalTxBID(TID hash) {
        return Optional.ofNullable(txIndex.get(hash));
    }

    private HLAPITransaction toHLAPITransaction(Transaction transaction) {
        return new HLAPITransaction(transaction, txIndex.get(transaction.getID()));
    }

    @Override
    public void sendTransaction(Transaction transaction) throws HLAPIException {
        rwl.writeLock().lock();
        try {
            mempool.put(transaction.getID(), transaction);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void registerRejectListener(RejectListener rejectListener) throws HLAPIException {
    }

    @Override
    public void removeRejectListener(RejectListener rejectListener) {
    }

    @Override
    public void sendBlock(Block block) throws HLAPIException {
        connector.consentData(block.toByteArray());
    }

    @Override
    public void registerTransactionListener(TransactionListener listener) throws HLAPIException {
        txListeners.add(listener);
    }

    @Override
    public void removeTransactionListener(TransactionListener listener) {
        txListeners.remove(listener);
    }

    @Override
    public void registerTrunkListener(TrunkListener listener) throws HLAPIException {
        trunkListeners.add(listener);
    }

    @Override
    public void removeTrunkListener(TrunkListener listener) {
        trunkListeners.add(listener);
    }

    @Override
    public void catchUp(List<BID> inventory, int limit, boolean headers, TrunkListener listener) throws HLAPIException {
    }

}
