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
package org.hyperledger.examples.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.api.HLAPIBlock;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class BlockRepresentation {
    public static BlockRepresentation create(HLAPIBlock block) {
        return new BlockRepresentation(
                block.getID().toUuidString(),
                block.getPreviousID().toUuidString(),
                block.getHeight(),
                block.getLocalCreateTime(),
                block.getMerkleRoot().toString(),
                block.getTransactions().stream().map(tx -> tx.getID().toUuidString()).collect(Collectors.toList())
        );
    }

    @JsonProperty
    private final String id;
    @JsonProperty
    private final String previousID;
    @JsonProperty
    private final LocalTime localCreateTime;
    @JsonProperty
    private final long height;
    @JsonProperty
    private final String merkleRoot;
    @JsonProperty
    private final List<String> transactions;

    public BlockRepresentation(String id,
                               String previousID,
                               long height,
                               LocalTime localCreateTime,
                               String merkleRoot,
                               List<String> transactions) {
        this.id = id;
        this.previousID = previousID;
        this.height = height;
        this.localCreateTime = localCreateTime;
        this.merkleRoot = merkleRoot;
        this.transactions = transactions;
    }
}
