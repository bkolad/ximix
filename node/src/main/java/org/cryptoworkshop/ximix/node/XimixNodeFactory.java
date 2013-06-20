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
package org.cryptoworkshop.ximix.node;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import org.cryptoworkshop.ximix.common.conf.Config;
import org.cryptoworkshop.ximix.common.conf.ConfigException;
import org.cryptoworkshop.ximix.common.service.ServicesConnection;
import org.cryptoworkshop.ximix.registrar.RegistrarConnectionException;
import org.cryptoworkshop.ximix.registrar.XimixRegistrar;
import org.cryptoworkshop.ximix.registrar.XimixRegistrarFactory;

public class XimixNodeFactory
{
    public static XimixNode createNode(final File peersConfig, final File config)
        throws RegistrarConnectionException, ConfigException
    {
        final Map<String, ServicesConnection> servicesMap = XimixRegistrarFactory.createServicesRegistrarMap(peersConfig);

        return new XimixNode()
        {
            private final Config nodeConfig = new Config(config);
            private final XimixNodeContext nodeContext = new XimixNodeContext(servicesMap, nodeConfig);

            final int portNo = nodeConfig.getIntegerProperty("portNo");

            public void start()
            {
                boolean stop = false;

                try
                {
                    ServerSocket ss = new ServerSocket(portNo);

                    while (!stop)
                    {
                         Socket s = ss.accept();

                         nodeContext.addConnection(new XimixServices(nodeContext, s));
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        };
    }
}