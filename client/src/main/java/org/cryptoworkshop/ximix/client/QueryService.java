/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.client;

import java.util.Set;

import org.cryptoworkshop.ximix.client.connection.ServiceConnectionException;

/**
 * Carrier service interface for methods associated with board related queries.
 */
public interface QueryService
    extends Service
{
    /**
     * Return a list of the currently available nodes.
     *
     * @return a list of active node names.
     * @exception org.cryptoworkshop.ximix.client.connection.ServiceConnectionException in case of error.
     */
    Set<String> getNodeNames()
        throws ServiceConnectionException;

    /**
     * Return true if the board with boardName exists in the network, false otherwise.
     *
     * @param boardName the name of the board to create.
     * @return true if board exists, false otherwise.
     * @exception org.cryptoworkshop.ximix.client.connection.ServiceConnectionException in case of error.
     */
    boolean isBoardExisting(String boardName)
        throws ServiceConnectionException;
}
