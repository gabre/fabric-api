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

import org.hyperledger.api.HLAPI;
import org.hyperledger.api.HLAPIBlock;
import org.hyperledger.api.HLAPIException;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.common.Hash;
import org.hyperledger.transaction.TID;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/explorer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExplorerResource {

    private final HLAPI api;

    public ExplorerResource(HLAPI api) {
        this.api = api;
    }

    @GET
    @Path("block/{blockNum}")
    public BlockRepresentation latestBlocks(@PathParam("blockNum") String blockNumStr) throws HLAPIException {
        long blockNum;
        if ("top".equals(blockNumStr)) {
            blockNum = api.getChainHeight() - 1;
        } else {
            try {
                blockNum = Long.parseLong(blockNumStr);
            } catch (IllegalArgumentException e) {
                throw new HLAPIException(e.getMessage());
            }
        }
        HLAPIBlock block = api.getBlock(blockNum);
        return BlockRepresentation.create(block);
    }

    @GET
    @Path("tx/{txId}")
    public TransactionRepresentation getTx(@PathParam("txId") String txIdStr) throws HLAPIException {
        TID txid = new TID(Hash.fromUuidString(txIdStr));
        HLAPITransaction tx = api.getTransaction(txid);
        return TransactionRepresentation.create(tx);
    }

    @GET
    @Path("chain/height")
    public ChainHeightRepresentation getChainHeight() throws HLAPIException {
        return ChainHeightRepresentation.create(api.getChainHeight());
    }
}
