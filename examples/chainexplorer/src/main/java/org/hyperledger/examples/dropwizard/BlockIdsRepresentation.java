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
import org.hyperledger.common.BID;

import java.util.List;

public class BlockIdsRepresentation {
    // TODO this should be able to get a blockIDList as parameter
    public static BlockIdsRepresentation create(BID blockID, int height, BID previousBlockId) {
        return new BlockIdsRepresentation(blockID, height, previousBlockId);
    }

    @JsonProperty
    private final BID blockId;
    @JsonProperty
    private final int height;
    @JsonProperty
    private final BID previousBlockId;

    public BlockIdsRepresentation(BID blockId, int height, BID previousBlockId) {
        this.blockId = blockId;
        this.height = height;
        this.previousBlockId = previousBlockId;
    }
}
