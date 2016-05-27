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


import com.google.common.base.Stopwatch;
import org.hyperledger.api.HLAPI;
import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.api.TransactionListener;
import org.hyperledger.common.Transaction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PerfTest {

    private static final int SIZE = 10000;
    private static final int NR_OF_GROUPS = 10;
    private static final int CHUNK_SIZE = SIZE / NR_OF_GROUPS;
    private static final int MAXTIME = 60;

    private HLAPI api = new GRPCClient("localhost", 30303, 31315);
    private ConcurrentMap<Transaction, Stopwatch> txs = new ConcurrentHashMap<Transaction, Stopwatch>();
    private List<Long> results = Collections.synchronizedList(new ArrayList<>(SIZE));
    private Random random = new Random();
    
    private class PerfListener implements TransactionListener {

        @Override
        public void process(HLAPITransaction t) throws HLAPIException {
            Stopwatch sw = txs.get(t);
            sw.stop();
            results.add(sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }
    PerfListener perfListener = new PerfListener();
 
    @Before
    public void setUp() {
        for (int i = 0; i < SIZE; i++) {
            byte[] data = new byte[100];
            random.nextBytes(data);
            Transaction t = new Transaction(data);
            txs.put(t, Stopwatch.createUnstarted());
        }
        try {
            api.registerTransactionListener(perfListener);
        } catch (HLAPIException e) {
            fail("A HLAPIException occured when trying to register a listener.");
        }
    }

    @Test
    public void performance() throws HLAPIException, InterruptedException {
        System.out.println("Sending Txs.");
        for (Entry<Transaction, Stopwatch> e : txs.entrySet()) {
            e.getValue().start();
            api.sendTransaction(e.getKey());
        }
        
        System.out.println("All Txs are sent. Now we have to wait some time to receive the transaction events...");
        int time;
        for(time=1; time<=MAXTIME && results.size() < SIZE; ++time) {
            Thread.sleep(1000);
        }
        if (results.size() != SIZE) {
            System.out.println("Received transaction count:");
            System.out.println(results.size());
            fail("Not enough transaction events were received from the ledger: results_count =/= sent_tx_count");
        }
        api.removeTransactionListener(perfListener);

        for (Transaction t : txs.keySet()) {
            Transaction storedTx = api.getTransaction(t.getID());
            assertNotNull("No transaction found with the id " + t.getID(), storedTx);
            assertEquals(t.getID(), storedTx.getID());
            assertArrayEquals(t.getPayload(), storedTx.getPayload());
        }

        System.out.println("====== Test results ======");
        System.out.println("Average transaction time: " + avg(results) + " ms");
        System.out.println("Distribution: ");
        for (int i = 0; i < NR_OF_GROUPS; i ++) {
            int lowerBound = i * CHUNK_SIZE;
            int upperBound = (i + 1) * CHUNK_SIZE;
            List<Long> chunk = results.subList(lowerBound, upperBound);
            System.out.println("  " + lowerBound + "-" + upperBound + ": " + avg(chunk) + " ms");
        }

    }

    private double avg(List<Long> l) {
        return l.stream().mapToLong(Long::longValue).average().getAsDouble();
    }
}
