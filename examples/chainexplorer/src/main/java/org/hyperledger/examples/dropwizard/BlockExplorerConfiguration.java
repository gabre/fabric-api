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
import io.dropwizard.Configuration;
import org.apache.commons.lang3.NotImplementedException;
// import org.hyperledger.dropwizard.EmbeddedHyperLedger;
// import org.hyperledger.dropwizard.HyperLedgerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BlockExplorerConfiguration extends Configuration {
    private static final Logger log = LoggerFactory.getLogger(BlockExplorerConfiguration.class);

    @JsonProperty
    private String host = "localhost";

    @JsonProperty
    private int port = 30303;

    @JsonProperty
    private int observerPort = 31315;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getObserverPort() {
        return observerPort;
    }
}
