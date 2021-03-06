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
package org.cryptoworkshop.ximix.node.crypto.service;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERUTF8String;
import org.cryptoworkshop.ximix.client.connection.signing.BLSSigningService;
import org.cryptoworkshop.ximix.client.connection.signing.ECDSASigningService;
import org.cryptoworkshop.ximix.common.asn1.message.AlgorithmServiceMessage;
import org.cryptoworkshop.ximix.common.asn1.message.CapabilityMessage;
import org.cryptoworkshop.ximix.common.asn1.message.ClientMessage;
import org.cryptoworkshop.ximix.common.asn1.message.CommandMessage;
import org.cryptoworkshop.ximix.common.asn1.message.Message;
import org.cryptoworkshop.ximix.common.asn1.message.MessageReply;
import org.cryptoworkshop.ximix.common.asn1.message.SignatureMessage;
import org.cryptoworkshop.ximix.common.config.Config;
import org.cryptoworkshop.ximix.common.crypto.Algorithm;
import org.cryptoworkshop.ximix.common.util.EventNotifier;
import org.cryptoworkshop.ximix.node.crypto.signature.BLSSignerEngine;
import org.cryptoworkshop.ximix.node.crypto.signature.ECDSASignerEngine;
import org.cryptoworkshop.ximix.node.service.BasicNodeService;
import org.cryptoworkshop.ximix.node.service.NodeContext;

/**
 * Service class for perform signing operations in a node.
 */
public class NodeSigningService
    extends BasicNodeService
{
    private final ECDSASignerEngine ecdsaSignerEngine;
    private final BLSSignerEngine blsSignerEngine;

    /**
     * Base constructor.
     *
     * @param nodeContext the context for the node we are in.
     * @param config source of config information if required.
     */
    public NodeSigningService(NodeContext nodeContext, Config config)
    {
        super(nodeContext);
        // TODO: make this configurable
        this.ecdsaSignerEngine = new ECDSASignerEngine(nodeContext);
        this.blsSignerEngine = new BLSSignerEngine(nodeContext);
    }

    public CapabilityMessage getCapability()
    {
        return new CapabilityMessage(CapabilityMessage.Type.SIGNING, new ASN1Encodable[0]); // TODO:
    }

    public MessageReply handle(final Message message)
    {
        try
        {
            AlgorithmServiceMessage serviceMessage = AlgorithmServiceMessage.getInstance(message.getPayload());

            if (message.getType() instanceof ClientMessage.Type)
            {
                switch (((ClientMessage)message).getType())
                {
                case CREATE_SIGNATURE:
                    if (serviceMessage.getAlgorithm() == Algorithm.ECDSA)
                    {
                        return ecdsaSignerEngine.handle(SignatureMessage.getInstance(ECDSASigningService.Type.values(), serviceMessage.getPayload()));
                    }
                    else
                    {
                        return blsSignerEngine.handle(SignatureMessage.getInstance(BLSSigningService.Type.values(), serviceMessage.getPayload()));
                    }
                default:
                    return new MessageReply(MessageReply.Type.ERROR, new DERUTF8String("Unknown client command in NodeSigningService."));
                }
            }
            else
            {
                switch (((CommandMessage)message).getType())
                {
                case SIGNATURE_MESSAGE:
                    if (serviceMessage.getAlgorithm() == Algorithm.ECDSA)
                    {
                        return ecdsaSignerEngine.handle(SignatureMessage.getInstance(ECDSASigningService.Type.values(), serviceMessage.getPayload()));
                    }
                    else
                    {
                        return blsSignerEngine.handle(SignatureMessage.getInstance(BLSSigningService.Type.values(), serviceMessage.getPayload()));
                    }
                default:
                    return new MessageReply(MessageReply.Type.ERROR, new DERUTF8String("Unknown command in NodeSigningService."));
                }
            }
        }
        catch (Exception e)
        {
            nodeContext.getEventNotifier().notify(EventNotifier.Level.ERROR, new DERUTF8String("NodeKeyGenerationService failure: " + e.getMessage()));

            return new MessageReply(MessageReply.Type.ERROR, new DERUTF8String("NodeKeyGenerationService failure: " + e.getMessage()));
        }
    }

    public boolean isAbleToHandle(Message message)
    {
        Enum type = message.getType();

        return type == ClientMessage.Type.CREATE_SIGNATURE || type == CommandMessage.Type.SIGNATURE_MESSAGE;
    }
}
