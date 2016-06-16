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
import org.hyperledger.api.HLAPITransaction;

/**
 *
 */
public class TransactionRepresentation {
    @JsonProperty
    private final String id;
    @JsonProperty
    private final String blockID;
    @JsonProperty
    private final byte[] payload;

    public TransactionRepresentation(String id, String blockID, byte[] payload) {

        this.id = id;
        this.blockID = blockID;
        this.payload = payload;
    }

    public static TransactionRepresentation create(HLAPITransaction tx) {
        return new TransactionRepresentation(
                tx.getID().toUuidString(),
                tx.getBlockID().toUuidString(),
                tx.getPayload()
        );
    }
}
