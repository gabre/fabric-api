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

import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.api.TrunkListener;
import org.hyperledger.block.BID;
import org.hyperledger.block.Block;
import org.hyperledger.block.Header;
import org.hyperledger.block.HyperledgerHeader;
import org.hyperledger.common.InMemoryBlockStore;
import org.hyperledger.merkletree.MerkleTree;
import org.hyperledger.transaction.Transaction;
import org.hyperledger.transaction.TransactionTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InMemoryBlockStoreTest {

    private InMemoryBlockStore store;

    @Mock
    private TransactionListener txListener;
    @Mock
    private TrunkListener trunkListener;

    @Before
    public void setUp() {
        store = new InMemoryBlockStore();
    }

    @Test
    public void listenersCalled() throws HLAPIException {
        store.registerTransactionListener(txListener);
        store.registerTrunkListener(trunkListener);

        Transaction tx = TransactionTest.randomTx();
        store.sendTransaction(tx);
        Block block = store.createBlock();
        store.addBlock(block, new byte[0]);

        verify(txListener).process(any(HLAPITransaction.class));
        verify(trunkListener).trunkUpdate(any());

        assertEquals(1, store.getChainHeight());
    }

    public static Block randomBlock() {
        List<Transaction> transactions = Collections.singletonList(TransactionTest.randomTx());
        Header header = new HyperledgerHeader(BID.INVALID, MerkleTree.computeMerkleRoot(transactions), 0);
        return new Block(header, transactions);
    }

    @Test
    public void gettersReturnItems() throws HLAPIException {
        Block block = randomBlock();
        store.addBlock(block, new byte[0]);

        assertNotNull(store.getBlock(block.getID()));
        assertNotNull(store.getBlockHeader(block.getID()));
        assertNotNull(store.getTransaction(block.getTransaction(0).getID()));
    }

    @Test
    public void gettersReturnNull() throws HLAPIException {
        Block block = randomBlock();

        assertNull(store.getBlock(block.getID()));
        assertNull(store.getBlockHeader(block.getID()));
        assertNull(store.getTransaction(block.getTransaction(0).getID()));
    }
}
