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
package org.cryptoworkshop.ximix.common.service;

import org.bouncycastle.asn1.ASN1Encodable;
import org.cryptoworkshop.ximix.common.message.Message;
import org.cryptoworkshop.ximix.common.message.MessageReply;

public interface ServicesConnection
{
    public MessageReply sendMessage(Message.Type type, ASN1Encodable messagePayload)
        throws ServiceConnectionException;

    public MessageReply sendThresholdMessage(Message.Type type, ASN1Encodable messagePayload)
        throws ServiceConnectionException;
}
