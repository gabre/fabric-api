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

import org.hyperledger.api.HLAPIBlock;
import org.hyperledger.api.HLAPITransaction;
import org.hyperledger.api.HLAPI;
import org.hyperledger.api.HLAPIException;
import org.hyperledger.common.BID;
import org.hyperledger.common.TID;

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
    @Path("block/{blockId}")
    public BlockRepresentation latestBlocks(@PathParam("blockId") String blockId) throws HLAPIException {
        BID bid = new BID(blockId);
        return BlockRepresentation.create(api.getBlock(bid));
    }

    @GET
    @Path("tx/{txId}")
    public TransactionRepresentation getTx(@PathParam("txId") String txId) throws HLAPIException {
        TID tid = null;
        System.out.println("HERE WE ARE");
        HLAPITransaction tx = api.getTransaction(tid);
        return TransactionRepresentation.create(tx);
    }

    @GET
    @Path("chain/height")
    public ChainHeightRepresentation getChainHeight() throws HLAPIException {
        return ChainHeightRepresentation.create(api.getChainHeight());
    }

    @GET
    @Path("chain/blockids")
    public BlockIdsRepresentation getBlockIds(@DefaultValue("top") @QueryParam("blockId") String blockId, @DefaultValue("20") @QueryParam("count") int count) throws HLAPIException {
        BID hash;
        if ("top".equals(blockId)) {
            hash = null;
        } else {
            try {
                hash = new BID(blockId);
            } catch (IllegalArgumentException e) {
                throw new HLAPIException(e.getMessage());
            }
        }
        // TODO this should be able to query for N blocks from a hash
        HLAPIBlock block = api.getBlock(hash);
        return BlockIdsRepresentation.create(block.getID(), block.getHeight(), block.getPreviousID());
    }


}
