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
import org.hyperledger.common.Transaction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PerfTest {

    private static final int SIZE = 10000;
    private static final int NR_OF_GROUPS = 10;
    private static final int CHUNK_SIZE = SIZE / NR_OF_GROUPS;

    private HLAPI api = new GRPCClient("localhost", 30303, 31315);
    private List<Transaction> txs = new ArrayList<>(SIZE);
    private List<Long> results = new ArrayList<>(SIZE);
    private Random random = new Random();

    @Before
    public void setUp() {
        for (int i = 0; i < SIZE; i++) {
            byte[] data = new byte[100];
            random.nextBytes(data);
            Transaction t = new Transaction(data);
            txs.add(t);
        }
    }

    @Test
    public void performance() throws HLAPIException, InterruptedException {
        for (Transaction t : txs) {
            Stopwatch watch = Stopwatch.createStarted();
            api.sendTransaction(t);
            watch.stop();
            results.add(watch.elapsed(TimeUnit.MILLISECONDS));
        }

        Thread.sleep(1000); // TODO

        for (Transaction t : txs) {
            Transaction storedTx = api.getTransaction(t.getID());
            assertNotNull("No transaction found with the id " + t.getID() + " at index " + txs.indexOf(t), storedTx);
            assertEquals(t.getID(), storedTx.getID());
            assertArrayEquals(t.getPayload(), storedTx.getPayload());
        }

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
