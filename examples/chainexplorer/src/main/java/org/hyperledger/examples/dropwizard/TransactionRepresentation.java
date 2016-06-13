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
import org.hyperledger.common.BID;
import org.hyperledger.common.TID;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class TransactionRepresentation {
    @JsonProperty
    private final TID id;
    @JsonProperty
    private final BID blockID;
    @JsonProperty
    private final byte[] payload;

    public TransactionRepresentation(TID id, BID blockID, byte[] payload) {

        this.id = id;
        this.blockID = blockID;
        this.payload = payload;
    }

    public static TransactionRepresentation create(HLAPITransaction tx) {
        return new TransactionRepresentation(
                tx.getID(),
                tx.getBlockID(),
                tx.getPayload()
        );
    }
}
